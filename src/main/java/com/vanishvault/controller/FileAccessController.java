package com.vanishvault.controller;

import com.vanishvault.model.FileMetadata;
import com.vanishvault.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileAccessController {

    private final S3Service s3Service;
    private final FileMetadataService fileMetadataService;
    private final NotificationService notificationService;
    private final AccessLogService accessLogService;
    private final EmailService emailService;
    private final EncryptionService encryptionService;
    private final BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/{fileId}/verify")
    public ResponseEntity<?> verifyAndAccess(
            @PathVariable String fileId,
            @RequestBody  Map<String, String> body,
            @RequestHeader(value = "X-Forwarded-For", defaultValue = "unknown") String ipAddress
    ) throws Exception {
        String password = body.get("password");
        Optional<FileMetadata> opt = fileMetadataService.findById(fileId);

        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "File not found or already deleted"));
        }

        FileMetadata meta = opt.get();

        // TTL expired
        if (Instant.now().getEpochSecond() > meta.getTtl()) {
            s3Service.delete(meta.getS3Key());
            fileMetadataService.delete(fileId);
            accessLogService.log(fileId, ipAddress, "EXPIRED", false);
            notificationService.notifyFileDeleted(meta.getSenderId(), fileId, "Link expired");
            if (!meta.getSenderEmail().isEmpty())
                emailService.sendFileDeletedAlert(meta.getSenderEmail(), fileId, "Link expired");
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("error", "This link has expired"));
        }

        // IP lock check
        if (!meta.getAllowedIp().isEmpty() && !meta.getAllowedIp().equals(ipAddress)) {
            accessLogService.log(fileId, ipAddress, "IP_BLOCKED", false);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied from your IP address"));
        }

        // Download limit check
        if (meta.getMaxDownloads() > 0 && meta.getDownloadCount() >= meta.getMaxDownloads()) {
            s3Service.delete(meta.getS3Key());
            fileMetadataService.delete(fileId);
            accessLogService.log(fileId, ipAddress, "LIMIT_REACHED", false);
            notificationService.notifyFileDeleted(meta.getSenderId(), fileId, "Download limit reached");
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("error", "Download limit reached. File deleted."));
        }

        // Wrong password
        if (!passwordEncoder.matches(password, meta.getHashedPassword())) {
            fileMetadataService.incrementFailCount(fileId);
            int newFail = meta.getFailCount() + 1;
            accessLogService.log(fileId, ipAddress, "WRONG_PASSWORD", false);
            notificationService.notifyWrongPassword(meta.getSenderId(), fileId, newFail);

            if (newFail >= 3) {
                s3Service.delete(meta.getS3Key());
                fileMetadataService.delete(fileId);
                notificationService.notifyFileDeleted(meta.getSenderId(), fileId, "Too many failed attempts");
                if (!meta.getSenderEmail().isEmpty())
                    emailService.sendFileDeletedAlert(meta.getSenderEmail(), fileId, "Too many failed attempts");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Too many failed attempts. File deleted."));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Wrong password", "attemptsLeft", 3 - newFail));
        }

        // ✅ Correct password — decrypt and serve file
        byte[] encryptedBytes = s3Service.downloadEncrypted(meta.getS3Key());
        SecretKey aesKey = encryptionService.decodeKey(meta.getEncryptionKey());
        byte[] decryptedBytes = encryptionService.decrypt(encryptedBytes, aesKey);

        fileMetadataService.incrementDownloadCount(fileId);
        accessLogService.log(fileId, ipAddress, "FILE_OPENED", true);
        notificationService.notifyFileOpened(meta.getSenderId(), fileId);
        if (!meta.getSenderEmail().isEmpty())
            emailService.sendFileOpenedAlert(meta.getSenderEmail(), fileId, ipAddress);

        // Burn after read — delete immediately after serving
        if (meta.isBurnAfterRead()) {
            s3Service.delete(meta.getS3Key());
            fileMetadataService.delete(fileId);
            notificationService.notifyFileDeleted(meta.getSenderId(), fileId, "Burned after read");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.getOriginalFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(decryptedBytes);
    }

    // Revoke link manually
    @DeleteMapping("/{fileId}/revoke")
    public ResponseEntity<?> revokeFile(@PathVariable String fileId) {
        Optional<FileMetadata> opt = fileMetadataService.findById(fileId);
        if (opt.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "File not found"));

        FileMetadata meta = opt.get();
        s3Service.delete(meta.getS3Key());
        fileMetadataService.delete(fileId);
        notificationService.notifyFileDeleted(meta.getSenderId(), fileId, "Manually revoked by sender");

        return ResponseEntity.ok(Map.of("message", "File revoked and deleted successfully"));
    }

    // Get access logs for a file
    @GetMapping("/{fileId}/logs")
    public ResponseEntity<?> getAccessLogs(@PathVariable String fileId) {
        return ResponseEntity.ok(accessLogService.getLogsForFile(fileId));
    }
}

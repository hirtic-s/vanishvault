package com.vanishvault.controller;

import com.vanishvault.model.FileMetadata;
import com.vanishvault.service.EncryptionService;
import com.vanishvault.service.FileMetadataService;
import com.vanishvault.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileUploadController {

    private final S3Service s3Service;
    private final FileMetadataService fileMetadataService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;

    @Value("${app.base-url}")
    private String baseUrl;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file")           MultipartFile file,
            @RequestParam("password")       String password,
            @RequestParam("senderId")       String senderId,
            @RequestParam(value = "senderEmail",    defaultValue = "")  String senderEmail,
            @RequestParam(value = "expiryMinutes",  defaultValue = "5") int expiryMinutes,
            @RequestParam(value = "maxDownloads",   defaultValue = "0") int maxDownloads,
            @RequestParam(value = "allowedIp",      defaultValue = "")  String allowedIp,
            @RequestParam(value = "burnAfterRead",  defaultValue = "false") boolean burnAfterRead
    ) throws Exception {
        String fileId = UUID.randomUUID().toString();
        String s3Key  = "uploads/" + fileId + "/" + file.getOriginalFilename();

        // Encrypt file before uploading to S3
        SecretKey aesKey = encryptionService.generateKey();
        byte[] encryptedBytes = encryptionService.encrypt(file.getBytes(), aesKey);

        s3Service.uploadEncrypted(encryptedBytes, s3Key, file.getContentType());

        fileMetadataService.save(FileMetadata.builder()
                .fileId(fileId)
                .s3Key(s3Key)
                .originalFileName(file.getOriginalFilename())
                .hashedPassword(passwordEncoder.encode(password))
                .senderId(senderId)
                .senderEmail(senderEmail)
                .failCount(0)
                .downloadCount(0)
                .maxDownloads(maxDownloads)
                .downloaded(false)
                .burnAfterRead(burnAfterRead)
                .allowedIp(allowedIp)
                .encryptionKey(encryptionService.encodeKey(aesKey))
                .ttl(Instant.now().plusSeconds((long) expiryMinutes * 60).getEpochSecond())
                .status("ACTIVE")
                .build());

        return ResponseEntity.ok(Map.of(
                "fileId",       fileId,
                "link",         baseUrl + "/file/" + fileId,
                "expiresIn",    expiryMinutes + " minutes",
                "maxDownloads", maxDownloads == 0 ? "unlimited" : maxDownloads,
                "encrypted",    true
        ));
    }
}

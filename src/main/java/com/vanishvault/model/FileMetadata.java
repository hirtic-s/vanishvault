package com.vanishvault.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {
    private String fileId;
    private String s3Key;
    private String originalFileName;
    private String hashedPassword;
    private String senderId;
    private int failCount;
    private long ttl;
    private String status;

    // Phase 1 new fields
    private boolean downloaded;        // one-time download
    private int maxDownloads;          // download limit (0 = unlimited)
    private int downloadCount;         // how many times downloaded
    private String allowedIp;          // IP lock (empty = any IP allowed)
    private boolean burnAfterRead;     // delete immediately after first view
    private String encryptionKey;      // AES key used to encrypt this file
    private String senderEmail;        // for AWS SES email alerts
}

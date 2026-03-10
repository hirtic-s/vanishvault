package com.vanishvault.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessLog {
    private String logId;       // UUID (partition key)
    private String fileId;      // which file
    private String ipAddress;   // who tried
    private String event;       // FILE_OPENED / WRONG_PASSWORD / EXPIRED / DELETED
    private String timestamp;   // ISO timestamp
    private boolean success;
}

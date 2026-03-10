package com.vanishvault.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyFileOpened(String senderId, String fileId) {
        messagingTemplate.convertAndSendToUser(senderId, "/queue/alerts", Map.of(
                "event",   "FILE_OPENED",
                "fileId",  fileId,
                "message", "✅ Your file was successfully opened!"
        ));
    }

    public void notifyWrongPassword(String senderId, String fileId, int failCount) {
        messagingTemplate.convertAndSendToUser(senderId, "/queue/alerts", Map.of(
                "event",     "WRONG_PASSWORD",
                "fileId",    fileId,
                "failCount", String.valueOf(failCount),
                "message",   "❌ Wrong password attempt #" + failCount
        ));
    }

    public void notifyFileDeleted(String senderId, String fileId, String reason) {
        messagingTemplate.convertAndSendToUser(senderId, "/queue/alerts", Map.of(
                "event",   "FILE_DELETED",
                "fileId",  fileId,
                "reason",  reason,
                "message", "🗑️ File deleted: " + reason
        ));
    }
}

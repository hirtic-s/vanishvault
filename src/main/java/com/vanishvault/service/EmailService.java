package com.vanishvault.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final SesClient sesClient;

    public void sendFileOpenedAlert(String toEmail, String fileId, String ipAddress) {
        try {
            sesClient.sendEmail(SendEmailRequest.builder()
                    .destination(Destination.builder().toAddresses(toEmail).build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data("✅ VanishVault — Your file was opened")
                                    .build())
                            .body(Body.builder()
                                    .text(Content.builder()
                                            .data("Your file (ID: " + fileId + ") was successfully opened from IP: "
                                                    + ipAddress + ". The file has been delivered.")
                                            .build())
                                    .build())
                            .build())
                    .source("noreply@yourdomain.com")
                    .build());
        } catch (Exception e) {
            // Email fails silently — don't break the main flow
            System.out.println("Email alert failed: " + e.getMessage());
        }
    }

    public void sendFileDeletedAlert(String toEmail, String fileId, String reason) {
        try {
            sesClient.sendEmail(SendEmailRequest.builder()
                    .destination(Destination.builder().toAddresses(toEmail).build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data("🗑️ VanishVault — Your file was deleted")
                                    .build())
                            .body(Body.builder()
                                    .text(Content.builder()
                                            .data("Your file (ID: " + fileId + ") was deleted. Reason: " + reason)
                                            .build())
                                    .build())
                            .build())
                    .source("noreply@yourdomain.com")
                    .build());
        } catch (Exception e) {
            System.out.println("Email alert failed: " + e.getMessage());
        }
    }
}

package com.vanishvault.service;

import com.vanishvault.model.FileMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileMetadataService {

    private final DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.table-name}")
    private String tableName;

    public void save(FileMetadata metadata) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("fileId",           AttributeValue.fromS(metadata.getFileId()));
        item.put("s3Key",            AttributeValue.fromS(metadata.getS3Key()));
        item.put("originalFileName", AttributeValue.fromS(metadata.getOriginalFileName()));
        item.put("hashedPassword",   AttributeValue.fromS(metadata.getHashedPassword()));
        item.put("senderId",         AttributeValue.fromS(metadata.getSenderId()));
        item.put("senderEmail",      AttributeValue.fromS(metadata.getSenderEmail() != null ? metadata.getSenderEmail() : ""));
        item.put("failCount",        AttributeValue.fromN("0"));
        item.put("downloadCount",    AttributeValue.fromN("0"));
        item.put("maxDownloads",     AttributeValue.fromN(String.valueOf(metadata.getMaxDownloads())));
        item.put("downloaded",       AttributeValue.fromBool(false));
        item.put("burnAfterRead",    AttributeValue.fromBool(metadata.isBurnAfterRead()));
        item.put("allowedIp",        AttributeValue.fromS(metadata.getAllowedIp() != null ? metadata.getAllowedIp() : ""));
        item.put("encryptionKey",    AttributeValue.fromS(metadata.getEncryptionKey() != null ? metadata.getEncryptionKey() : ""));
        item.put("ttl",              AttributeValue.fromN(String.valueOf(metadata.getTtl())));
        item.put("status",           AttributeValue.fromS("ACTIVE"));

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());
    }

    public Optional<FileMetadata> findById(String fileId) {
        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("fileId", AttributeValue.fromS(fileId)))
                .build());

        if (!response.hasItem()) return Optional.empty();

        Map<String, AttributeValue> item = response.item();
        return Optional.of(FileMetadata.builder()
                .fileId(item.get("fileId").s())
                .s3Key(item.get("s3Key").s())
                .originalFileName(item.get("originalFileName").s())
                .hashedPassword(item.get("hashedPassword").s())
                .senderId(item.get("senderId").s())
                .senderEmail(item.getOrDefault("senderEmail", AttributeValue.fromS("")).s())
                .failCount(Integer.parseInt(item.get("failCount").n()))
                .downloadCount(Integer.parseInt(item.getOrDefault("downloadCount", AttributeValue.fromN("0")).n()))
                .maxDownloads(Integer.parseInt(item.getOrDefault("maxDownloads", AttributeValue.fromN("0")).n()))
                .downloaded(item.getOrDefault("downloaded", AttributeValue.fromBool(false)).bool())
                .burnAfterRead(item.getOrDefault("burnAfterRead", AttributeValue.fromBool(false)).bool())
                .allowedIp(item.getOrDefault("allowedIp", AttributeValue.fromS("")).s())
                .encryptionKey(item.getOrDefault("encryptionKey", AttributeValue.fromS("")).s())
                .ttl(Long.parseLong(item.get("ttl").n()))
                .status(item.get("status").s())
                .build());
    }

    public void incrementFailCount(String fileId) {
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("fileId", AttributeValue.fromS(fileId)))
                .updateExpression("SET failCount = failCount + :inc")
                .expressionAttributeValues(Map.of(":inc", AttributeValue.fromN("1")))
                .build());
    }

    public void incrementDownloadCount(String fileId) {
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("fileId", AttributeValue.fromS(fileId)))
                .updateExpression("SET downloadCount = downloadCount + :inc, downloaded = :true")
                .expressionAttributeValues(Map.of(
                        ":inc",  AttributeValue.fromN("1"),
                        ":true", AttributeValue.fromBool(true)
                ))
                .build());
    }

    public void delete(String fileId) {
        dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("fileId", AttributeValue.fromS(fileId)))
                .build());
    }
}

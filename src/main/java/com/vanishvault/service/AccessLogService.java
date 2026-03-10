package com.vanishvault.service;

import com.vanishvault.model.AccessLog;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccessLogService {

    private final DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.logs-table-name}")
    private String logsTableName;

    public void log(String fileId, String ipAddress, String event, boolean success) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("logId",     AttributeValue.fromS(UUID.randomUUID().toString()));
        item.put("fileId",    AttributeValue.fromS(fileId));
        item.put("ipAddress", AttributeValue.fromS(ipAddress));
        item.put("event",     AttributeValue.fromS(event));
        item.put("timestamp", AttributeValue.fromS(Instant.now().toString()));
        item.put("success",   AttributeValue.fromBool(success));

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(logsTableName)
                .item(item)
                .build());
    }

    public List<AccessLog> getLogsForFile(String fileId) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(logsTableName)
                .indexName("fileId-index")
                .keyConditionExpression("fileId = :fid")
                .expressionAttributeValues(Map.of(
                        ":fid", AttributeValue.fromS(fileId)
                ))
                .build());

        return response.items().stream().map(item -> AccessLog.builder()
                .logId(item.get("logId").s())
                .fileId(item.get("fileId").s())
                .ipAddress(item.get("ipAddress").s())
                .event(item.get("event").s())
                .timestamp(item.get("timestamp").s())
                .success(item.get("success").bool())
                .build()
        ).collect(Collectors.toList());
    }
}

package com.utam.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Service
public class MinioService {

    private static final Logger logger = LoggerFactory.getLogger(MinioService.class);
    private final MinioClient minioClient;

    @Value("${minio.bucket.raw-data:tam-raw-data}")
    private String rawDataBucket;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Upload a JSON string to MinIO.
     */
    public void uploadJson(String objectName, String jsonContent) {
        try {
            byte[] contentBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(contentBytes);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(rawDataBucket)
                            .object(objectName)
                            .stream(inputStream, contentBytes.length, -1)
                            .contentType("application/json")
                            .build()
            );
            logger.debug("Uploaded {} to MinIO bucket {}", objectName, rawDataBucket);
        } catch (Exception e) {
            logger.error("Error uploading to MinIO: {}", e.getMessage());
        }
    }
}

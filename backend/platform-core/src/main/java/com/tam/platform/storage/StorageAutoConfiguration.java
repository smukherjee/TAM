package com.tam.platform.storage;

import com.tam.platform.observability.MinioHealthIndicator;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class StorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "platform.storage.minio.url")
    public MinioClient minioClient(
            @Value("${platform.storage.minio.url}") String url,
            @Value("${platform.storage.minio.access-key}") String accessKey,
            @Value("${platform.storage.minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "platform.storage.minio.url")
    public ObjectStorageService objectStorageService(MinioClient minioClient) {
        return new MinioStorageService(minioClient);
    }

    @Bean
    @ConditionalOnMissingBean(name = "minioHealthIndicator")
    @ConditionalOnProperty(name = "platform.storage.minio.url")
    public HealthIndicator minioHealthIndicator(MinioClient minioClient) {
        return new MinioHealthIndicator(minioClient);
    }
}

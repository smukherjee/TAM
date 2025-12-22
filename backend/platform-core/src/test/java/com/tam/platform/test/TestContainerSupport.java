package com.tam.platform.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class TestContainerSupport {

    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine")).withExposedPorts(6379);
    static final GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse("minio/minio"))
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server /data");

    static {
        kafka.start();
        postgres.start();
        redis.start();
        minio.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("platform.storage.minio.url", () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        registry.add("platform.storage.minio.access-key", () -> "minioadmin");
        registry.add("platform.storage.minio.secret-key", () -> "minioadmin");
    }
}

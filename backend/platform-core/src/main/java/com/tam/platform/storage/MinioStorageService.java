package com.tam.platform.storage;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MinioStorageService implements ObjectStorageService {

    private final MinioClient minioClient;

    @Override
    @SneakyThrows
    public void upload(String bucket, String key, InputStream data, long size, String contentType) {
        // MinIO uploads disabled: previously invoked minioClient.putObject(...)
        // Commenting out to stop writes to object storage.
        // minioClient.putObject(
        //         PutObjectArgs.builder()
        //                 .bucket(bucket)
        //                 .object(key)
        //                 .stream(data, size, -1)
        //                 .contentType(contentType)
        //                 .build()
        // );
    }

    @Override
    @SneakyThrows
    public InputStream download(String bucket, String key) {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build()
        );
    }

    @Override
    @SneakyThrows
    public String getPresignedUrl(String bucket, String key, Duration expiry) {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(key)
                        .expiry((int) expiry.getSeconds(), TimeUnit.SECONDS)
                        .build()
        );
    }

    @Override
    @SneakyThrows
    public void delete(String bucket, String key) {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build()
        );
    }
}

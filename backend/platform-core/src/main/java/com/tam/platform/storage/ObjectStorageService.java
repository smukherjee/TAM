package com.tam.platform.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * Interface for object storage operations.
 */
public interface ObjectStorageService {

    void upload(String bucket, String key, InputStream data, long size, String contentType);

    InputStream download(String bucket, String key);

    String getPresignedUrl(String bucket, String key, Duration expiry);

    void delete(String bucket, String key);
}

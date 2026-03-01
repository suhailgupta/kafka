package com.example.analyticsservice.s3;

import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.Result;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class S3AnalyticsService {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * List object keys in the orders-events bucket (data written by S3 sink connector).
     */
    public List<String> listObjectKeys() {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucket).recursive(true).build());
        return StreamSupport.stream(results.spliterator(), false)
                .map(r -> {
                    try {
                        return r.get().objectName();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Read content of an S3 object as text (JSON lines from S3 sink).
     */
    public String getObjectContent(String objectKey) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
            return new Scanner(stream, StandardCharsets.UTF_8).useDelimiter("\\A").next();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read object: " + objectKey, e);
        }
    }

    /**
     * Return summary: count of objects and sample of keys.
     */
    public S3Summary getSummary() {
        List<String> keys = listObjectKeys();
        List<String> sample = keys.isEmpty() ? List.of() : keys.stream().limit(20).toList();
        return new S3Summary(keys.size(), sample);
    }

    public record S3Summary(long totalObjects, List<String> sampleKeys) {}
}

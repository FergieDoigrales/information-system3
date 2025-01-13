package com.fergie.lab1.services;

import io.minio.*;
import io.minio.http.Method;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
public class StorageService {

    private final MinioClient minioClient;

    public StorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public void uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType) {
        String tempBucket = bucketName + "-temp";
        String tempObjectName = "temp-" + objectName;

        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            boolean tempBucketFound = minioClient.bucketExists(BucketExistsArgs.builder().bucket(tempBucket).build());
            if (!tempBucketFound) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(tempBucket).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder().bucket(tempBucket).object(tempObjectName).stream(
                                    inputStream, inputStream.available(), -1)
                            .contentType(contentType)
                            .build());

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .source(CopySource.builder()
                                    .bucket(tempBucket)
                                    .object(tempObjectName)
                                    .build())
                            .bucket(bucketName)
                            .object(objectName)
                            .build());

            minioClient.removeObject(RemoveObjectArgs.builder().bucket(tempBucket).object(tempObjectName).build());

        } catch (Exception e) {
            throw new RuntimeException("Error during two-phase commit occurred: " + e.getMessage());
        }
    }

    public String generateUrl(String bucketName, String objectName) {
        try {
            GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .method(Method.GET)
                    .expiry(1, TimeUnit.HOURS) //?? ??????
                    .build();

            return minioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);
        } catch (Exception e) {
            throw new RuntimeException("Error generating URL", e);
        }
    }

}

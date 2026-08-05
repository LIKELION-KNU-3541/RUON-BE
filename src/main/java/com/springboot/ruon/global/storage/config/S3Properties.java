package com.springboot.ruon.global.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(String region, String bucket) {

    public S3Properties {
        requireText(region, "aws.s3.region");
        requireText(bucket, "aws.s3.bucket");
    }

    private static void requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(propertyName + " 설정이 비어 있습니다.");
        }
    }
}

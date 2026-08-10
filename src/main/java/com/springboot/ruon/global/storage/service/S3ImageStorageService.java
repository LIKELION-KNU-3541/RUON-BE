package com.springboot.ruon.global.storage.service;

import com.springboot.ruon.global.config.S3Properties;
import com.springboot.ruon.global.exception.ErrorCode;
import com.springboot.ruon.global.exception.Image.ImageStorageException;
import com.springboot.ruon.global.storage.key.ImageObjectKeyGenerator;
import com.springboot.ruon.global.storage.validation.ImageFileValidator;
import com.springboot.ruon.global.storage.validation.ImageFormat;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3 기반 {@link ImageStorageService} 구현체.
 * AWS SDK 예외 변환을 이 클래스에 모아, 애플리케이션의 다른 부분이
 * SDK 예외 타입에 의존하지 않도록 한다.
 */
@Service
@RequiredArgsConstructor
public class S3ImageStorageService implements ImageStorageService {

    private final S3Client s3Client;
    private final S3Properties properties;
    private final ImageFileValidator validator;
    private final ImageObjectKeyGenerator keyGenerator;

    @Override
    public String upload(Long userId, MultipartFile file) {
        ImageFormat format = validator.validate(file);
        return putObject(userId, readBytes(file), format);
    }

    @Override
    public String upload(Long userId, byte[] image, String contentType) {
        ImageFormat format = validator.validate(image, contentType);
        return putObject(userId, image, format);
    }

    //objectKey의 확장자와 Content-Type은 검증을 통과한 ImageFormat에서만 나온다.
    private String putObject(Long userId, byte[] bytes, ImageFormat format) {
        String objectKey = keyGenerator.generate(userId, format);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(format.contentType())
                .contentLength((long) bytes.length)
                .build();
        try {
            s3Client.putObject(request, RequestBody.fromBytes(bytes));
        } catch (SdkException e) {
            throw new ImageStorageException(ErrorCode.INTERNAL_ERROR, "S3 이미지 업로드에 실패했습니다: " + objectKey, e);
        }
        return objectKey;
    }

    @Override
    public byte[] download(String objectKey) {
        requireObjectKey(objectKey);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();
        try {
            return s3Client.getObjectAsBytes(request).asByteArray();
        } catch (NoSuchKeyException e) {
            throw new ImageStorageException(ErrorCode.NOT_FOUND, "이미지를 찾을 수 없습니다: " + objectKey, e);
        } catch (SdkException e) {
            throw new ImageStorageException(ErrorCode.INTERNAL_ERROR, "S3 이미지 조회에 실패했습니다: " + objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        requireObjectKey(objectKey);
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();
        try {
            s3Client.deleteObject(request);
        } catch (SdkException e) {
            throw new ImageStorageException(ErrorCode.INTERNAL_ERROR, "S3 이미지 삭제에 실패했습니다: " + objectKey, e);
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ImageStorageException(ErrorCode.INTERNAL_ERROR, "업로드 파일을 읽는 중 오류가 발생했습니다.", e);
        }
    }

    private void requireObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey must not be blank");
        }
    }
}

package com.springboot.ruon.global.storage.validation;

import com.springboot.ruon.global.exception.ErrorCode;
import com.springboot.ruon.global.exception.Image.ImageStorageException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageFileValidator {

    // 7MB: 이후 Google Vision REST 요청에서 Base64 인코딩 시
    // 데이터가 약 33% 커지는 점을 고려한 한도.
    static final long MAX_FILE_SIZE_BYTES = 7 * 1024 * 1024;


    public ImageFormat validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageStorageException(ErrorCode.INVALID_IMAGE, "업로드된 이미지 파일이 비어 있습니다.");
        }
        ImageFormat format = validate(file.getSize(), file.getContentType(), readHeader(file));
        //파일명이 있는 업로드에만 확장자를 검사한다.
        if (!format.supportsExtension(extractExtension(file.getOriginalFilename()))) {
            throw new ImageStorageException(ErrorCode.INVALID_IMAGE,
                    "파일 확장자가 Content-Type과 일치하지 않습니다: " + file.getOriginalFilename());
        }
        return format;
    }

    //카카오 API를 통해서 가져온 이미지 검증 로직 분리
    public ImageFormat validate(byte[] image, String contentType) {
        if (image == null || image.length == 0) {
            throw new ImageStorageException(ErrorCode.INVALID_IMAGE, "이미지 데이터가 비어 있습니다.");
        }
        return validate(image.length, contentType, header(image));
    }

    //출처와 무관하게 공통으로 거치는 검증 로직
    private ImageFormat validate(long size, String contentType, byte[] header) {
        if (size > MAX_FILE_SIZE_BYTES) {
            throw new ImageStorageException(ErrorCode.INVALID_IMAGE,
                    "이미지 크기는 7MB를 초과할 수 없습니다. (현재: " + size + " bytes)");
        }
        ImageFormat format = ImageFormat.fromContentType(contentType)
                .orElseThrow(() -> new ImageStorageException(ErrorCode.INVALID_IMAGE,
                        "지원하지 않는 이미지 형식입니다. (JPEG, PNG, WEBP만 허용): " + contentType));
        if (!format.matchesSignature(header)) {
            throw new ImageStorageException(ErrorCode.INVALID_IMAGE,
                    "파일 내용이 선언된 이미지 형식과 일치하지 않습니다: " + contentType);
        }
        return format;
    }

    private byte[] header(byte[] image) {
        return Arrays.copyOf(image, Math.min(image.length, ImageFormat.SIGNATURE_HEADER_LENGTH));
    }

    private byte[] readHeader(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(ImageFormat.SIGNATURE_HEADER_LENGTH);
        } catch (IOException e) {
            throw new ImageStorageException(ErrorCode.INVALID_IMAGE, "업로드 파일을 읽는 중 오류가 발생했습니다.");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(dotIndex + 1);
    }
}

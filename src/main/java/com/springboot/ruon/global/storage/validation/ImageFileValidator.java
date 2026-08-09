package com.springboot.ruon.global.storage.validation;

import com.springboot.ruon.global.exception.ErrorCode;
import com.springboot.ruon.global.exception.Image.ImageStorageException;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageFileValidator {

    // 7MB: 이후 Google Vision REST 요청에서 Base64 인코딩 시
    // 데이터가 약 33% 커지는 점을 고려한 한도.
    static final long MAX_FILE_SIZE_BYTES = 7 * 1024 * 1024;

    /**
     * 업로드된 파일을 검증하고 신뢰할 수 있는 {@link ImageFormat}을 결정한다.
     * Content-Type과 확장자는 클라이언트가 조작할 수 있으므로,
     * 마지막에 파일 선두 바이트(매직 바이트)로 실제 형식을 확인한다.
     *
     * @throws ImageStorageException 파일이 비었거나, 크기를 초과했거나,
     *         지원하지 않는 Content-Type이거나, 확장자가 일치하지 않거나,
     *         실제 파일 내용이 선언된 형식과 다른 경우
     */
    public ImageFormat validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageStorageException(ErrorCode.INVALID_IMAGE, "업로드된 이미지 파일이 비어 있습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ImageStorageException(ErrorCode.INVALID_IMAGE,
                    "이미지 크기는 7MB를 초과할 수 없습니다. (현재: " + file.getSize() + " bytes)");
        }
        ImageFormat format = ImageFormat.fromContentType(file.getContentType())
                .orElseThrow(() -> new ImageStorageException(ErrorCode.INVALID_IMAGE,
                        "지원하지 않는 이미지 형식입니다. (JPEG, PNG, WEBP만 허용): " + file.getContentType()));
        if (!format.supportsExtension(extractExtension(file.getOriginalFilename()))) {
            throw new ImageStorageException(ErrorCode.INVALID_IMAGE,
                    "파일 확장자가 Content-Type과 일치하지 않습니다: " + file.getOriginalFilename());
        }
        if (!format.matchesSignature(readHeader(file))) {
            throw new ImageStorageException(ErrorCode.INVALID_IMAGE,
                    "파일 내용이 선언된 이미지 형식과 일치하지 않습니다: " + file.getContentType());
        }
        return format;
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

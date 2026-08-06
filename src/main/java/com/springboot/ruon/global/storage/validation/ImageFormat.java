package com.springboot.ruon.global.storage.validation;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 스캔 업로드에 허용되는 이미지 형식 화이트리스트.
 * 저장에 사용되는 확장자는 항상 이 enum에서만 나오고 사용자 입력은 쓰지 않으므로,
 * 파일명을 통한 objectKey 조작이 불가능하다.
 */
public enum ImageFormat {

    JPEG("image/jpeg", "jpg", Set.of("jpg", "jpeg")),
    PNG("image/png", "png", Set.of("png")),
    WEBP("image/webp", "webp", Set.of("webp"));

    private final String contentType;
    private final String extension;
    private final Set<String> allowedExtensions;

    ImageFormat(String contentType, String extension, Set<String> allowedExtensions) {
        this.contentType = contentType;
        this.extension = extension;
        this.allowedExtensions = allowedExtensions;
    }

    /** 매직 바이트 판별에 필요한 파일 선두 바이트 수 (WEBP가 12바이트로 가장 김).
     * JPEG, PNG, WEBP
     * */
    public static final int SIGNATURE_HEADER_LENGTH = 12;

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE =
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] WEBP_RIFF_SIGNATURE = {0x52, 0x49, 0x46, 0x46}; // "RIFF"
    private static final byte[] WEBP_FORMAT_SIGNATURE = {0x57, 0x45, 0x42, 0x50}; // "WEBP"

    public static Optional<ImageFormat> fromContentType(String contentType) {
        if (contentType == null) {
            return Optional.empty();
        }
        // "image/jpeg; charset=utf-8"처럼 파라미터가 붙어 와도 미디어 타입만 비교하도록 처리.
        String normalized = contentType.toLowerCase(Locale.ROOT);
        int semicolonIndex = normalized.indexOf(';');
        if (semicolonIndex >= 0) {
            normalized = normalized.substring(0, semicolonIndex);
        }
        String mediaType = normalized.strip();
        return Arrays.stream(values())
                .filter(format -> format.contentType.equals(mediaType))
                .findFirst();
    }

    public boolean supportsExtension(String candidate) {
        return candidate != null && allowedExtensions.contains(candidate.toLowerCase(Locale.ROOT));
    }

    /**
     * 파일 선두 바이트가 이 형식의 매직 바이트와 일치하는지 검사한다.
     * Content-Type과 확장자는 클라이언트가 조작할 수 있으므로,
     * 실제 파일 내용 기반의 이 검사가 최종 판단 기준이다.
     */
    public boolean matchesSignature(byte[] header) {
        if (header == null) {
            return false;
        }
        return switch (this) {
            case JPEG -> startsWith(header, 0, JPEG_SIGNATURE);
            case PNG -> startsWith(header, 0, PNG_SIGNATURE);
            case WEBP -> startsWith(header, 0, WEBP_RIFF_SIGNATURE)
                    && startsWith(header, 8, WEBP_FORMAT_SIGNATURE);
        };
    }

    private static boolean startsWith(byte[] header, int offset, byte[] signature) {
        if (header.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[offset + i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }
}

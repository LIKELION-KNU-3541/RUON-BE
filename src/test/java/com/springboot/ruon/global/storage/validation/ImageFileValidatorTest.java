package com.springboot.ruon.global.storage.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.springboot.ruon.global.exception.ErrorCode;
import com.springboot.ruon.global.exception.Image.ImageStorageException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;

class ImageFileValidatorTest {

    private final ImageFileValidator validator = new ImageFileValidator();

    /** 형식별 유효한 매직 바이트로 시작하는 테스트 데이터를 만든다. */
    static byte[] validBytes(ImageFormat format) {
        return switch (format) {
            case JPEG -> new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
            case PNG -> new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
            case WEBP -> new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        };
    }

    @ParameterizedTest
    @CsvSource({
            "photo.jpg, image/jpeg, JPEG",
            "photo.jpeg, image/jpeg, JPEG",
            "photo.PNG, image/png, PNG",
            "photo.webp, image/webp, WEBP"
    })
    @DisplayName("허용된 형식의 정상 이미지는 검증을 통과하고 ImageFormat을 반환한다")
    void validImagePassesValidation(String filename, String contentType, ImageFormat expected) {
        MockMultipartFile file = new MockMultipartFile("image", filename, contentType, validBytes(expected));

        assertThat(validator.validate(file)).isEqualTo(expected);
    }

    @Test
    @DisplayName("파라미터가 붙은 Content-Type(image/jpeg; charset=utf-8)도 정상 인식한다")
    void contentTypeWithParameterIsAccepted() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg; charset=utf-8", validBytes(ImageFormat.JPEG));

        assertThat(validator.validate(file)).isEqualTo(ImageFormat.JPEG);
    }

    @Test
    @DisplayName("빈 파일은 거부한다")
    void emptyFileIsRejected() {
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ImageStorageException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_IMAGE);
    }

    @Test
    @DisplayName("null 파일은 거부한다")
    void nullFileIsRejected() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(ImageStorageException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_IMAGE);
    }

    @Test
    @DisplayName("허용되지 않은 Content-Type은 거부한다")
    void unsupportedContentTypeIsRejected() {
        MockMultipartFile file = new MockMultipartFile("image", "doc.gif", "image/gif", new byte[]{1});

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ImageStorageException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_IMAGE);
    }

    @Test
    @DisplayName("Content-Type과 확장자가 일치하지 않으면 거부한다")
    void mismatchedExtensionIsRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "photo.png", "image/jpeg", validBytes(ImageFormat.JPEG));

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ImageStorageException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_IMAGE);
    }

    @Test
    @DisplayName("확장자가 없는 파일명은 거부한다")
    void missingExtensionIsRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "photo", "image/jpeg", validBytes(ImageFormat.JPEG));

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ImageStorageException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_IMAGE);
    }

    @Test
    @DisplayName("Content-Type과 확장자를 이미지로 위장한 비이미지 파일은 매직 바이트 검사에서 거부한다")
    void spoofedNonImageContentIsRejected() {
        byte[] htmlBytes = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", htmlBytes);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ImageStorageException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_IMAGE);
    }

    @Test
    @DisplayName("실제 내용이 다른 이미지 형식이면(PNG 바이트를 JPEG로 선언) 거부한다")
    void wrongFormatBytesAreRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", validBytes(ImageFormat.PNG));

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ImageStorageException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_IMAGE);
    }

    @Test
    @DisplayName("7MB를 초과하는 파일은 거부한다")
    void oversizedFileIsRejected() {
        byte[] oversized = new byte[(int) ImageFileValidator.MAX_FILE_SIZE_BYTES + 1];
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", oversized);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ImageStorageException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_IMAGE);
    }

    @Test
    @DisplayName("정확히 7MB인 파일은 허용한다")
    void exactLimitFileIsAccepted() {
        byte[] atLimit = new byte[(int) ImageFileValidator.MAX_FILE_SIZE_BYTES];
        byte[] jpegHeader = validBytes(ImageFormat.JPEG);
        System.arraycopy(jpegHeader, 0, atLimit, 0, jpegHeader.length);
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", atLimit);

        assertThat(validator.validate(file)).isEqualTo(ImageFormat.JPEG);
    }
}

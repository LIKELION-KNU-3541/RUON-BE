package com.springboot.ruon.global.storage.key;

import com.springboot.ruon.global.storage.validation.ImageFormat;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * {@code scans/{userId}/{uuid}.{extension}} 형식의 S3 objectKey를 생성한다.
 * 숫자 userId와 화이트리스트 확장자만 사용하므로
 * 외부 입력이 키 경로에 영향을 줄 수 없다.
 */
@Component
public class ImageObjectKeyGenerator {

    private static final String KEY_FORMAT = "scans/%d/%s.%s";

    public String generate(Long userId, ImageFormat format) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return KEY_FORMAT.formatted(userId, UUID.randomUUID(), format.extension());
    }
}

package com.springboot.ruon.global.storage.key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.springboot.ruon.global.storage.validation.ImageFormat;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageObjectKeyGeneratorTest {

    private static final String KEY_PATTERN =
            "scans/\\d+/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)";

    private final ImageObjectKeyGenerator generator = new ImageObjectKeyGenerator();

    @Test
    @DisplayName("objectKey는 scans/{userId}/{uuid}.{extension} 형식이다")
    void keyMatchesExpectedFormat() {
        String key = generator.generate(1L, ImageFormat.JPEG);

        assertThat(key).matches(KEY_PATTERN);
        assertThat(key).startsWith("scans/1/");
        assertThat(key).endsWith(".jpg");
    }

    @Test
    @DisplayName("UUID 덕분에 같은 사용자·형식이라도 키가 중복되지 않는다")
    void generatedKeysAreUnique() {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            keys.add(generator.generate(1L, ImageFormat.PNG));
        }

        assertThat(keys).hasSize(1000);
    }

    @Test
    @DisplayName("userId가 null이면 예외가 발생한다")
    void nullUserIdIsRejected() {
        assertThatThrownBy(() -> generator.generate(null, ImageFormat.JPEG))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

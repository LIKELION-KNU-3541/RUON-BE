package com.springboot.ruon.global.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.ruon.global.config.S3Properties;
import com.springboot.ruon.global.storage.key.ImageObjectKeyGenerator;
import com.springboot.ruon.global.storage.service.ImageStorageService;
import com.springboot.ruon.global.storage.service.S3ImageStorageService;
import com.springboot.ruon.global.storage.validation.ImageFileValidator;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * 실제 AWS S3 버킷을 대상으로 하는 수동 스모크 테스트.
 * <p>
 * Mock 기반 단위 테스트로는 확인할 수 없는 실제 연결·IAM 권한·버킷 설정을
 * PR 전에 한 번 확인하기 위한 용도이며, 업로드한 테스트 객체는 finally에서 반드시 삭제한다.
 * <p>
 * {@code @SpringBootTest}를 사용하지 않으므로 프로젝트의 DB 설정과 무관하게 실행된다.
 * 일반 {@code ./gradlew test}에서는 {@code integration} 태그로 제외되며,
 * {@code AWS_S3_BUCKET} 환경변수가 설정된 경우에만 실행된다.
 * <p>
 * 실행: {@code AWS_S3_BUCKET=... ./gradlew integrationTest}
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "AWS_S3_BUCKET", matches = ".+")
class S3ImageStorageSmokeTest {

    private static final String DEFAULT_REGION = "ap-northeast-2";

    /** 스모크 테스트 전용 사용자 식별자 (실제 사용자 데이터와 섞이지 않도록 0을 사용). */
    private static final Long SMOKE_TEST_USER_ID = 0L;

    @Test
    @DisplayName("실제 S3 버킷에 업로드 → 조회 → 삭제가 정상 동작한다")
    void uploadDownloadDeleteAgainstRealBucket() throws IOException {
        // 자격 증명은 AWS 기본 자격 증명 체인(환경변수 / IAM Role 등)에서 해석되며
        // 코드나 테스트 리소스에 어떤 인증 정보도 두지 않는다.
        String region = envOrDefault("AWS_REGION", DEFAULT_REGION);
        String bucket = System.getenv("AWS_S3_BUCKET");

        S3Client s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();

        try {
            ImageStorageService storageService = new S3ImageStorageService(
                    s3Client,
                    S3Presigner.builder()
                            .region(Region.of(region))
                            .credentialsProvider(DefaultCredentialsProvider.builder().build())
                            .build(),
                    new S3Properties(region, bucket),
                    new ImageFileValidator(),
                    new ImageObjectKeyGenerator());

            byte[] originalBytes = createPngBytes();
            MultipartFile file = new MockMultipartFile(
                    "image", "s3-smoke-test.png", "image/png", originalBytes);

            String objectKey = null;
            try {
                // objectKey는 UUID 기반으로 생성되므로 실행할 때마다 고유하다.
                objectKey = storageService.upload(SMOKE_TEST_USER_ID, file);
                assertThat(objectKey)
                        .startsWith("scans/" + SMOKE_TEST_USER_ID + "/")
                        .endsWith(".png");

                byte[] downloadedBytes = storageService.download(objectKey);
                assertThat(downloadedBytes).isEqualTo(originalBytes);
            } finally {
                // 테스트 객체가 버킷에 남지 않도록 반드시 삭제한다.
                if (objectKey != null) {
                    storageService.delete(objectKey);
                }
            }
        } finally {
            s3Client.close();
        }
    }

    /** 검증을 통과하는 작은 정상 PNG 이미지를 생성한다. */
    private byte[] createPngBytes() throws IOException {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}

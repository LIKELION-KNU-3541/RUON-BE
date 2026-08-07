package com.springboot.ruon.global.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.ruon.global.config.VisionConfig;
import com.springboot.ruon.global.config.VisionProperties;
import com.springboot.ruon.global.ocr.service.GoogleVisionOcrService;
import com.springboot.ruon.global.ocr.service.OcrService;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

/**
 * 실제 Google Vision API를 호출하는 수동 스모크 테스트.
 * <p>
 * Mock 기반 단위 테스트로는 확인할 수 없는 것 — API 키 유효성, 엔드포인트, 실제 응답 구조가
 * 우리 DTO와 맞는지 — 를 PR 전에 한 번 확인하기 위한 용도다.
 * <p>
 * {@code @SpringBootTest}를 사용하지 않으므로 프로젝트의 DB 설정과 무관하게 실행된다.
 * 일반 {@code ./gradlew test}에서는 {@code integration} 태그로 제외되며,
 * {@code GOOGLE_VISION_API_KEY} 환경변수가 설정된 경우에만 실행된다.
 * <p>
 * 실행: {@code GOOGLE_VISION_API_KEY=... ./gradlew integrationTest}
 * <p>
 * 실제 호출이므로 과금 대상이다. (월 무료 할당량 내에서는 비용이 발생하지 않는다.)
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "GOOGLE_VISION_API_KEY", matches = ".+")
class GoogleVisionOcrSmokeTest {

    /**
     * 이미지에 그려 넣을 문구.
     * <p>
     * 한글은 실행 환경에 해당 글꼴이 없으면 네모로 렌더링되어 테스트가 환경을 타므로,
     * 연결 확인 목적에는 ASCII를 쓴다. 한글 인식 품질은 실제 성분표 사진으로 별도 확인해야 한다.
     */
    private static final String EXPECTED_TEXT = "RUON OCR TEST 1234";

    @Test
    @DisplayName("실제 Vision API가 이미지에서 텍스트를 추출한다")
    void extractTextAgainstRealVisionApi() throws IOException {
        // 키는 환경변수에서만 읽으며 코드나 테스트 리소스에 어떤 인증 정보도 두지 않는다.
        VisionProperties properties = new VisionProperties(
                System.getenv("GOOGLE_VISION_API_KEY"), null, null, null);

        // 운영에서 쓰는 설정 코드를 그대로 사용해 헤더 인증 경로까지 함께 검증한다.
        RestClient visionRestClient = new VisionConfig().visionRestClient(properties);
        OcrService ocrService = new GoogleVisionOcrService(visionRestClient);

        byte[] imageBytes = createTextImageBytes(EXPECTED_TEXT);
        String extracted = ocrService.extractText(imageBytes);

        System.out.println("[smoke] Vision이 추출한 텍스트:\n" + extracted);

        assertThat(normalize(extracted))
                .as("추출 결과: %s", extracted)
                .contains(normalize(EXPECTED_TEXT));
    }

    /** 흰 배경에 검은 글자를 그린 PNG를 만든다. 파일을 저장소에 두지 않기 위해 코드로 생성한다. */
    private byte[] createTextImageBytes(String text) throws IOException {
        System.setProperty("java.awt.headless", "true");

        BufferedImage image = new BufferedImage(900, 220, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 64));
            graphics.drawString(text, 40, 130);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /** OCR 결과에는 줄바꿈·공백이 섞이므로 비교 전에 걷어낸다. */
    private String normalize(String text) {
        return text.replaceAll("\\s+", "").toUpperCase();
    }
}

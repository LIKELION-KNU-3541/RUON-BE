package com.springboot.ruon.global.ocr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.springboot.ruon.global.exception.ErrorCode;
import com.springboot.ruon.global.exception.Ocr.OcrException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoogleVisionOcrServiceTest {

    private static final String ENDPOINT = "https://vision.googleapis.com/v1/images:annotate";
    private static final String API_KEY = "test-api-key";
    private static final byte[] IMAGE = "fake-image-bytes".getBytes(StandardCharsets.UTF_8);

    private MockRestServiceServer server;
    private GoogleVisionOcrService ocrService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient visionRestClient = builder
                .baseUrl(ENDPOINT)
                .defaultHeader("X-Goog-Api-Key", API_KEY)
                .build();
        ocrService = new GoogleVisionOcrService(visionRestClient);
    }

    @Test
    @DisplayName("fullTextAnnotation의 텍스트를 추출한다")
    void 정상_추출() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                        {"responses":[{"fullTextAnnotation":{"text":"정제수, 글리세린"}}]}
                        """,
                        MediaType.APPLICATION_JSON));

        assertThat(ocrService.extractText(IMAGE)).isEqualTo("정제수, 글리세린");
        server.verify();
    }

    @Test
    @DisplayName("요청 본문에 base64 이미지와 DOCUMENT_TEXT_DETECTION이 담긴다")
    void 요청_본문() {
        String expectedBase64 = Base64.getEncoder().encodeToString(IMAGE);
        server.expect(requestTo(ENDPOINT))
                .andExpect(jsonPath("$.requests[0].image.content").value(expectedBase64))
                .andExpect(jsonPath("$.requests[0].features[0].type").value("DOCUMENT_TEXT_DETECTION"))
                .andRespond(withSuccess(
                        """
                        {"responses":[{"fullTextAnnotation":{"text":"텍스트"}}]}
                        """,
                        MediaType.APPLICATION_JSON));

        ocrService.extractText(IMAGE);
        server.verify();
    }

    @Test
    @DisplayName("API 키는 헤더로만 전달되고 URI에 노출되지 않는다")
    void 키_노출_방지() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(header("X-Goog-Api-Key", API_KEY))
                .andExpect(request -> assertThat(request.getURI().toString()).doesNotContain(API_KEY))
                .andRespond(withSuccess(
                        """
                        {"responses":[{"fullTextAnnotation":{"text":"텍스트"}}]}
                        """,
                        MediaType.APPLICATION_JSON));

        ocrService.extractText(IMAGE);
        server.verify();
    }

    @Test
    @DisplayName("fullTextAnnotation이 없으면 추출 실패로 처리한다")
    void 텍스트_없음() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess("""
                        {"responses":[{}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> ocrService.extractText(IMAGE))
                .isInstanceOf(OcrException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OCR_EXTRACTION_FAILED);
    }

    @Test
    @DisplayName("추출된 텍스트가 공백뿐이면 추출 실패로 처리한다")
    void 공백_텍스트() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess("""
                        {"responses":[{"fullTextAnnotation":{"text":"   "}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> ocrService.extractText(IMAGE))
                .isInstanceOf(OcrException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OCR_EXTRACTION_FAILED);
    }

    @Test
    @DisplayName("200 응답이어도 본문에 error가 있으면 호출 실패로 처리한다")
    void 응답_내_오류() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess("""
                        {"responses":[{"error":{"code":3,"message":"Bad image data"}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> ocrService.extractText(IMAGE))
                .isInstanceOf(OcrException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_ERROR);
    }

    @Test
    @DisplayName("responses가 비어 있으면 호출 실패로 처리한다")
    void 빈_응답() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess("""
                        {"responses":[]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> ocrService.extractText(IMAGE))
                .isInstanceOf(OcrException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_ERROR);
    }

    @Test
    @DisplayName("HTTP 오류 응답은 호출 실패로 변환된다")
    void HTTP_오류() {
        server.expect(requestTo(ENDPOINT)).andRespond(withServerError());

        assertThatThrownBy(() -> ocrService.extractText(IMAGE))
                .isInstanceOf(OcrException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_ERROR);
    }

    @Test
    @DisplayName("빈 이미지는 API를 호출하지 않고 거부한다")
    void 빈_이미지() {
        assertThatThrownBy(() -> ocrService.extractText(new byte[0]))
                .isInstanceOf(OcrException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_ERROR);
        assertThatThrownBy(() -> ocrService.extractText(null))
                .isInstanceOf(OcrException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_ERROR);
        server.verify();
    }

    @Test
    @DisplayName("base64 변환 후 허용 크기를 넘으면 API를 호출하지 않고 거부한다")
    void 크기_초과() {
        // base64는 원본의 4/3 크기이므로, 허용치를 넘도록 원본 크기를 잡는다.
        int oversized = GoogleVisionOcrService.MAX_BASE64_LENGTH / 4 * 3 + 1024;

        assertThatThrownBy(() -> ocrService.extractText(new byte[oversized]))
                .isInstanceOf(OcrException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_IMAGE);
        server.verify();
    }

    @Test
    @DisplayName("JSON 껍데기 몫을 제외하고 검사하므로 허용 크기는 Vision 제한보다 작다")
    void 껍데기_여유() {
        assertThat(GoogleVisionOcrService.MAX_BASE64_LENGTH).isLessThan(10 * 1024 * 1024);
    }

    @Test
    @DisplayName("content-type은 JSON으로 전송된다")
    void 요청_헤더() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess("""
                        {"responses":[{"fullTextAnnotation":{"text":"텍스트"}}]}
                        """, MediaType.APPLICATION_JSON));

        ocrService.extractText(IMAGE);
        server.verify();
    }
}

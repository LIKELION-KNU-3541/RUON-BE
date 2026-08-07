package com.springboot.ruon.global.ocr.service;

import com.springboot.ruon.global.exception.Ocr.OcrExtractionFailedException;
import com.springboot.ruon.global.exception.Ocr.OcrImageTooLargeException;
import com.springboot.ruon.global.exception.Ocr.OcrRequestFailedException;
import com.springboot.ruon.global.ocr.dto.VisionAnnotateRequest;
import com.springboot.ruon.global.ocr.dto.VisionAnnotateResponse;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class GoogleVisionOcrService implements OcrService {

    private static final Logger log = LoggerFactory.getLogger(GoogleVisionOcrService.class);

    /** Vision images:annotate의 JSON 요청 크기 제한. */
    private static final int MAX_REQUEST_BYTES = 10 * 1024 * 1024;

    /**
     *base64 외에 요청 JSON이 차지하는 몫
     **/
    private static final int JSON_OVERHEAD_BYTES = 1024;

    static final int MAX_BASE64_LENGTH = MAX_REQUEST_BYTES - JSON_OVERHEAD_BYTES;

    private final RestClient visionRestClient;

    public GoogleVisionOcrService(RestClient visionRestClient) {
        this.visionRestClient = visionRestClient;
    }

    @Override
    public String extractText(byte[] image) {
        if (image == null || image.length == 0) {
            throw new OcrRequestFailedException("OCR 대상 이미지가 비어 있습니다.");
        }

        String base64Image = Base64.getEncoder().encodeToString(image);
        if (base64Image.length() > MAX_BASE64_LENGTH) {
            throw new OcrImageTooLargeException(
                    "이미지가 너무 커서 인식할 수 없습니다. (base64 %d bytes, 허용 %d bytes)"
                            .formatted(base64Image.length(), MAX_BASE64_LENGTH));
        }

        VisionAnnotateResponse response = requestAnnotate(base64Image);
        return extractFullText(response);
    }

    private VisionAnnotateResponse requestAnnotate(String base64Image) {
        try {
            return visionRestClient.post()
                    .body(VisionAnnotateRequest.documentTextDetection(base64Image))
                    .retrieve()
                    .body(VisionAnnotateResponse.class);
        } catch (RestClientResponseException e) {
            log.error("Vision API 오류 응답: status={}", e.getStatusCode(), e);
            throw new OcrRequestFailedException(
                    "Vision API 호출에 실패했습니다. (status %s)".formatted(e.getStatusCode()), e);
        } catch (ResourceAccessException e) {
            log.error("Vision API 통신 실패", e);
            throw new OcrRequestFailedException("Vision API 통신에 실패했습니다.", e);
        } catch (RestClientException e) {
            log.error("Vision API 응답 처리 실패", e);
            throw new OcrRequestFailedException("Vision API 응답을 처리하지 못했습니다.", e);
        }
    }

    /**
     * Vision은 개별 이미지 처리 실패를 HTTP 오류가 아니라 응답 안의 error로 처리
     */
    private String extractFullText(VisionAnnotateResponse response) {
        List<VisionAnnotateResponse.AnnotateImageResponse> responses =
                response == null ? null : response.responses();
        if (responses == null || responses.isEmpty()) {
            throw new OcrRequestFailedException("Vision API 응답이 비어 있습니다.");
        }

        VisionAnnotateResponse.AnnotateImageResponse first = responses.getFirst();
        if (first.error() != null) {
            log.error("Vision API 처리 오류: code={}, message={}",
                    first.error().code(), first.error().message());
            throw new OcrRequestFailedException("Vision API가 이미지 처리에 실패했습니다.");
        }

        VisionAnnotateResponse.FullTextAnnotation annotation = first.fullTextAnnotation();
        if (annotation == null || annotation.text() == null || annotation.text().isBlank()) {
            throw new OcrExtractionFailedException("이미지에서 텍스트를 찾지 못했습니다.");
        }
        return annotation.text();
    }
}

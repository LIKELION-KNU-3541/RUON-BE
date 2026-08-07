package com.springboot.ruon.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.springboot.ruon.global.exception.Image.ImageStorageException;
import com.springboot.ruon.global.exception.Ocr.OcrException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * 이미지 스토리지 예외가 팀 공통 응답 포맷과 HTTP 상태로 변환되는지 검증.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("검증 실패는 400과 INVALID_IMAGE로 변환되고, 실패 사유가 그대로 전달된다")
    void 검증_실패() throws Exception {
        mockMvc.perform(get("/test/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_IMAGE"))
                .andExpect(jsonPath("$.error.message").value("파일 크기가 7MB를 초과했습니다."));
    }

    @Test
    @DisplayName("이미지 없음은 404와 NOT_FOUND로 변환된다")
    void 이미지_없음() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("업로드 실패는 500과 INTERNAL_ERROR로 변환된다")
    void 업로드_실패() throws Exception {
        mockMvc.perform(get("/test/upload"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));
    }

    @Test
    @DisplayName("삭제 실패도 상위 타입 핸들러가 받아 INTERNAL_ERROR로 변환된다")
    void 삭제_실패() throws Exception {
        mockMvc.perform(get("/test/delete"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));
    }

    @Test
    @DisplayName("업로드 실패 응답에 objectKey 등 내부 정보가 노출되지 않는다")
    void 내부_정보_비노출() throws Exception {
        mockMvc.perform(get("/test/upload"))
                .andExpect(jsonPath("$.error.message").value("서버 오류"));
    }

    @Test
    @DisplayName("스토리지와 무관한 예외는 기존 캐치올이 INTERNAL_ERROR로 처리한다")
    void 그_외_예외() throws Exception {
        mockMvc.perform(get("/test/other"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));
    }

    @Test
    @DisplayName("OCR 예외는 별도 핸들러 없이 CustomException 경로로 ErrorCode에 맞게 변환된다")
    void OCR_인식실패() throws Exception {
        mockMvc.perform(get("/test/ocr-extraction"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("OCR_EXTRACTION_FAILED"));
    }

    @Test
    @DisplayName("OCR 호출 실패 응답에 외부 API 상세가 노출되지 않는다")
    void OCR_호출실패() throws Exception {
        mockMvc.perform(get("/test/ocr-request"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("서버 오류"));
    }

    /** 예외만 던지는 테스트용 컨트롤러. */
    @RestController
    static class ThrowingController {

        @GetMapping("/test/ocr-extraction")
        void OCR인식실패() {
            throw new OcrException(ErrorCode.OCR_EXTRACTION_FAILED, "이미지에서 텍스트를 찾지 못했습니다.");
        }

        @GetMapping("/test/ocr-request")
        void OCR호출실패() {
            throw new OcrException(ErrorCode.INTERNAL_ERROR,
                    "Vision API 호출에 실패했습니다. (status 403 PERMISSION_DENIED)",
                    new RuntimeException("api key invalid"));
        }

        @GetMapping("/test/validation")
        void 검증실패() {
            throw new ImageStorageException(ErrorCode.INVALID_IMAGE, "파일 크기가 7MB를 초과했습니다.");
        }

        @GetMapping("/test/not-found")
        void 이미지없음() {
            throw new ImageStorageException(ErrorCode.NOT_FOUND, "이미지를 찾을 수 없습니다: scans/1/test.jpg",
                    NoSuchKeyException.builder().build());
        }

        @GetMapping("/test/upload")
        void 업로드실패() {
            throw new ImageStorageException(ErrorCode.INTERNAL_ERROR, "S3 이미지 업로드에 실패했습니다: scans/1/test.jpg",
                    new RuntimeException("access denied"));
        }

        @GetMapping("/test/delete")
        void 삭제실패() {
            throw new ImageStorageException(ErrorCode.INTERNAL_ERROR, "S3 이미지 삭제에 실패했습니다: scans/1/test.jpg",
                    new RuntimeException("access denied"));
        }

        @GetMapping("/test/other")
        void 그외예외() {
            throw new IllegalStateException("스토리지와 무관한 예외");
        }
    }
}

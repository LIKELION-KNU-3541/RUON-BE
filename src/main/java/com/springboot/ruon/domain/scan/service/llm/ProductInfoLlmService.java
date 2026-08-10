package com.springboot.ruon.domain.scan.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;
import com.springboot.ruon.global.llm.OpenAiClient;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

//OCR 텍스트 추출 결과 구조화
@Service
@RequiredArgsConstructor
public class ProductInfoLlmService {

    private static final Logger log = LoggerFactory.getLogger(ProductInfoLlmService.class);

    private static final String SCHEMA_NAME = "product_info";

    //값이 없다는 뜻으로 모델이 흔히 써 보내는 표현들. 소문자로 비교한다.
    private static final Set<String> EMPTY_VALUES =
            Set.of("null", "none", "n/a", "unknown", "없음", "미상", "알 수 없음", "-");

    private static final String SYSTEM_PROMPT = """
            당신은 화장품 용기에서 OCR로 추출한 텍스트에서 제품 정보를 뽑아내는 도구입니다.

            규칙:
            - 입력 텍스트에 실제로 적혀 있는 내용만 사용합니다. 추측하거나 지어내지 마세요.
            - 아는 제품이라도 기억에 의존하지 말고, 입력 텍스트에 있는 글자만 사용합니다.
            - 알 수 없는 값은 JSON의 null로 둡니다. "null", "없음", "미상" 같은 글자를 문자열로 넣지 마세요.
            - brandName: 브랜드 또는 제조사 이름.
            - productName: 제품 이름. 브랜드명과 용량은 빼고 제품 이름만 남깁니다.
            - capacity: 용량. 텍스트에 적힌 숫자와 단위를 그대로 옮깁니다. (예: 50ml)
            - fullIngredients: 전성분 목록. 적힌 순서를 그대로 유지하고, 임의로 추가·삭제·번역·중복 제거를 하지 마세요.
            - 전성분을 찾을 수 없으면 빈 배열로 둡니다.
            - 성분의 효능이나 안전성은 판단하지 마세요.
            """;

    /*
     * strict 모드 제약에 맞춘 스키마.
     * 모든 필드가 required여야 하고, additionalProperties는 false여야 하며,
     * 값이 없을 수 있는 필드는 타입에 "null"을 함께 적어야 한다.
     */
    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "object",
            "additionalProperties", false,
            "required", List.of("brandName", "productName", "capacity", "fullIngredients"),
            "properties", Map.of(
                    "brandName", Map.of("type", List.of("string", "null")),
                    "productName", Map.of("type", List.of("string", "null")),
                    "capacity", Map.of("type", List.of("string", "null")),
                    "fullIngredients", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string")
                    )
            )
    );

    private final OpenAiClient openAiClient;

    //Spring Boot 4에서는 ObjectMapper 빈이 자동으로 등록되지 않아 직접 만든다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductInfoLlmResult extract(String rawIngredientText) {
        if (rawIngredientText == null || rawIngredientText.isBlank()) {
            throw new CustomException(ErrorCode.PRODUCT_INFO_EXTRACTION_FAILED);
        }

        String content = requestExtraction(rawIngredientText);
        try {
            return normalize(objectMapper.readValue(content, ProductInfoLlmResult.class));
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("제품 정보 응답을 해석하지 못했습니다.", e);
            throw new CustomException(ErrorCode.PRODUCT_INFO_EXTRACTION_FAILED);
        }
    }

    /*
     * 스키마가 문자열을 허용하다 보니 값이 없을 때 "null", "없음" 같은 글자가 들어온다.
     * 그대로 두면 검색어가 "null 수분크림"처럼 만들어져 엉뚱한 이미지를 찾는다.
     */
    private ProductInfoLlmResult normalize(ProductInfoLlmResult result) {
        return new ProductInfoLlmResult(
                normalizeText(result.brandName()),
                normalizeText(result.productName()),
                normalizeText(result.capacity()),
                normalizeIngredients(result.fullIngredients())
        );
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.isEmpty() || EMPTY_VALUES.contains(trimmed.toLowerCase(Locale.ROOT))) {
            return null;
        }
        return trimmed;
    }

    private List<String> normalizeIngredients(List<String> ingredients) {
        if (ingredients == null) {
            return List.of();
        }
        //순서가 의미를 가지므로 걸러내기만 하고 재정렬하지 않는다.
        return ingredients.stream()
                .map(this::normalizeText)
                .filter(Objects::nonNull)
                .toList();
    }

    //DB에는 JSON 문자열로 보관하므로, 검증을 마친 결과를 다시 문자열로 만둠.
    public String toJson(ProductInfoLlmResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("제품 정보를 JSON으로 변환하지 못했습니다.", e);
            throw new CustomException(ErrorCode.PRODUCT_INFO_EXTRACTION_FAILED);
        }
    }

    //저장해둔 Json 문자열 -> 객체 , 구조화 전 값 null, 값이 꺠진 경우도 null로 처리.
    public ProductInfoLlmResult fromJson(String structuredResult) {
        if (structuredResult == null || structuredResult.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(structuredResult, ProductInfoLlmResult.class);
        } catch (Exception e) {
            log.error("저장된 제품 정보를 읽지 못했습니다: {}", structuredResult, e);
            return null;
        }
    }

    //OpenAiClient는 루틴 생성용 에러 코드를 던지므로 스캔용으로 바꿔서 올리도록 처리.
    private String requestExtraction(String rawIngredientText) {
        try {
            return openAiClient.requestStructuredCompletion(
                    SYSTEM_PROMPT, rawIngredientText, SCHEMA_NAME, SCHEMA);
        } catch (CustomException e) {
            throw new CustomException(ErrorCode.PRODUCT_INFO_EXTRACTION_FAILED);
        }
    }
}

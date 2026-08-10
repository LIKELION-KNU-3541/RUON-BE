package com.springboot.ruon.global.llm;

import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    //connectTimeout 5초 이상 지연시 포기, readTimeout 30초로 타임 아웃을 걸어 무한 대기 방지
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private final RestClient restClient;
    private final String model;

    public OpenAiClient(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model
    ) {
        this.model = model;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public String requestJsonCompletion(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", messages(systemPrompt, userPrompt)
        );
        return call(requestBody);
    }

    //JSON Schema를 강제해 응답 형식을 보장
    public String requestStructuredCompletion(String systemPrompt, String userPrompt,
                                              String schemaName, Map<String, Object> jsonSchema) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of(
                                "name", schemaName,
                                "strict", true,
                                "schema", jsonSchema
                        )
                ),
                "messages", messages(systemPrompt, userPrompt)
        );
        return call(requestBody);
    }

    private List<Map<String, String>> messages(String systemPrompt, String userPrompt) {
        return List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );
    }

    @SuppressWarnings("unchecked")
    private String call(Map<String, Object> requestBody) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

            //모델이 요청을 거절하면 content 대신 refusal이 온다. 그대로 두면 null이 흘러간다.
            String refusal = (String) message.get("refusal");
            if (refusal != null) {
                log.error("OpenAI가 응답을 거절했습니다: {}", refusal);
                throw new CustomException(ErrorCode.LLM_GENERATION_FAILED);
            }
            return (String) message.get("content");
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI 호출 실패", e);
            throw new CustomException(ErrorCode.LLM_GENERATION_FAILED);
        }
    }
}

package com.springboot.ruon.global.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Vision 호출 전용 {@link RestClient} 설정.
 * 인증 방식이 바뀌더라도(예: 서비스 계정, 워크로드 아이덴티티) 여기만 교체
 */
@Configuration
@EnableConfigurationProperties(VisionProperties.class)
public class VisionConfig {

    private static final Logger log = LoggerFactory.getLogger(VisionConfig.class);

    /** 키를 헤더로 보냄 **/
    private static final String API_KEY_HEADER = "X-Goog-Api-Key";

    /**
     * {@code RestClient.Builder} 빈을 주입받지 않고 직접 만든다.
     * 그 빈은 spring-boot-restclient 모듈이 제공하는데 이 프로젝트 의존성에 없다.
     */
    @Bean
    public RestClient visionRestClient(VisionProperties properties) {
        if (!properties.hasApiKey()) {
            log.warn("GOOGLE_VISION_API_KEY가 설정되지 않았습니다. OCR 호출은 인증 실패로 처리됩니다.");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.endpoint())
                .requestFactory(requestFactory)
                .defaultHeader(API_KEY_HEADER, properties.apiKey())
                .build();
    }
}

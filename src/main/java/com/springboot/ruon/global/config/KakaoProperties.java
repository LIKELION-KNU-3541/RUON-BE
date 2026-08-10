package com.springboot.ruon.global.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 카카오 검색 API 연동 설정.
 * REST API 키는 앱 단위라 검색 외의 카카오 API를 쓰게 되어도 그대로 쓴다.
 */
@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(
        String restApiKey,
        String imageSearchEndpoint,
        int imageSearchSize,
        Duration connectTimeout,
        Duration readTimeout) {

    private static final String DEFAULT_IMAGE_SEARCH_ENDPOINT = "https://dapi.kakao.com/v2/search/image";

    //기본값이 80이라 지정하지 않으면 매번 80건을 받는다. 대표 이미지 한 장만 고르면 되므로 적게 받는다.
    private static final int DEFAULT_IMAGE_SEARCH_SIZE = 5;
    private static final int MAX_IMAGE_SEARCH_SIZE = 80;

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);

    public KakaoProperties {
        if (restApiKey == null) {
            restApiKey = "";
        }
        if (imageSearchEndpoint == null || imageSearchEndpoint.isBlank()) {
            imageSearchEndpoint = DEFAULT_IMAGE_SEARCH_ENDPOINT;
        }
        if (imageSearchSize <= 0) {
            imageSearchSize = DEFAULT_IMAGE_SEARCH_SIZE;
        }
        if (imageSearchSize > MAX_IMAGE_SEARCH_SIZE) {
            imageSearchSize = MAX_IMAGE_SEARCH_SIZE;
        }
        if (connectTimeout == null) {
            connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        }
        if (readTimeout == null) {
            readTimeout = DEFAULT_READ_TIMEOUT;
        }
    }

    public boolean hasRestApiKey() {
        return !restApiKey.isBlank();
    }
}

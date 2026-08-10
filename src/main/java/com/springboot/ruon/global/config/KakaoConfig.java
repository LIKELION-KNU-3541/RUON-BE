package com.springboot.ruon.global.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KakaoProperties.class)
public class KakaoConfig {

    private static final Logger log = LoggerFactory.getLogger(KakaoConfig.class);

    private static final String AUTHORIZATION_PREFIX = "KakaoAK ";

    @Bean
    public RestClient kakaoRestClient(KakaoProperties properties) {
        if (!properties.hasRestApiKey()) {
            log.warn("KAKAO_REST_API_KEY가 설정되지 않았습니다. 대표 이미지 검색은 인증 실패로 처리됩니다.");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.imageSearchEndpoint())
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", AUTHORIZATION_PREFIX + properties.restApiKey())
                .build();
    }
}

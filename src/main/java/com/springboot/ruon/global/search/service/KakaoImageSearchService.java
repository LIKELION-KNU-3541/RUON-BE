package com.springboot.ruon.global.search.service;

import com.springboot.ruon.global.config.KakaoProperties;
import com.springboot.ruon.global.search.dto.KakaoImageSearchResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 카카오 이미지 검색으로 제품 이미지 후보를 찾는다.
 * 대표 이미지는 선택 기능이므로 검색이 실패해도 예외를 던지지 않고 빈 목록.
 */
@Service
public class KakaoImageSearchService implements ImageSearchService {

    private static final Logger log = LoggerFactory.getLogger(KakaoImageSearchService.class);

    //정확도순으로 이미지 가져옴
    private static final String SORT_BY_ACCURACY = "accuracy";

    private final RestClient kakaoRestClient;
    private final KakaoProperties properties;

    public KakaoImageSearchService(RestClient kakaoRestClient, KakaoProperties properties) {
        this.kakaoRestClient = kakaoRestClient;
        this.properties = properties;
    }

    @Override
    public List<String> searchImageUrls(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        if (!properties.hasRestApiKey()) {
            log.warn("KAKAO_REST_API_KEY가 없어 대표 이미지 검색을 건너뜁니다.");
            return List.of();
        }

        try {
            KakaoImageSearchResponse response = kakaoRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("query", query)
                            .queryParam("sort", SORT_BY_ACCURACY)
                            .queryParam("size", properties.imageSearchSize())
                            .build())
                    .retrieve()
                    .body(KakaoImageSearchResponse.class);

            if (response == null) {
                return List.of();
            }
            return response.documentsOrEmpty().stream()
                    .map(KakaoImageSearchResponse.Document::imageUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .toList();
        } catch (RestClientException e) {
            //검색 실패로 스캔 전체를 실패시키지 않는다.
            log.warn("대표 이미지 검색에 실패했습니다: query={}", query, e);
            return List.of();
        }
    }
}

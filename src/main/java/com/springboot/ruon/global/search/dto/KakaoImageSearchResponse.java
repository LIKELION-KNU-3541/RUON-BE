package com.springboot.ruon.global.search.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

//카카오 이미지 검색 응답 중 필요한것받는 Dto정의
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoImageSearchResponse(List<Document> documents) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
            @JsonProperty("image_url") String imageUrl,
            @JsonProperty("thumbnail_url") String thumbnailUrl,
            Integer width,
            Integer height) {
    }

    public List<Document> documentsOrEmpty() {
        return documents == null ? List.of() : documents;
    }
}

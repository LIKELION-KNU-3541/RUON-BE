package com.springboot.ruon.domain.scan.service.llm;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

 //OCR 원문에서 뽑아낸 제품 정보.
public record ProductInfoLlmResult(
        String brandName,
        String productName,
        String capacity,
        List<String> fullIngredients
) {

    /**
     * 대표 이미지를 찾기 위한 검색어.
     * 값이 없는 항목은 빼고 이어 붙임
     */
    public String toSearchQuery() {
        return Stream.of(brandName, productName, capacity)
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }
}

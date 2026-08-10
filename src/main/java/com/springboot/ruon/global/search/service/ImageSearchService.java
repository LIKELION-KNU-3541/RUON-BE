package com.springboot.ruon.global.search.service;

import java.util.List;

//제품 대표 이미지 후보를 찾는 추상화.

public interface ImageSearchService {

    //실패시 빈 목록으로 반환
    List<String> searchImageUrls(String query);
}

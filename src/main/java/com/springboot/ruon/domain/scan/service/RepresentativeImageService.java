package com.springboot.ruon.domain.scan.service;

import com.springboot.ruon.domain.scan.service.llm.ProductInfoLlmResult;
import com.springboot.ruon.global.search.service.ImageSearchService;
import com.springboot.ruon.global.storage.service.ImageStorageService;
import com.springboot.ruon.global.storage.service.RemoteImageDownloader;
import com.springboot.ruon.global.storage.service.RemoteImageDownloader.DownloadedImage;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 구조화한 제품 정보로 대표 이미지를 찾아 저장한다.
 * 대표 이미지는 선택 기능이라 어느 단계에서 실패하든 예외를 던지지 않고 null을 돌려준다.
 */
@Service
@RequiredArgsConstructor
public class RepresentativeImageService {

    private static final Logger log = LoggerFactory.getLogger(RepresentativeImageService.class);

    private final ImageSearchService imageSearchService;
    private final RemoteImageDownloader remoteImageDownloader;
    private final ImageStorageService imageStorageService;

    /**
     * 저장한 대표 이미지의 objectKey. 찾지 못하면 null.
     */
    public String findAndStore(Long userId, ProductInfoLlmResult productInfo) {
        String query = productInfo.toSearchQuery();
        if (query.isBlank()) {
            //제품 정보를 하나도 못 뽑은 경우다. 검색하면 엉뚱한 이미지가 붙는다.
            log.info("검색어를 만들 수 없어 대표 이미지 검색을 건너뜁니다: userId={}", userId);
            return null;
        }

        //앞 후보가 실패해도 다음 후보로 넘어간다. 죽은 링크나 이미지가 아닌 응답이 흔하다.
        for (String imageUrl : imageSearchService.searchImageUrls(query)) {
            String objectKey = tryStore(userId, imageUrl);
            if (objectKey != null) {
                return objectKey;
            }
        }

        log.info("쓸 수 있는 대표 이미지를 찾지 못했습니다: query={}", query);
        return null;
    }

    private String tryStore(Long userId, String imageUrl) {
        try {
            Optional<DownloadedImage> downloaded = remoteImageDownloader.download(imageUrl);
            if (downloaded.isEmpty()) {
                return null;
            }
            DownloadedImage image = downloaded.get();
            //검증은 업로드 쪽에서 한다. 이미지가 아니면 예외가 나고 다음 후보로 넘어간다.
            return imageStorageService.upload(userId, image.body(), image.contentType());
        } catch (RuntimeException e) {
            log.debug("대표 이미지 후보를 쓰지 못했습니다: url={}", imageUrl, e);
            return null;
        }
    }
}

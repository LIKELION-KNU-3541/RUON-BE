package com.springboot.ruon.domain.scan.service;

import com.springboot.ruon.domain.scan.entity.ScanJob;
import com.springboot.ruon.global.storage.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * ScanJob에 연결된 이미지의 조회용 URL을 만든다.
 * 대표 이미지(카카오 검색 결과)가 있으면 그걸, 없으면 사용자가 올린 원본 이미지를 사용한다.
 *
 * URL이 presigned라 만료되므로 저장해두지 않고, 필요할 때마다 새로 발급한다.
 * (ProductService/RoutineService 양쪽에서 같은 로직을 쓰기 위해 공통으로 뺌)
 */
@Service
@RequiredArgsConstructor
public class ScanImageUrlService {

    private final ImageStorageService imageStorageService;

    // scanJob이 없으면(직접 등록한 제품 등) 이미지 없음
    public String resolveViewUrl(ScanJob scanJob) {
        if (scanJob == null) {
            return null;
        }
        return imageStorageService.generateViewUrl(viewImageObjectKey(scanJob));
    }

    private String viewImageObjectKey(ScanJob scanJob) {
        String representativeImageObjectKey = scanJob.getRepresentativeImageObjectKey();
        return representativeImageObjectKey != null
                ? representativeImageObjectKey
                : scanJob.getUploadedImageObjectKey();
    }
}

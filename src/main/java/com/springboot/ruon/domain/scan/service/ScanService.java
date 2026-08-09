package com.springboot.ruon.domain.scan.service;

import com.springboot.ruon.domain.scan.entity.ScanJob;
import com.springboot.ruon.domain.scan.repository.ScanJobRepository;
import com.springboot.ruon.global.storage.service.ImageStorageService;
import java.util.concurrent.RejectedExecutionException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 스캔 요청을 접수하는 서비스.
 * <p>
 * 이미지 검증과 S3 업로드는 동기로 처리하고, OCR은 이후 비동기 단계에서 진행한다.
 */
@Service
@RequiredArgsConstructor
public class ScanService {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);

    private final ImageStorageService imageStorageService;
    private final ScanJobRepository scanJobRepository;
    private final ScanProcessor scanProcessor;

    /**
     * 이미지를 저장하고 스캔 작업을 생성한다.
     * <p>
     * 이미지 검증은 {@link ImageStorageService#upload}에서 수행하므로, 검증에 실패하면
     * S3에 올라가지도 스캔 작업이 생기지도 않는다.
     *
     * @return 생성된 스캔 작업 (상태는 UPLOADED)
     */
    public ScanJob createScan(Long userId, MultipartFile image) {
        String objectKey = imageStorageService.upload(userId, image);

        ScanJob scanJob;
        try {
            scanJob = scanJobRepository.save(ScanJob.builder()
                    .userId(userId)
                    .uploadedImageObjectKey(objectKey)
                    .build());
        } catch (RuntimeException e) {
            // 업로드는 됐는데 작업 생성에 실패하면 버킷에 고아 객체가 남으므로 되돌린다.
            deleteQuietly(objectKey);
            throw e;
        }

        startProcessing(scanJob, objectKey);
        return scanJob;
    }

    /**
     * 스레드풀이 가득 차 작업을 넘기지 못하면 그대로 두면 UPLOADED인 채로 방치된다.
     * 사용자가 재시도할 수 있도록 실패로 기록한다.
     */
    private void startProcessing(ScanJob scanJob, String objectKey) {
        try {
            scanProcessor.process(scanJob.getScanId(), objectKey);
        } catch (RejectedExecutionException e) {
            log.error("스캔 처리를 시작하지 못했습니다: scanId={}", scanJob.getScanId(), e);
            scanJob.markFailed();
            scanJobRepository.save(scanJob);
        }
    }

    /** 보상 삭제가 실패해도 원래 예외를 가리지 않도록 삼키고 로그만 남긴다. */
    private void deleteQuietly(String objectKey) {
        try {
            imageStorageService.delete(objectKey);
        } catch (RuntimeException e) {
            log.error("스캔 작업 생성 실패 후 이미지 정리에 실패했습니다: {}", objectKey, e);
        }
    }
}

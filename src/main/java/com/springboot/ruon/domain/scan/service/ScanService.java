package com.springboot.ruon.domain.scan.service;

import com.springboot.ruon.domain.scan.dto.response.ScanDetailResponse;
import com.springboot.ruon.domain.rag.dto.response.IngredientAnalysisResponse;
import com.springboot.ruon.domain.rag.service.RagService;
import com.springboot.ruon.domain.scan.entity.ScanFailureStage;
import com.springboot.ruon.domain.scan.entity.ScanJob;
import com.springboot.ruon.domain.scan.repository.ScanJobRepository;
import com.springboot.ruon.domain.scan.service.llm.ProductInfoLlmService;
import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;
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
    private final ProductInfoLlmService productInfoLlmService;
    private final RagService ragService;
    private final AnalysisSummaryService analysisSummaryService;

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
     * 스캔 작업을 조회한다.
     * 처리가 비동기라 프론트는 이 메서드를 폴링해서 완료를 확인한다.
     *
     * @throws CustomException 스캔이 없거나(404) 다른 사용자의 스캔인 경우(403)
     */
    public ScanDetailResponse getScanDetail(Long userId, Long scanId) {
        ScanJob scanJob = getScan(userId, scanId);
        IngredientAnalysisResponse ingredientAnalysis =
                ragService.fromJson(scanJob.getIngredientAnalysisResult());
        //DB에는 JSON 문자열로 있어서, 그대로 내보내면 프론트가 한 번 더 파싱해야 한다.
        return ScanDetailResponse.from(
                scanJob,
                productInfoLlmService.fromJson(scanJob.getStructuredResult()),
                ingredientAnalysis,
                ingredientAnalysis == null ? null : analysisSummaryService.create(ingredientAnalysis),
                imageStorageService.generateViewUrl(viewImageObjectKey(scanJob)));
    }

    //대표 이미지는 못 찾을 수 있다. 그때는 사용자가 올린 사진을 보여준다.
    private String viewImageObjectKey(ScanJob scanJob) {
        String representativeImageObjectKey = scanJob.getRepresentativeImageObjectKey();
        return representativeImageObjectKey != null
                ? representativeImageObjectKey
                : scanJob.getUploadedImageObjectKey();
    }

    private ScanJob getScan(Long userId, Long scanId) {
        ScanJob scanJob = scanJobRepository.findById(scanId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND, "스캔 작업을 찾을 수 없습니다.", null));

        if (!scanJob.getUserId().equals(userId)) {
            //남의 스캔에는 성분표 원문이 그대로 들어 있어 노출되면 안 된다.
            throw new CustomException(ErrorCode.FORBIDDEN, "본인의 스캔만 조회할 수 있습니다.", null);
        }
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
            scanJob.markFailed(ScanFailureStage.PROCESSING_START, ErrorCode.TOO_MANY_REQUESTS.name());
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

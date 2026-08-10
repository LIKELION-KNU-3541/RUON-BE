package com.springboot.ruon.domain.scan.service;

import com.springboot.ruon.domain.scan.entity.ScanJob;
import com.springboot.ruon.domain.scan.repository.ScanJobRepository;
import com.springboot.ruon.domain.scan.service.llm.ProductInfoLlmResult;
import com.springboot.ruon.domain.scan.service.llm.ProductInfoLlmService;
import com.springboot.ruon.global.config.AsyncConfig;
import com.springboot.ruon.global.ocr.service.OcrService;
import com.springboot.ruon.global.storage.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScanProcessor {

    private static final Logger log = LoggerFactory.getLogger(ScanProcessor.class);

    private final ScanJobRepository scanJobRepository;
    private final ImageStorageService imageStorageService;
    private final OcrService ocrService;
    private final ProductInfoLlmService productInfoLlmService;
    private final RepresentativeImageService representativeImageService;

    @Async(AsyncConfig.SCAN_EXECUTOR)
    public void process(Long scanId, String objectKey) {
        ScanJob scanJob = scanJobRepository.findById(scanId).orElse(null);
        if (scanJob == null) {
            log.error("스캔 작업을 찾을 수 없어 처리를 중단합니다: scanId={}", scanId);
            return;
        }

        try {
            scanJob.markOcrProcessing();
            scanJobRepository.save(scanJob);

            byte[] image = imageStorageService.download(objectKey);
            String rawIngredientText = ocrService.extractText(image);

            scanJob.completeOcr(rawIngredientText);
            scanJobRepository.save(scanJob);

            ProductInfoLlmResult productInfo = productInfoLlmService.extract(rawIngredientText);
            scanJob.completeStructuring(productInfoLlmService.toJson(productInfo));
            scanJobRepository.save(scanJob);

            //대표 이미지는 선택 기능이라 못 찾으면 null이 오고, 그래도 완료로 본다.
            String representativeImageObjectKey =
                    representativeImageService.findAndStore(scanJob.getUserId(), productInfo);
            scanJob.complete(representativeImageObjectKey);
            scanJobRepository.save(scanJob);
        } catch (RuntimeException e) {
            log.error("스캔 처리에 실패했습니다: scanId={}", scanId, e);
            markFailedQuietly(scanJob);
        }
    }

    private void markFailedQuietly(ScanJob scanJob) {
        try {
            scanJob.markFailed();
            scanJobRepository.save(scanJob);
        } catch (RuntimeException e) {
            log.error("실패 상태 저장에 실패했습니다: scanId={}", scanJob.getScanId(), e);
        }
    }
}

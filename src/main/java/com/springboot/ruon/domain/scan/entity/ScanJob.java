package com.springboot.ruon.domain.scan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OCR 스캔 작업 (ERD의 SCAN_JOB 테이블).
 * 이미지에서 텍스트를 추출하는 과정을 기록.
 */
@Entity
@Table(name = "scan_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScanJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scan_id")
    private Long scanId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ScanStatus status;

    //사용자가 올린 원본 이미지
    @Column(name = "uploaded_image_object_key", nullable = false)
    private String uploadedImageObjectKey;

    /*카카오 검색으로 찾은 대표 이미지. 검색에 실패해도 스캔은 완료되므로 null 일 가능성 존재 */
    @Column(name = "representative_image_object_key")
    private String representativeImageObjectKey;


    //OCR이 추출한 원문 전체
    // 길이를 안 주면 기본값 255로 TINYTEXT가 되어 성분표 원문이 잘린다. (한글은 글자당 3바이트)
    @Lob
    @Column(name = "raw_ingredient_text", length = 100_000)
    private String rawIngredientText;

    @Builder
    public ScanJob(Long userId, String uploadedImageObjectKey) {
        this.userId = userId;
        this.uploadedImageObjectKey = uploadedImageObjectKey;
        this.status = ScanStatus.UPLOADED;
    }

    public void markOcrProcessing() {
        this.status = ScanStatus.OCR_PROCESSING;
    }
    //원문과 상태를 동일한 형태로 맞춤
    public void completeWithOcrText(String rawIngredientText) {
        this.rawIngredientText = rawIngredientText;
        this.status = ScanStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = ScanStatus.FAILED;
    }
}

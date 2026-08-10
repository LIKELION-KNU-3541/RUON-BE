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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    //ddl-auto=update는 Enum 정의가 바뀌는걸 고려해 varchar로 고정
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private ScanStatus status;

    //사용자가 올린 원본 이미지
    @Column(name = "uploaded_image_object_key", nullable = false)
    private String uploadedImageObjectKey;

    //카카오 검색으로 찾은 대표 이미지. 검색에 실패해도 스캔은 완료되므로 null 일 가능성 존재
    @Column(name = "representative_image_object_key")
    private String representativeImageObjectKey;

    //OCR이 추출한 원문 전체
    //길이를 안 주면 기본값 255로 TINYTEXT가 되어 성분표 원문이 잘린다. (한글은 글자당 3바이트)
    @Lob
    @Column(name = "raw_ingredient_text", length = 100_000)
    private String rawIngredientText;

    //원문에서 뽑아낸 제품 정보(브랜드/제품명/용량/전성분)를 JSON 문자열로 보관
    @Lob
    @Column(name = "structured_result", length = 100_000)
    private String structuredResult;

    @Builder
    public ScanJob(Long userId, String uploadedImageObjectKey) {
        this.userId = userId;
        this.uploadedImageObjectKey = uploadedImageObjectKey;
        this.status = ScanStatus.UPLOADED;
    }

    public void markOcrProcessing() {
        this.status = ScanStatus.OCR_PROCESSING;
    }

    //OCR 단계 완료
    public void completeOcr(String rawIngredientText) {
        this.rawIngredientText = rawIngredientText;
        this.status = ScanStatus.STRUCTURING;
    }

    //구조화 단계 완료
    public void completeStructuring(String structuredResult) {
        this.structuredResult = structuredResult;
        this.status = ScanStatus.IMAGE_SEARCHING;
    }

    //모든 처리 완료. 대표 이미지는 선택 기능이라 못 찾은 경우 null 가능성 허용
    public void complete(String representativeImageObjectKey) {
        this.representativeImageObjectKey = representativeImageObjectKey;
        this.status = ScanStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = ScanStatus.FAILED;
    }
}

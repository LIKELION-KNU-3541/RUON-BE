package com.springboot.ruon.domain.scan.entity;

public enum ScanFailureStage {
    PROCESSING_START,
    IMAGE_DOWNLOAD,
    OCR,
    STRUCTURING,
    RAG_ANALYSIS,
    REPRESENTATIVE_IMAGE
}

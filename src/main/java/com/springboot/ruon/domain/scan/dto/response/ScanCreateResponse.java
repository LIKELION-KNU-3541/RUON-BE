package com.springboot.ruon.domain.scan.dto.response;

import com.springboot.ruon.domain.scan.entity.ScanJob;
import com.springboot.ruon.domain.scan.entity.ScanStatus;

//스캔 요청 접수 응답 (POST /api/v1/scan).

public record ScanCreateResponse(Long scanId, ScanStatus status) {

    public static ScanCreateResponse from(ScanJob scanJob) {
        return new ScanCreateResponse(scanJob.getScanId(), scanJob.getStatus());
    }
}

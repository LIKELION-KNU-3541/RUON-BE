package com.springboot.ruon.domain.scan.controller;

import com.springboot.ruon.auth.security.CustomUserDetails;
import com.springboot.ruon.domain.scan.dto.response.ScanCreateResponse;
import com.springboot.ruon.domain.scan.entity.ScanJob;
import com.springboot.ruon.domain.scan.service.ScanService;
import com.springboot.ruon.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/scan")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;

    //이미지 검증 및 S3 업로드 생성완료 201, 접수 202)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ScanCreateResponse>> createScan(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("image") MultipartFile image) {

        ScanJob scanJob = scanService.createScan(userDetails.getUserId(), image);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(ScanCreateResponse.from(scanJob)));
    }
}

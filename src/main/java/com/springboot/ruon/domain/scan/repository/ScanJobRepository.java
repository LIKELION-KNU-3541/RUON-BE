package com.springboot.ruon.domain.scan.repository;

import com.springboot.ruon.domain.scan.entity.ScanJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanJobRepository extends JpaRepository<ScanJob, Long> {
}

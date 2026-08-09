package com.springboot.ruon.domain.routine.repository;

import com.springboot.ruon.domain.routine.entity.Step;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StepRepository extends JpaRepository<Step, Long> {
}

package com.springboot.ruon.domain.routine.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 유저에게 생성된 스킨케어 루틴 (ERD의 ROUTINE 테이블)
 * userId는 User 엔티티와 직접 연관관계를 맺지 않고 Long으로만 둠
 */
@Entity
@Table(name = "routine")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Routine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "routine_id")
    private Long routineId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "generated_at", nullable = false) //언제 만들어졌는지, 날짜 조회용
    private LocalDateTime generatedAt;

    @Column(name = "based_on_pregnancy_week")
    private Integer basedOnPregnancyWeek; // nullable: 임신 여부/주차 모르는 유저도 있을 수 있음

    @Lob
    @Column(name = "explanation")
    private String explanation; // LLM이 생성한 루틴 설명

    @Lob
    @Column(name = "recommended_action")
    private String recommendedAction; // LLM이 생성한 행동 추천

    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Step> steps = new ArrayList<>();

    private Routine(Long userId, Integer basedOnPregnancyWeek) {
        this.userId = userId;
        this.basedOnPregnancyWeek = basedOnPregnancyWeek;
        this.generatedAt = LocalDateTime.now();
    }

    public static Routine create(Long userId, Integer basedOnPregnancyWeek) {
        return new Routine(userId, basedOnPregnancyWeek);
    }

    public void addStep(Step step) {
        steps.add(step);
        step.assignRoutine(this);
    }

    public void applyLlmResult(String explanation, String recommendedAction) {
        this.explanation = explanation;
        this.recommendedAction = recommendedAction;
    }
}

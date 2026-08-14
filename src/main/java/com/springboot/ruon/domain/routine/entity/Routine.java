package com.springboot.ruon.domain.routine.entity;

import com.springboot.ruon.auth.data.entity.RoutineTimeAvailable;
import com.springboot.ruon.domain.routine.dto.request.SkinFeeling;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    // "오늘의 컨디션"에서 선택한 피부 느낌들을 SkinFeeling enum name으로 콤마 연결해 저장.
    // 값이 없으면(null/빈 문자열) 프론트에서 "기록이 없어요" 상태로 보여줌. List로 바로 꺼내려면 getSkinFeelings() 사용.
    @Column(name = "skin_feelings", length = 200)
    private String skinFeelingsRaw;

    @Column(name = "custom_feeling")
    private String customFeeling; // skinFeelings에 CUSTOM이 포함된 경우 직접 작성한 텍스트

    // 이 루틴을 생성할 당시 사용자가 선택했던 "오늘 사용 가능한 시간" 스냅샷.
    // User.routineTimeAvailable은 매일 덮어써지는 값이라 나중에 조회할 때 그때 뭘 선택했는지 알 수 없어서 별도 저장.
    @Enumerated(EnumType.STRING)
    @Column(name = "routine_time_available", length = 20)
    private RoutineTimeAvailable routineTimeAvailable;

    // "반응 기록"에서 사용자가 남긴 오늘 루틴 반응 점수 (1~5, nullable).
    // 이모지 5개 → 점수 매핑(왼쪽부터): 당기고 건조했어요=1, 따갑거나 붉어졌어요=2, 평소와 같아요=3, 편안했어요=4, 촉촉해졌어요=5.
    // 하루에 이 루틴 하나에 대해 반응 하나만 남기며, 재제출 시 덮어씀(upsert).
    @Column(name = "reaction_score")
    private Integer reactionScore;

    @Lob
    @Column(name = "explanation")
    private String explanation; // LLM이 생성한 루틴 설명

    @Lob
    @Column(name = "recommended_action")
    private String recommendedAction; // LLM이 생성한 행동 추천

    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Step> steps = new ArrayList<>();

    private Routine(Long userId, Integer basedOnPregnancyWeek, String skinFeelingsRaw, String customFeeling,
                     RoutineTimeAvailable routineTimeAvailable) {
        this.userId = userId;
        this.basedOnPregnancyWeek = basedOnPregnancyWeek;
        this.generatedAt = LocalDateTime.now();
        this.skinFeelingsRaw = skinFeelingsRaw;
        this.customFeeling = customFeeling;
        this.routineTimeAvailable = routineTimeAvailable;
    }

    public static Routine create(Long userId, Integer basedOnPregnancyWeek, List<SkinFeeling> skinFeelings,
                                  String customFeeling, RoutineTimeAvailable routineTimeAvailable) {
        return new Routine(userId, basedOnPregnancyWeek, joinSkinFeelings(skinFeelings), customFeeling,
                routineTimeAvailable);
    }

    private static String joinSkinFeelings(List<SkinFeeling> skinFeelings) {
        return skinFeelings == null || skinFeelings.isEmpty()
                ? null
                : skinFeelings.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    // 저장된 콤마 문자열을 List<SkinFeeling>으로 꺼내서 씀 (기록 없으면 빈 리스트)
    public List<SkinFeeling> getSkinFeelings() {
        if (skinFeelingsRaw == null || skinFeelingsRaw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(skinFeelingsRaw.split(","))
                .map(SkinFeeling::valueOf)
                .toList();
    }

    public void addStep(Step step) {
        steps.add(step);
        step.assignRoutine(this);
    }

    public void applyLlmResult(String explanation, String recommendedAction) {
        this.explanation = explanation;
        this.recommendedAction = recommendedAction;
    }

    // 반응 기록 저장/재제출(upsert). 유효성(1~5)은 컨트롤러/서비스 진입 시점에 이미 검증됨.
    public void applyReaction(Integer reactionScore) {
        this.reactionScore = reactionScore;
    }
}

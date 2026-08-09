package com.springboot.ruon.domain.routine.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "step")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Step {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "step_id")
    private Long stepId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    private Routine routine;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_of_day", nullable = false)
    private TimeOfDay timeOfDay;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private StepAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StepStatus status;

    private Step(Long productId, TimeOfDay timeOfDay, Integer stepOrder, StepAction action) {
        this.productId = productId;
        this.timeOfDay = timeOfDay;
        this.stepOrder = stepOrder;
        this.action = action;
        this.status = StepStatus.PENDING;
    }

    public static Step create(Long productId, TimeOfDay timeOfDay, Integer stepOrder, StepAction action) {
        return new Step(productId, timeOfDay, stepOrder, action);
    }

    void assignRoutine(Routine routine) {
        this.routine = routine;
    }

    public void changeStatus(StepStatus status) {
        this.status = status;
    }
}

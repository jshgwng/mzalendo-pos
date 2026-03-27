package com.joshuaogwang.mzalendopos.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import com.joshuaogwang.mzalendopos.entity.PromotionType;

@Data
public class PromotionRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private PromotionType type;

    private Double conditionValue;

    private Long conditionProductId;

    @Positive
    private double rewardValue;

    private Long rewardProductId;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;

    private int usageLimit;
}

package com.c0nnect.nutripick.domain.food.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "food_nutrition")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodNutrition {

    @Id
    private Long foodId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id")
    private Food food;

    @Column(name = "basis_value", precision = 10, scale = 3)
    private BigDecimal basisValue;

    @Column(name = "basis_unit", length = 20)
    private String basisUnit;

    @Column(name = "serving_text", length = 300)
    private String servingText;

    @Column(precision = 10, scale = 3)
    private BigDecimal energy;

    @Column(precision = 10, scale = 3)
    private BigDecimal carbohydrate;

    @Column(precision = 10, scale = 3)
    private BigDecimal protein;

    @Column(precision = 10, scale = 3)
    private BigDecimal fat;

    @Column(precision = 10, scale = 3)
    private BigDecimal sugar;

    @Column(precision = 10, scale = 3)
    private BigDecimal sodium;

    @Column(name = "saturated_fat", precision = 10, scale = 3)
    private BigDecimal saturatedFat;

    @Column(name = "trans_fat", precision = 10, scale = 3)
    private BigDecimal transFat;

    @Column(precision = 10, scale = 3)
    private BigDecimal cholesterol;

    @Column(precision = 10, scale = 3)
    private BigDecimal calcium;

    @Column(name = "dietary_fiber", precision = 10, scale = 3)
    private BigDecimal dietaryFiber;

    @Enumerated(EnumType.STRING)
    @Column(name = "missing_status", nullable = false, length = 20)
    private MissingStatus missingStatus = MissingStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "normalization_status", nullable = false, length = 20)
    private NormalizationStatus normalizationStatus = NormalizationStatus.UNAVAILABLE;

    @Column(name = "normalized_basis_value", precision = 10, scale = 3)
    private BigDecimal normalizedBasisValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "normalized_basis_unit", length = 20)
    private Unit normalizedBasisUnit;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private FoodNutrition(
            Food food, BigDecimal basisValue, String basisUnit, String servingText, BigDecimal energy, BigDecimal carbohydrate,
            BigDecimal protein, BigDecimal fat, BigDecimal sugar, BigDecimal sodium, BigDecimal saturatedFat,
            BigDecimal transFat, BigDecimal cholesterol, BigDecimal calcium, BigDecimal dietaryFiber,
            MissingStatus missingStatus, NormalizationStatus normalizationStatus, BigDecimal normalizedBasisValue,
            Unit normalizedBasisUnit
    ) {
        this.food = food;
        this.basisValue = basisValue;
        this.basisUnit = basisUnit;
        this.servingText = servingText;
        this.energy = energy;
        this.carbohydrate = carbohydrate;
        this.protein = protein;
        this.fat = fat;
        this.sugar = sugar;
        this.sodium = sodium;
        this.saturatedFat = saturatedFat;
        this.transFat = transFat;
        this.cholesterol = cholesterol;
        this.calcium = calcium;
        this.dietaryFiber = dietaryFiber;
        this.missingStatus = missingStatus;
        this.normalizationStatus = normalizationStatus;
        this.normalizedBasisValue = normalizedBasisValue;
        this.normalizedBasisUnit = normalizedBasisUnit;
    }

    public static FoodNutrition createLocal(
            Food food, BigDecimal basisValue, String basisUnit, String servingText, BigDecimal energy, BigDecimal carbohydrate,
            BigDecimal protein, BigDecimal fat, BigDecimal sugar, BigDecimal sodium, BigDecimal saturatedFat,
            BigDecimal transFat, BigDecimal cholesterol, BigDecimal calcium, BigDecimal dietaryFiber,
            BigDecimal normalizedBasisValue,
            Unit normalizedBasisUnit
    ){
        if (("g".equals(basisUnit) && normalizedBasisUnit == Unit.mL)
                || (("ml".equals(basisUnit) || "mL".equals(basisUnit)) && normalizedBasisUnit == Unit.g)) {
            normalizedBasisValue = null;
            normalizedBasisUnit = null;
        }

        MissingStatus missingStatus = determineMissingStatus(
                energy,
                carbohydrate,
                protein,
                fat,
                sugar,
                sodium,
                saturatedFat,
                transFat,
                cholesterol,
                calcium,
                dietaryFiber
        );

        NormalizationStatus normalizationStatus = determineNormalizationStatus(
                basisValue,
                basisUnit,
                normalizedBasisValue,
                normalizedBasisUnit,
                missingStatus
        );

        return new FoodNutrition(
                food,
                basisValue,
                basisUnit,
                servingText,
                energy,
                carbohydrate,
                protein,
                fat,
                sugar,
                sodium,
                saturatedFat,
                transFat,
                cholesterol,
                calcium,
                dietaryFiber,
                missingStatus,
                normalizationStatus,
                normalizedBasisValue,
                normalizedBasisUnit
        );
    }

    private static MissingStatus determineMissingStatus(BigDecimal...nutrients){
        int nullCount = 0;

        for(BigDecimal v : nutrients){
            if(v == null){
                nullCount++;
            }
        }

        if(nullCount==0){
            return MissingStatus.NONE;
        } else if (nullCount == nutrients.length) {
            return MissingStatus.ALL;
        } else{
            return MissingStatus.PARTIAL;
        }
    }

    private static NormalizationStatus determineNormalizationStatus(
            BigDecimal basisValue, String basisUnit, BigDecimal normalizedBasisValue, Unit normalizedBasisUnit, MissingStatus missingStatus
    ) {
        if (basisValue == null || basisValue.signum() <= 0) {
            return NormalizationStatus.UNAVAILABLE;
        }
        if(basisUnit == null){
            return NormalizationStatus.UNAVAILABLE;
        }
        if(normalizedBasisValue == null || normalizedBasisValue.signum() <= 0){
            return NormalizationStatus.UNAVAILABLE;
        }
        if(normalizedBasisUnit == null){
            return NormalizationStatus.UNAVAILABLE;
        }
        if(missingStatus == MissingStatus.ALL){
            return NormalizationStatus.UNAVAILABLE;
        }
        if(missingStatus == MissingStatus.PARTIAL){
            return NormalizationStatus.PARTIAL;
        }
        return NormalizationStatus.AVAILABLE;
    }
}

package com.c0nnect.nutripick.domain.comparison_list.entity.ListNutrient;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Locale;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComparisonListNutrientId implements Serializable {

    @Column(name = "list_id")
    private Long listId;

    @Column(name = "nutrient_code", length = 50, nullable = false)
    private String nutrientCode;

    public ComparisonListNutrientId(Long listId, NutrientCode nutrientCode){
        this.listId = listId;
        this.nutrientCode = nutrientCode.name().toLowerCase(Locale.ROOT);
    }
}

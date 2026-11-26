package com.c0nnect.nutripick.domain.comparison_list.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class ComparisonListItemId implements Serializable {

    @Column(name = "list_id")
    private Long listId;

    @Column(name = "food_id")
    private Long foodId;

    public ComparisonListItemId(Long listId, Long foodId) {
        this.listId = listId;
        this.foodId = foodId;
    }
}

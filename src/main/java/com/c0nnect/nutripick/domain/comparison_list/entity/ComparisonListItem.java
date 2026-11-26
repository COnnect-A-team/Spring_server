package com.c0nnect.nutripick.domain.comparison_list.entity;

import com.c0nnect.nutripick.domain.food.entity.Food;
import com.c0nnect.nutripick.global.error.CustomException;
import com.c0nnect.nutripick.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "comparison_list_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComparisonListItem {

    @EmbeddedId
    private ComparisonListItemId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("listId")
    @JoinColumn(name = "list_id", nullable = false)
    private ComparisonList comparisonList;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("foodId")
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    @CreationTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "added_at", nullable = false)
    private OffsetDateTime addedAt;

    private ComparisonListItem(ComparisonList list, Food food, short displayOrder){
        if(list == null || food == null){throw new CustomException(ErrorCode.INVALID_REQUEST);}
        this.comparisonList = list;
        this.food = food;
        this.displayOrder = displayOrder;
    }

    public static ComparisonListItem createLocal(ComparisonList list, Food food, short displayOrder){
        return new ComparisonListItem(
            list,
            food,
            displayOrder
        );
    }

}

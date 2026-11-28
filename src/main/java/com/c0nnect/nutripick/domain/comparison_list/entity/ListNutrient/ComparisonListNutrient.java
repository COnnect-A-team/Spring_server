package com.c0nnect.nutripick.domain.comparison_list.entity.ListNutrient;

import com.c0nnect.nutripick.domain.comparison_list.entity.ComparisonList;
import com.c0nnect.nutripick.domain.user.model.NutrientSelection;
import com.c0nnect.nutripick.global.error.CustomException;
import com.c0nnect.nutripick.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "comparison_list_nutrients")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComparisonListNutrient {

    @EmbeddedId
    private ComparisonListNutrientId id;

    @ManyToOne
    @MapsId("listId")
    @JoinColumn(name = "list_id", nullable = false)
    private ComparisonList comparisonList;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'NORMAL'")
    @Column(length = 10, nullable = false)
    private NutrientSelection.Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private NutrientSelection.Direction direction;

    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private ComparisonListNutrient(
            ComparisonList list, NutrientCode nutrientCode, NutrientSelection.Priority priority,
            NutrientSelection.Direction direction, short displayOrder
    ){
        this.comparisonList = list;
        this.id = new ComparisonListNutrientId(list.getId(), nutrientCode);
        this.priority = priority;
        this.direction = direction;
        this.displayOrder = displayOrder;
    }

    public static ComparisonListNutrient createLocal(
            ComparisonList list, NutrientCode nutrientCode, NutrientSelection.Priority priority,
            NutrientSelection.Direction direction, short displayOrder
    ){
        if(list == null || nutrientCode == null || direction == null){
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if(priority == null){
            priority = NutrientSelection.Priority.NORMAL;
        }

        return new ComparisonListNutrient(
                list,
                nutrientCode,
                priority,
                direction,
                displayOrder
        );
    }
}

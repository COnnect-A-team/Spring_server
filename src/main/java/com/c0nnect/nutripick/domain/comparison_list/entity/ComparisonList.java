package com.c0nnect.nutripick.domain.comparison_list.entity;

import com.c0nnect.nutripick.domain.food.model.Unit;
import com.c0nnect.nutripick.domain.user.entity.User;
import com.c0nnect.nutripick.global.error.CustomException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Getter
@Table(name = "comparison_lists")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComparisonList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "list_name", length = 100, nullable = false)
    private String listName;

    @Column(name = "sort_nutrient", length = 50)
    private String sortNutrient;

    @Column(name = "sort_direction", length = 10)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ASC'")
    private SortDirection sortDirection;

    @Column(name = "basis_value", precision = 10, scale = 3, nullable = false)
    @ColumnDefault("100")
    private BigDecimal basisValue = BigDecimal.valueOf(100);

    @Enumerated(EnumType.STRING)
    @Column(name = "basis_unit", nullable = false, length = 20)
    private Unit basisUnit;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private ComparisonList(
            User user , String listName, String sortNutrient, SortDirection sortDirection,
            BigDecimal basisValue, Unit basisUnit
    ){
      this.user = user;
      if(listName == null || listName.isBlank()){throw new IllegalArgumentException("비교 목록 이름은 비어 있을 수 없습니다.");}
      this.listName = listName;
      this.sortNutrient = sortNutrient;
      this.sortDirection = (sortDirection == null) ? SortDirection.ASC : sortDirection;
      this.basisValue = (basisValue == null) ? BigDecimal.valueOf(100) : basisValue;
      if(basisUnit == null){throw new IllegalArgumentException("비교 기준량 단위는 비어 있을 수 없습니다.");}
      this.basisUnit = basisUnit;
    }

    public static ComparisonList createLocal(
            User user, String listName, String sortNutrient, SortDirection sortDirection, BigDecimal basisValue, Unit basisUnit
    ){
        return new ComparisonList(
                user,
                listName,
                sortNutrient,
                sortDirection,
                basisValue,
                basisUnit
        );
    }

    public void setComparisonList(String listName, String sortNutrient, SortDirection direction, BigDecimal basisValue, Unit basisUnit){
        if(listName != null && !listName.isBlank()){
            this.listName = listName;
        }
        if(sortNutrient != null && !sortNutrient.isBlank()){
            this.sortNutrient = sortNutrient;
        }
        if(direction != null){
            this.sortDirection = direction;
        }
        if(basisValue != null){
            this.basisValue = basisValue;
        }
        if(basisUnit != null){
            this.basisUnit = basisUnit;
        }
    }

}

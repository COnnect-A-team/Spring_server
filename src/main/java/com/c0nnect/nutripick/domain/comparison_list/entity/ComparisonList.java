package com.c0nnect.nutripick.domain.comparison_list.entity;

import com.c0nnect.nutripick.domain.food.model.Unit;
import com.c0nnect.nutripick.domain.user.entity.User;
import com.c0nnect.nutripick.global.error.CustomException;
import com.c0nnect.nutripick.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

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

    @Column(name = "basis_value", precision = 10, scale = 3, nullable = false)
    @ColumnDefault("100")
    private BigDecimal basisValue = BigDecimal.valueOf(100);

    @Enumerated(EnumType.STRING)
    @Column(name = "basis_unit", nullable = false, length = 20)
    private Unit basisUnit;

    @CreationTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private ComparisonList(
            User user , String listName, BigDecimal basisValue, Unit basisUnit
    ){
      this.user = user;
      this.listName = listName;
      this.basisValue = basisValue;
      this.basisUnit = basisUnit;
    }

    public static ComparisonList createLocal(
            User user, String listName, BigDecimal basisValue, Unit basisUnit
    ){
        if(listName == null || listName.isBlank()){throw new CustomException(ErrorCode.INVALID_REQUEST);}

        basisValue = (basisValue == null) ? BigDecimal.valueOf(100) : basisValue;

        if(basisValue.setScale(3, RoundingMode.DOWN).signum() != 1 ){throw new CustomException(ErrorCode.INVALID_REQUEST);}

        if(basisUnit == null){throw new CustomException(ErrorCode.INVALID_REQUEST);}

        return new ComparisonList(
                user,
                listName,
                basisValue,
                basisUnit
        );
    }

    public void setComparisonList(String listName, BigDecimal basisValue, Unit basisUnit){
        if (basisValue != null && basisValue.setScale(3, RoundingMode.DOWN).signum() <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if(listName != null && !listName.isBlank()){
            this.listName = listName;
        }
        if(basisValue != null){
            this.basisValue = basisValue;
        }
        if(basisUnit != null){
            this.basisUnit = basisUnit;
        }
    }

}

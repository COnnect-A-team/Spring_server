package com.c0nnect.nutripick.domain.comparison_list.entity;

import com.c0nnect.nutripick.domain.user.entity.User;
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
    private String sortDirection;

    @Column(name = "basis_value", precision = 10, scale = 3, nullable = false)
    private BigDecimal basisValue;

    @Column(name = "basis_unit", nullable = false, length = 20)
    private String basisUnit;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

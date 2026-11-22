package com.c0nnect.nutripick.domain.food.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "foods")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "food_name", length = 500, nullable = false)
    private String foodName;

    @Column(name = "manufacturer_name", length = 300)
    private String manufacturerName;

    @Column(name = "food_category", length = 200)
    private String foodCategory;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private Food(String foodName, String manufacturerName, String foodCategory){
        this.foodName = foodName;
        this.manufacturerName = manufacturerName;
        this.foodCategory = foodCategory;
    }

    public static Food createLocal(String foodName, String manufacturerName, String foodCategory){
        return new Food(
                foodName,
                manufacturerName,
                foodCategory
        );
    }
}

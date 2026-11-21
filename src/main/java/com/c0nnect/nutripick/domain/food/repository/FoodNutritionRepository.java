package com.c0nnect.nutripick.domain.food.repository;

import com.c0nnect.nutripick.domain.food.entity.Food;
import com.c0nnect.nutripick.domain.food.entity.FoodNutrition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodNutritionRepository extends JpaRepository<FoodNutrition, Long> {
}

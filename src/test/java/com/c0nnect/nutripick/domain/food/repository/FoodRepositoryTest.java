package com.c0nnect.nutripick.domain.food.repository;

import com.c0nnect.nutripick.domain.food.entity.Food;
import com.c0nnect.nutripick.domain.food.entity.FoodNutrition;
import com.c0nnect.nutripick.domain.food.entity.MissingStatus;
import com.c0nnect.nutripick.domain.food.entity.NormalizationStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class FoodRepositoryTest {

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private FoodNutritionRepository foodNutritionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesFoodAndFindsItByGeneratedId() {
        Food food = Food.createLocal("테스트 식품", "테스트 제조사", "가공식품");

        Food savedFood = foodRepository.saveAndFlush(food);

        assertNotNull(savedFood.getId());

        entityManager.clear();
        Food foundFood = foodRepository.findById(savedFood.getId()).orElseThrow();

        assertEquals("테스트 식품", foundFood.getFoodName());
        assertEquals("테스트 제조사", foundFood.getManufacturerName());
        assertEquals("가공식품", foundFood.getFoodCategory());
        assertNotNull(foundFood.getCreatedAt());
        assertNotNull(foundFood.getUpdatedAt());
    }

    @Test
    void savesNutritionLinkedToFoodAndFindsItByFoodId() {
        Food savedFood = foodRepository.saveAndFlush(
                Food.createLocal("영양정보 테스트 식품", "테스트 제조사", "가공식품")
        );

        FoodNutrition nutrition = FoodNutrition.createLocal(
                savedFood,
                new BigDecimal("100.000"),
                "g",
                "100g당",
                new BigDecimal("250.000"),
                new BigDecimal("30.000"),
                new BigDecimal("12.500"),
                new BigDecimal("8.000"),
                new BigDecimal("4.000"),
                new BigDecimal("500.000"),
                new BigDecimal("2.000"),
                new BigDecimal("0.000"),
                new BigDecimal("10.000"),
                new BigDecimal("100.000"),
                new BigDecimal("3.000"),
                null,
                null
        );

        foodNutritionRepository.saveAndFlush(nutrition);
        entityManager.clear();

        FoodNutrition foundNutrition = foodNutritionRepository.findById(savedFood.getId()).orElseThrow();

        assertEquals(savedFood.getId(), foundNutrition.getFoodId());
        assertEquals(savedFood.getId(), foundNutrition.getFood().getId());
        assertEquals(new BigDecimal("100.000"), foundNutrition.getBasisValue());
        assertEquals(new BigDecimal("12.500"), foundNutrition.getProtein());
        assertEquals(MissingStatus.NONE, foundNutrition.getMissingStatus());
        assertEquals(NormalizationStatus.UNAVAILABLE, foundNutrition.getNormalizationStatus());
        assertNotNull(foundNutrition.getCreatedAt());
        assertNotNull(foundNutrition.getUpdatedAt());
        assertTrue(foodNutritionRepository.existsById(savedFood.getId()));
    }
}

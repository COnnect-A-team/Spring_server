package com.c0nnect.nutripick.domain.food.repository;

import com.c0nnect.nutripick.domain.food.entity.Food;
import com.c0nnect.nutripick.domain.food.entity.FoodNutrition;
import com.c0nnect.nutripick.domain.food.entity.MissingStatus;
import com.c0nnect.nutripick.domain.food.entity.NormalizationStatus;
import com.c0nnect.nutripick.domain.food.model.Unit;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void marksNutritionPartialWhenSomeNutrientValuesAreMissing() {
        Food savedFood = foodRepository.saveAndFlush(
                Food.createLocal("부분 누락 테스트 식품", "테스트 제조사", "가공식품")
        );
        FoodNutrition nutrition = createNutrition(savedFood, "g", null, new BigDecimal("100.000"), Unit.g);

        foodNutritionRepository.saveAndFlush(nutrition);
        entityManager.clear();

        FoodNutrition foundNutrition = foodNutritionRepository.findById(savedFood.getId()).orElseThrow();

        assertEquals(MissingStatus.PARTIAL, foundNutrition.getMissingStatus());
        assertEquals(NormalizationStatus.PARTIAL, foundNutrition.getNormalizationStatus());
        assertEquals("g", foundNutrition.getBasisUnit());
    }

    @Test
    void marksNutritionAvailableWhenRawAndNormalizedBasisCanBeMatched() {
        Food savedFood = foodRepository.saveAndFlush(
                Food.createLocal("환산 가능 테스트 식품", "테스트 제조사", "가공식품")
        );
        FoodNutrition nutrition = createNutrition(
                savedFood, "g", new BigDecimal("30.000"), new BigDecimal("100.000"), Unit.g
        );

        foodNutritionRepository.saveAndFlush(nutrition);
        entityManager.clear();

        FoodNutrition foundNutrition = foodNutritionRepository.findById(savedFood.getId()).orElseThrow();

        assertEquals(MissingStatus.NONE, foundNutrition.getMissingStatus());
        assertEquals(NormalizationStatus.AVAILABLE, foundNutrition.getNormalizationStatus());
        assertEquals("g", foundNutrition.getBasisUnit());
        assertEquals(Unit.g, foundNutrition.getNormalizedBasisUnit());
    }

    @Test
    void preservesOriginalUnitWhenNormalizationDataIsUnavailable() {
        Food savedFood = foodRepository.saveAndFlush(
                Food.createLocal("환산 불가 테스트 식품", "테스트 제조사", "가공식품")
        );
        FoodNutrition nutrition = createNutrition(
                savedFood, "개", new BigDecimal("30.000"), null, null
        );

        foodNutritionRepository.saveAndFlush(nutrition);
        entityManager.clear();

        FoodNutrition foundNutrition = foodNutritionRepository.findById(savedFood.getId()).orElseThrow();

        assertEquals("개", foundNutrition.getBasisUnit());
        assertEquals(NormalizationStatus.UNAVAILABLE, foundNutrition.getNormalizationStatus());
    }

    @Test
    void allowsNormalizedDataWhenOriginalUnitDiffersFromNormalizedUnit() {
        Food savedFood = foodRepository.saveAndFlush(
                Food.createLocal("단위 환산 테스트 식품", "테스트 제조사", "가공식품")
        );
        FoodNutrition nutrition = createNutrition(
                savedFood, "개", new BigDecimal("30.000"), new BigDecimal("100.000"), Unit.g
        );

        foodNutritionRepository.saveAndFlush(nutrition);
        entityManager.clear();

        FoodNutrition foundNutrition = foodNutritionRepository.findById(savedFood.getId()).orElseThrow();

        assertEquals("개", foundNutrition.getBasisUnit());
        assertEquals(Unit.g, foundNutrition.getNormalizedBasisUnit());
        assertEquals(NormalizationStatus.AVAILABLE, foundNutrition.getNormalizationStatus());
    }

    @Test
    void marksGramToMilliliterNormalizationUnavailableWhenDensityIsMissing() {
        assertMassVolumeNormalizationUnavailable("g", Unit.mL);
    }

    @Test
    void marksMilliliterToGramNormalizationUnavailableWhenDensityIsMissing() {
        assertMassVolumeNormalizationUnavailable("mL", Unit.g);
    }

    @Test
    void marksWhitespacePaddedGramToMilliliterNormalizationUnavailable() {
        assertMassVolumeNormalizationUnavailable(" g ", Unit.mL);
    }

    @Test
    void marksUppercaseMilliliterToGramNormalizationUnavailable() {
        assertMassVolumeNormalizationUnavailable("ML", Unit.g);
    }

    @Test
    void marksNormalizationUnavailableWhenOriginalUnitContainsOnlyWhitespace() {
        assertMassVolumeNormalizationUnavailable("   ", Unit.g);
    }

    @Test
    void marksNormalizationUnavailableWhenOriginalUnitIsNull() {
        assertMassVolumeNormalizationUnavailable(null, Unit.g);
    }

    @Test
    void keepsOriginalUnitUnchangedWhenCaseAndWhitespaceDoNotChangeUnitType() {
        Food food = Food.createLocal("공백 단위 테스트 식품", "테스트 제조사", "가공식품");
        FoodNutrition nutrition = createNutrition(
                food, " G ", new BigDecimal("30.000"), new BigDecimal("100.000"), Unit.g
        );

        assertEquals(" G ", nutrition.getBasisUnit());
        assertEquals(NormalizationStatus.AVAILABLE, nutrition.getNormalizationStatus());
        assertEquals(new BigDecimal("100.000"), nutrition.getNormalizedBasisValue());
        assertEquals(Unit.g, nutrition.getNormalizedBasisUnit());
    }

    private void assertMassVolumeNormalizationUnavailable(String originalUnit, Unit normalizedUnit) {
        Food savedFood = foodRepository.saveAndFlush(
                Food.createLocal("밀도 정보 없는 테스트 식품", "테스트 제조사", "가공식품")
        );
        FoodNutrition nutrition = createNutrition(
                savedFood, originalUnit, new BigDecimal("30.000"), new BigDecimal("100.000"), normalizedUnit
        );

        foodNutritionRepository.saveAndFlush(nutrition);
        entityManager.clear();

        FoodNutrition foundNutrition = foodNutritionRepository.findById(savedFood.getId()).orElseThrow();

        assertEquals(originalUnit, foundNutrition.getBasisUnit());
        assertEquals(NormalizationStatus.UNAVAILABLE, foundNutrition.getNormalizationStatus());
        assertNull(foundNutrition.getNormalizedBasisValue());
        assertNull(foundNutrition.getNormalizedBasisUnit());
    }

    private FoodNutrition createNutrition(
            Food food, String basisUnit, BigDecimal carbohydrate, BigDecimal normalizedBasisValue, Unit normalizedBasisUnit
    ) {
        return FoodNutrition.createLocal(
                food,
                new BigDecimal("100.000"),
                basisUnit,
                "100" + basisUnit + "당",
                new BigDecimal("250.000"),
                carbohydrate,
                new BigDecimal("12.500"),
                new BigDecimal("8.000"),
                new BigDecimal("4.000"),
                new BigDecimal("500.000"),
                new BigDecimal("2.000"),
                new BigDecimal("0.000"),
                new BigDecimal("10.000"),
                new BigDecimal("100.000"),
                new BigDecimal("3.000"),
                normalizedBasisValue,
                normalizedBasisUnit
        );
    }
}

package com.c0nnect.nutripick.domain.comparison_list.repository;

import com.c0nnect.nutripick.domain.comparison_list.entity.ListNutrient.ComparisonListNutrient;
import com.c0nnect.nutripick.domain.comparison_list.entity.ListNutrient.ComparisonListNutrientId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComparisonListNutrientRepository extends JpaRepository<ComparisonListNutrient, ComparisonListNutrientId> {
    List<ComparisonListNutrient> findByComparisonList_IdOrderByDisplayOrder(Long listId);
}

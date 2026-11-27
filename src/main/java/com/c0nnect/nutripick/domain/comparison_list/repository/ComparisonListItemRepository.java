package com.c0nnect.nutripick.domain.comparison_list.repository;

import com.c0nnect.nutripick.domain.comparison_list.entity.ListItem.ComparisonListItem;
import com.c0nnect.nutripick.domain.comparison_list.entity.ListItem.ComparisonListItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComparisonListItemRepository extends JpaRepository<ComparisonListItem, ComparisonListItemId> {
    List<ComparisonListItem> findAllByComparisonList_IdOrderByDisplayOrderAsc(Long listId);
}

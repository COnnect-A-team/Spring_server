package com.c0nnect.nutripick.domain.comparison_list.repository;

import com.c0nnect.nutripick.domain.comparison_list.entity.ComparisonList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComparisonListRepository extends JpaRepository<ComparisonList, Long> {
    @Query( value = """
            SELECT EXISTS (
                SELECT 1 FROM comparison_lists WHERE user_id =:userId
                    AND LOWER(REGEXP_REPLACE(list_name, '\\s+', '', 'g')) --문자열 내부의 공백을 모두 제거
                        = LOWER(REGEXP_REPLACE(:listName, '\\s+', '', 'g'))
                )
    """, nativeQuery = true)
    boolean existsByUser_IdAndListName(
            @Param("userId") Long userId,
            @Param("listName") String listName
    );

    List<ComparisonList> findAllByUser_IdOrderByUpdatedAtDesc(Long userId);
}

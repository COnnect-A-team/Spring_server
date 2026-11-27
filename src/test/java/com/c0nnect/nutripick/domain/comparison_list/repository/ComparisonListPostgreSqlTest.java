package com.c0nnect.nutripick.domain.comparison_list.repository;

import com.c0nnect.nutripick.domain.comparison_list.entity.ComparisonList;
import com.c0nnect.nutripick.domain.comparison_list.entity.ListItem.ComparisonListItem;
import com.c0nnect.nutripick.domain.comparison_list.entity.ListItem.ComparisonListItemId;
import com.c0nnect.nutripick.domain.comparison_list.entity.ListNutrient.ComparisonListNutrient;
import com.c0nnect.nutripick.domain.comparison_list.entity.ListNutrient.ComparisonListNutrientId;
import com.c0nnect.nutripick.domain.comparison_list.entity.ListNutrient.NutrientCode;
import com.c0nnect.nutripick.domain.food.entity.Food;
import com.c0nnect.nutripick.domain.food.model.Unit;
import com.c0nnect.nutripick.domain.food.repository.FoodRepository;
import com.c0nnect.nutripick.domain.user.entity.User;
import com.c0nnect.nutripick.domain.user.model.NutrientSelection.Direction;
import com.c0nnect.nutripick.domain.user.model.NutrientSelection.Priority;
import com.c0nnect.nutripick.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.defer-datasource-initialization=true",
        "spring.sql.init.mode=always",
        "spring.sql.init.platform=postgresql"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class ComparisonListPostgreSqlTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    private ComparisonListRepository comparisonLists;

    @Autowired
    private UserRepository users;

    @Autowired
    private FoodRepository foods;

    @Autowired
    private ComparisonListItemRepository comparisonItems;

    @Autowired
    private ComparisonListNutrientRepository comparisonNutrients;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findsExistingNameForSameUserIgnoringCaseAndWhitespace() {
        User owner = saveUser("owner@example.com");
        User otherUser = saveUser("other@example.com");
        saveList(owner, " Protein 간식 비교 ");

        assertTrue(comparisonLists.existsByUser_IdAndListName(owner.getId(), "protein\t간식비교"));
        assertFalse(comparisonLists.existsByUser_IdAndListName(otherUser.getId(), "protein\t간식비교"));
        assertFalse(comparisonLists.existsByUser_IdAndListName(owner.getId(), "다른 비교 목록"));
    }

    @Test
    void databaseRejectsNormalizedDuplicateOnlyForSameUser() {
        User owner = saveUser("owner@example.com");
        User otherUser = saveUser("other@example.com");
        saveList(owner, " Protein 간식 비교 ");

        assertNotNull(saveList(otherUser, "protein\t간식비교").getId());

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> saveList(owner, "protein\t간식비교")
        );
        SQLException cause = assertInstanceOf(SQLException.class, exception.getMostSpecificCause());
        assertEquals("23505", cause.getSQLState());
        assertTrue(cause.getMessage().contains("uq_comparison_lists_user_name"));
    }

    @Test
    void savesFoodsAndReadsOnlySelectedListInDisplayOrder() {
        User owner = saveUser("owner@example.com");
        ComparisonList list = saveList(owner, "아침 비교");
        ComparisonList otherList = saveList(owner, "저녁 비교");
        Food firstFood = foods.save(Food.createLocal("두부", null, null));
        Food secondFood = foods.save(Food.createLocal("우유", null, null));

        comparisonItems.saveAllAndFlush(List.of(
                ComparisonListItem.createLocal(list, secondFood, (short) 2),
                ComparisonListItem.createLocal(list, firstFood, (short) 1),
                ComparisonListItem.createLocal(otherList, firstFood, (short) 1)
        ));
        entityManager.clear();

        List<ComparisonListItem> items = comparisonItems.findAllByComparisonList_IdOrderByDisplayOrderAsc(list.getId());

        assertEquals(List.of(
                new ComparisonListItemId(list.getId(), firstFood.getId()),
                new ComparisonListItemId(list.getId(), secondFood.getId())
        ), items.stream().map(ComparisonListItem::getId).toList());
        assertEquals(List.of(firstFood.getId(), secondFood.getId()),
                items.stream().map(item -> item.getFood().getId()).toList());
        for (ComparisonListItem item : items) {
            assertEquals(list.getId(), item.getComparisonList().getId());
            assertEquals(owner.getId(), item.getComparisonList().getUser().getId());
            assertNotNull(item.getAddedAt());
        }
    }

    @Test
    void savesNutrientSettingsAndReadsOnlySelectedListInDisplayOrder() {
        User owner = saveUser("owner@example.com");
        ComparisonList list = saveList(owner, "아침 비교");
        ComparisonList otherList = saveList(owner, "저녁 비교");

        comparisonNutrients.saveAllAndFlush(List.of(
                ComparisonListNutrient.createLocal(list, NutrientCode.SATURATED_FAT, Priority.HIGH,
                        Direction.LOWER, (short) 2),
                ComparisonListNutrient.createLocal(list, NutrientCode.PROTEIN, null, Direction.HIGHER, (short) 1),
                ComparisonListNutrient.createLocal(otherList, NutrientCode.PROTEIN, Priority.LOW,
                        Direction.HIGHER, (short) 1)
        ));
        entityManager.clear();

        List<ComparisonListNutrient> nutrients = comparisonNutrients.findByComparisonList_IdOrderByDisplayOrder(list.getId());

        assertEquals(List.of(
                new ComparisonListNutrientId(list.getId(), NutrientCode.PROTEIN),
                new ComparisonListNutrientId(list.getId(), NutrientCode.SATURATED_FAT)
        ), nutrients.stream().map(ComparisonListNutrient::getId).toList());
        assertEquals(List.of(Priority.NORMAL, Priority.HIGH),
                nutrients.stream().map(ComparisonListNutrient::getPriority).toList());
        assertEquals(List.of(Direction.HIGHER, Direction.LOWER),
                nutrients.stream().map(ComparisonListNutrient::getDirection).toList());
        for (ComparisonListNutrient nutrient : nutrients) {
            assertEquals(list.getId(), nutrient.getComparisonList().getId());
            assertNotNull(nutrient.getCreatedAt());
            assertNotNull(nutrient.getUpdatedAt());
        }

        List<String> storedCodes = jdbcTemplate.queryForList("""
                SELECT nutrient_code
                FROM comparison_list_nutrients
                WHERE list_id = ?
                ORDER BY display_order
                """, String.class, list.getId());
        assertEquals(List.of("protein", "saturated_fat"), storedCodes);
    }

    private User saveUser(String email) {
        return users.saveAndFlush(User.createLocal(email, "test-password-hash", "테스트 사용자"));
    }

    private ComparisonList saveList(User user, String name) {
        return comparisonLists.saveAndFlush(ComparisonList.createLocal(user, name, null, null, null, Unit.g));
    }
}

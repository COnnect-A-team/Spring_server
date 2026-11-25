package com.c0nnect.nutripick.domain.comparison_list.repository;

import com.c0nnect.nutripick.domain.comparison_list.entity.ComparisonList;
import com.c0nnect.nutripick.domain.food.model.Unit;
import com.c0nnect.nutripick.domain.user.entity.User;
import com.c0nnect.nutripick.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.SQLException;

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

    private User saveUser(String email) {
        return users.saveAndFlush(User.createLocal(email, "test-password-hash", "테스트 사용자"));
    }

    private ComparisonList saveList(User user, String name) {
        return comparisonLists.saveAndFlush(ComparisonList.createLocal(user, name, null, null, null, Unit.g));
    }
}

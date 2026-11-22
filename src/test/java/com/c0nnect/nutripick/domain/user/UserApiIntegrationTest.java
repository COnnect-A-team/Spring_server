package com.c0nnect.nutripick.domain.user;

import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c0nnect.nutripick.domain.user.entity.User;
import com.c0nnect.nutripick.domain.user.repository.UserRepository;
import com.c0nnect.nutripick.global.security.AccessTokenIssuer;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class UserApiIntegrationTest {
    private static final String ME = "/api/v1/users/me";
    private static final String NAME = ME + "/name";
    private static final String ONBOARDING = ME + "/onboarding";
    private static final String PROTEIN = preference("protein", "HIGH", "HIGHER", 1);
    private static final String SUGAR = preference("sugar", "HIGH", "LOWER", 2);

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AccessTokenIssuer tokens;
    private User owner;
    private String authorization;

    @BeforeEach
    void createAccount() {
        owner = account("기존 이름");
        authorization = "Bearer " + tokens.issue(owner.getId());
    }

    @Test
    void newAccountHasNoPreferencesAndNameChangeAffectsOnlyTheTokenOwner() throws Exception {
        User other = account("다른 이름");
        String otherId = other.getId().toString();

        mvc.perform(get(ME).queryParam("userId", otherId).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(owner.getId()))
                .andExpect(jsonPath("$.email").value(owner.getEmail()))
                .andExpect(jsonPath("$.name").value("기존 이름"))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.tutorialCompleted").value(false));
        expectOnboarding(authorization, onboarding("기존 이름", false));

        mvc.perform(patch(NAME).queryParam("userId", otherId).header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"바꾼 이름\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"name\":\"바꾼 이름\"}", STRICT));
        expectOnboarding(authorization, onboarding("바꾼 이름", false));
        expectOnboarding("Bearer " + tokens.issue(other.getId()), onboarding("다른 이름", false));
    }

    @Test
    void onboardingReplacesTheOwnersPreferencesAndReturnsThemInSelectionOrder() throws Exception {
        User other = account("다른 이름");
        String saved = onboarding("온보딩", true, PROTEIN, SUGAR);
        mvc.perform(putOnboarding(onboarding("온보딩", true, SUGAR, PROTEIN))
                        .queryParam("userId", other.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(content().json(saved, STRICT));
        expectOnboarding(authorization, saved);

        String sodium = preference("sodium", "NORMAL", "LOWER", 1);
        String protein = preference("protein", "LOW", "HIGHER", 2);
        String withoutPriority = "{\"nutrientCode\":\"sodium\",\"direction\":\"LOWER\",\"selectionOrder\":1}";
        String replacement = onboarding("새 이름", false, sodium, protein);
        mvc.perform(putOnboarding(onboarding("새 이름", false, protein, withoutPriority))
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(content().json(replacement, STRICT));
        expectOnboarding(authorization, replacement);
        expectOnboarding("Bearer " + tokens.issue(other.getId()), onboarding("다른 이름", false));
    }

    @ParameterizedTest
    @MethodSource("invalidSelections")
    void invalidSelectionLeavesTheNameTutorialAndExistingPreferencesIntact(String preferences) throws Exception {
        String baseline = onboarding("저장된 이름", true, PROTEIN, SUGAR);
        mvc.perform(putOnboarding(baseline).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk());
        mvc.perform(putOnboarding(onboarding("롤백할 이름", false, preferences))
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("UNPROCESSABLE_ENTITY"));
        expectOnboarding(authorization, baseline);
    }

    static Stream<String> invalidSelections() {
        return Stream.of(
                "",
                PROTEIN,
                String.join(",", PROTEIN, SUGAR, preference("sodium", "HIGH", "LOWER", 2)),
                String.join(",", PROTEIN, preference("protein", "LOW", "LOWER", 2)),
                String.join(",", PROTEIN, preference("sugar", "HIGH", "LOWER", 1)),
                String.join(",", PROTEIN, preference("sugar", "HIGH", "LOWER", 3))
        );
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void invalidRequestFieldsReturnBadRequest(String body) throws Exception {
        mvc.perform(putOnboarding(body).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    static Stream<String> invalidRequests() {
        return Stream.of(
                "{\"name\":\"학생\",\"nutrientPreferences\":[" + PROTEIN + "," + SUGAR + "]}",
                "{\"name\":\"학생\",\"tutorialCompleted\":true}",
                "{\"name\":\"학생\",\"tutorialCompleted\":true,\"nutrientPreferences\":{}}",
                onboarding("학생", true, SUGAR, PROTEIN.replace("\"selectionOrder\":1", "\"selectionOrder\":1.5")),
                onboarding("학생", true, SUGAR, PROTEIN.replace("\"priority\":\"HIGH\"", "\"priority\":0")),
                onboarding("학생", true, SUGAR, preference("protein", "UNKNOWN", "HIGHER", 1)),
                onboarding("학생", true, SUGAR, preference("protein", "HIGH", "UNKNOWN", 1)),
                onboarding("학생", true, SUGAR, preference(" ", "HIGH", "HIGHER", 1)),
                onboarding("학생", true, SUGAR, preference("x".repeat(51), "HIGH", "HIGHER", 1))
        );
    }

    @Test
    void blankOrOversizedNameDoesNotChangeTheProfile() throws Exception {
        for (String name : List.of(" ", "가".repeat(101))) {
            mvc.perform(patch(NAME).header(HttpHeaders.AUTHORIZATION, authorization)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
        expectOnboarding(authorization, onboarding("기존 이름", false));
    }

    @Test
    void allUserRoutesRequireAuthenticationAndAnExistingAccount() throws Exception {
        String missingAccount = "Bearer " + tokens.issue(Long.MAX_VALUE);
        for (MockHttpServletRequestBuilder request : List.of(get(ME), get(ONBOARDING),
                patch(NAME).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"새 이름\"}"),
                putOnboarding(onboarding("새 이름", true, PROTEIN, SUGAR)))) {
            mvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
            mvc.perform(request.header(HttpHeaders.AUTHORIZATION, missingAccount))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    private User account(String name) {
        User user = User.createLocal(UUID.randomUUID() + "@example.test", "test-hash", name);
        user.verifyEmail();
        return users.saveAndFlush(user);
    }

    private void expectOnboarding(String bearer, String expected) throws Exception {
        mvc.perform(get(ONBOARDING).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, STRICT));
    }

    private static MockHttpServletRequestBuilder putOnboarding(String body) {
        return put(ONBOARDING).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String onboarding(String name, boolean completed, String... preferences) {
        return """
                {"name":"%s","tutorialCompleted":%s,"nutrientPreferences":[%s]}
                """.formatted(name, completed, String.join(",", preferences));
    }

    private static String preference(String code, String priority, String direction, int order) {
        return """
                {"nutrientCode":"%s","priority":"%s","direction":"%s","selectionOrder":%d}
                """.formatted(code, priority, direction, order);
    }
}

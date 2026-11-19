package com.c0nnect.nutripick.global.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GlobalExceptionHandlerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void customExceptionUsesItsStatusCodeAndDetails() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.details.resourceId").value(12));
    }

    @Test
    void notFoundExceptionReturns404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void invalidRequestBodyReturns400() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details.fieldErrors[0].field").value("name"));
    }

    @Test
    void missingRequiredParameterReturns400() throws Exception {
        mockMvc.perform(get("/test/required"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details.parameter").value("keyword"));
    }

    @Test
    void parameterWithInvalidTypeReturns400() throws Exception {
        mockMvc.perform(get("/test/query").param("page", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details.parameter").value("page"));
    }

    @Test
    void unexpectedExceptionReturnsGeneric500() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("예기치 않은 서버 내부 오류"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))
                ));
    }

    @RestController
    static class TestController {
        @GetMapping("/test/conflict")
        String conflict() {
            throw new CustomException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    Map.of("resourceId", 12)
            );
        }

        @GetMapping("/test/not-found")
        String notFound() {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        @PostMapping("/test/validation")
        String validation(@Valid @RequestBody TestRequest request) {
            return request.name();
        }

        @GetMapping("/test/required")
        String required(@RequestParam String keyword) {
            return keyword;
        }

        @GetMapping("/test/query")
        String query(@RequestParam int page) {
            return Integer.toString(page);
        }

        @GetMapping("/test/unexpected")
        String unexpected() {
            throw new IllegalStateException("secret internal details");
        }
    }

    record TestRequest(@NotBlank String name) {
    }
}

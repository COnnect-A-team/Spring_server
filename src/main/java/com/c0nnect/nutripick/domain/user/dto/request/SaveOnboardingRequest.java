package com.c0nnect.nutripick.domain.user.dto.request;

import com.c0nnect.nutripick.domain.user.model.NutrientSelection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveOnboardingRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Boolean tutorialCompleted,
        @NotNull List<@NotNull @Valid NutrientSelection> nutrientPreferences
) {}

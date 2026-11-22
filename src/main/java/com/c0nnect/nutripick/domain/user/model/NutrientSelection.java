package com.c0nnect.nutripick.domain.user.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record NutrientSelection(
        @NotBlank @Size(max = 50) String nutrientCode,
        Priority priority,
        @NotNull Direction direction,
        @NotNull Integer selectionOrder
) {
    public NutrientSelection {
        if (nutrientCode != null) {
            nutrientCode = nutrientCode.strip().toLowerCase(Locale.ROOT);
        }
        if (priority == null) {
            priority = Priority.NORMAL;
        }
    }

    public enum Priority { LOW, NORMAL, HIGH }

    public enum Direction { HIGHER, LOWER }
}

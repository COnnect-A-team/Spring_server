package com.c0nnect.nutripick.domain.user.dto.response;

import com.c0nnect.nutripick.domain.user.entity.User;
import com.c0nnect.nutripick.domain.user.model.NutrientSelection;
import java.util.List;

public record OnboardingResponse(String name, boolean tutorialCompleted, List<NutrientSelection> nutrientPreferences) {
    public static OnboardingResponse from(User user) {
        return new OnboardingResponse(user.getName(), user.isTutorialCompleted(), user.selectionsInOrder());
    }
}

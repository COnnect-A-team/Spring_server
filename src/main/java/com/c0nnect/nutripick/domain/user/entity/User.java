package com.c0nnect.nutripick.domain.user.entity;

import com.c0nnect.nutripick.domain.user.model.NutrientSelection;
import com.c0nnect.nutripick.global.error.CustomException;
import com.c0nnect.nutripick.global.error.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "google_id")
    private String googleId;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "tutorial_completed", nullable = false)
    private boolean tutorialCompleted;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKey(name = "nutrientCode")
    @Getter(AccessLevel.NONE)
    private Map<String, UserNutrientPreference> nutrientPreferences = new HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private User(String email, String passwordHash, String name) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public List<NutrientSelection> selectionsInOrder() {
        return nutrientPreferences.values().stream()
                .map(UserNutrientPreference::toSelection)
                .sorted(Comparator.comparingInt(NutrientSelection::selectionOrder))
                .toList();
    }

    public void updateOnboarding(String name, boolean completed, List<NutrientSelection> selections) {
        Set<String> codes = selections.stream().map(NutrientSelection::nutrientCode).collect(Collectors.toSet());
        Set<Integer> order = selections.stream().map(NutrientSelection::selectionOrder).collect(Collectors.toSet());
        if (selections.size() != 2 || codes.size() != 2 || !order.equals(Set.of(1, 2))) {
            throw new CustomException(ErrorCode.UNPROCESSABLE_ENTITY,
                    Map.of("nutrientPreferences", "서로 다른 영양소 두 개에 선택 순서 1과 2를 지정해 주세요."));
        }

        changeName(name);
        tutorialCompleted = completed;
        nutrientPreferences.keySet().retainAll(codes);
        for (NutrientSelection selection : selections) {
            UserNutrientPreference preference = nutrientPreferences.computeIfAbsent(
                    selection.nutrientCode(), code -> new UserNutrientPreference(this, code));
            preference.update(selection);
        }
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public static User createLocal(String email, String passwordHash, String name) {
        int domainStart = email.lastIndexOf('@') + 1;
        String domain = email.substring(domainStart).toLowerCase(Locale.ROOT);
        return new User(email.substring(0, domainStart) + domain, passwordHash, name);
    }
}

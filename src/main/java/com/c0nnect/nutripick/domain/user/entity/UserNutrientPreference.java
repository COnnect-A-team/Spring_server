package com.c0nnect.nutripick.domain.user.entity;

import com.c0nnect.nutripick.domain.user.model.NutrientSelection;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "user_nutrient_preferences", check = @CheckConstraint(
        name = "ck_preference_selection_order", constraint = "selection_order between 1 and 2"))
@IdClass(UserNutrientPreference.Key.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNutrientPreference {
    public record Key(Long user, String nutrientCode) implements Serializable {}

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @Column(name = "nutrient_code", nullable = false, length = 50)
    private String nutrientCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @ColumnDefault("'NORMAL'")
    private NutrientSelection.Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NutrientSelection.Direction direction;

    @Column(name = "selection_order", nullable = false)
    private short selectionOrder;

    @CreationTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    UserNutrientPreference(User user, String nutrientCode) {
        this.user = user;
        this.nutrientCode = nutrientCode;
    }

    void update(NutrientSelection selection) {
        priority = selection.priority();
        direction = selection.direction();
        selectionOrder = selection.selectionOrder().shortValue();
    }

    NutrientSelection toSelection() {
        return new NutrientSelection(nutrientCode, priority, direction, (int) selectionOrder);
    }
}

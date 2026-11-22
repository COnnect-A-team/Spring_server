package com.c0nnect.nutripick.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Locale;
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

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public static User createLocal(String email, String passwordHash, String name) {
        int domainStart = email.lastIndexOf('@') + 1;
        String domain = email.substring(domainStart).toLowerCase(Locale.ROOT);
        return new User(email.substring(0, domainStart) + domain, passwordHash, name);
    }
}

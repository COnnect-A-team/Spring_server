package com.c0nnect.nutripick.domain.user.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name="users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name="password_hash")
    private String passwordHash;

    @Column(name="google_id")
    private String googleId;

    @Column(length=100, nullable = false)
    private String name;

    @Column(name="email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name="tutorial_completed", nullable = false)
    private boolean tutorialCompleted;

    @CreationTimestamp
    @Column(name="created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name="updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private User(String email, String passwordHash, String name){
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
    }

    public static User createLocal(
            String email, String passwordHash, String name
    ) {
      return new User(
              email,
              passwordHash,
              name
      );
    }

}


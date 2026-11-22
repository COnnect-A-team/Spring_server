package com.c0nnect.nutripick.domain.user.repository;

import com.c0nnect.nutripick.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "nutrientPreferences")
    Optional<User> findWithPreferencesById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findForUpdateById(Long id);

    default Optional<User> findByEmail(String email) {
        return uniqueAccount(findTop2ByEmailStartingWithAndEmailIgnoreCase(localPrefix(email), email));
    }

    default Optional<User> findForVerificationByEmail(String email) {
        return uniqueAccount(findTop2ForVerificationByEmailStartingWithAndEmailIgnoreCase(localPrefix(email), email));
    }

    default boolean existsByEmail(String email) {
        return !findTop2ByEmailStartingWithAndEmailIgnoreCase(localPrefix(email), email).isEmpty();
    }

    List<User> findTop2ByEmailStartingWithAndEmailIgnoreCase(String prefix, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<User> findTop2ForVerificationByEmailStartingWithAndEmailIgnoreCase(String prefix, String email);

    private static String localPrefix(String email) {
        return email.substring(0, email.lastIndexOf('@') + 1);
    }

    // A legacy database can contain two rows that differ only in domain case.
    private static Optional<User> uniqueAccount(List<User> accounts) {
        return accounts.size() == 1 ? Optional.of(accounts.getFirst()) : Optional.empty();
    }
}

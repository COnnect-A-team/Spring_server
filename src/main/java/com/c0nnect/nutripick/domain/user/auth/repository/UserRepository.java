package com.c0nnect.nutripick.domain.user.auth.repository;

import com.c0nnect.nutripick.domain.user.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    String findByEmail(String email);
    boolean existsByEmail(String email);
}

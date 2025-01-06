package com.example.TestProject.repo;

import com.example.TestProject.entity.Erole;
import com.example.TestProject.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsernameAndPassword(String username, String password);
    boolean existsByUsername(String username);
    @Query("SELECT u.role FROM UserEntity u WHERE u.email = :email")
    Optional<Erole> findRoleByEmail(@Param("email") String email);
}

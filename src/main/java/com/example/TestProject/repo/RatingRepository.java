package com.example.TestProject.repo;

import com.example.TestProject.entity.Rating;
import com.example.TestProject.entity.University;
import com.example.TestProject.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    long countByUniversity(University university);
    boolean existsByUserAndUniversity(UserEntity user, University university);
    Optional<Rating> findByUserAndUniversity(UserEntity user, University university);
    List<Rating> findByUniversity(University university);
}

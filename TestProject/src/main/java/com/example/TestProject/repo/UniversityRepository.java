package com.example.TestProject.repo;

import com.example.TestProject.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UniversityRepository extends JpaRepository<University, Long> {
    Optional<University> findByName(String name);
    Optional<University> findById(long id);
}

package com.example.TestProject.repo;

import com.example.TestProject.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findAll();
    void deleteByFileName(String fileName);
    List<FileEntity> findAllByUniversityId(Long universityId);
    boolean existsByFileNameAndUniversityIdAndSemester(String fileName, Long universityId, Integer semester);
}

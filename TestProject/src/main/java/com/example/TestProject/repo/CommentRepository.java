package com.example.TestProject.repo;

import com.example.TestProject.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPageIdOrderByCreatedAtDesc(Long pageId);
}

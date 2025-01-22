package com.example.TestProject.repo;

import com.example.TestProject.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> { //This interface extends the JpaRepository interface and specifies the type of the entity and the type of the primary key
    //We find all comments by pageId and sort them by createdAt in descending order
    List<Comment> findByPageIdOrderByCreatedAtDesc(Long pageId);
}

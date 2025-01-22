package com.example.TestProject.service;

import com.example.TestProject.dto.CommentDTO;
import com.example.TestProject.entity.Comment;
import com.example.TestProject.entity.UserEntity;
import com.example.TestProject.repo.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;

    @Autowired
    public CommentService(CommentRepository commentRepository) { //constructor injection
        this.commentRepository = commentRepository;
    }

    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId); //from JpaRepository
    }

    public CommentDTO createComment(String content, Long pageId, UserEntity user) {
        Comment comment = new Comment(); //create a new Comment object
        comment.setContent(content); //set the content field with the value passed as an argument
        comment.setUser(user); //set the user field with the value passed as an argument
        comment.setPageId(pageId); //set the pageId field with the value passed as an argument
        comment.setCreatedAt(LocalDateTime.now()); //update the createdAt field with the current date and time

        Comment savedComment = commentRepository.save(comment); //save the comment to the database
        CommentDTO commentDTO = convertToDTO(savedComment); //convert the saved comment to a DTO
        return commentDTO; //return the DTO
    }

    private CommentDTO convertToDTO(Comment comment) { //class dto to convert entity to dto
        CommentDTO dto = new CommentDTO(); //dto object
        dto.setId(comment.getId()); //set the id field with the value from the comment object
        dto.setContent(comment.getContent()); //set the content field with the value from the comment object
        dto.setUsername(comment.getUser().getUsername());//set the username field with the value from the comment object
        dto.setCreatedAt(comment.getCreatedAt());//set the createdAt field with the value from the comment object
        dto.setPageId(comment.getPageId());//set the pageId field with the value from the comment object
        return dto;
    }

    public List<CommentDTO> getCommentsByPageId(Long pageId) { //method to get all comments by pageId
        return commentRepository.findByPageIdOrderByCreatedAtDesc(pageId) //from JpaRepository and sort them by createdAt in descending order
                .stream() //convert the list to a stream
                .map(this::convertToDTO) //convert each comment to a DTO
                .collect(Collectors.toList()); //collect the DTOs into a list
    }
}
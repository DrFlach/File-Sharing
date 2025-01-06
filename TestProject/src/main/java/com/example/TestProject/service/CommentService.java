package com.example.TestProject.service;

import com.example.TestProject.dto.CommentDTO;
import com.example.TestProject.entity.Comment;
import com.example.TestProject.entity.UserEntity;
import com.example.TestProject.repo.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    //private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public CommentService(CommentRepository commentRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.commentRepository = commentRepository;
        //this.messagingTemplate = messagingTemplate;
    }

    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    public CommentDTO createComment(String content, Long pageId, UserEntity user) {
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setUser(user);
        comment.setPageId(pageId);
        comment.setCreatedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        CommentDTO commentDTO = convertToDTO(savedComment);

        // Отправляем уведомление через WebSocket
//        messagingTemplate.convertAndSend(
//                "/topic/comments/" + pageId,
//                commentDTO
//        );

        return commentDTO;
    }

    private CommentDTO convertToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setUsername(comment.getUser().getUsername());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setPageId(comment.getPageId());
        return dto;
    }

    public List<CommentDTO> getCommentsByPageId(Long pageId) {
        return commentRepository.findByPageIdOrderByCreatedAtDesc(pageId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}
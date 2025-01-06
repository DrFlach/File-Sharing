package com.example.TestProject.controller;

import com.example.TestProject.dto.CommentDTO;
import com.example.TestProject.dto.CommentMessageDTO;
import com.example.TestProject.entity.Erole;
import com.example.TestProject.entity.UserEntity;
import com.example.TestProject.security.UserDetailsImpl;
import com.example.TestProject.service.CommentService;
import com.example.TestProject.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;

@Controller
public class CommentController {
    private final CommentService commentService;
    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);
    private final UserService userService;

    @Autowired
    public CommentController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    @MessageExceptionHandler
    public void handleException(Throwable exception) {
        logger.error("Error processing message", exception);
    }

    @MessageMapping("/comment/{pageId}")
    @SendTo("/topic/comments/{pageId}")
    public CommentDTO processComment(@DestinationVariable Long pageId,
                                     @Payload CommentMessageDTO message,
                                     Principal principal) {
        logger.info("Processing comment for pageId: {} from user: {}", pageId, principal.getName());
        try {
            if (principal == null) {
                logger.error("User not authenticated");
                throw new IllegalStateException("User not authenticated");
            }

            String userEmail = principal.getName();
            UserEntity user = userService.findByEmail(userEmail);

            if (user == null) {
                throw new UsernameNotFoundException("User not found");
            }

            return commentService.createComment(message.getContent(), pageId, user);
        } catch (Exception e) {
            logger.error("Error processing comment", e);
            throw e;
        }
    }

    @GetMapping("/api/comments/{pageId}")
    @ResponseBody
    public List<CommentDTO> getComments(@PathVariable Long pageId) {
        logger.info("Getting comments for page: {}", pageId);
        List<CommentDTO> comments = commentService.getCommentsByPageId(pageId);
        logger.info("Found {} comments", comments.size());
        return comments;
    }

    @DeleteMapping("/api/comments/{commentId}")
    @ResponseBody
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId, Principal principal) {
        logger.info("Attempting to delete comment: {}", commentId);
        try {
            String userEmail = principal.getName();
            UserEntity user = userService.findByEmail(userEmail);

            if (user == null || user.getRole() != Erole.ADMIN_ROLE) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can delete comments");
            }

            commentService.deleteComment(commentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting comment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting comment");
        }
    }
}
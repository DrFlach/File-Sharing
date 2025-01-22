package com.example.TestProject.controller;

import com.example.TestProject.dto.CommentDTO;
import com.example.TestProject.dto.CommentMessageDTO;
import com.example.TestProject.entity.Erole;
import com.example.TestProject.entity.UserEntity;
import com.example.TestProject.service.CommentService;
import com.example.TestProject.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.List;

@Controller
public class CommentController {
    private final CommentService commentService;
    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);
    private final UserService userService;

    @Autowired
    public CommentController(CommentService commentService, UserService userService) { //constructor injection
        this.commentService = commentService;
        this.userService = userService;
    }

    @MessageExceptionHandler
    public void handleException(Throwable exception) {
        logger.error("Error processing message", exception);
    } //This method is used to handle exceptions that occur during message processing

    @MessageMapping("/comment/{pageId}") //This annotation is used to map a message to a specific destination
    @SendTo("/topic/comments/{pageId}") //This annotation is used to send a message to a specific destination
    public CommentDTO processComment(@DestinationVariable Long pageId, //This annotation is used to map a destination variable to a method parameter(извлекает pageId из URL)
                                     @Payload CommentMessageDTO message, //This annotation is used to map the payload of a message to a method parameter(содержимое сообщения)
                                     Principal principal) { //This class represents the currently authenticated user
        logger.info("Processing comment for pageId: {} from user: {}", pageId, principal.getName());
        try {
            if (principal == null) {
                logger.error("User not authenticated");
                throw new IllegalStateException("User not authenticated");
            }

            String userEmail = principal.getName(); //get the email of the authenticated user
            UserEntity user = userService.findByEmail(userEmail); // get the user entity from the database

            logger.info("email from CommentController", userEmail);
            logger.info("User found: {}", user);

            if (user == null) {
                throw new UsernameNotFoundException("User not found");
            }

            return commentService.createComment(message.getContent(), pageId, user); //create a new comment
        } catch (Exception e) {
            logger.error("Error processing comment", e);
            throw e;
        }
    }

    @GetMapping("/api/comments/{pageId}") //This annotation is used to map HTTP GET requests onto specific handler methods
    @ResponseBody
    public List<CommentDTO> getComments(@PathVariable Long pageId) { //This method retrieves all comments for a given page
        logger.info("Getting comments for page: {}", pageId);
        List<CommentDTO> comments = commentService.getCommentsByPageId(pageId); //get all comments by pageId
        logger.info("Found {} comments", comments.size());
        return comments;
    }

    @DeleteMapping("/api/comments/{commentId}") //This annotation is used to map HTTP DELETE requests onto specific handler methods
    @ResponseBody
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId, //This annotation is used to map a method parameter to a URI template variable
                                           Principal principal) { //This method deletes a comment by id
        logger.info("Attempting to delete comment: {}", commentId);
        try {
            String userEmail = principal.getName();
            UserEntity user = userService.findByEmail(userEmail);

            if (user == null || user.getRole() != Erole.ADMIN_ROLE) { //check if the user is an admin
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can delete comments");
            }

            commentService.deleteComment(commentId); //delete the comment
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting comment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting comment");
        }
    }
}
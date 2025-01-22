package com.example.TestProject.controller;

import com.example.TestProject.entity.UserEntity;
import com.example.TestProject.repo.UserRepo;
import com.example.TestProject.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserRepo userRepo;
    private final JwtService jwtService;

    @Autowired
    public AuthController(UserRepo userRepo, JwtService jwtService) { //constructor injection
        this.userRepo = userRepo;
        this.jwtService = jwtService;
    }

    @GetMapping("/userinfo")
    public ResponseEntity<?> getUserInfo() { //This method retrieves the user information
        logger.info("Received request to /api/userinfo");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Current authentication: {}", authentication);

        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("No authentication found or not authenticated");
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        try {
            String username = authentication.getName();
            logger.info("Looking up user info for username: {}", username);

            // Попробуем найти пользователя и по username, и по email
            // Try to find the user by both username and email
            Optional<UserEntity> userOpt = userRepo.findByUsername(username);
            if (userOpt.isEmpty()) {
                userOpt = userRepo.findByEmail(username);
            }

            if (userOpt.isEmpty()) {
                logger.error("User not found for username/email: {}", username);
                return ResponseEntity.status(404)
                        .body(Map.of("error", "User not found", "username", username));
            }

            UserEntity user = userOpt.get();

            Map<String, Object> response = Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "roles", List.of(user.getRole())
            );

            logger.info("Successfully retrieved user info: {}", response);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

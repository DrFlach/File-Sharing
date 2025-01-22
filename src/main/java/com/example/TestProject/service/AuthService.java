package com.example.TestProject.service;

import com.example.TestProject.entity.Erole;
import com.example.TestProject.entity.UserEntity;
import com.example.TestProject.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service //when class has this annotation, it is automatically detected by Spring during component scanning and registered as a Spring bean
public class AuthService {
    @Autowired
    private UserRepo userRepository;

    public UserEntity getCurrentUser() { // This method retrieves the current authenticated user from the security context
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Authentication is null");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User with email " + email + " not found"));
    }

    public boolean hasRole(UserEntity user, Erole role) { //This method checks whether a given user has a particular role
        return user.getRole() == role;
    }
}

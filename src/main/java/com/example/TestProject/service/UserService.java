package com.example.TestProject.service;

import com.example.TestProject.entity.UserEntity;
import com.example.TestProject.repo.UserRepo;
import com.example.TestProject.security.UserDetailsImpl;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Optional;

import static com.example.TestProject.entity.Erole.*;

@Service //when class has this annotation, it is automatically detected by Spring during component scanning and registered as a Spring bean
@AllArgsConstructor // this is a part of library lombok that can creating a Getter, Setter and Constructor.
public class UserService {
    private final UserRepo userRepo;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private BCryptPasswordEncoder encoder(){ //encrypt the password
        return new BCryptPasswordEncoder();
    }

    public boolean isUserExistsUserName(String username){ //checks that username is not previously exist
        return userRepo.existsByUsername(username);
    }

    public boolean isUserExistsEmail(String email){ //checks that email is not previously exist
        return userRepo.existsByEmail(email);
    }

    public void checkRole(UserEntity userEntity){ //It checks the email; if the email ends with 'gmail.com', it is considered a regular user.
        if (userEntity.getEmail().endsWith("@gmail.com")){
            userEntity.setRole(USER_ROLE);
        } else if( (userEntity.getEmail().endsWith(".pl")) &&  (userEntity.getEmail().contains("edu")) ){ //If the email ends with 'pl' or 'edu', it means the user is a student.
            userEntity.setRole(STUDENT_ROLE);
        } else {throw new IllegalArgumentException("Invalid email domain for user role assignment");} // else will be exception
    }

    public void saveUser(UserEntity userEntity){
        checkRole(userEntity); //we check the role from method checkRole
        userEntity.setPassword(encoder().encode(userEntity.getPassword())); //encrypt the password
        userRepo.save(userEntity); // else we save him
    }

    public Optional<UserEntity> findById(Long id) {
        return userRepo.findById(id);
    }

    public UserEntity findByUsername(String username) {
        return userRepo.findByUsername(username).orElse(null);
    }

    public UserEntity findByEmail(String email) {
        return userRepo.findByEmail(email).orElse(null);
    }

    public UserEntity findUserByAuthentication(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        if (authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return findByEmail(userDetails.getEmail() );
        }

        String email = authentication.getName();
        UserEntity user = findByEmail(email);
        if (user == null) {
            user = findByEmail(email);
        }
        return user;
    }

    public Long getCurrentUserId(Principal principal) {
        if (principal == null) {
            logger.warn("Principal is null, cannot retrieve user ID.");
            return null;
        }

        String username = principal.getName();
        logger.info("Attempting to retrieve user ID for username: {}", username);

        Optional<UserEntity> userOptional = userRepo.findByUsername(username);
        if (userOptional.isEmpty()) {
            logger.warn("No user found for username: {}", username);
            return null;
        }


        Long userId = userOptional.get().getId();
        logger.info("Found user ID: {} for username: {}", userId, username);
        return userId;
    }

}

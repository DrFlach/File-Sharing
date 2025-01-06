package com.example.TestProject.security;

import com.example.TestProject.entity.UserEntity;
import com.example.TestProject.repo.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepo userRepo;

    @Autowired
    public UserDetailsServiceImpl(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    @Transactional
    public UserDetailsImpl loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userRepo.findByUsername(username)
                .orElseGet(() -> userRepo.findByEmail(username)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found")));
        System.out.println("Loaded user: id=" + userEntity.getId() +
                ", email=" + userEntity.getEmail() +
                ", username=" + userEntity.getUsername() +
                ", password=" + userEntity.getPassword() +
                ", role=" + userEntity.getRole());

        String formattedRole = userEntity.getRole().name().replace("_ROLE", ""); // Убираем _ROLE
        formattedRole = "ROLE_" + formattedRole; // Добавляем корректный префикс

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(formattedRole);
        System.out.println("Assigned authority: " + authority);

        return new UserDetailsImpl(
                userEntity.getEmail(),
                userEntity.getPassword(),
                Collections.singletonList(authority),
                userEntity
                );
    }
}

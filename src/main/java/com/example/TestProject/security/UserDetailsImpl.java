package com.example.TestProject.security;

import com.example.TestProject.entity.UserEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class UserDetailsImpl implements UserDetails {
    private Long id;
    private String name;
    private String email;
    private String password;
    private UserEntity userEntity;
    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(UserEntity userEntity) {
        this.id = userEntity.getId();
        this.name = userEntity.getUsername();
        this.email = userEntity.getEmail();
        this.password = userEntity.getPassword();
        this.authorities = List.of(
                new SimpleGrantedAuthority(userEntity.getRole().name())
        );
        this.userEntity = userEntity;
    }

    // Добавляем метод получения UserEntity
    public UserEntity getUser() {
        return userEntity;
    }


    // Конструктор с параметрами
    public UserDetailsImpl(String email, String password, Collection<? extends GrantedAuthority> authorities, UserEntity userEntity) {
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.userEntity = userEntity;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public static UserDetailsImpl build(UserEntity userEntity) {
        return new UserDetailsImpl(userEntity);
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

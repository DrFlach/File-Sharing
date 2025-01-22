package com.example.TestProject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Table(name = "userEntity")
@AllArgsConstructor
@NoArgsConstructor
@ToString
//in this file contains entity for a user
public class UserEntity {

    @Id // id, primary key in DB
    @GeneratedValue(strategy = GenerationType.IDENTITY) // will add a plus one
    private long id; //user has an id, primary key
    private String username; //user has a name
    private String password; // user has a password
    private String email; // user has a email
    private Erole role = Erole.ADMIN_ROLE; //At the start, the Erole will be set to admin, but after registration, it will be changed depending on the email.
}

package com.example.TestProject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter @Setter
    private String fileName; // file name
    @Getter @Setter
    private String filePath; //path to file
    @Getter @Setter
    private String faculty; //faculty
    @Getter @Setter
    private Integer semester; //semester


    @ManyToOne @Setter
    private UserEntity uploadedBy; //The user who uploaded the file
    @ManyToOne @Getter @Setter
    private University university;
}

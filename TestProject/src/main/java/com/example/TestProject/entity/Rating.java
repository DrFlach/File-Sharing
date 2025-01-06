package com.example.TestProject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rating", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "university_id"})
})
@Getter @Setter
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // Для корректного использования JPA можно добавить связь, если понадобится:
    @ManyToOne
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    private double rating;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

//    @Getter @Setter
//    private Long userId;
//    @Getter @Setter
//    private Long universityId;

}

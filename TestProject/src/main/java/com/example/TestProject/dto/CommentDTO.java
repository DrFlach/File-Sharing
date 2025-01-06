package com.example.TestProject.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class CommentDTO {
    private Long id;
    private String content;
    private String username;
    private LocalDateTime createdAt;
    private Long pageId;
}

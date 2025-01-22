package com.example.TestProject.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class CommentDTO {//class dto for comment
    private Long id;//id of the comment
    private String content;//content of the comment
    private String username;//username of the user who created the comment
    private LocalDateTime createdAt;//date and time when the comment was created
    private Long pageId;//id of the page where the comment was created
}

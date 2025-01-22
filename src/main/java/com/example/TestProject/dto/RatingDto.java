package com.example.TestProject.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RatingDto {

    private Long userId;

    private Long universityId;

    private double rating;
}

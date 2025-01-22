package com.example.TestProject.controller;

import com.example.TestProject.dto.RatingDto;
import com.example.TestProject.entity.University;
import com.example.TestProject.entity.UserEntity;
import com.example.TestProject.repo.UniversityRepository;
import com.example.TestProject.service.JwtService;
import com.example.TestProject.service.RatingService;
import com.example.TestProject.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/uni")
public class RatingController {

    private final JwtService jwtTokenProvider;
    private final RatingService ratingService;
    private final UserService userService;
    private final UniversityRepository universityRepository;
    private static final Logger logger = LoggerFactory.getLogger(RatingController.class);

    @Autowired
    public RatingController(RatingService ratingService, UserService userService, JwtService jwtTokenProvider, UniversityRepository universityRepository) {
        this.ratingService = ratingService;
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.universityRepository = universityRepository;
    }

    @GetMapping("/{universityName}")
    public String getUniversityPage(@PathVariable String universityName, Model model, Principal principal) {
        // Найдем университет по имени или ID (можно обновить метод поиска в сервисе)
        String email = principal.getName();
        UserEntity user = userService.findByEmail(email);
        Long userId = user.getId();
        University university = ratingService.getUniversityByName(universityName);

        model.addAttribute("userId", userId);
        model.addAttribute("university", university);
        model.addAttribute("averageRating", ratingService.getAverageRating(university.getId()));
        model.addAttribute("universityId", university.getId()); // Добавляем ID университета в модель
        logger.info("University ID: {}", university.getId());
        logger.info("University found: {}", university.getName());
        return "uni/" + universityName; // Подгружаем шаблон для конкретного университета
    }


    @PostMapping(value = "/{universityId}/ratings/add",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addRating(@PathVariable Long universityId, //receive the rating
                                       @RequestBody RatingDto ratingDto) { //send request to add rating
        try {
            ratingService.addRating(ratingDto.getUserId(), universityId, ratingDto.getRating()); //add rating
            logger.debug("Received rating request: rating={}, userId={}, universityId={}",
                    ratingDto.getRating(),
                    ratingDto.getUserId(),
                    universityId);
            double newAverage = ratingService.getAverageRating(universityId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Rating added successfully"); //send response
            response.put("averageRating", newAverage);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error adding rating", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    //receive the list of products for rating
    @GetMapping
    public ResponseEntity<List<University>> getUniversities() { //send request to get the list of products
        return ResponseEntity.ok(ratingService.getUniversities());
    }

    @GetMapping("/{universityId}/ratings/average")
    public ResponseEntity<?> getAverageRating(@PathVariable Long universityId) { //receive the average rating
        try {
            Map<String, Object> statistics = ratingService.getUniversityStatistics(universityId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error getting average rating", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/{universityId}/ratings/check")
    @ResponseBody
    public ResponseEntity<Boolean> hasUserVotedForUniversity(@RequestParam Long userId, @PathVariable Long universityId) { //receive the rating
        boolean hasRated = ratingService.hasUserRated(userId, universityId);
        return ResponseEntity.ok(hasRated);
    }

    @GetMapping("/{universityId}/ratings/checkVoteStatus")
    public ResponseEntity<?> checkVoteStatus(@PathVariable Long universityId, //function to check vote status
                                             @RequestParam Long userId) {
        try {
            boolean hasVoted = ratingService.hasUserVoted(userId, universityId);
            return ResponseEntity.ok(Map.of(
                    "canVote", !hasVoted,
                    "hasVoted", hasVoted
            ));
        } catch (Exception e) {
            logger.error("Error checking vote status", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{universityId}/ratings/remove")
    public ResponseEntity<?> removeRating(@PathVariable Long universityId, //function to remove the rating
                                          @RequestParam Long userId) {
        try {
            boolean removed = ratingService.removeRating(userId, universityId);
            if (removed) {
                double newAverage = ratingService.getAverageRating(universityId);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Rating removed successfully");
                response.put("averageRating", newAverage);

                return ResponseEntity.ok(response);
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Rating not found")); //return error message if rating not found
        } catch (Exception e) {
            logger.error("Error removing rating", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/{universityId}/ratings/user")
    public ResponseEntity<?> getUserRating(@PathVariable Long universityId, //function to get user rating
                                           @RequestParam Long userId) {
        try {
            Optional<Double> rating = ratingService.getUserRating(userId, universityId);
            Map<String, Object> response = new HashMap<>();
            response.put("rating", rating.orElse(null));
            response.put("hasVoted", rating.isPresent());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting user rating", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

//    тест
@GetMapping("/ratings")
public ResponseEntity<List<Map<String, Object>>> getAllUniversityRatings() { //function to get all university ratings
    logger.debug("Fetching all university ratings");
    List<University> universities = universityRepository.findAll();
    List<Map<String, Object>> ratings = universities.stream()
            .map(university -> {
                Map<String, Object> ratingData = new HashMap<>();
                ratingData.put("universityId", university.getId());
                ratingData.put("name", university.getName());
                Double avgRating = ratingService.getAverageRating(university.getId());
                ratingData.put("averageRating", avgRating);
                logger.debug("University {} (ID: {}) has average rating: {}", university.getName(), university.getId(), avgRating);
                ratingData.put("voteCount", ratingService.getVoteCount(university.getId()));

                return ratingData;
            })
            .collect(Collectors.toList());

    return ResponseEntity.ok(ratings);
}

    @MessageMapping("/rating-update")
    @SendTo("/topic/ratings")
    public Map<String, Object> broadcastRatingUpdate(RatingDto ratingDto) { //function to broadcast rating update
        logger.debug("Broadcasting rating update for university ID: {}", ratingDto.getUniversityId());
        Map<String, Object> response = new HashMap<>();
        University university = universityRepository.findById(ratingDto.getUniversityId())
                .orElseThrow(() -> new EntityNotFoundException("University not found"));

        double averageRating = ratingService.getAverageRating(university.getId());
        logger.debug("New average rating: {}", averageRating);

        response.put("universityName", university.getName());
        response.put("universityId", university.getId());
        response.put("averageRating", averageRating);
        response.put("voteCount", ratingService.getVoteCount(university.getId()));

        return response;
    }

}

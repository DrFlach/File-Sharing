package com.example.TestProject.service;

import com.example.TestProject.dto.RatingDto;
import com.example.TestProject.entity.Rating;
import com.example.TestProject.entity.University;
import com.example.TestProject.entity.UserEntity;
import com.example.TestProject.repo.RatingRepository;
import com.example.TestProject.repo.UniversityRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class RatingService {
    private final RatingRepository ratingRepository;
    private final UniversityRepository universityRepository;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(RatingService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    public RatingService(RatingRepository ratingRepository,
                         UniversityRepository universityRepository,
                         UserService userService) {
        this.universityRepository = universityRepository;
        this.ratingRepository = ratingRepository;
        this.userService = userService;
    }

    public University getUniversityByName(String name) { //переместить на UniversityService
        return universityRepository.findByName(name)
                .orElse(null); // Предполагается, что в репозитории есть метод findByName
    }

    public List<University> getUniversities() {
        return universityRepository.findAll();
    }

    public boolean hasUserRated(Long userId, Long universityId) {
        Optional<UserEntity> user = userService.findById(userId);
        Optional<University> university = universityRepository.findById(universityId);

        if (user.isPresent() && university.isPresent()) {
            return ratingRepository.existsByUserAndUniversity(user.get(), university.get());
        }
        return false;
    }

    @Transactional
    public boolean addRating(Long userId, Long universityId, double rating) {
        try{
            if (rating < 1 || rating > 5) {
                throw new IllegalArgumentException("Rating must be between 1 and 5");
            }

            UserEntity user = userService.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            University university = universityRepository.findById(universityId)
                    .orElseThrow(() -> new EntityNotFoundException("University not found"));

            if (hasUserVoted(userId, universityId)) {
                throw new IllegalStateException("User has already rated this university");
            }

            Rating newRating = new Rating();
            newRating.setUser(user);
            newRating.setUniversity(university);
            newRating.setRating(rating);
            //ratingRepository.save(newRating);
            //тест
            Rating savedRating = ratingRepository.save(newRating);
            Map<String, Object> update = new HashMap<>();
            update.put("universityName", university.getName());
            update.put("universityId", universityId);
            update.put("averageRating", getAverageRating(universityId));
            update.put("voteCount", getVoteCount(universityId));

            messagingTemplate.convertAndSend("/topic/ratings", update);

            logger.info("Added rating {} for university {} by user {}", rating, universityId, userId);
            return true;
        } catch (Exception e) {
            logger.error("Error adding rating: {}", e.getMessage());
            return false;
        }

    }

    public Double getAverageRating(Long universityId) {
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new EntityNotFoundException("University not found"));

        List<Rating> ratings = ratingRepository.findByUniversity(university);
        if (ratings.isEmpty()) {
            return 0.0;
        }

        double sum = ratings.stream()
                .mapToDouble(Rating::getRating)
                .sum();
        return sum / ratings.size();
    }

    public boolean hasUserVoted(Long userId, Long universityId) {
        UserEntity user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new EntityNotFoundException("University not found"));

        return ratingRepository.existsByUserAndUniversity(user, university);
    }

    public boolean removeRating(Long userId, Long universityId) {
        try {
            UserEntity user = userService.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            University university = universityRepository.findById(universityId)
                    .orElseThrow(() -> new EntityNotFoundException("University not found"));

            Optional<Rating> existingRating = ratingRepository.findByUserAndUniversity(user, university);
            if (existingRating.isPresent()) {
                ratingRepository.delete(existingRating.get());
                logger.info("Removed rating for university {} by user {}", universityId, userId);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error removing rating: {}", e.getMessage());
            return false;
        }
    }

    public Optional<Double> getUserRating(Long userId, Long universityId) {
        UserEntity user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new EntityNotFoundException("University not found"));

        return ratingRepository.findByUserAndUniversity(user, university)
                .map(Rating::getRating);
    }

    public long getVoteCount(Long universityId) {
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new EntityNotFoundException("University not found"));

        return ratingRepository.countByUniversity(university);
    }

    // Можно также создать метод, возвращающий всю статистику сразу
    public Map<String, Object> getUniversityStatistics(Long universityId) {
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new EntityNotFoundException("University not found"));

        double averageRating = getAverageRating(universityId);
        long voteCount = getVoteCount(universityId);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("averageRating", averageRating);
        statistics.put("voteCount", voteCount);

        return statistics;
    }

}

package com.example.TestProject.service;

import com.example.TestProject.entity.University;
import com.example.TestProject.repo.UniversityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UniversityService {

    private UniversityRepository universityRepository;

    @Autowired
    public UniversityService(UniversityRepository universityRepository) {
        this.universityRepository = universityRepository;
    }

    public University getUniversityById(long id) { //to find university by Id
        return universityRepository.findById(id)
                .orElse(null); // Предполагается, что в репозитории есть метод findById
    }
}

package com.example.TestProject.service;

import com.example.TestProject.entity.FileEntity;
import com.example.TestProject.repo.FileRepository;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Getter
    @Value("${file.upload-dir}")
    private String uploadDir; //in here store the place where will be a files

    @Autowired
    private FileRepository fileRepository;

    public void save(FileEntity fileEntity) { // in this method we save file
        boolean fileExists = fileRepository.existsByFileNameAndUniversityIdAndSemester(
                fileEntity.getFileName(),
                fileEntity.getUniversity().getId(),
                fileEntity.getSemester()
        );

        if (fileExists) {
            throw new IllegalArgumentException(
                    "File with name '" + fileEntity.getFileName() +
                            "' already exists in semester " + fileEntity.getSemester() +
                            " of this university"
            );
        }

        fileRepository.save(fileEntity);
        //fileRepository.save(fileEntity);
    }

    @Transactional
    public void removeNonExistentFiles() {
        // We retrieve all the files displayed in the modal window (from the database)
        List<String> filesInModalWindow = getFilesFromModalWindow();  // This method now returns a list of files from the database

        // Папка с файлами на диске
        String uploadDir = "D:/upload/";

        // We go through the files displayed in the modal window
        for (String fileName : filesInModalWindow) {

            Path filePath = Paths.get(uploadDir, fileName); //path to file

            // check, is file exists&
            if (!Files.exists(filePath)) {
                // Logging that the file was not found on the disk
                logger.warn("The file {} does not exist on the disk. Deleting from the database ", fileName);

                // Deleting the file from the database
                removeFileFromDatabase(fileName);  // Method for deleting a file from the database
            }
        }
    }

    @Transactional
    public  void removeFileFromDatabase(String fileName) {

        fileRepository.deleteByFileName(fileName);
        logger.info("Файл {} удален из базы данных.", fileName);
    }

    private List<String> getFilesFromModalWindow() {
        // to receive all files from DB
        List<FileEntity> filesFromDatabase = fileRepository.findAll();

        // Returning a list of file names displayed in the modal window
        return filesFromDatabase.stream()
                .map(FileEntity::getFileName)  // receive name of files
                .collect(Collectors.toList());
    }

    @Scheduled(fixedRate = 60000)  // Every 60 seconds.
    @Scheduled(fixedDelay = 60000) // We use fixedDelay to ensure the previous task is completed before starting the next one
    @Transactional
    public void cleanupFiles() {
        logger.info("Starting file cleanup...");
        removeNonExistentFiles();
    }

    public void uploadFile(MultipartFile file) throws IOException {
        createDirectoryIfNotExists(Paths.get(uploadDir));

        // Define the file path and save the file
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name is empty");
        }

        // Checking the uniqueness of the file name in the database
//        boolean exists = fileRepository.existsByFileName(fileName);
//
//        if (exists) {
//            throw new IllegalArgumentException("A file with this name already exists: " + fileName);
//        }


        Path filePath = Paths.get(uploadDir, fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("the file {} has been uploaded to the directory {}", fileName, uploadDir);
    }

    public List<FileEntity> findAllByUniversityId(Long universityId) {
        return fileRepository.findAllByUniversityId(universityId);
    }



    private void createDirectoryIfNotExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            logger.info("The directory {} has been created", path);
        }
    }

    public List<String> getAllFiles() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            List<String> files = Files.walk(uploadPath)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());

            System.out.println("Files on disk: " + files); // Logging files on the disk
            return files;
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<FileEntity> findAll() {
        return fileRepository.findAll(); // Retrieving all records from the database
    }

}
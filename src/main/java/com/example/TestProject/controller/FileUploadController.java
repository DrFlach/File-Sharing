package com.example.TestProject.controller;

import com.example.TestProject.entity.Erole;
import com.example.TestProject.entity.FileEntity;
import com.example.TestProject.entity.University;
import com.example.TestProject.entity.UserEntity;
import com.example.TestProject.service.AuthService;
import com.example.TestProject.service.DocxReader;
import com.example.TestProject.service.FileService;
import com.example.TestProject.service.UniversityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/files")
public class FileUploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private AuthService authService;

    @Autowired
    private FileService fileService;

    @Autowired
    private UniversityService universityService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(@RequestParam String fileName) { // This is a Spring annotation that binds a request parameter (fileName) to a method parameter.
        try {
            Path filePath = Paths.get(uploadDir, fileName); //Path to the file

            if (!Files.exists(filePath)) { // Checks for the existing file
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
            }

            // determine the MIME-type of the file
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream"; // default type, if cannot determine
            }

            // Setting headers for file download and offering to open the file in the corresponding application
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"") // "attachment" - for download
                    .header("Content-Type", contentType) // MIME type for browser
                    .body(Files.readAllBytes(filePath)); // we send file in response
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading file: " + e.getMessage());
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/content")
    public ResponseEntity<?> getFileContent(@RequestParam String fileName) { //in this method we want to read the content of file.
        try {
            // logging name of file
            System.out.println("Request file contents: " + fileName);

            // path to file
            Path filePath = Paths.get(uploadDir, fileName);
            System.out.println("Path to file: " + filePath);

            // to check that file exist
            if (!Files.exists(filePath)) {
                System.out.println("File not found: " + fileName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
            }

            // read contest file
            String content = readFileContent(filePath, fileName);
            System.out.println("File contents read successfully");

            return ResponseEntity
                    .ok()
                    .header("Content-Type", "text/plain; charset=UTF-8") // we accept only files with UTF-8
                    .body(content);
        } catch (IOException e) {
            // logging errors during reading
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading file" + e.getMessage());
        } catch (Exception e) {
            // any other exceptions
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while processing the request");
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<String> getFiles() { //in this method we remove files if they no longer exist
        // logging the start of the process of deleting non-existent files
        System.out.println("Starting the deletion of non-existent files...");

        // deleting database records of files that do not exist on the disk.
        fileService.removeNonExistentFiles();

        // Logging the completion of deleting non-existent files
        System.out.println("Deletion of non-existent files is complete.");

        // to receive the current list of files from the database
        List<String> allFiles = fileService.getAllFiles();

        // logging the current list of files from DB
        System.out.println("list of files from DB: " + allFiles);

        // Filtering out files that do not exist on the disk
        List<String> filteredFiles = allFiles.stream()
                .filter(fileName -> {
                    // path to file
                    Path filePath = Paths.get(uploadDir, fileName);
                    boolean fileExists = filePath.toFile().exists();

                    // log if file not exist
                    if (!fileExists) {
                        System.out.println("The file was not found on the disk and will be deleted: " + fileName);
                    }

                    return fileExists;  // Checking if the file exists on the disk
                })
                .collect(Collectors.toList());

        //logging the final list of files after filtering
        System.out.println("Filtered files: " + filteredFiles);

        // logging the completion of the process of deleting empty sections
        System.out.println("Deletion of empty sections is complete.");

        return filteredFiles;
    }

    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("faculty") String faculty,
            @RequestParam("semester") Integer semester,
            @RequestParam("universityId") Long universityId) {
        try {
            fileService.uploadFile(file);  // Now works because uploadFile is defined in FileService

            UserEntity currentUser = authService.getCurrentUser();

            University university = universityService.getUniversityById(universityId);
            if (university == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("University not found");
            }

            // Check if the user has the right to upload
            if ((!authService.hasRole(currentUser, Erole.STUDENT_ROLE)) && (!authService.hasRole(currentUser, Erole.ADMIN_ROLE))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have permission to upload files");
            }

            // Save file info into the database
            FileEntity fileEntity = new FileEntity();
            String fileName = file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir).resolve(fileName);

            fileEntity.setFileName(fileName); //set FileName
            fileEntity.setFilePath(filePath.toString()); //set path to file
            fileEntity.setUploadedBy(currentUser); //the current user who uploaded file
            fileEntity.setFaculty(faculty); //set faculty
            fileEntity.setSemester(semester); //set semester
            fileEntity.setUniversity(university); //want to add file for only some University
            fileService.save(fileEntity); //save

            return ResponseEntity.ok("The file has been successfully uploaded");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload error");
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/grouped")
    public ResponseEntity<?> listFilesGroupedByFaculty(@RequestParam Long universityId) {
        try {
            // Grouping files by faculties
            var groupedFiles = fileService.findAllByUniversityId(universityId)
                    .stream()
                    .collect(Collectors.groupingBy( // почему подходит только "Семестр" узнать как работает этот поток
                            file -> file.getFaculty() + " - Семестр " + file.getSemester(),  // Grouping by faculty and semester
                            Collectors.mapping(FileEntity::getFileName, Collectors.toList()) // Collecting all file names into a list
                    ));

            return ResponseEntity.ok(groupedFiles);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing data");
        }
    }

    private String readFileContent(Path filePath, String fileName) throws IOException { //this method allow to read content in the file
        if (fileName.endsWith(".txt")) {
            return new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8); // read txt files
        } else if (fileName.endsWith(".docx")) {
            return DocxReader.readDocx(filePath); // read DOCX files
        } else {
            return "Unable to read the file content. Only TXT and DOCX formats are supported.";
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(@RequestParam String fileName) {
        try {
            Path filePath = Paths.get(uploadDir, fileName);

            // Проверяем существование файла
            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
            }

            // Удаляем файл с диска
            Files.delete(filePath);

            // Удаляем запись из базы данных
            fileService.removeFileFromDatabase(fileName);

            return ResponseEntity.ok("File successfully deleted");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting file: " + e.getMessage());
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/unique-files-count")
    public ResponseEntity<?> countUniqueFiles() {
        try {
            //get all files from database
            List<String> allFiles = fileService.getAllFiles();

            //create a Set collection to get only unique files
            Set<String> uniqueFiles = new HashSet<>(allFiles);

            // logging the number of unique files
            System.out.println("Counter unique files: " + uniqueFiles.size());

            // return the number of unique files
            return ResponseEntity.ok(Map.of("uniqueFileCount", uniqueFiles.size()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

}
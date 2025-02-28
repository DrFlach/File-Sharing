## Zastrzeżenie: Poniżej przedstawiam jedynie mój własnoręcznie napisany przykładowy kod backendu oraz mój testowy projekt graficzny strony internetowej, który stworzyłem w ramach mojego zespołu. W rzeczywistym projekcie mogą wystąpić zmiany zarówno w backendzie, jak i we frontendzie.

## Disclaimer: Below, I present only my own written sample backend code and my own test design for the website, which I created within my team. In the actual project, there may be changes in both the backend and the frontend.

# File-Sharing
A web platform for students to share and access academic notes and materials.

Overview
File-Sharing is a secure web application built with Spring Security that implements a role-based access control system. Upon registration, users are assigned one of three roles:
Student: Automatically assigned to users registering with academic email addresses (e.g., @student.san.edu.pl)
Regular User: Assigned to users registering with standard email addresses (e.g., @gmail.com)
Admin: Manually assigned by the system creator
![image](https://github.com/user-attachments/assets/9a22b4a9-cb3c-4811-8c30-4cd0cc21c3f4)
Saving data and logging in:
![image](https://github.com/user-attachments/assets/cc65b491-0ee4-4d3e-8add-f30792b9b563)
![image](https://github.com/user-attachments/assets/c8af509b-cb2e-4bc0-b7e2-408660681f64)
The main page is presented as a list of all universities where users can select their university, write comments, rate it on a 5-point scale, and most importantly, upload their useful notes to help other students. Importantly, all uploaded notes are not moderated initially, but if a note does not match the topic, it will be removed by the admin. In case of repeated violations, a BAN is possible! :)
![image](https://github.com/user-attachments/assets/74741ce2-7a94-422b-973e-8985f77e7dde)
There is also a personal profile that contains basic information about the user. For instance, if you registered using a @gmail.com email, you are considered a regular user. This means you cannot moderate or upload files, as file uploads are restricted to users with the "student" role. However, you can still download and view files.
![image](https://github.com/user-attachments/assets/7da65c8d-87e0-4e87-808c-55a960b061e9)
The university page is designed to include a rating feature, the ability to add comments, upload files, and perform moderation if the appropriate role is assigned. Once you vote, the stars highlight the rating you gave the university. For example, if you rated it a 5, all five stars will be lit up.
![image](https://github.com/user-attachments/assets/dfb38416-2991-4b09-bfcd-4e1c475f3c64)
Finally, when adding files, they are uploaded to the selected semester. For example, if you have notes for the 1st semester in computer science, you select the 1st semester, choose the subject (e.g., Informatics), and upload your file. Importantly, only users with the roles of "Student" or "Admin" can upload files. After selecting the file, click the "Add" button, and the file will be saved.
![image](https://github.com/user-attachments/assets/b8d57d9c-88f2-49ee-a76d-42d7ef2d308b)
Below is a modal window displayed to a regular user. Since they cannot delete or add files, those buttons are not present.
![image](https://github.com/user-attachments/assets/31fd2b13-3099-40fa-959d-d0f63eef82ca)
And there's a Log Out button, which allows you to successfully log out of your profile. :)
![image](https://github.com/user-attachments/assets/da5883f3-43b1-4ce2-a853-c9048766506b)
Technical Stack
Backend

Java 17
Spring Framework

Spring Boot
Spring Security with JWT Authentication
Spring Data JPA


WebSocket for real-time features
MySQL

Frontend

JavaScript
HTML5
CSS3
Bootstrap 

Security

JWT (JSON Web Tokens) for authorization
Role-based access control
Email validation system


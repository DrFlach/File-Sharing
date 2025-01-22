package com.example.TestProject.controller;

import com.example.TestProject.entity.UserEntity;
import com.example.TestProject.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import javax.validation.Valid;

@Controller
@AllArgsConstructor // this is a part of library lombok that can creating a Getter, Setter and Constructor.
// Create a separate controller that will process the registration system
public class RegistrationController {
    UserService userService; //create a userService with a type UserService

    @GetMapping("/registration") //Used to handle HTTP GET requests in the controller
    public String registration(Model model) { //the model is like a container, and it is used to send data between the controller and the HTML page
        model.addAttribute("userEntity", new UserEntity()); //in this situation, we use the Model object to add attributes that will be used in the submission.
        //We create a new object, UserEntity, add it to the Model with the name 'userEntity', and after adding it, we can use it in the submission (HTML page)
        // to display the registration form.
        //UserEntity it is our class with the methods like password, email and so on.
        return "registration"; //return a registration.html
    }

    @PostMapping("/registration") //This annotation is applied when we receive data from the client and add it to the database.
    public String registration(
            @Valid @ModelAttribute("userEntity") UserEntity userEntity, //An annotation @Valid is used to trigger validation on the userEntity, spring will validate
            //the object against any validation constraints specified on the fields of the UserEntity class, such as @Size, @Email and so soon.
            // The @ModelAttribute annotation binds the form data (usually from an HTML form) to the userEntity object.
            Model model){
            System.out.println("Received registration request:");
            System.out.println("Username: " + userEntity.getUsername());
            System.out.println("Email: " + userEntity.getEmail());
            System.out.println("Password length: " + userEntity.getPassword().length());
            if(userService.isUserExistsUserName(userEntity.getUsername())) { //this is a checks if username has been previously created
                model.addAttribute("registrationError",
                        "A user with this username already exists.");
                return "registration";
            } else if (userService.isUserExistsEmail(userEntity.getEmail())) { //this is a checks if email has been previously created
                model.addAttribute("registrationError", "A user with this email already exists");
                return "registration";
            } else if (userEntity.getPassword().length() < 8) {
                    model.addAttribute("registrationError",
                            "Password must be at least 8 characters"); // this is a checks on a password, the password must be longer than 7 characters
                    return "registration";
            } else if (userEntity.getPassword().replaceAll("[^a-zA-Z]", "").length() < 6) { // this is a checks on a password,
                model.addAttribute("registrationError",
                        "Password must contain at least 6 letters");  // the password must contain a minimum of 6 letters
                return "registration";
            }  else if ((userEntity.getUsername().length() < 3 || userEntity.getUsername().length() > 20)){
                model.addAttribute("registrationError",
                        "Username must be between 3 and 20 characters"); //the username must have a minimum 3 characters and like a maximum 20
                return "registration";
            } else if (!isValidEmail(userEntity.getEmail())) {
                model.addAttribute("registrationError", "Invalid email format"); // the checks that email is valid
                return "registration";
            } else {
                userService.saveUser(userEntity);
                model.addAttribute("successMessage", "Registration successful! Please Log in"); //if everything is ok, the user can be saved in DB
                return "registration";
            }
    }

    private boolean isValidEmail(String email) { //checks on the email
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return email != null && email.matches(emailRegex);
    }
}

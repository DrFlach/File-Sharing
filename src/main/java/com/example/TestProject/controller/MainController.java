package com.example.TestProject.controller;

import com.example.TestProject.dto.AuthRequest;
import com.example.TestProject.entity.University;
import com.example.TestProject.entity.UserEntity;
import com.example.TestProject.repo.UserRepo;
import com.example.TestProject.service.JwtService;
import com.example.TestProject.service.RatingService;
import com.example.TestProject.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

//This is a Spring MVC controller that processes an HTTP request and returns submissions (HTML page).
@Controller
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private RatingService ratingService;
    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtTokenProvider;
    @Autowired
    private UserRepo userRepo;

    @GetMapping("/login")
    public String login() {
        return "login";  //return login.html
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        // Проверьте логин и пароль
        try {
            //authenticate the user
            Authentication authentication = authenticationManager.authenticate( //This method authenticates the user
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getEmail(),
                            authRequest.getPassword()
                    )
            );
            UserEntity userEntity = userRepo.findByEmail(authRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            // Устанавливаем аутентификацию в контексте безопасности
            //set the authentication in the security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String token = jwtTokenProvider.generateToken( //generate a token
                    userEntity.getEmail(),
                    userEntity.getId(),
                    authentication.getAuthorities()
            );
            logger.info("User {} logged in with roles: {} and with id: {}",
                    authRequest.getEmail(), authentication.getAuthorities(), userEntity.getId());
            logger.info("Generated token for user {}: {}", userEntity.getEmail(), token);
            return ResponseEntity.ok(Map.of( //return a response entity with a map
                    "token", token,
                    "username", userEntity.getUsername()
            ));
        } catch (BadCredentialsException e) {
            logger.error("Bad credentials for user: {}", authRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    @GetMapping("/")   // This is an annotation that indicates the method should handle HTTP GET requests.
    //Root path. This method handles requests for the homepage.
    public String main() {
        return "main"; //return main.html
    }

    @GetMapping("/profile")   // This is an annotation that indicates the method should handle HTTP GET requests.
    //Root path. This method handles requests for the homepage.
    public String profile(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (authentication == null || !authentication.isAuthenticated()) {
            authentication.getName().equals("anonymousUser");
            return "redirect:/login";
        }
        UserEntity user = userService.findByEmail(userDetails.getUsername());
        String userRole = userDetails.getAuthorities().stream()
                .findFirst().map(GrantedAuthority::getAuthority).orElse("");
        UserEntity email = userService.findUserByAuthentication(authentication);
        model.addAttribute("user", user);
        model.addAttribute("userRole", userRole);
        model.addAttribute("email", email.getEmail());
        return "profile"; //return main.html
    }

    @GetMapping("/faq")   // This is an annotation that indicates the method should handle HTTP GET requests.
    //Root path. This method handles requests for the homepage.
    public String faq() {

        return "faq"; //return main.html
    }

    @GetMapping("/contact")   // This is an annotation that indicates the method should handle HTTP GET requests.
    //Root path. This method handles requests for the homepage.
    public String contact() {

        return "contact"; //return main.html
    }

    @GetMapping("/uni/san")
    public String san(Model model) { //This method returns the SAN page
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); //This class represents the current authentication token
        UserDetails userDetails = (UserDetails) authentication.getPrincipal(); //This class represents the currently authenticated user
        logger.debug("Current authentication: {}", authentication);

        if (authentication == null || !authentication.isAuthenticated()) {
            authentication.getName().equals("anonymousUser");
            return "redirect:/login";
        }

        try {
            UserEntity user = userService.findUserByAuthentication(authentication); //This method retrieves the user entity from the database
            if (user == null) {
                logger.error("User not found in database. Authentication name: {}", authentication.getName());
                return "redirect:/login";
            }

            // Получаем университет
            University university = ratingService.getUniversityByName("SAN"); //This method retrieves the university by name
            if (university == null) {
                logger.error("University 'SAN' not found");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "University not found");
            }

            String userRole = userDetails.getAuthorities().stream()
                    .findFirst().map(GrantedAuthority::getAuthority).orElse(""); //This method retrieves the user role

            logger.debug("Loading page for user {} and university {}", user.getId(), university.getId());

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            // Добавляем данные в модель
            model.addAttribute("isAdmin", isAdmin); //add the isAdmin attribute to the model
            model.addAttribute("university", university); //add the university attribute to the model
            model.addAttribute("userId", user.getId());//add the userId attribute to the model
            model.addAttribute("averageRating", ratingService.getAverageRating(university.getId()));//add the averageRating attribute to the model
            model.addAttribute("hasVoted", ratingService.hasUserVoted(user.getId(), university.getId()));//add the hasVoted attribute to the model
            model.addAttribute("voteCount", ratingService.getVoteCount(university.getId()));//add the voteCount attribute to the model
            model.addAttribute("userRole", userRole);//add the userRole attribute to the model
            return "uni/san";
        } catch (Exception e) {
            logger.error("Error loading SAN page", e);
            return "redirect:/error";
        }
    }

    @GetMapping("/uni/polibuda")
    public String polibuda(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        logger.debug("Current authentication: {}", authentication);

        if (authentication == null || !authentication.isAuthenticated()) {
            authentication.getName().equals("anonymousUser");
            return "redirect:/login";
        }

        try {
            UserEntity user = userService.findUserByAuthentication(authentication);
            if (user == null) {
                logger.error("User not found in database. Authentication name: {}", authentication.getName());
                return "redirect:/login";
            }

            // Получаем университет
            University university = ratingService.getUniversityByName("polibuda");
            if (university == null) {
                logger.error("University 'polibuda' not found");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "University not found");
            }

            String userRole = userDetails.getAuthorities().stream()
                    .findFirst().map(GrantedAuthority::getAuthority).orElse("");

            logger.debug("Loading page for user {} and university {}", user.getId(), university.getId());

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            // Добавляем данные в модель
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("university", university);
            model.addAttribute("userId", user.getId());
            model.addAttribute("averageRating", ratingService.getAverageRating(university.getId()));
            model.addAttribute("hasVoted", ratingService.hasUserVoted(user.getId(), university.getId()));
            model.addAttribute("voteCount", ratingService.getVoteCount(university.getId()));
            model.addAttribute("userRole", userRole);
            return "uni/polibuda";
        } catch (Exception e) {
            logger.error("Error loading polibuda page", e);
            return "redirect:/error";
        }
    }

    @GetMapping("/uni/ul")
    public String ul(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        logger.debug("Current authentication: {}", authentication);

        if (authentication == null || !authentication.isAuthenticated()) {
            authentication.getName().equals("anonymousUser");
            return "redirect:/login";
        }

        try {
            UserEntity user = userService.findUserByAuthentication(authentication);
            if (user == null) {
                logger.error("User not found in database. Authentication name: {}", authentication.getName());
                return "redirect:/login";
            }

            // Получаем университет
            University university = ratingService.getUniversityByName("University of Lodz");
            if (university == null) {
                logger.error("University 'University of Lodz' not found");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "University not found");
            }

            String userRole = userDetails.getAuthorities().stream()
                    .findFirst().map(GrantedAuthority::getAuthority).orElse("");

            logger.debug("Loading page for user {} and university {}", user.getId(), university.getId());

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            // Добавляем данные в модель
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("university", university);
            model.addAttribute("userId", user.getId());
            model.addAttribute("averageRating", ratingService.getAverageRating(university.getId()));
            model.addAttribute("hasVoted", ratingService.hasUserVoted(user.getId(), university.getId()));
            model.addAttribute("voteCount", ratingService.getVoteCount(university.getId()));
            model.addAttribute("userRole", userRole);
            return "uni/ul";
        } catch (Exception e) {
            logger.error("Error loading UL page", e);
            return "redirect:/error";
        }
    }

    @GetMapping("/uni/ahe")
    public String ahe(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        logger.debug("Current authentication: {}", authentication);


        if (authentication == null || !authentication.isAuthenticated()) {
            authentication.getName().equals("anonymousUser");
            return "redirect:/login";
        }

        try {
            UserEntity user = userService.findUserByAuthentication(authentication);
            if (user == null) {
                logger.error("User not found in database. Authentication name: {}", authentication.getName());
                return "redirect:/login";
            }

            // Получаем университет
            University university = ratingService.getUniversityByName("AHE");
            if (university == null) {
                logger.error("University 'AHE' not found");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "University not found");
            }

            String userRole = userDetails.getAuthorities().stream()
                    .findFirst().map(GrantedAuthority::getAuthority).orElse("");

            logger.debug("Loading page for user {} and university {}", user.getId(), university.getId());

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("university", university);
            model.addAttribute("userId", user.getId());
            model.addAttribute("averageRating", ratingService.getAverageRating(university.getId()));
            model.addAttribute("hasVoted", ratingService.hasUserVoted(user.getId(), university.getId()));
            model.addAttribute("voteCount", ratingService.getVoteCount(university.getId()));
            model.addAttribute("userRole", userRole);
            return "uni/ahe";
        } catch (Exception e) {
            logger.error("Error loading UL page", e);
            return "redirect:/error";
        }
    }

    @GetMapping("/uni/sfl")
    public String sfl(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        logger.debug("Current authentication: {}", authentication);

        if (authentication == null || !authentication.isAuthenticated()) {
            authentication.getName().equals("anonymousUser");
            return "redirect:/login";
        }

        try {
            UserEntity user = userService.findUserByAuthentication(authentication);
            if (user == null) {
                logger.error("User not found in database. Authentication name: {}", authentication.getName());
                return "redirect:/login";
            }

            // Получаем университет
            University university = ratingService.getUniversityByName("sfl");
            if (university == null) {
                logger.error("University 'sfl' not found");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "University not found");
            }

            String userRole = userDetails.getAuthorities().stream()
                    .findFirst().map(GrantedAuthority::getAuthority).orElse("");

            logger.debug("User role for SFL page: {}", userRole);

            logger.debug("Loading page for user {} and university {}", user.getId(), university.getId());

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("university", university);
            model.addAttribute("userId", user.getId());
            model.addAttribute("averageRating", ratingService.getAverageRating(university.getId()));
            model.addAttribute("hasVoted", ratingService.hasUserVoted(user.getId(), university.getId()));
            model.addAttribute("voteCount", ratingService.getVoteCount(university.getId()));
            model.addAttribute("userRole", userRole);
            return "uni/sfl";
        } catch (Exception e) {
            logger.error("Error loading UL page", e);
            return "redirect:/login";
        }
    }

}

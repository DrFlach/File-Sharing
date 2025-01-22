package com.example.TestProject.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;

import static org.springframework.security.authorization.AuthenticatedAuthorizationManager.anonymous;

@EnableWebSecurity
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final SecurityContextLoggingFilter securityContextLoggingFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Autowired
    public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter, // constructor injection
                                 UserDetailsService userDetailsService,
                                 SecurityContextLoggingFilter securityContextLoggingFilter,
                                 CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.securityContextLoggingFilter = securityContextLoggingFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception { // method injection
        return authenticationConfiguration.getAuthenticationManager(); //authenticationManager is a method of AuthenticationConfiguration which is a part of Spring Security
    }

    @Bean
    public BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    } // BCryptPasswordEncoder is a class from Spring. its needed to encode passwords

    @Bean
    public LocaleResolver localeResolver() {
        return new AcceptHeaderLocaleResolver(); // AcceptHeaderLocaleResolver is a class from Spring. its needed to resolve locale
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() { // DaoAuthenticationProvider is a class from Spring. its needed to authenticate users
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(encoder());
        return auth;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception { // SecurityFilterChain is a class from Spring. its needed to configure security
        http
                .csrf(csrf -> csrf.disable()) // csrf is a method of HttpSecurity which is a part of Spring Security
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // session is a method of HttpSecurity which is a part of Spring Security
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/api/login").permitAll()
                        .requestMatchers("/registration").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/css/**", "/js/**").permitAll()
                        .requestMatchers("/resources/static/**").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/uni/*/ratings/average").permitAll()
                        .requestMatchers("/uni/**").authenticated()
                        .requestMatchers("/api/userinfo").authenticated()
                        .requestMatchers("/api/comments/**").authenticated()
                        .requestMatchers("/topic/comments/**").authenticated() // Для WebSocket
                        .requestMatchers("/files/upload").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers("/files/**").permitAll()
                        .anyRequest().permitAll() // anyRequest is a method of HttpSecurity which is a part of Spring Security
                )
                .securityContext(context -> { // securityContext is a method of HttpSecurity which is a part of Spring Security
                    context.requireExplicitSave(false);
                    context.securityContextRepository(new DelegatingSecurityContextRepository(
                            new RequestAttributeSecurityContextRepository(),
                            new HttpSessionSecurityContextRepository()
                    ));
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // addFilterBefore is a method of HttpSecurity which is a part of Spring Security

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/ws/**");
    }// WebSecurityCustomizer is a class from Spring. its needed to customize web security
}

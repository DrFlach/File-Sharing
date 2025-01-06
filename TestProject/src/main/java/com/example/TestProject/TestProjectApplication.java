package com.example.TestProject;

import io.jsonwebtoken.security.Keys;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Base64;

@SpringBootApplication(scanBasePackages = "com.example.TestProject")
@EnableScheduling
@EnableTransactionManagement
public class TestProjectApplication {
	public static void main(String[] args) {
		String key = Base64.getEncoder().encodeToString(Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS512).getEncoded());
		System.out.println("Generated Key: " + key);
		SpringApplication.run(TestProjectApplication.class, args);
	}

}

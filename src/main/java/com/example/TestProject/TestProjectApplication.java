package com.example.TestProject;

import io.jsonwebtoken.security.Keys;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import java.util.Random;
import java.util.Base64;

@SpringBootApplication(scanBasePackages = "com.example.TestProject")
@EnableScheduling
@EnableTransactionManagement
public class TestProjectApplication {
	//generate password
	public static String generatePassword() {
		String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String NUMBERS = "0123456789";
		String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

		Random random = new Random();
		char[] password = new char[9]; // 6 letters, 2 numbers, 1 special character

		// generate 6 letters
		// Генерация 6 букв
		int i = 0;
		while (i < 6) {
			password[i] = LETTERS.charAt(random.nextInt(LETTERS.length()));
			i++;
		}
		// generate 2 numbers
		i = 6;
		while (i < 8) {
			password[i] = NUMBERS.charAt(random.nextInt(NUMBERS.length()));
			i++;
		}

		// generate 1 special character
		password[8] = SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length()));

		return new String(password);
	}
	public static void main(String[] args) {
		String password = generatePassword();
		System.out.println("Generated password: " + password);
		String key = Base64.getEncoder().encodeToString(Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS512).getEncoded());
		System.out.println("Generated Key: " + key);
		SpringApplication.run(TestProjectApplication.class, args);
	}

}

package com.lichess.insights;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@SpringBootApplication
public class InsightsApplication /* implements CommandLineRunner */ {

	@Autowired
	private GameDao gameDao;

	public static void main(String[] args) {

		SpringApplication.run(InsightsApplication.class, args);

	}

//	@Override
//	public void run(String... args) {
		// Example operations
//		userDao.saveUser("John Doe", "john.doe@example.com");
//		System.out.println(userDao.findAllUsers());
//		System.out.println(gameDao.findAllGames());
//	}
}

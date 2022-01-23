package nl.wilcokas.planetherapy;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PlanetherapyApplication {

	public static void main(String[] args) throws IOException {
		System.setProperty("java.awt.headless", "false");
		SpringApplication.run(PlanetherapyApplication.class, args);
	}
}

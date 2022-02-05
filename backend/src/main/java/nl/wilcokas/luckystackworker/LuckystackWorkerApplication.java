package nl.wilcokas.luckystackworker;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class LuckystackWorkerApplication {

	public static void main(String[] args) throws IOException {
		System.setProperty("java.awt.headless", "false");

		log.info("Java home is {}", System.getProperty("java.home"));
		log.info("Java vendor is {}", System.getProperty("java.vendor"));
		log.info("Java version is {}", System.getProperty("java.version"));

		SpringApplication.run(LuckystackWorkerApplication.class, args);
	}
}

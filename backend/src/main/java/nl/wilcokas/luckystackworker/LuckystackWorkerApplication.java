package nl.wilcokas.luckystackworker;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;

@Slf4j
@SpringBootApplication
public class LuckystackWorkerApplication {

	public static void main(String[] args) throws IOException {
		System.setProperty("java.awt.headless", "false");

		String profile = System.getProperty("spring.profiles.active");
		if (Constants.SYSTEM_PROFILE_WINDOWS.equals(profile)) {
			log.info("Starting electron GUI");
			Runtime.getRuntime().exec(".\\lsw_gui.exe");
		}

		SpringApplication.run(LuckystackWorkerApplication.class, args);
	}
}

package nl.wilcokas.planetherapy;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;

import nl.wilcokas.planetherapy.worker.Worker;

@SpringBootApplication
public class PlanetherapyApplication {

	public static void main(String[] args) throws IOException {
		System.setProperty("java.awt.headless", "false");
		SpringApplication.run(PlanetherapyApplication.class, args);

		Worker worker = PlanetherapyContext.getWorker();

	}

	@ConfigurationProperties
	public void configure() {
		// TODO: must load the Profiles and settings from the db/repositories, into the
		// worker properties

	}

}

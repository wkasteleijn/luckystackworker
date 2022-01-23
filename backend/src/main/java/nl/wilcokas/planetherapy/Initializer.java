package nl.wilcokas.planetherapy;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.planetherapy.repository.ProfileRepository;
import nl.wilcokas.planetherapy.repository.SettingsRepository;

@Slf4j
@Component
public class Initializer {

	@Autowired
	private ProfileRepository profileRepository;

	@Autowired
	private SettingsRepository settingsRepository;

	@PostConstruct
	public void init() throws IOException {
		PlanetherapyContext.loadWorkerProperties(profileRepository.findAll().iterator(),
				settingsRepository.findAll().iterator().next());

		PlanetherapyContext.getWorker().start();
	}
}

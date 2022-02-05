package nl.wilcokas.luckystackworker;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.repository.ProfileRepository;
import nl.wilcokas.luckystackworker.repository.SettingsRepository;

@Slf4j
@Component
public class Initializer {

	@Autowired
	private ProfileRepository profileRepository;

	@Autowired
	private SettingsRepository settingsRepository;

	@PostConstruct
	public void init() throws IOException {
		LuckyStackWorkerContext.loadWorkerProperties(profileRepository.findAll().iterator(),
				settingsRepository.findAll().iterator().next());
		LuckyStackWorkerContext.getWorker().start();
	}
}

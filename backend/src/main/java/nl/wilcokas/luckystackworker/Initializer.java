package nl.wilcokas.luckystackworker;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.repository.ProfileRepository;
import nl.wilcokas.luckystackworker.repository.SettingsRepository;
import nl.wilcokas.luckystackworker.service.ReferenceImageService;

@Slf4j
@Component
public class Initializer {

	@Autowired
	private ProfileRepository profileRepository;

	@Autowired
	private SettingsRepository settingsRepository;

	@Autowired
	private ReferenceImageService referenceImageService;

	@PostConstruct
	public void init() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException,
			UnsupportedLookAndFeelException {
		LuckyStackWorkerContext.loadWorkerProperties(profileRepository.findAll().iterator(),
				settingsRepository.findAll().iterator().next());
		LuckyStackWorkerContext.getWorker().start();
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		referenceImageService.selectReferenceImage(LuckyStackWorkerContext.getWorkerProperties().get("inputFolder"));
	}
}

package nl.wilcokas.luckystackworker;

import java.io.IOException;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.service.ReferenceImageService;
import nl.wilcokas.luckystackworker.service.SettingsService;

@Slf4j
@RequiredArgsConstructor
@Component
public class Initializer {

    private final ReferenceImageService referenceImageService;
    private final SettingsService settingsService;

    @PostConstruct
    public void init() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException,
    UnsupportedLookAndFeelException {
        log.info("Java home is {}", System.getProperty("java.home"));
        log.info("Java vendor is {}", System.getProperty("java.vendor"));
        log.info("Java version is {}", System.getProperty("java.version"));
        log.info("Active profile is {}", System.getProperty("spring.profiles.active"));
        log.info("Current folder is {}", System.getProperty("user.dir"));

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        referenceImageService.selectReferenceImage(settingsService.getRootFolder());
    }
}

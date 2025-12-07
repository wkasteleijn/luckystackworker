package nl.wilcokas.luckystackworker;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.model.Settings;
import nl.wilcokas.luckystackworker.repository.SettingsRepository;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class Initializer {

    private final SettingsRepository settingsService;

    @PostConstruct
    public void init()
            throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException,
                    UnsupportedLookAndFeelException {
        log.warn("Java home is {}", System.getProperty("java.home"));
        log.warn("Java vendor is {}", System.getProperty("java.vendor"));
        log.warn("Java version is {}", System.getProperty("java.version"));
        log.warn("Active profile is {}", System.getProperty("spring.profiles.active"));
        log.warn("User home folder is {}", System.getProperty("user.home"));

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        Settings settings = settingsService.getSettings();
        settingsService.saveSettings(settings);
    }
}

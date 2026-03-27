package nl.wilcokas.luckystackworker;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import jakarta.annotation.PostConstruct;
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
    public void init() {
        log.warn("Java home is {}", System.getProperty("java.home"));
        log.warn("Java vendor is {}", System.getProperty("java.vendor"));
        log.warn("Java version is {}", System.getProperty("java.version"));
        log.warn("Active profile is {}", System.getProperty("spring.profiles.active"));
        log.warn("User home folder is {}", System.getProperty("user.home"));

        System.setProperty("apple.awt.graphics.UseQuartz", "true");
        System.setProperty("darklaf.allowNativeCode", "false");
        LafManager.install(new DarculaTheme());

        Settings settings = settingsService.getSettings();
        settingsService.saveSettings(settings);
    }
}

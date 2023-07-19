package nl.wilcokas.luckystackworker.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import nl.wilcokas.luckystackworker.model.Settings;
import nl.wilcokas.luckystackworker.repository.SettingsRepository;

@RequiredArgsConstructor
@Service
public class SettingsService {

    private final SettingsRepository settingsRepository;


    public void saveSettings(Settings settings) {
        settingsRepository.save(settings);
    }

    public String getRootFolder() {
        return getSettings().getRootFolder();
    }

    public String[] getExtensions() {
        return getSettings().getExtensions().split(",");
    }

    public String getDefaultProfile() {
        return getSettings().getDefaultProfile();
    }

    public Settings getSettings() {
        return settingsRepository.findAll().iterator().next();
    }
}

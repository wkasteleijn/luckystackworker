package nl.wilcokas.luckystackworker.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.model.Settings;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswUtil;

@Slf4j
@RequiredArgsConstructor
@Service
public class SettingsService {

    private static final String SETTINGS_FILE = "/settings.json";

    private final ObjectMapper objectMapper;
    private final GmicService gmicService;
    private Settings settings;

    public void saveSettings(Settings settings) {
        try {
            objectMapper.writeValue(new File(LswFileUtil.getDataFolder(LswUtil.getActiveOSProfile()) + SETTINGS_FILE), settings);
        } catch (Exception e) {
            log.error("Error writing settings: ", e);
        }
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
        if (settings == null) {
            readSettings();
        }
        return settings;
    }

    private void readSettings() {
        try {
            settings = objectMapper.readValue(Files.readString(Paths.get(LswFileUtil.getDataFolder(LswUtil.getActiveOSProfile()) + SETTINGS_FILE)),
                    Settings.class);
        } catch (Exception e) {
            log.warn("Settings file not found");
        }
        log.info("Reverting to the default settings");
        settings = getDefaultSettings();
    }

    private Settings getDefaultSettings() {
        String activeOs = LswUtil.getActiveOSProfile();
        return Settings.builder().defaultProfile("moon").extensions("tif,png,tiff").gmicAvailable(gmicService.isGmicAvailable(activeOs))
                .latestKnownVersion(LswUtil.getLswVersion()).latestKnownVersionChecked(null).outputFormat("tif")
                .rootFolder(Constants.SYSTEM_PROFILE_WINDOWS.equals(activeOs) ? "C:/" : "~").build();
    }
}

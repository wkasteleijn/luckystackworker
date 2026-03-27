package nl.wilcokas.luckystackworker;

import jakarta.annotation.PostConstruct;

import java.awt.*;
import java.io.IOException;
import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.model.Settings;
import nl.wilcokas.luckystackworker.repository.SettingsRepository;
import nl.wilcokas.luckystackworker.util.ui.LSWSwingTheme;
import org.springframework.stereotype.Component;

import static nl.wilcokas.luckystackworker.constants.Constants.TITLE_COLOR;

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

        // 1. Forceer de 'Steel' look en schakel Ocean/Bold effecten uit
        System.setProperty("swing.metalTheme", "steel");
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        UIManager.put("Metal.gradient", null);

        // 2. Kill de gradiënten (vervang de lijst met 3x dezelfde kleur om verloop te voorkomen)
        // Dit verwijdert de groene gloed op de actieve titelbalk
        java.util.List<Object> plainTitleGradient = java.util.Arrays.asList(
                0.0, 0.0, Constants.TITLE_COLOR, Constants.TITLE_COLOR, Constants.TITLE_COLOR
        );
        UIManager.put("InternalFrame.activeTitleGradient", plainTitleGradient);
        UIManager.put("Button.gradient", plainTitleGradient);

        // 3. Stel decoraties in (zodat Swing de titelbalk tekent op Mac)
        JDialog.setDefaultLookAndFeelDecorated(true);
        JFrame.setDefaultLookAndFeelDecorated(true);

        // 4. Pas het thema toe
        MetalLookAndFeel.setCurrentTheme(new LSWSwingTheme());

        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());

            // 5. UI Overrides voor de JFileChooser en algemene Dark Mode
            UIManager.put("FileChooser.listViewBackground", new ColorUIResource(Color.BLACK));
            UIManager.put("List.background", new ColorUIResource(Color.BLACK));
            UIManager.put("Table.background", new ColorUIResource(Color.BLACK));
            UIManager.put("Panel.background", new ColorUIResource(Constants.BACKGROUND_COLOR));
            UIManager.put("FileChooser.background", new ColorUIResource(Constants.BACKGROUND_COLOR));

            // Tekstkleuren
            UIManager.put("Label.foreground", new ColorUIResource(Color.WHITE));
            UIManager.put("Button.foreground", new ColorUIResource(Color.WHITE));
            UIManager.put("TextField.foreground", new ColorUIResource(Color.WHITE));
            UIManager.put("TextField.background", new ColorUIResource(new Color(50, 50, 50)));
            UIManager.put("TextField.caretForeground", new ColorUIResource(Color.WHITE));

            // Selectiekleuren (voor de pre-selected files)
            UIManager.put("List.selectionBackground", new ColorUIResource(new Color(70, 70, 70)));
            UIManager.put("List.selectionForeground", new ColorUIResource(Color.CYAN));
            UIManager.put("Table.selectionBackground", new ColorUIResource(new Color(70, 70, 70)));
            UIManager.put("Table.selectionForeground", new ColorUIResource(Color.CYAN));

        } catch (Exception e) {
            log.error("Error setting Look and Feel", e);
        }

        Settings settings = settingsService.getSettings();
        settingsService.saveSettings(settings);
    }
}

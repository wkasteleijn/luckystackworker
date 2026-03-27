package nl.wilcokas.luckystackworker.util.ui;

import nl.wilcokas.luckystackworker.constants.Constants;

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.FontUIResource;
import java.awt.*;

import static nl.wilcokas.luckystackworker.constants.Constants.BACKGROUND_COLOR;
import static nl.wilcokas.luckystackworker.constants.Constants.TITLEBAR_COLOR;
import static nl.wilcokas.luckystackworker.constants.Constants.TITLE_COLOR;

public class LSWSwingTheme extends DefaultMetalTheme {

    private final FontUIResource font = new FontUIResource("SansSerif", Font.PLAIN, 13);

    @Override
    public ColorUIResource getWindowTitleForeground() {
        return new ColorUIResource(Color.WHITE);
    }

    @Override
    public ColorUIResource getWindowTitleInactiveForeground() {
        return new ColorUIResource(new Color(180, 180, 180));
    }

    // De belangrijkste kleuren voor de titelbalk en randen
    @Override protected ColorUIResource getPrimary1() { return new ColorUIResource(TITLE_COLOR); }
    @Override protected ColorUIResource getPrimary2() { return new ColorUIResource(TITLE_COLOR); }
    @Override protected ColorUIResource getPrimary3() { return new ColorUIResource(TITLE_COLOR); }

    @Override protected ColorUIResource getSecondary1() { return new ColorUIResource(TITLEBAR_COLOR); }
    @Override protected ColorUIResource getSecondary2() { return new ColorUIResource(TITLEBAR_COLOR); }
    @Override protected ColorUIResource getSecondary3() { return new ColorUIResource(BACKGROUND_COLOR); }

    // Fonts
    @Override public FontUIResource getControlTextFont() { return font; }
    @Override public FontUIResource getSystemTextFont() { return font; }
    @Override public FontUIResource getUserTextFont() { return font; }
    @Override public FontUIResource getMenuTextFont() { return font; }
    @Override public FontUIResource getWindowTitleFont() { return new FontUIResource("SansSerif", Font.BOLD, 14); }
    @Override public FontUIResource getSubTextFont() { return new FontUIResource("SansSerif", Font.PLAIN, 11); }

    @Override
    public ColorUIResource getPrimaryControlDarkShadow() {
        return new ColorUIResource(Constants.BORDER_COLOR);
    }

    @Override
    public ColorUIResource getPrimaryControlShadow() {
        return new ColorUIResource(Constants.TITLE_COLOR);
    }

    @Override
    public ColorUIResource getPrimaryControlInfo() {
        return new ColorUIResource(Constants.TITLE_COLOR);
    }
}
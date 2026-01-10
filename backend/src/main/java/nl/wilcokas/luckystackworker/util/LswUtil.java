package nl.wilcokas.luckystackworker.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import org.apache.commons.io.IOUtils;
import org.springframework.util.ReflectionUtils;

@Slf4j
public class LswUtil {

    private LswUtil() {}

    public static void runCliCommand(String activeOSProfile, List<String> arguments, boolean await)
            throws IOException, InterruptedException {
        ProcessBuilder processBuilder = getProcessBuilder(activeOSProfile, arguments);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        if (!logOutput(process, await ? 12 : 3)) {
            throw new IOException("CLI execution failed");
        }
        if (process.exitValue() != 0) {
            throw new IOException("CLI execution failed");
        }
    }

    public static void delayMacOS() {
        String activeOSProfile = getActiveOSProfile();
        // On macs (and maybe linux?), we need to wait a bit for the frame to be initialized
        if (Constants.SYSTEM_PROFILE_MAC.equals(activeOSProfile)
                || Constants.SYSTEM_PROFILE_LINUX.equals(activeOSProfile)) {
            // Workaround for issue on macs, somehow needs to wait some milliseconds for the
            // frame to be initialized.
            LswUtil.waitMilliseconds(500);
        }
    }

    public static String getActiveOSProfile() {
        return System.getProperty("spring.profiles.active");
    }

    public static String getMacOSArch() {
        return System.getProperty("macos.arch");
    }

    public static void waitMilliseconds(int milliseconds) {
        try {
            Thread.currentThread().sleep(milliseconds);
        } catch (InterruptedException e) {
            log.warn("Thread waiting for folder chooser got interrupted: {}", e.getMessage());
        }
    }

    public static void setPrivateField(Object object, Class<?> objectClass, String fieldName, Object value) {
        Field field = ReflectionUtils.findField(objectClass, fieldName);
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, object, value);
    }

    public static Object getPrivateField(Object object, Class<?> objectClass, String fieldName) {
        Field field = ReflectionUtils.findField(objectClass, fieldName);
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, object);
    }

    public static String getFullObjectName(String profileName) {
        return switch (profileName) {
            case "sun" -> "Sun";
            case "moon" -> "Moon";
            case "mer" -> "Mercury";
            case "ven" -> "Venus";
            case "mar" -> "Mars";
            case "jup" -> "Jupiter";
            case "sat" -> "Saturn";
            case "uranus" -> "Uranus";
            case "neptune" -> "Neptune";
            case "iss" -> "ISS";
            case "eur" -> "Europa";
            case "gan" -> "Ganymede";
            case "cal" -> "Callisto";
            case "io" -> "Io";
            case "tit" -> "Titan";
            default -> "Unspecified";
        };
    }

    private static boolean logOutput(final Process process, int timeoutSeconds)
            throws IOException, InterruptedException {
        log.info("=== CLI output start ===");
        log.info(IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8.name()));
        log.info("==== CLI output end ====");
        return process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
    }

    private static ProcessBuilder getProcessBuilder(String activeOSProfile, List<String> arguments) {
        if (Constants.SYSTEM_PROFILE_WINDOWS.equals(activeOSProfile)) {
            return new ProcessBuilder(arguments);
        } else {
            String joinedArguments = arguments.stream().collect(Collectors.joining(" "));
            String shellType =
                    switch (activeOSProfile) {
                        case Constants.SYSTEM_PROFILE_MAC -> "zsh";
                        case Constants.SYSTEM_PROFILE_LINUX -> "bash";
                        default -> "bash";
                    };
            return new ProcessBuilder(shellType, "-c", joinedArguments);
        }
    }
}

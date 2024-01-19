package nl.wilcokas.luckystackworker.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.springframework.util.ReflectionUtils;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;

@Slf4j
public class LswUtil {

    private LswUtil() {
    }

    public static void runCliCommand(String activeOSProfile, List<String> arguments, boolean await) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = getProcessBuilder(activeOSProfile, arguments);
        if (await) {
            processBuilder.redirectErrorStream(true);
            if (logOutput(processBuilder.start()) != 0) {
                throw new IOException("CLI execution failed");
            }
        } else {
            processBuilder.start();
        }
    }

    private static int logOutput(final Process process) throws IOException, InterruptedException {
        log.info("=== CLI output start ===");
        log.info(IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8.name()));
        log.info("==== CLI output end ====");
        return process.waitFor();
    }

    private static ProcessBuilder getProcessBuilder(String activeOSProfile, List<String> arguments) {
        if (Constants.SYSTEM_PROFILE_WINDOWS.equals(activeOSProfile)) {
            return new ProcessBuilder(arguments);
        } else {
            String joinedArguments = arguments.stream().collect(Collectors.joining(" "));
            String shellType = switch (activeOSProfile) {
            case Constants.SYSTEM_PROFILE_MAC -> "zsh";
            case Constants.SYSTEM_PROFILE_LINUX -> "bash";
            default -> "bash";
            };
            return new ProcessBuilder(shellType, "-c", joinedArguments);
        }
    }

    public static String getActiveOSProfile() {
        return System.getProperty("spring.profiles.active");
    }

    public static String getLswVersion() {
        return System.getProperty("lsw.version");
    }

    public static void waitMilliseconds(int milliseconds) {
        try {
            Thread.currentThread().sleep(milliseconds);
        } catch (InterruptedException e) {
            log.warn("Thread waiting for folder chooser got interrupted: {}", e.getMessage());
        }
    }

    public static Executor getParallelExecutor() {
        int numThreads = Runtime.getRuntime().availableProcessors();
        return Executors.newFixedThreadPool(numThreads);
    }

    public static void stopAndAwaitParallelExecutor(Executor executor) {
        ((ExecutorService) executor).shutdown();
        try {
            ((ExecutorService) executor).awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            log.warn("LSWSharpenFilter thread execution was stopped: ", e);
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
}

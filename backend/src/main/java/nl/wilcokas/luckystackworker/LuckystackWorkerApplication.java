package nl.wilcokas.luckystackworker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.yaml.snakeyaml.Yaml;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.DataInfo;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswUtil;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class LuckystackWorkerApplication {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.setProperty("java.awt.headless", "false");

        String osProfile = LswUtil.getActiveOSProfile();
        String dataFolder = LswFileUtil.getDataFolder(osProfile);
        createDataFolderWhenMissing(dataFolder);
        if (Constants.SYSTEM_PROFILE_WINDOWS.equals(osProfile)) {
            log.info("Starting electron GUI");
            try {
                LswUtil.runCliCommand(osProfile, Arrays.asList("./lsw_gui.exe", ">>", dataFolder + "/lsw-gui.log"), false);
            } catch (Exception e) {
                log.warn("GUI wasn't started, are you running on windows in development mode?");
            }
        }

        log.info("Determining database version and replace it if needed..");
        String lswVersion = LswUtil.getLswVersion();
        if (lswVersion != null) {
            log.info("Current app version is {}", lswVersion);
            DataInfo dataInfo = getDataInfo(dataFolder);
            if (dataInfo != null) {

                // This is to prevent double servers running when the user accidentally doubly clicked the icon
                // instead of single click.
                if (dataInfo.getLastExecutionTime() != null) {
                    LocalDateTime lastExecutionTime = LocalDateTime.parse(dataInfo.getLastExecutionTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    if (lastExecutionTime.plusSeconds(Constants.SECONDS_AFTER_NEXT_EXECUTION).isAfter(LocalDateTime.now())) {
                        log.error("Another instance was already started, shutting down!");
                        System.exit(1);
                    }
                }

                if (!dataInfo.getVersion().equals(lswVersion)) {
                    log.warn(
                            "Overwriting current data file as the version does not correspond, data file : {}",
                            dataInfo.getVersion());
                    copyDbFile(osProfile, dataFolder);
                }
                writeDataInfoFile(lswVersion, dataFolder);
            } else {
                log.info("First time the app was started, copying db file and data info file to data folder {}",
                        dataFolder);
                copyDbFile(osProfile, dataFolder);
                writeDataInfoFile(lswVersion, dataFolder);
            }
        } else {
            log.warn("Could not determine app version");
        }

        SpringApplication.run(LuckystackWorkerApplication.class, args);
    }

    private static DataInfo getDataInfo(String dataFolder) {
        DataInfo dataInfo = null;
        try {
            String dataInfoStr = Files.readString(Paths.get(dataFolder + "/data-info.yml"));
            dataInfo = new Yaml().load(dataInfoStr);
        } catch (IOException e) {
            log.info("Data info file not found on app data folder {}", dataFolder);
        }
        return dataInfo;
    }

    private static void createDataFolderWhenMissing(String dataFolder) throws IOException {
        Path path = Paths.get(dataFolder);
        if (!Files.exists(path)) {
            log.info("Creating data folder {}", dataFolder);
            Files.createDirectories(path);
        }
    }

    private static void writeDataInfoFile(String lswVersion, String dataFolder) throws IOException {
        DataInfo dataInfo;
        dataInfo = DataInfo.builder().version(lswVersion).instalationDate(LocalDate.now().toString())
                .lastExecutionTime(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()))
                .build();
        String dataInfoStr = new Yaml().dump(dataInfo);
        Files.writeString(Paths.get(dataFolder + "/data-info.yml"), dataInfoStr);
    }

    private static void copyDbFile(String profile, String dataFolder) throws IOException {
        String resourceFolder = getResourceFolder(profile);
        String lswDbFile = "/lsw_db.mv.db";
        Path dbFile = Paths.get(resourceFolder + lswDbFile);
        Files.copy(dbFile, Paths.get(dataFolder + lswDbFile), StandardCopyOption.REPLACE_EXISTING);
        log.info("Writing new data info file");
    }

    private static String getResourceFolder(String osProfile) {
        String resourceFolder = System.getProperty("user.dir");
        if (Constants.SYSTEM_PROFILE_MAC.equals(osProfile) && !"true".equals(System.getProperty("dev.mode"))) {
            resourceFolder += "/Resources";
        }
        return resourceFolder;
    }
}
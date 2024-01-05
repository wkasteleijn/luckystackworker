package nl.wilcokas.luckystackworker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.yaml.snakeyaml.Yaml;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.DataInfo;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class LuckystackWorkerApplication {

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "false");

        String profile = Util.getActiveOSProfile();
        if (Constants.SYSTEM_PROFILE_WINDOWS.equals(profile)) {
            log.info("Starting electron GUI");
            try {
                Runtime.getRuntime().exec(".\\lsw_gui.exe");
            } catch (IOException e) {
                log.error("Failed to start GUI! ", e);
            }
        }

        log.info("Determining database version and replace it if needed..");
        String lswVersion = Util.getLswVersion();
        if (lswVersion != null) {
            log.info("Current app version is {}", lswVersion);
            String dataFolder = Util.getDataFolder(profile);
            createDataFolderWhenMissing(dataFolder);
            DataInfo dataInfo = getDataInfo(dataFolder);
            if (dataInfo != null) {
                if (!dataInfo.getVersion().equals(lswVersion)) {
                    log.info(
                            "Overwriting current data file as the version does not correspond, data file : {}",
                            dataInfo.getVersion());
                    copyDbFile(profile, dataFolder);
                    writeDataInfoFile(lswVersion, dataFolder);
                }
            } else {
                log.info("First time the app was started, copying db file and data info file to data folder {}",
                        dataFolder);
                copyDbFile(profile, dataFolder);
                writeDataInfoFile(lswVersion, dataFolder);
            }
        } else {
            log.info("Could not determine app version");
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

    private static String getResourceFolder(String profile) {
        String resourceFolder = System.getProperty("user.dir");
        if (Constants.SYSTEM_PROFILE_MAC.equals(profile) && !"true".equals(System.getProperty("dev.mode"))) {
            resourceFolder += "/Resources";
        }
        return resourceFolder;
    }
}
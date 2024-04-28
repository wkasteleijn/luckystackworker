package nl.wilcokas.luckystackworker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class,
        org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class })
public class LuckystackWorkerApplication {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.setProperty("java.awt.headless", "false");

        String osProfile = LswUtil.getActiveOSProfile();
        String dataFolder = LswFileUtil.getDataFolder(osProfile);
        createDataFolderWhenMissing(dataFolder);
        log.info("Current folder is "+System.getProperty("user.dir"));
        if (Constants.SYSTEM_PROFILE_WINDOWS.equals(osProfile)) {
            log.info("Starting electron GUI from windows in current folder");
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
            // This is to prevent double servers running when the user accidentally double
            // clicked the icon instead of single click.
            if (dataInfo != null && dataInfo.getLastExecutionTime() != null) {
                LocalDateTime lastExecutionTime = LocalDateTime.parse(dataInfo.getLastExecutionTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                if (lastExecutionTime.plusSeconds(Constants.SECONDS_AFTER_NEXT_EXECUTION).isAfter(LocalDateTime.now())) {
                    log.error("Another instance was already started, shutting down!");
                    System.exit(1);
                }
            }
            writeDataInfoFile(lswVersion, dataFolder, dataInfo == null ? null : dataInfo.getInstalationDate());
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

    private static void writeDataInfoFile(String lswVersion, String dataFolder, String installationDate) throws IOException {
        DataInfo dataInfo;
        String now = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
        dataInfo = DataInfo.builder().version(lswVersion).instalationDate(installationDate == null ? now : installationDate).lastExecutionTime(now)
                .build();
        String dataInfoStr = new Yaml().dump(dataInfo);
        Files.writeString(Paths.get(dataFolder + "/data-info.yml"), dataInfoStr);
    }
}
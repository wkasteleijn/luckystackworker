package nl.wilcokas.luckystackworker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.wilcokas.luckystackworker.model.Settings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.yaml.snakeyaml.Yaml;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.AppInfo;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswUtil;

@Slf4j
@EnableScheduling
@SpringBootApplication(exclude = {org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
  org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class,
  org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration.class,
  org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
  org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.class,
  org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration.class,
  org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
  org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
  org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class})
public class LuckystackWorkerApplication {

  public static void main(String[] args) throws IOException, InterruptedException {
    System.setProperty("java.awt.headless", "false");

    String osProfile = LswUtil.getActiveOSProfile();
    String dataFolder = LswFileUtil.getDataFolder(osProfile);
    createDataFolderWhenMissing(dataFolder);
    log.info("Current folder is " + System.getProperty("user.dir"));
    AppInfo appInfo = getAppInfo(dataFolder);
    // This is to prevent double servers running when the user accidentally double
    // clicked the icon instead of single click.
    if (appInfo != null && appInfo.getLastExecutionTime() != null) {
      LocalDateTime lastExecutionTime = LocalDateTime.parse(appInfo.getLastExecutionTime(),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      if (lastExecutionTime.plusSeconds(Constants.SECONDS_AFTER_NEXT_EXECUTION).isAfter(LocalDateTime.now())) {
        log.error("Another instance was already started, shutting down!");
        System.exit(1);
      }
    }
    writeAppInfoFile(dataFolder, appInfo == null ? null : appInfo.getInstallationDate());

    SpringApplication.run(LuckystackWorkerApplication.class, args);
  }

  private static AppInfo getAppInfo(String dataFolder) {
    AppInfo appInfo = null;
    try {
      appInfo = new ObjectMapper().readValue(Files.readString(Paths.get(dataFolder + "/app-info.json")), AppInfo.class);
    } catch (IOException e) {
      log.info("App info file not found on app data folder {}", dataFolder);
    }
    return appInfo;
  }

  private static void createDataFolderWhenMissing(String dataFolder) throws IOException {
    Path path = Paths.get(dataFolder);
    if (!Files.exists(path)) {
      log.info("Creating data folder {}", dataFolder);
      Files.createDirectories(path);
    }
  }

  private static void writeAppInfoFile(String dataFolder, String installationDate) throws IOException {
    AppInfo appInfo;
    String now = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
    appInfo = AppInfo.builder().installationDate(installationDate == null ? now : installationDate)
      .lastExecutionTime(now).build();
    new ObjectMapper().writeValue(new File(dataFolder + "/app-info.json"), appInfo);
  }
}

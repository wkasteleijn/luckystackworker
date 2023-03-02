package nl.wilcokas.luckystackworker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.yaml.snakeyaml.Yaml;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.DataInfo;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
@SpringBootApplication
public class LuckystackWorkerApplication {

	public static void main(String[] args) throws IOException {
		System.setProperty("java.awt.headless", "false");

		String profile = System.getProperty("spring.profiles.active");
		if (Constants.SYSTEM_PROFILE_WINDOWS.equals(profile)) {
			log.info("Starting electron GUI");
			Runtime.getRuntime().exec(".\\lsw_gui.exe");
		}

		String lswVersion = Util.getLswVersion();
		if (lswVersion != null) {
			log.info("Current app version is {}", lswVersion);
			String dataFolder = getDataFolder(profile);
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

	private static String getDataFolder(String profile) {
		String dataFolder = System.getProperty("user.home");
		if (Constants.SYSTEM_PROFILE_WINDOWS.equals(profile)) {
			dataFolder += "/.lsw";
		} else if (Constants.SYSTEM_PROFILE_MAC.equals(profile)) {
			dataFolder += "/AppData/Local/LuckyStackWorker";
		}
		return dataFolder;
	}

	private static void writeDataInfoFile(String lswVersion, String dataFolder) throws IOException {
		DataInfo dataInfo;
		dataInfo = DataInfo.builder().version(lswVersion).installationDate(LocalDate.now())
				.build();
		String dataInfoStr = new Yaml().dump(dataInfo);
		Files.writeString(Paths.get(dataFolder + "/data-info.yml"), dataInfoStr);
	}

	private static void copyDbFile(String profile, String dataFolder) throws IOException {
		String resourceFolder = System.getProperty("user.dir");
		if (Constants.SYSTEM_PROFILE_MAC.equals(profile)) {
			resourceFolder += "/Contents/Resources";
		}
		Path dbFile = Paths.get(resourceFolder + "/lsw_db.mv.db");
		Files.copy(dbFile, Paths.get(dataFolder), StandardCopyOption.REPLACE_EXISTING);
		log.info("Writing new data info file");
	}
}
package nl.wilcokas.luckystackworker.api;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.repository.ProfileRepository;
import nl.wilcokas.luckystackworker.service.ReferenceImageService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/reference")
@Slf4j
public class ReferenceController {

	@Autowired
	private ProfileRepository profileRepository;

	@Autowired
	private ReferenceImageService referenceImageService;

	@GetMapping("/open")
	public Profile openReferenceImage(@RequestParam String path) throws IOException {
		final String base64DecodedPath = new String(Base64.getDecoder().decode(path));
		return referenceImageService.selectReferenceImage(base64DecodedPath);
	}

	@GetMapping("/rootfolder")
	public Profile selectRootFolder() {
		JFrame frame = referenceImageService.getParentFrame();
		JFileChooser jfc = referenceImageService
				.getJFileChooser(LuckyStackWorkerContext.getWorkerProperties().get("inputFolder"));
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnValue = jfc.showOpenDialog(frame);
		frame.dispose();
		Profile profile = new Profile();
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFolder = jfc.getSelectedFile();
			String rootFolder = selectedFolder.getAbsolutePath();
			log.info("RootFolder selected {} ", rootFolder);
			referenceImageService.updateSettings(rootFolder, profile);
		}
		return profile;
	}

	@PutMapping("/save")
	public void saveReferenceImage(@RequestBody String path) throws IOException {
		JFrame frame = referenceImageService.getParentFrame();
		// Ignoring path received from frontend as it isn't used.
		String realPath = LuckyStackWorkerContext.getWorkerProperties().get("inputFolder");
		JFileChooser jfc = referenceImageService.getJFileChooser(realPath);
		jfc.setFileFilter(new FileNameExtensionFilter("TIFF", "tif"));
		int returnValue = jfc.showDialog(frame, "Save reference image");
		frame.dispose();
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = jfc.getSelectedFile();
			referenceImageService.saveReferenceImage(selectedFile.getAbsolutePath());
		}
	}

	@PutMapping("/zoomin")
	public void zoomIn() {
		referenceImageService.zoomIn();
	}

	@PutMapping("/zoomout")
	public void zoomOut() {
		referenceImageService.zoomOut();
	}
}

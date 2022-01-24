package nl.wilcokas.planetherapy.api;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ij.io.OpenDialog;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.planetherapy.model.Profile;
import nl.wilcokas.planetherapy.repository.ProfileRepository;
import nl.wilcokas.planetherapy.service.ReferenceImageService;
import nl.wilcokas.planetherapy.util.Util;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/reference")
@Slf4j
public class ReferenceController {

	@Autowired
	private ProfileRepository profileRepository;

	@Autowired
	private ReferenceImageService referenceImageService;

	@PutMapping("/open")
	public String openReferenceImage(@RequestBody String path) {
		OpenDialog od = new OpenDialog("Open Reference Image", null);
		String referenceImage = od.getDirectory() + od.getFileName();
		log.info("Image selected {} ", referenceImage);
		String profileName = Util.deriveProfileFromImageName(referenceImage);
		Profile profile = null;
		if (profileName != null) {
			profile = profileRepository.findByName(profileName)
					.orElseThrow(() -> new ResourceNotFoundException(String.format("Unknown profile %s", profileName)));
		}
		referenceImageService.openReferenceImage(referenceImage, profile);
		return profileName;
	}

	@PutMapping("/save")
	public void saveReferenceImage() {
		referenceImageService.saveReferenceImage();
	}
}

package nl.wilcokas.luckystackworker.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import nl.wilcokas.luckystackworker.LswConfiguration;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.dto.VersionDTO;
import nl.wilcokas.luckystackworker.model.Settings;
import nl.wilcokas.luckystackworker.repository.ProfileRepository;
import nl.wilcokas.luckystackworker.repository.SettingsRepository;
import nl.wilcokas.luckystackworker.service.client.GithubClientService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReferenceImageServiceTest {

    private static final String LATESTVERSIONLOCAL = "6.12.1";
    private static final String NEWERVERSIONSERVER = "6.15.0";
    private static final LocalDateTime TODAY = LocalDateTime.now();
    private static final LocalDateTime MORE_THAN_2_WEEKS_AGO = TODAY.minusDays(15);
    private static final LocalDateTime LESS_THAN_2_WEEKS_AGO = TODAY.minusDays(13);

    @Mock
    private SettingsRepository settingsService;

    @Mock
    private ProfileRepository profileService;

    @Mock
    private FilterService operationService;

    @Mock
    private LuckyStackWorkerContext luckyStackWorkerContext;

    @Mock
    private BuildProperties buildProperties;

    @Mock
    private GithubClientService githubClientService;

    @Mock
    private DeRotationService deRotationService;

    @Mock
    private StackService stackService;

    @Mock
    private ObjectMapper snakeCaseObjectMapper;

    @InjectMocks
    private ReferenceImageService referenceImageService;

    @BeforeEach
    void setup() {
        referenceImageService = new ReferenceImageService(
                settingsService,
                profileService,
                operationService,
                luckyStackWorkerContext,
                snakeCaseObjectMapper,
                buildProperties,
                githubClientService,
                deRotationService,
                stackService);
        ReflectionTestUtils.setField(
                referenceImageService, "snakeCaseObjectMapper", LswConfiguration.createSnakeCaseObjectMapper());
    }

    @Test
    void testGetLatestVersion_firstUsage_noLatestVersionFromServer() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, null);

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        assertEquals(LATESTVERSIONLOCAL, result.getLatestVersion());
        Assertions.assertFalse(result.isNewVersion());
        assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        assertEquals(LATESTVERSIONLOCAL, settings.getLatestKnownVersion());
        assertNull(result.getReleaseNotes());
    }

    @Test
    void testGetLatestVersion_firstUsage_receivedLatestVersionFromServer() {

        Settings settings = mockSettings(null, null);
        mockServerResponse(NEWERVERSIONSERVER);

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        assertEquals(NEWERVERSIONSERVER, result.getLatestVersion());
        assertTrue(result.isNewVersion());
        assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        assertEquals(NEWERVERSIONSERVER, settings.getLatestKnownVersion());
        assertEquals(3, result.getReleaseNotes().size());
        assertEquals(
                "Fixed bug when setting sharpen amount to 0 the next time you start the app it would not work anymore",
                result.getReleaseNotes().get(0));
        assertEquals(
                "Fixed bug in the windows startup that only happened for users with a space character in their username",
                result.getReleaseNotes().get(1));
        assertEquals(
                "Fixed bug in Bilateral Denoise not able to change values for green and blue channel individually.",
                result.getReleaseNotes().get(2));
    }

    @Test
    void testGetLatestVersion_moreThan2WeeksLater_noLatestVersionFromServer() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, MORE_THAN_2_WEEKS_AGO);

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        assertEquals(LATESTVERSIONLOCAL, result.getLatestVersion());
        Assertions.assertFalse(result.isNewVersion());
        assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        assertEquals(LATESTVERSIONLOCAL, settings.getLatestKnownVersion());
        assertNull(result.getReleaseNotes());
    }

    @Test
    void testGetLatestVersion_moreThan2WeeksLater_receivedLatestVersionFromServer() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, MORE_THAN_2_WEEKS_AGO);
        mockServerResponse(NEWERVERSIONSERVER);

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        assertEquals(NEWERVERSIONSERVER, result.getLatestVersion());
        assertTrue(result.isNewVersion());
        assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        assertEquals(NEWERVERSIONSERVER, settings.getLatestKnownVersion());
        assertEquals(3, result.getReleaseNotes().size());
        assertEquals(
                "Fixed bug when setting sharpen amount to 0 the next time you start the app it would not work anymore",
                result.getReleaseNotes().get(0));
        assertEquals(
                "Fixed bug in the windows startup that only happened for users with a space character in their username",
                result.getReleaseNotes().get(1));
        assertEquals(
                "Fixed bug in Bilateral Denoise not able to change values for green and blue channel individually.",
                result.getReleaseNotes().get(2));
    }

    @Test
    void testGetLatestVersion_moreThan2WeeksLater_receivedSameVersionFromServer() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, MORE_THAN_2_WEEKS_AGO);
        mockServerResponse(LATESTVERSIONLOCAL);

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        assertEquals(LATESTVERSIONLOCAL, result.getLatestVersion());
        Assertions.assertFalse(result.isNewVersion());
        assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        assertEquals(LATESTVERSIONLOCAL, settings.getLatestKnownVersion());
        assertNull(result.getReleaseNotes());
    }

    @Test
    void testGetLatestVersion_lessThan2WeeksAgo() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, LESS_THAN_2_WEEKS_AGO);

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        assertEquals(LATESTVERSIONLOCAL, result.getLatestVersion());
        Assertions.assertFalse(result.isNewVersion());
        assertEquals(LESS_THAN_2_WEEKS_AGO, settings.getLatestKnownVersionChecked());
        assertEquals(LATESTVERSIONLOCAL, settings.getLatestKnownVersion());
        assertNull(result.getReleaseNotes());
        Mockito.verifyNoInteractions(githubClientService);
    }

    @Test
    void testGetLatestVersion_receivedUnrecognizedResponseFromServer() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, MORE_THAN_2_WEEKS_AGO);

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        assertEquals(LATESTVERSIONLOCAL, result.getLatestVersion());
        Assertions.assertFalse(result.isNewVersion());
        assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        assertEquals(LATESTVERSIONLOCAL, settings.getLatestKnownVersion());
        assertNull(result.getReleaseNotes());
    }

    @Test
    void testGetLatestVersion_receivedEmptyVersionResponseFromServer() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, MORE_THAN_2_WEEKS_AGO);
        mockServerResponse("");

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        assertEquals(LATESTVERSIONLOCAL, result.getLatestVersion());
        Assertions.assertFalse(result.isNewVersion());
        assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        assertEquals(LATESTVERSIONLOCAL, settings.getLatestKnownVersion());
        assertNull(result.getReleaseNotes());
    }

    @Test
    void testGetLatestVersion_receivedInvalidVersionResponseFromServer() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, MORE_THAN_2_WEEKS_AGO);
        mockServerResponse("100.100");

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        assertEquals(LATESTVERSIONLOCAL, result.getLatestVersion());
        Assertions.assertFalse(result.isNewVersion());
        assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        assertEquals(LATESTVERSIONLOCAL, settings.getLatestKnownVersion());
        assertNull(result.getReleaseNotes());
    }

    private Settings mockSettings(String version, LocalDateTime lastChecked) {
        Settings settings = Settings.builder()
                .latestKnownVersion(version)
                .latestKnownVersionChecked(lastChecked)
                .build();
        when(settingsService.getSettings()).thenReturn(settings);
        return settings;
    }

    private void mockServerResponse(String version) {
        when(githubClientService.getAppInfo()).thenReturn(String.format("""
                {
                    "tag_name": "v%s",
                    "body": "Some random text. ## Release notes\\r\\n- Fixed bug when setting sharpen amount to 0 the next time you start the app it would not work anymore\\r\\n- Fixed bug in the windows startup that only happened for users with a space character in their username\\r\\n- Fixed bug in Bilateral Denoise not able to change values for green and blue channel individually."
                }""", version));
    }
}

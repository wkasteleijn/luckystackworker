package nl.wilcokas.luckystackworker.service;

import java.net.http.HttpClient;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.wilcokas.luckystackworker.LswConfiguration;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.VersionDTO;
import nl.wilcokas.luckystackworker.model.Settings;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceImageServiceTest {

    private static final String LATESTVERSIONLOCAL = "6.12.1";
    private static final String NEWERVERSIONSERVER = "6.15.0";
    private static final LocalDateTime TODAY = LocalDateTime.now();
    private static final LocalDateTime MORE_THAN_2_WEEKS_AGO = TODAY.minusDays(15);
    private static final LocalDateTime LESS_THAN_2_WEEKS_AGO = TODAY.minusDays(13);

    @Mock
    private SettingsService settingsService;
    @Mock
    private HttpService httpService;
    @Mock
    private ProfileService profileService;
    @Mock
    private FilterService operationService;
    @Mock
    private LuckyStackWorkerContext luckyStackWorkerContext;

    @InjectMocks
    private ReferenceImageService referenceImageService;
    @Mock
    private ObjectMapper snakeCaseObjectMapper;

    @BeforeEach
    void setup() {
        referenceImageService = new ReferenceImageService(settingsService, httpService, profileService, operationService, luckyStackWorkerContext, snakeCaseObjectMapper);
        ReflectionTestUtils.setField(referenceImageService, "currentVersion", "0.0.0");
        ReflectionTestUtils.setField(referenceImageService, "githubApiUrl", "https://api.github.com/repos/wilcokas/luckystackworker/releases/latest");
        ReflectionTestUtils.setField(referenceImageService, "snakeCaseObjectMapper", LswConfiguration.createSnakeCaseObjectMapper());
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
        assertEquals("Fixed bug when setting sharpen amount to 0 the next time you start the app it would not work anymore", result.getReleaseNotes().get(0));
        assertEquals("Fixed bug in the windows startup that only happened for users with a space character in their username", result.getReleaseNotes().get(1));
        assertEquals("Fixed bug in Bilateral Denoise not able to change values for green and blue channel individually.", result.getReleaseNotes().get(2));
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
        assertEquals("Fixed bug when setting sharpen amount to 0 the next time you start the app it would not work anymore", result.getReleaseNotes().get(0));
        assertEquals("Fixed bug in the windows startup that only happened for users with a space character in their username", result.getReleaseNotes().get(1));
        assertEquals("Fixed bug in Bilateral Denoise not able to change values for green and blue channel individually.", result.getReleaseNotes().get(2));
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
        Mockito.verifyNoInteractions(httpService);
    }

    @Test
    void testGetLatestVersion_receivedUnrecognizedResponseFromServer() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, MORE_THAN_2_WEEKS_AGO);
        when(httpService.sendHttpGetRequest(eq(HttpClient.Version.HTTP_1_1), anyString(),
                eq(Constants.VERSION_REQUEST_TIMEOUT))).thenReturn("<html><body>nothing</body></html>");

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
        Settings settings = Settings.builder().latestKnownVersion(version).latestKnownVersionChecked(lastChecked)
                .build();
        when(settingsService.getSettings()).thenReturn(settings);
        return settings;
    }

    private void mockServerResponse(String version) {
        when(httpService.sendHttpGetRequest(eq(HttpClient.Version.HTTP_1_1), anyString(),
                eq(Constants.VERSION_REQUEST_TIMEOUT))).thenReturn(String.format("""
                {
                    "tag_name": "v%s", 
                    "body": "Some random text. ## Release notes\\r\\n- Fixed bug when setting sharpen amount to 0 the next time you start the app it would not work anymore\\r\\n- Fixed bug in the windows startup that only happened for users with a space character in their username\\r\\n- Fixed bug in Bilateral Denoise not able to change values for green and blue channel individually."
                }""",
                version));
    }

}

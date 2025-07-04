package nl.wilcokas.luckystackworker.service;

import java.net.http.HttpClient;
import java.time.LocalDateTime;

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

@ExtendWith(MockitoExtension.class)
class ReferenceImageServiceTest {

    private static final String LATESTVERSIONLOCAL = "1.5.0";
    private static final String NEWERVERSIONSERVER = "1.6.1";
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
    private OperationService operationService;
    @Mock
    private LuckyStackWorkerContext luckyStackWorkerContext;

    @InjectMocks
    private ReferenceImageService referenceImageService;

    @BeforeEach
    void setup() {
        referenceImageService = new ReferenceImageService(settingsService, httpService, profileService, operationService, luckyStackWorkerContext);
    }

    @Test
    void testGetLatestVersion_firstUsage_noLatestVersionFromServer() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, null);

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        Assertions.assertEquals(LATESTVERSIONLOCAL, result.getLatestVersion());
        Assertions.assertFalse(result.isNewVersion());
        Assertions.assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        Assertions.assertEquals(LATESTVERSIONLOCAL, settings.getLatestKnownVersion());
    }

    @Test
    void testGetLatestVersion_firstUsage_receivedLatestVersionFromServer() {

        Settings settings = mockSettings(null, null);
        mockServerResponse(NEWERVERSIONSERVER);

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        Assertions.assertEquals(NEWERVERSIONSERVER, result.getLatestVersion());
        Assertions.assertTrue(result.isNewVersion());
        Assertions.assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        Assertions.assertEquals(NEWERVERSIONSERVER, settings.getLatestKnownVersion());
    }

    @Test
    void testGetLatestVersion_moreThan2WeeksLater_noLatestVersionFromServer() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, MORE_THAN_2_WEEKS_AGO);

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        Assertions.assertEquals(LATESTVERSIONLOCAL, result.getLatestVersion());
        Assertions.assertFalse(result.isNewVersion());
        Assertions.assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        Assertions.assertEquals(LATESTVERSIONLOCAL, settings.getLatestKnownVersion());
    }

    @Test
    void testGetLatestVersion_moreThan2WeeksLater_receivedLatestVersionFromServer() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, MORE_THAN_2_WEEKS_AGO);
        mockServerResponse(NEWERVERSIONSERVER);

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        Assertions.assertEquals(NEWERVERSIONSERVER, result.getLatestVersion());
        Assertions.assertTrue(result.isNewVersion());
        Assertions.assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        Assertions.assertEquals(NEWERVERSIONSERVER, settings.getLatestKnownVersion());
    }

    @Test
    void testGetLatestVersion_moreThan2WeeksLater_receivedSameVersionFromServer() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, MORE_THAN_2_WEEKS_AGO);
        mockServerResponse(LATESTVERSIONLOCAL);

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        Assertions.assertEquals(LATESTVERSIONLOCAL, result.getLatestVersion());
        Assertions.assertFalse(result.isNewVersion());
        Assertions.assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        Assertions.assertEquals(LATESTVERSIONLOCAL, settings.getLatestKnownVersion());
    }

    @Test
    void testGetLatestVersion_lessThan2WeeksAgo() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, LESS_THAN_2_WEEKS_AGO);

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        Assertions.assertEquals(LATESTVERSIONLOCAL, result.getLatestVersion());
        Assertions.assertFalse(result.isNewVersion());
        Assertions.assertEquals(LESS_THAN_2_WEEKS_AGO, settings.getLatestKnownVersionChecked());
        Assertions.assertEquals(LATESTVERSIONLOCAL, settings.getLatestKnownVersion());
        Mockito.verifyNoInteractions(httpService);
    }

    @Test
    void testGetLatestVersion_receivedUnrecognizedResponseFromServer() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, MORE_THAN_2_WEEKS_AGO);
        Mockito.when(httpService.sendHttpGetRequest(HttpClient.Version.HTTP_1_1, Constants.VERSION_URL,
                Constants.VERSION_REQUEST_TIMEOUT)).thenReturn("<html><body>nothing</body></html>");

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        Assertions.assertEquals(LATESTVERSIONLOCAL, result.getLatestVersion());
        Assertions.assertFalse(result.isNewVersion());
        Assertions.assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        Assertions.assertEquals(LATESTVERSIONLOCAL, settings.getLatestKnownVersion());
    }

    @Test
    void testGetLatestVersion_receivedEmptyVersionResponseFromServer() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, MORE_THAN_2_WEEKS_AGO);
        mockServerResponse("");

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        Assertions.assertEquals(LATESTVERSIONLOCAL, result.getLatestVersion());
        Assertions.assertFalse(result.isNewVersion());
        Assertions.assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        Assertions.assertEquals(LATESTVERSIONLOCAL, settings.getLatestKnownVersion());
    }

    @Test
    void testGetLatestVersion_receivedMissingEndMarkerResponseFromServer() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, MORE_THAN_2_WEEKS_AGO);
        Mockito.when(httpService.sendHttpGetRequest(HttpClient.Version.HTTP_1_1, Constants.VERSION_URL,
                Constants.VERSION_REQUEST_TIMEOUT)).thenReturn("<html><body>{lswVersion:%s</body></html>");

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        Assertions.assertEquals(LATESTVERSIONLOCAL, result.getLatestVersion());
        Assertions.assertFalse(result.isNewVersion());
        Assertions.assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        Assertions.assertEquals(LATESTVERSIONLOCAL, settings.getLatestKnownVersion());
    }

    @Test
    void testGetLatestVersion_receivedInvalidVersionResponseFromServer() {

        Settings settings = mockSettings(LATESTVERSIONLOCAL, MORE_THAN_2_WEEKS_AGO);
        mockServerResponse("100.100.100");

        VersionDTO result = referenceImageService.getLatestVersion(TODAY);

        Assertions.assertEquals(LATESTVERSIONLOCAL, result.getLatestVersion());
        Assertions.assertFalse(result.isNewVersion());
        Assertions.assertEquals(TODAY, settings.getLatestKnownVersionChecked());
        Assertions.assertEquals(LATESTVERSIONLOCAL, settings.getLatestKnownVersion());
    }

    private Settings mockSettings(String version, LocalDateTime lastChecked) {
        Settings settings = Settings.builder().latestKnownVersion(version).latestKnownVersionChecked(lastChecked)
                .build();
        Mockito.when(settingsService.getSettings()).thenReturn(settings);
        return settings;
    }

    private void mockServerResponse(String version) {
        Mockito.when(httpService.sendHttpGetRequest(HttpClient.Version.HTTP_1_1, Constants.VERSION_URL,
                Constants.VERSION_REQUEST_TIMEOUT)).thenReturn(String.format("<html>{lswVersion:%s}</html>", version));
    }

}

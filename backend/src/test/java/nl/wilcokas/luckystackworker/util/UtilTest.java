package nl.wilcokas.luckystackworker.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UtilTest {
        private static final String PATH_WITH_DOT_IN_FOLDER = "/pad.x/file.png";
        private static final String PATH_WITH_DOT_IN_FILE = "/pad.x/file.xyz.png";
        private static final String PATH_WITHOUT_FOLDER = "file.png";
        private static final String PATH_WITHOUT_EXTENSION_AND_FOLDER = "file";
        private static final String PATH_WITH_DOT_IN_FOLDER_PREFIX = "/x/.pad/file.PNG";
        private static final String PATH_WITH_NO_EXTENSION = "/pad/file";
        private static final String PATH_WITH_ONLY_EXTENSION = "/pad/.file";

        @Test
        void testGetFilename() {
            String expectedFilename = "file";
            Assertions.assertEquals(expectedFilename, Util.getFilename(PATH_WITH_DOT_IN_FOLDER));
            Assertions.assertEquals(expectedFilename, Util.getFilename(PATH_WITHOUT_FOLDER));
            Assertions.assertEquals(expectedFilename, Util.getFilename(PATH_WITHOUT_EXTENSION_AND_FOLDER));
            Assertions.assertEquals(expectedFilename, Util.getFilename(PATH_WITH_DOT_IN_FOLDER_PREFIX));
            Assertions.assertEquals(expectedFilename, Util.getFilename(PATH_WITH_NO_EXTENSION));
            Assertions.assertEquals(".file", Util.getFilename(PATH_WITH_ONLY_EXTENSION));
            Assertions.assertEquals("file.xyz", Util.getFilename(PATH_WITH_DOT_IN_FILE));
        }

        @Test
        void testGetFilenameExtension() {
            String expectedFilenameExtension = "png";
            Assertions.assertEquals(expectedFilenameExtension, Util.getFilenameExtension(PATH_WITH_DOT_IN_FOLDER));
            Assertions.assertEquals(expectedFilenameExtension, Util.getFilenameExtension(PATH_WITHOUT_FOLDER));
            Assertions.assertEquals("", Util.getFilenameExtension(PATH_WITHOUT_EXTENSION_AND_FOLDER));
            Assertions.assertEquals(expectedFilenameExtension, Util.getFilenameExtension(PATH_WITH_DOT_IN_FOLDER_PREFIX));
            Assertions.assertEquals("", Util.getFilenameExtension(PATH_WITH_NO_EXTENSION));
            Assertions.assertEquals("", Util.getFilenameExtension(PATH_WITH_ONLY_EXTENSION));
        }
    
}

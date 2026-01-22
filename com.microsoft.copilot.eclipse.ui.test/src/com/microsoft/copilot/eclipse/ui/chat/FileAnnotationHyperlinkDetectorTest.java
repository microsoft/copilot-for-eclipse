package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.utils.FileUtils;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;

/**
 * Tests for path normalization in FileAnnotationHyperlinkDetector.
 * Tests the FileUtils.normalizeToUri utility method which handles Windows and Unix paths.
 */
class FileAnnotationHyperlinkDetectorTest {

  @Test
  void testNormalizeWindowsAbsolutePath() {
    if (PlatformUtils.isWindows()) {
      // Test Windows absolute path
      String windowsPath = "C:\\Users\\user\\file.java";
      String result = FileUtils.normalizeToUri(windowsPath);
      assertNotNull(result);
      assertEquals("file:///C:/Users/user/file.java", result);
    }
  }

  @Test
  void testNormalizeWindowsPathWithLineNumber() {
    if (PlatformUtils.isWindows()) {
      // Test Windows path with line number fragment
      String windowsPath = "C:\\Users\\user\\file.java#L100";
      String result = FileUtils.normalizeToUri(windowsPath);
      assertNotNull(result);
      assertEquals("file:///C:/Users/user/file.java#L100", result);
    }
  }

  @Test
  void testNormalizeWindowsPathWithLineRange() {
    if (PlatformUtils.isWindows()) {
      // Test Windows path with line range fragment
      String windowsPath = "C:\\file.java#L10-L20";
      String result = FileUtils.normalizeToUri(windowsPath);
      assertNotNull(result);
      assertEquals("file:///C:/file.java#L10-L20", result);
    }
  }

  @Test
  void testNormalizeUnixAbsolutePath() {
    if (!PlatformUtils.isWindows()) {
      // Test Unix absolute path
      String unixPath = "/home/user/file.java";
      String result = FileUtils.normalizeToUri(unixPath);
      assertNotNull(result);
      assertEquals("file:///home/user/file.java", result);
    }
  }

  @Test
  void testNormalizeUnixPathWithLineNumber() {
    if (!PlatformUtils.isWindows()) {
      // Test Unix path with line number fragment
      String unixPath = "/home/user/file.java#L50";
      String result = FileUtils.normalizeToUri(unixPath);
      assertNotNull(result);
      assertEquals("file:///home/user/file.java#L50", result);
    }
  }

  @Test
  void testNormalizeUriFormattedPath() {
    // Test already properly formatted URI
    String uriPath = "file:///C:/Users/user/file.java";
    String result = FileUtils.normalizeToUri(uriPath);
    assertNotNull(result);
    assertEquals("file:///C:/Users/user/file.java", result);
  }

  @Test
  void testNormalizeUriWithFragment() {
    // Test URI with fragment
    String uriPath = "file:///home/user/file.java#L25";
    String result = FileUtils.normalizeToUri(uriPath);
    assertNotNull(result);
    assertEquals("file:///home/user/file.java#L25", result);
  }

  @Test
  void testNormalizeWindowsPathWithSpaces() {
    if (PlatformUtils.isWindows()) {
      // Test Windows path with spaces
      String windowsPath = "C:\\My Files\\project\\file.java";
      String result = FileUtils.normalizeToUri(windowsPath);
      assertNotNull(result);
      // Paths.get().toUri() properly encodes spaces
      assertTrue(result.startsWith("file:///C:/"));
      assertTrue(result.contains("My%20Files") || result.contains("My Files"));
    }
  }

  @Test
  void testNormalizeUnixPathWithSpaces() {
    if (!PlatformUtils.isWindows()) {
      // Test Unix path with spaces
      String unixPath = "/home/user/my project/file.java";
      String result = FileUtils.normalizeToUri(unixPath);
      assertNotNull(result);
      assertTrue(result.startsWith("file:///home/user/"));
    }
  }

  @Test
  void testNormalizeNullPath() {
    // Test null input
    String result = FileUtils.normalizeToUri(null);
    assertNull(result);
  }

  @Test
  void testNormalizeEmptyPath() {
    // Test empty string
    String result = FileUtils.normalizeToUri("");
    assertNull(result);
  }

  @Test
  void testNormalizeRelativePath() {
    // Test relative path - should return null since it cannot be resolved to URI
    String relativePath = "src/main/java/File.java";
    String result = FileUtils.normalizeToUri(relativePath);
    assertNull(result);
  }

  @Test
  void testNormalizeWindowsUncPath() {
    if (PlatformUtils.isWindows()) {
      // Test Windows UNC path
      String uncPath = "\\\\server\\share\\file.java";
      String result = FileUtils.normalizeToUri(uncPath);
      assertNotNull(result);
      assertTrue(result.startsWith("file:"));
    }
  }

  @Test
  void testNormalizeHttpUri() {
    // Test HTTP URI (should work as valid URI)
    String httpUri = "http://example.com/file.java";
    String result = FileUtils.normalizeToUri(httpUri);
    assertNotNull(result);
    assertEquals("http://example.com/file.java", result);
  }

  @Test
  void testNormalizeWindowsPathVariousDriveLetters() {
    if (PlatformUtils.isWindows()) {
      // Test various drive letters
      for (char drive : new char[]{'D', 'E', 'Z'}) {
        String path = drive + ":\\folder\\file.java";
        String result = FileUtils.normalizeToUri(path);
        assertNotNull(result, "Failed for drive " + drive);
        assertTrue(result.startsWith("file:///" + drive + ":/"),
            "Expected file URI for drive " + drive + ", got: " + result);
      }
    }
  }

  @Test
  void testNormalizePreservesFragmentFormat() {
    if (PlatformUtils.isWindows()) {
      // Test that various fragment formats are preserved
      String path1 = "C:\\file.java#L1";
      String result1 = FileUtils.normalizeToUri(path1);
      assertTrue(result1.endsWith("#L1"));

      String path2 = "C:\\file.java#line123";
      String result2 = FileUtils.normalizeToUri(path2);
      assertTrue(result2.endsWith("#line123"));
    }
  }
}


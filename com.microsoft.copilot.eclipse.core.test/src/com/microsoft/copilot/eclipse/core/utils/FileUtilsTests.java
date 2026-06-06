// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileUtilsTests {

  @Test
  void testGetLocalFilePath_absolutePath_returnsNormalizedPath(@TempDir Path tempDir) {
    Path expected = tempDir.resolve("external-file.txt").toAbsolutePath().normalize();

    assertEquals(expected, FileUtils.getLocalFilePath(expected.toString()));
  }

  @Test
  void testGetLocalFilePath_fileUriWithFragment_ignoresFragment(@TempDir Path tempDir) {
    Path expected = tempDir.resolve("external-file.txt").toAbsolutePath().normalize();

    assertEquals(expected, FileUtils.getLocalFilePath(expected.toUri() + "#L10"));
  }

  @Test
  void testGetLocalFilePath_relativePath_returnsNull() {
    assertNull(FileUtils.getLocalFilePath("src/main/java/File.java"));
  }

  @Test
  void testGetLocalFilePath_nonFileUri_returnsNull() {
    assertNull(FileUtils.getLocalFilePath("https://example.com/file.java"));
  }
}

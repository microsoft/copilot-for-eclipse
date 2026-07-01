// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  @Test
  void testIsPathWithin_directChild_returnsTrue(@TempDir Path tempDir) {
    assertTrue(FileUtils.isPathWithin(tempDir, tempDir.resolve("file.txt"), false));
  }

  @Test
  void testIsPathWithin_nestedDescendant_returnsTrue(@TempDir Path tempDir) {
    assertTrue(FileUtils.isPathWithin(tempDir, tempDir.resolve("sub/deep/file.txt"), false));
  }

  @Test
  void testIsPathWithin_self_returnsTrueWhenNotStrict(@TempDir Path tempDir) {
    assertTrue(FileUtils.isPathWithin(tempDir, tempDir, false));
  }

  @Test
  void testIsPathWithin_self_returnsFalseWhenStrict(@TempDir Path tempDir) {
    assertFalse(FileUtils.isPathWithin(tempDir, tempDir, true));
  }

  @Test
  void testIsPathWithin_descendant_returnsTrueWhenStrict(@TempDir Path tempDir) {
    assertTrue(FileUtils.isPathWithin(tempDir, tempDir.resolve("file.txt"), true));
  }

  @Test
  void testIsPathWithin_sibling_returnsFalse(@TempDir Path tempDir) {
    Path parent = tempDir.resolve("parent");
    Path sibling = tempDir.resolve("other/file.txt");
    assertFalse(FileUtils.isPathWithin(parent, sibling, false));
  }

  @Test
  void testIsPathWithin_parent_returnsFalse(@TempDir Path tempDir) {
    Path child = tempDir.resolve("child");
    assertFalse(FileUtils.isPathWithin(child, tempDir, false));
  }

  @Test
  void testIsPathWithin_nullArguments_returnFalse(@TempDir Path tempDir) {
    assertFalse(FileUtils.isPathWithin(null, tempDir, false));
    assertFalse(FileUtils.isPathWithin(tempDir, null, false));
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Bundle;

class BundleUtilsTests {

  private static final String PATH = "resources/sample.dat";

  private static URL writeTempFile(Path dir, String name, byte[] content) throws Exception {
    Path file = dir.resolve(name);
    Files.write(file, content);
    return file.toUri().toURL();
  }

  private static Bundle mockBundle(String path, URL entry) {
    Bundle bundle = mock(Bundle.class);
    when(bundle.getEntry(path)).thenReturn(entry);
    return bundle;
  }

  @Test
  void readResourceAsString_returnsUtf8Content(@TempDir Path dir) throws Exception {
    String content = "Hello, \u00e4\u00f6\u00fc \u2013 SVG <svg/>";
    URL url = writeTempFile(dir, "sample.dat", content.getBytes(StandardCharsets.UTF_8));
    Bundle bundle = mockBundle(PATH, url);

    assertEquals(content, BundleUtils.readResourceAsString(bundle, PATH));
  }

  @Test
  void readResourceAsBytes_returnsRawBytes(@TempDir Path dir) throws Exception {
    byte[] content = {0, 1, 2, 3, (byte) 0xFF, 10, 20};
    URL url = writeTempFile(dir, "sample.dat", content);
    Bundle bundle = mockBundle(PATH, url);

    assertArrayEquals(content, BundleUtils.readResourceAsBytes(bundle, PATH));
  }

  @Test
  void readResourceAsDataUri_encodesBase64WithMimeType(@TempDir Path dir) throws Exception {
    byte[] content = {10, 20, 30, 40};
    URL url = writeTempFile(dir, "sample.dat", content);
    Bundle bundle = mockBundle(PATH, url);

    String dataUri = BundleUtils.readResourceAsDataUri(bundle, PATH, "image/png");

    String prefix = "data:image/png;base64,";
    assertTrue(dataUri.startsWith(prefix), "unexpected data URI: " + dataUri);
    byte[] decoded = Base64.getDecoder().decode(dataUri.substring(prefix.length()));
    assertArrayEquals(content, decoded);
  }

  @Test
  void readResourceAsString_missingEntry_returnsNull() {
    Bundle bundle = mockBundle(PATH, null);
    assertNull(BundleUtils.readResourceAsString(bundle, PATH));
  }

  @Test
  void readResourceAsBytes_missingEntry_returnsNull() {
    Bundle bundle = mockBundle(PATH, null);
    assertNull(BundleUtils.readResourceAsBytes(bundle, PATH));
  }

  @Test
  void readResourceAsDataUri_missingEntry_returnsEmptyString() {
    Bundle bundle = mockBundle(PATH, null);
    assertEquals("", BundleUtils.readResourceAsDataUri(bundle, PATH, "image/png"));
  }

  @Test
  void readResourceAsBytes_nullBundle_returnsNull() {
    assertNull(BundleUtils.readResourceAsBytes(null, PATH));
  }

  @Test
  void readResourceAsBytes_nullPath_returnsNull() {
    assertNull(BundleUtils.readResourceAsBytes(mock(Bundle.class), null));
  }
}

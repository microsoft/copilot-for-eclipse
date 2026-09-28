// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

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
  void testGetResourceAsUri_withLocation_returnsLspEclipseUri() throws Exception {
    IResource resource = mock(IResource.class);
    IPath location = mock(IPath.class);
    when(resource.getLocation()).thenReturn(location);
    URI expected = new URI("file:///project/file.txt");

    try (MockedStatic<LSPEclipseUtils> lspMock = mockStatic(LSPEclipseUtils.class)) {
      lspMock.when(() -> LSPEclipseUtils.toUri(resource)).thenReturn(expected);

      assertEquals(expected, FileUtils.getResourceAsUri(resource));
    }
  }

  @Test
  void testGetResourceAsUri_withLocationDerivedFromAbsolutePath_prefersFileUriOverLocationUri() throws Exception {
    IResource resource = mock(IResource.class);
    IPath location = mock(IPath.class);
    when(location.toFile()).thenReturn(new File("/someproject/somefile.txt"));
    when(resource.getLocation()).thenReturn(location);
    // getLocationURI() reflects the source-control provider's scheme; it must be ignored since
    // getLocation() is non-null and LSPEclipseUtils.toUri() resolves the local filesystem URI first.
    when(resource.getLocationURI()).thenReturn(new URI("sourcecontrol:///someproject/somefile.txt"));
    URI expected = new URI("file:///someproject/somefile.txt");

    try (MockedStatic<LSPEclipseUtils> lspMock = mockStatic(LSPEclipseUtils.class)) {
      lspMock.when(() -> LSPEclipseUtils.toUri(resource)).thenReturn(expected);

      assertEquals(expected, FileUtils.getResourceAsUri(resource));
    }
  }

  @Test
  void testGetResourceAsUri_withoutLocationButWithLocationUri_returnsLocationUri() throws Exception {
    IResource resource = mock(IResource.class);
    when(resource.getLocation()).thenReturn(null);
    URI expected = new URI("semanticfs:///project/file.txt");
    when(resource.getLocationURI()).thenReturn(expected);

    assertEquals(expected, FileUtils.getResourceAsUri(resource));
  }

  @Test
  void testGetResourceAsUri_withoutLocationAndLocationUri_returnsPlatformResourceUri() throws Exception {
    IResource resource = mock(IResource.class);
    IPath fullPath = mock(IPath.class);
    when(resource.getLocation()).thenReturn(null);
    when(resource.getLocationURI()).thenReturn(null);
    when(resource.getFullPath()).thenReturn(fullPath);
    when(fullPath.toPortableString()).thenReturn("/Project/file.txt");

    assertEquals(new URI("platform:/resource/Project/file.txt"), FileUtils.getResourceAsUri(resource));
  }
}

package com.microsoft.copilot.eclipse.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ChatMessageUtilsTests {

  @Test
  void testIsImageFile_withImageExtensions() {
    IFile pngFile = Mockito.mock(IFile.class);
    Mockito.when(pngFile.getFileExtension()).thenReturn("png");
    assertTrue(ChatMessageUtils.isImageFile(pngFile));

    IFile jpgFile = Mockito.mock(IFile.class);
    Mockito.when(jpgFile.getFileExtension()).thenReturn("jpg");
    assertTrue(ChatMessageUtils.isImageFile(jpgFile));
  }

  @Test
  void testIsImageFile_withNonImageExtension() {
    IFile txtFile = Mockito.mock(IFile.class);
    Mockito.when(txtFile.getFileExtension()).thenReturn("txt");
    assertFalse(ChatMessageUtils.isImageFile(txtFile));
  }

  @Test
  void testIsImageFile_withNullFileOrExtension() {
    assertFalse(ChatMessageUtils.isImageFile(null));
    IFile file = Mockito.mock(IFile.class);
    Mockito.when(file.getFileExtension()).thenReturn(null);
    assertFalse(ChatMessageUtils.isImageFile(file));
  }

  @Test
  void testConvertImageToBase64_validImage() throws CoreException, IOException {
    IFile pngFile = Mockito.mock(IFile.class);
    Mockito.when(pngFile.getFileExtension()).thenReturn("png");
    byte[] imageBytes = new byte[] { 1, 2, 3, 4, 5 };
    InputStream inputStream = new ByteArrayInputStream(imageBytes);
    Mockito.when(pngFile.getContents()).thenReturn(inputStream);
    String base64 = ChatMessageUtils.convertImageToBase64(pngFile);
    assertNotNull(base64);
    assertTrue(base64.startsWith("data:image/png;base64,"));
  }

  @Test
  void testConvertImageToBase64_nonImageFile() {
    IFile txtFile = Mockito.mock(IFile.class);
    Mockito.when(txtFile.getFileExtension()).thenReturn("txt");
    assertNull(ChatMessageUtils.convertImageToBase64(txtFile));
  }

  @Test
  void testCreateMessageWithImages_modelSupportsVision() throws CoreException, IOException {
    IFile pngFile = Mockito.mock(IFile.class);
    Mockito.when(pngFile.getFileExtension()).thenReturn("png");
    Mockito.when(pngFile.getContents()).thenReturn(new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
    var result = ChatMessageUtils.createMessageWithImages("hello", List.of(pngFile), true);
    assertTrue(result.isRight());
    assertEquals(2, result.getRight().size()); // text + image
  }

  @Test
  void testCreateMessageWithImages_modelDoesNotSupportVision() {
    IFile pngFile = Mockito.mock(IFile.class);
    Mockito.when(pngFile.getFileExtension()).thenReturn("png");
    var result = ChatMessageUtils.createMessageWithImages("hello", List.of(pngFile), false);
    assertTrue(result.isLeft());
    assertEquals("hello", result.getLeft());
  }
}

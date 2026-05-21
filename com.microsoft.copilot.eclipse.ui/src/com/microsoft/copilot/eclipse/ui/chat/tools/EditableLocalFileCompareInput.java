// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Editable local file compare input class to handle file content editing on the compare editor.
 */
final class EditableLocalFileCompareInput implements ITypedElement, IEncodedStreamContentAccessor, IEditableContent {
  private final Path file;
  private byte[] modifiedContent = null;

  /**
   * Constructor for EditableLocalFileCompareInput.
   *
   * @param file The local file to be edited.
   */
  EditableLocalFileCompareInput(Path file) {
    this.file = normalizeLocalPath(file);
  }

  @Override
  public String getName() {
    Path fileName = file.getFileName();
    return fileName == null ? file.toString() : fileName.toString();
  }

  @Override
  public Image getImage() {
    return null;
  }

  @Override
  public String getType() {
    return getLocalFileExtension(file);
  }

  @Override
  public InputStream getContents() throws CoreException {
    if (modifiedContent != null) {
      return new ByteArrayInputStream(modifiedContent);
    }
    try {
      return Files.newInputStream(file);
    } catch (IOException e) {
      throw new CoreException(Status.error("Error reading local file", e));
    }
  }

  @Override
  public String getCharset() throws CoreException {
    return StandardCharsets.UTF_8.name();
  }

  @Override
  public boolean isEditable() {
    return true;
  }

  @Override
  public void setContent(byte[] newContent) {
    this.modifiedContent = newContent;
  }

  @Override
  public ITypedElement replace(ITypedElement dest, ITypedElement src) {
    if (src instanceof IStreamContentAccessor sca) {
      try (InputStream is = sca.getContents()) {
        modifiedContent = is.readAllBytes();
      } catch (IOException | CoreException e) {
        CopilotCore.LOGGER.error("Error occurred while replacing local file content", e);
      }
    }
    return this;
  }

  private static Path normalizeLocalPath(Path file) {
    return file.toAbsolutePath().normalize();
  }

  private static String getLocalFileExtension(Path file) {
    String name = file.getFileName() == null ? file.toString() : file.getFileName().toString();
    int index = name.lastIndexOf('.');
    if (index < 0 || index == name.length() - 1) {
      return "";
    }
    return name.substring(index + 1);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(file);
    result = 31 * result + Arrays.hashCode(modifiedContent);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof EditableLocalFileCompareInput other)) {
      return false;
    }
    return Objects.equals(file, other.file) && Arrays.equals(modifiedContent, other.modifiedContent);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("file", file);
    builder.append("modifiedContent", modifiedContent);
    return builder.toString();
  }
}
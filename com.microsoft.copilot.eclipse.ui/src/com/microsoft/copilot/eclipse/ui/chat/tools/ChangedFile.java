// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.nio.file.Path;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.core.resources.IFile;

/**
 * Represents a file tracked in the file change summary bar.
 */
public final class ChangedFile {
  private final IFile workspaceFile;
  private final Path localPath;

  private ChangedFile(IFile workspaceFile, Path localPath) {
    this.workspaceFile = workspaceFile;
    this.localPath = localPath;
  }

  /**
   * Creates a changed file entry for a workspace file.
   *
   * @param file the workspace file
   * @return the changed file entry
   */
  public static ChangedFile workspace(IFile file) {
    return new ChangedFile(Objects.requireNonNull(file), null);
  }

  /**
   * Creates a changed file entry for a local file.
   *
   * @param path the local file path
   * @return the changed file entry
   */
  public static ChangedFile local(Path path) {
    return new ChangedFile(null, normalize(path));
  }

  /**
   * Returns true if this entry represents a workspace file.
   *
   * @return true for workspace files, false for local files
   */
  public boolean isWorkspaceFile() {
    return workspaceFile != null;
  }

  /**
   * Gets the workspace file for this entry.
   *
   * @return the workspace file, or null for local files
   */
  public IFile getWorkspaceFile() {
    return workspaceFile;
  }

  /**
   * Gets the local path for this entry.
   *
   * @return the local path, or null for workspace files
   */
  public Path getLocalPath() {
    return localPath;
  }

  /**
   * Gets the display name for this file.
   *
   * @return the file name
   */
  public String getName() {
    if (workspaceFile != null) {
      return workspaceFile.getName();
    }
    Path fileName = localPath.getFileName();
    return fileName == null ? localPath.toString() : fileName.toString();
  }

  /**
   * Gets the display path for this file.
   *
   * @return the workspace path or local filesystem path
   */
  public String getDisplayPath() {
    if (workspaceFile != null) {
      return workspaceFile.getFullPath().toString();
    }
    return localPath.toString();
  }

  private static Path normalize(Path path) {
    return Objects.requireNonNull(path).toAbsolutePath().normalize();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ChangedFile other)) {
      return false;
    }
    return Objects.equals(workspaceFile, other.workspaceFile) && Objects.equals(localPath, other.localPath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workspaceFile, localPath);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("workspaceFile", workspaceFile);
    builder.append("localPath", localPath);
    return builder.toString();
  }
}
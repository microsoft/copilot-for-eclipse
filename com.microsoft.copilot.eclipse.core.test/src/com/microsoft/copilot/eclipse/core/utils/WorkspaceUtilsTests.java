// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceUtilsTests {

  @TempDir
  Path tempDir;

  @Test
  void testIsGitRepository_withGitFolder() throws IOException {
    // Create a temporary .git directory
    Path gitDir = tempDir.resolve(".git");
    Files.createDirectory(gitDir);

    IProject project = mock(IProject.class);
    IPath projectLocation = mock(IPath.class);

    when(project.isAccessible()).thenReturn(true);
    when(project.getLocation()).thenReturn(projectLocation);
    when(projectLocation.toFile()).thenReturn(tempDir.toFile());

    assertTrue(WorkspaceUtils.isGitRepository(project));
  }

  @Test
  void testIsGitRepository_withoutGitFolder() {
    IProject project = mock(IProject.class);
    IPath projectLocation = mock(IPath.class);

    when(project.isAccessible()).thenReturn(true);
    when(project.getLocation()).thenReturn(projectLocation);
    when(projectLocation.toFile()).thenReturn(tempDir.toFile());

    assertFalse(WorkspaceUtils.isGitRepository(project));
  }

  @Test
  void testIsGitRepository_withGitFile() throws IOException {
    // Create a .git file instead of directory
    Path gitFile = tempDir.resolve(".git");
    Files.createFile(gitFile);

    IProject project = mock(IProject.class);
    IPath projectLocation = mock(IPath.class);

    when(project.isAccessible()).thenReturn(true);
    when(project.getLocation()).thenReturn(projectLocation);
    when(projectLocation.toFile()).thenReturn(tempDir.toFile());

    assertFalse(WorkspaceUtils.isGitRepository(project));
  }

  @Test
  void testIsGitRepository_withNullProject() {
    assertFalse(WorkspaceUtils.isGitRepository(null));
  }

  @Test
  void testIsGitRepository_withInaccessibleProject() {
    IProject project = mock(IProject.class);
    when(project.isAccessible()).thenReturn(false);

    assertFalse(WorkspaceUtils.isGitRepository(project));
  }

  @Test
  void testIsGitRepository_withClosedProject() {
    IProject project = mock(IProject.class);
    when(project.isAccessible()).thenReturn(false);

    assertFalse(WorkspaceUtils.isGitRepository(project));
  }

  @Test
  void testIsGitRepository_withNullLocation() {
    IProject project = mock(IProject.class);
    when(project.isAccessible()).thenReturn(true);
    when(project.getLocation()).thenReturn(null);

    assertFalse(WorkspaceUtils.isGitRepository(project));
  }

}

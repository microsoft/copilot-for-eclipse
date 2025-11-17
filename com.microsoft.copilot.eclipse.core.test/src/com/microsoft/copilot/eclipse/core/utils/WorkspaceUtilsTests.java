package com.microsoft.copilot.eclipse.core.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.junit.jupiter.api.Test;

class WorkspaceUtilsTests {

  @Test
  void testIsGitRepository_withGitFolder() {
    IProject project = mock(IProject.class);
    IFolder gitFolder = mock(IFolder.class);

    when(project.isAccessible()).thenReturn(true);
    when(project.getFolder(".git")).thenReturn(gitFolder);
    when(gitFolder.exists()).thenReturn(true);

    assertTrue(WorkspaceUtils.isGitRepository(project));
  }

  @Test
  void testIsGitRepository_withoutGitFolder() {
    IProject project = mock(IProject.class);
    IFolder gitFolder = mock(IFolder.class);

    when(project.isAccessible()).thenReturn(true);
    when(project.getFolder(".git")).thenReturn(gitFolder);
    when(gitFolder.exists()).thenReturn(false);

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

}

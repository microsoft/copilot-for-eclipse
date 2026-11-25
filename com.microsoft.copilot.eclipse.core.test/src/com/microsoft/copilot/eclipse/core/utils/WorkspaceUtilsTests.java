// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microsoft.copilot.eclipse.core.Constants;

class WorkspaceUtilsTests {

  private static final String UI_PREFERENCE_NODE = "com.microsoft.copilot.eclipse.ui";

  @TempDir
  Path tempDir;

  @AfterEach
  void tearDown() {
    uiPreferences().remove(Constants.CUSTOM_INSTRUCTIONS_PARENT_REPO_ENABLED);
  }

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

  @Test
  void testFindRepositoryRoot_withGitFolderInFolderItself() throws IOException {
    Path repo = Files.createDirectories(tempDir.resolve("repo/.git")).getParent();

    assertEquals(Optional.of(repo), WorkspaceUtils.findRepositoryRoot(repo));
  }

  @Test
  void testFindRepositoryRoot_withGitFolderInAncestor() throws IOException {
    Path repo = Files.createDirectories(tempDir.resolve("repo/.git")).getParent();
    Path module = Files.createDirectories(repo.resolve("modules/app"));

    assertEquals(Optional.of(repo), WorkspaceUtils.findRepositoryRoot(module));
  }

  @Test
  void testFindRepositoryRoot_withGitFile() throws IOException {
    // git worktrees and submodules use a .git file instead of a folder
    Path worktree = Files.createDirectories(tempDir.resolve("worktree"));
    Files.createFile(worktree.resolve(".git"));
    Path module = Files.createDirectories(worktree.resolve("app"));

    assertEquals(Optional.of(worktree), WorkspaceUtils.findRepositoryRoot(module));
  }

  @Test
  void testFindRepositoryRoot_outsideRepository() throws IOException {
    Path folder = Files.createDirectories(tempDir.resolve("no-repo/app"));

    // only check below the temp dir, the temp dir itself might be located in a git repository
    assertTrue(WorkspaceUtils.findRepositoryRoot(folder).filter(root -> root.startsWith(tempDir)).isEmpty());
  }

  @Test
  void testWithParentRepositoryFolders_addsAncestorsUpToRepositoryRoot() throws IOException {
    Path repo = Files.createDirectories(tempDir.resolve("repo/.git")).getParent();
    Path modules = Files.createDirectories(repo.resolve("modules"));
    Path app = Files.createDirectories(modules.resolve("app"));

    List<WorkspaceFolder> result = WorkspaceUtils.withParentRepositoryFolders(List.of(toWorkspaceFolder(app)));

    assertEquals(List.of(app, modules, repo), toPaths(result));
    assertEquals(List.of("app", "modules", "repo"), result.stream().map(WorkspaceFolder::getName).toList());
  }

  @Test
  void testWithParentRepositoryFolders_keepsRepositoryRootUnchanged() throws IOException {
    Path repo = Files.createDirectories(tempDir.resolve("repo/.git")).getParent();
    List<WorkspaceFolder> folders = List.of(toWorkspaceFolder(repo));

    assertEquals(folders, WorkspaceUtils.withParentRepositoryFolders(folders));
  }

  @Test
  void testWithParentRepositoryFolders_avoidsDuplicates() throws IOException {
    Path repo = Files.createDirectories(tempDir.resolve("repo/.git")).getParent();
    Path moduleA = Files.createDirectories(repo.resolve("a"));
    Path moduleB = Files.createDirectories(repo.resolve("b"));

    List<WorkspaceFolder> result = WorkspaceUtils.withParentRepositoryFolders(
        List.of(toWorkspaceFolder(moduleA), toWorkspaceFolder(moduleB), toWorkspaceFolder(repo)));

    assertEquals(List.of(moduleA, moduleB, repo), toPaths(result));
  }

  @Test
  void testWithParentRepositoryFolders_stopsAtSubmoduleRoot() throws IOException {
    Path repo = Files.createDirectories(tempDir.resolve("repo/.git")).getParent();
    Path submodule = Files.createDirectories(repo.resolve("submodule"));
    Files.createFile(submodule.resolve(".git"));
    Path app = Files.createDirectories(submodule.resolve("app"));

    List<WorkspaceFolder> result = WorkspaceUtils.withParentRepositoryFolders(List.of(toWorkspaceFolder(app)));

    assertEquals(List.of(app, submodule), toPaths(result));
  }

  @Test
  void testWithParentRepositoryFolders_keepsNonFileFolders() {
    List<WorkspaceFolder> folders = List.of(new WorkspaceFolder("jdt://contents/rt.jar", "rt.jar"));

    assertEquals(folders, WorkspaceUtils.withParentRepositoryFolders(folders));
  }

  @Test
  void testListParentRepositoryFolders_returnsOnlyAdditionalAncestors() throws IOException {
    Path repo = Files.createDirectories(tempDir.resolve("repo/.git")).getParent();
    Path modules = Files.createDirectories(repo.resolve("modules"));
    Path app = Files.createDirectories(modules.resolve("app"));

    List<WorkspaceFolder> result = WorkspaceUtils.listParentRepositoryFolders(
        List.of(toWorkspaceFolder(app), toWorkspaceFolder(modules)));

    assertEquals(List.of(repo), toPaths(result));
  }

  @Test
  void testIsParentRepositoryEnabled_trueByDefault() {
    assertTrue(WorkspaceUtils.isParentRepositoryEnabled());
  }

  @Test
  void testWithParentRepositoryFoldersIfEnabled_addsAncestorsWhenEnabled() throws IOException {
    Path repo = Files.createDirectories(tempDir.resolve("repo/.git")).getParent();
    Path app = Files.createDirectories(repo.resolve("app"));
    uiPreferences().putBoolean(Constants.CUSTOM_INSTRUCTIONS_PARENT_REPO_ENABLED, true);

    List<WorkspaceFolder> result = WorkspaceUtils.withParentRepositoryFoldersIfEnabled(
        List.of(toWorkspaceFolder(app)));

    assertEquals(List.of(app, repo), toPaths(result));
  }

  @Test
  void testWithParentRepositoryFoldersIfEnabled_keepsFoldersWhenDisabled() throws IOException {
    Path repo = Files.createDirectories(tempDir.resolve("repo/.git")).getParent();
    Path app = Files.createDirectories(repo.resolve("app"));
    uiPreferences().putBoolean(Constants.CUSTOM_INSTRUCTIONS_PARENT_REPO_ENABLED, false);
    List<WorkspaceFolder> folders = List.of(toWorkspaceFolder(app));

    assertEquals(folders, WorkspaceUtils.withParentRepositoryFoldersIfEnabled(folders));
  }

  private static IEclipsePreferences uiPreferences() {
    return InstanceScope.INSTANCE.getNode(UI_PREFERENCE_NODE);
  }

  private static WorkspaceFolder toWorkspaceFolder(Path path) {
    return new WorkspaceFolder(path.toUri().toASCIIString(), path.getFileName().toString());
  }

  private static List<Path> toPaths(List<WorkspaceFolder> folders) {
    return folders.stream().map(folder -> FileUtils.getLocalFilePath(folder.getUri())).toList();
  }

}

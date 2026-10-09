// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4j.WorkspaceFolder;

import com.microsoft.copilot.eclipse.core.Constants;

/**
 * Utils for workspace-related operations.
 */
public class WorkspaceUtils {

  private static final String GIT_FOLDER = ".git";
  private static final String UI_PREFERENCE_NODE = "com.microsoft.copilot.eclipse.ui";

  /**
   * List all top-level workspace projects in the current workspace.
   *
   * @return list of top-level workspace projects
   */
  public static List<IProject> listTopLevelProjects() {
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

    List<IProject> accessibleProjects = new ArrayList<>();

    // Collect accessible projects
    for (IProject project : projects) {
      if (project.isAccessible()) {
        accessibleProjects.add(project);
      }
    }

    // Filter to only keep parent projects (not nested within another project)
    List<IProject> topLevelProjects = new ArrayList<>();
    for (IProject project : accessibleProjects) {
      URI uri = project.getLocationURI();
      if (uri == null) {
        continue;
      }

      boolean isTopLevel = true;
      String uriPath = uri.toString();

      for (IProject otherProject : accessibleProjects) {
        if (project.equals(otherProject)) {
          continue;
        }

        URI otherUri = otherProject.getLocationURI();
        if (otherUri == null) {
          continue;
        }

        String otherPath = otherUri.toString();
        // Check if this project is nested within another project
        if (uriPath.startsWith(otherPath + "/")) {
          isTopLevel = false;
          break;
        }
      }

      if (isTopLevel) {
        topLevelProjects.add(project);
      }
    }

    return topLevelProjects;
  }

  /**
   * List all top level projects that are git repositories.
   *
   * @return list of top-level projects that are git repositories
   */
  public static List<IProject> listTopLevelProjectsWithGitRepository() {
    return listTopLevelProjects().stream().filter(WorkspaceUtils::isGitRepository).toList();
  }

  /**
   * List all top level projects as workspace folders in the current workspace.
   */
  public static List<WorkspaceFolder> listWorkspaceFolders() {
    List<IProject> projects = WorkspaceUtils.listTopLevelProjects();

    List<WorkspaceFolder> folders = new ArrayList<>();
    for (IProject project : projects) {
      URI uri = project.getLocationURI();
      if (uri != null) {
        WorkspaceFolder folder = new WorkspaceFolder();
        folder.setUri(uri.toASCIIString());
        folder.setName(project.getName());
        folders.add(folder);
      }
    }
    return folders;
  }

  /**
   * List the workspace folders in which customization files such as instructions, prompts, skills and agents are
   * discovered, i.e. all top level projects, extended by their parent repository folders if enabled.
   *
   * @return list of workspace folders for discovering customization files
   */
  public static List<WorkspaceFolder> listCustomizationFolders() {
    return withParentRepositoryFoldersIfEnabled(listWorkspaceFolders());
  }

  /**
   * Check if the preference for discovering customization files in the parent git repositories of the projects is
   * enabled.
   *
   * @return true if the parent repository folders should be included, false otherwise
   */
  public static boolean isParentRepositoryEnabled() {
    return Platform.getPreferencesService().getBoolean(UI_PREFERENCE_NODE,
        Constants.CUSTOM_INSTRUCTIONS_PARENT_REPO_ENABLED, true, null);
  }

  /**
   * Extend the given workspace folders by {@link #withParentRepositoryFolders(List)} if
   * {@link #isParentRepositoryEnabled()}.
   *
   * @param folders the workspace folders to extend
   * @return the extended workspace folders, or the given folders if the preference is disabled
   */
  public static List<WorkspaceFolder> withParentRepositoryFoldersIfEnabled(List<WorkspaceFolder> folders) {
    return isParentRepositoryEnabled() ? withParentRepositoryFolders(folders) : folders;
  }

  /**
   * Check if a project is a git repository by looking for the .git folder.
   *
   * @param project the project to check
   * @return true if the project contains a .git folder, false otherwise
   */
  public static boolean isGitRepository(IProject project) {
    if (project == null || !project.isAccessible()) {
      return false;
    }

    // Use java.io.File API to check for .git folder directly in the file system
    // This works even when .git is excluded in the .project file
    IPath location = project.getLocation();
    if (location == null) {
      return false;
    }

    File gitFolder = new File(location.toFile(), GIT_FOLDER);
    return gitFolder.exists() && gitFolder.isDirectory();
  }

  /**
   * Find the root of the git repository containing the given folder. Unlike {@link #isGitRepository(IProject)}, a .git
   * file is accepted as well, as it is used by git worktrees and submodules.
   *
   * @param folder the folder to start the search from
   * @return the repository root, or an empty optional if the folder is not located in a git repository
   */
  public static Optional<Path> findRepositoryRoot(Path folder) {
    for (Path current = folder; current != null; current = current.getParent()) {
      if (Files.exists(current.resolve(GIT_FOLDER))) {
        return Optional.of(current);
      }
    }
    return Optional.empty();
  }

  /**
   * Extend the given workspace folders with the folders of their enclosing git repositories. For every workspace folder
   * that is located inside a git repository but is not its root, all ancestor folders up to and including the
   * repository root are appended. This way customization files such as .github/copilot-instructions.md are discovered
   * even if only a nested project of the repository is imported into the Eclipse workspace.
   *
   * @param folders the workspace folders to extend
   * @return the given folders, followed by the additional ancestor folders without duplicates
   */
  public static List<WorkspaceFolder> withParentRepositoryFolders(List<WorkspaceFolder> folders) {
    List<WorkspaceFolder> result = new ArrayList<>(folders);
    result.addAll(listParentRepositoryFolders(folders));
    return result;
  }

  /**
   * List the ancestor folders up to and including the git repository root of the given workspace folders, as appended
   * by {@link #withParentRepositoryFolders(List)}.
   *
   * @param folders the workspace folders
   * @return the ancestor folders that are not contained in the given folders, without duplicates
   */
  public static List<WorkspaceFolder> listParentRepositoryFolders(List<WorkspaceFolder> folders) {
    List<WorkspaceFolder> result = new ArrayList<>();
    Set<Path> knownPaths = new HashSet<>();
    List<Path> folderPaths = new ArrayList<>();
    for (WorkspaceFolder folder : folders) {
      Path path = FileUtils.getLocalFilePath(folder.getUri());
      if (path != null && knownPaths.add(path)) {
        folderPaths.add(path);
      }
    }

    for (Path path : folderPaths) {
      Optional<Path> repositoryRoot = findRepositoryRoot(path);
      if (repositoryRoot.isEmpty()) {
        continue;
      }
      for (Path ancestor = path.getParent(); ancestor != null
          && ancestor.startsWith(repositoryRoot.get()); ancestor = ancestor.getParent()) {
        if (knownPaths.add(ancestor)) {
          result.add(toWorkspaceFolder(ancestor));
        }
      }
    }
    return result;
  }

  private static WorkspaceFolder toWorkspaceFolder(Path path) {
    Path fileName = path.getFileName();
    return new WorkspaceFolder(path.toUri().toASCIIString(), fileName != null ? fileName.toString() : path.toString());
  }

}

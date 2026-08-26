// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.lsp4j.WorkspaceFolder;

/**
 * Utils for workspace-related operations.
 */
public class WorkspaceUtils {

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
   * List all top level projects as workspace folders in the current workspace, including the workspace root.
   */
  public static List<WorkspaceFolder> listWorkspaceFolders() {
    List<IProject> projects = WorkspaceUtils.listTopLevelProjects();

    List<WorkspaceFolder> folders = new ArrayList<>();
    java.util.Set<String> seenUris = new java.util.HashSet<>();

    // Add workspace root first so it can be searched for root-level prompt files and skills
    try {
      URI workspaceRootUri = ResourcesPlugin.getWorkspace().getRoot().getLocationURI();
      if (workspaceRootUri != null) {
        String rootUriString = workspaceRootUri.toASCIIString();
        WorkspaceFolder rootFolder = new WorkspaceFolder();
        rootFolder.setUri(rootUriString);
        rootFolder.setName("workspace-root");
        folders.add(rootFolder);
        seenUris.add(rootUriString);
      }
    } catch (Exception e) {
      // If we can't get workspace root URI, continue with projects only
    }

    // Add top-level projects (avoiding duplicates)
    for (IProject project : projects) {
      URI uri = project.getLocationURI();
      if (uri != null) {
        String uriString = uri.toASCIIString();
        // Avoid adding the same URI twice (project might be at workspace root in some configurations)
        if (!seenUris.contains(uriString)) {
          WorkspaceFolder folder = new WorkspaceFolder();
          folder.setUri(uriString);
          folder.setName(project.getName());
          folders.add(folder);
          seenUris.add(uriString);
        }
      }
    }
    return folders;
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

    File gitFolder = new File(location.toFile(), ".git");
    return gitFolder.exists() && gitFolder.isDirectory();
  }

}

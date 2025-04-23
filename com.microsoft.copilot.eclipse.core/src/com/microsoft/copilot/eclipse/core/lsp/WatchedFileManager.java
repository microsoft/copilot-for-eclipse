package com.microsoft.copilot.eclipse.core.lsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jgit.ignore.IgnoreNode;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.DidChangeCopilotWatchedFilesParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.GetWatchedFilesRequest;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;

/**
 * Listener for watched files.
 */
class WatchedFileManager {

  public static final String GITIGNORE = ".gitignore";

  private static final String GIT = ".git";

  /**
   * Currently the CLS only accept at-most 10000 files to index.
   */
  private static final int MAX_WATCHED_FILE_NUM = 10000;

  /**
   * the map of all the .gitignore, key is the folder path containing the .gitignore.
   */
  private Map<String, IgnoreNode> ignoreNodeMap;

  /**
   * For some unknown reason, 'copilot/watchedFiles' will be called multiple times. So we cached the file list to save
   * the calculation time.
   */
  private Set<String> files;

  /**
   * Constructor.
   */
  public WatchedFileManager() {
    ignoreNodeMap = new HashMap<>();
    addWatchedFileChangeListener();
  }

  /**
   * Get the list of watched files.
   */
  public synchronized List<String> getWatchedFiles(GetWatchedFilesRequest params) {
    if (files != null) {
      return new ArrayList<>(files);
    }
    files = new LinkedHashSet<>();
    IProject[] projects = ResourcesPlugin.getPlugin().getWorkspace().getRoot().getProjects();
    if (params.isExcludeGitignoredFiles()) {
      for (IProject project : projects) {
        addGitIgnorePatterns(project);
      }
    }

    for (IProject project : projects) {
      try {
        collectFiles(project);
      } catch (CoreException e) {
        CopilotCore.LOGGER.error("Error when collect files", e);
        return Collections.emptyList();
      }
    }

    return new ArrayList<>(files);
  }

  private void addGitIgnorePatterns(IContainer container) {
    if (isInvalidToScan(container)) {
      return;
    }

    try {
      for (IResource member : container.members()) {
        if (!member.exists()) {
          continue;
        }

        if (GITIGNORE.equals(member.getName()) && member instanceof IFile ignoreFile) {
          IgnoreNode ignoreNode = new IgnoreNode();
          ignoreNode.parse(ignoreFile.getContents());
          ignoreNodeMap.put(FileUtils.getResourceUri(container), ignoreNode);
        }
      }
    } catch (CoreException | IOException e) {
      CopilotCore.LOGGER.error("Error when add git ignore patterns", e);
    }
  }

  private void collectFiles(IContainer container) throws CoreException {
    if (files.size() >= MAX_WATCHED_FILE_NUM) {
      return;
    }

    if (isInvalidToScan(container)) {
      return;
    }

    // Process all resources in the container
    for (IResource member : container.members()) {
      // skip IProject member to avoid duplication, (nested project will be scanned in the outer loop)
      if (!member.exists() || isProject(member)) {
        continue;
      }

      String uri = FileUtils.getResourceUri(member);
      if (uri == null) {
        continue;
      }
      boolean isDirectory = member instanceof IContainer;
      if (shouldCollect(uri, isDirectory)) {
        if (isDirectory) {
          // Recursively process subdirectory
          collectFiles((IContainer) member);
        } else {
          // Add file to the list
          files.add(uri);
        }
      }
    }
  }

  private boolean isInvalidToScan(IContainer container) {
    if (container == null || !container.exists() || (container instanceof IProject project && !project.isOpen())) {
      return true;
    }

    // Do not include .git content, this block list may need to expand per requirement.
    if (GIT.equals(container.getName())) {
      return true;
    }

    return false;
  }

  private boolean isProject(IResource resource) {
    if (!(resource instanceof IContainer)) {
      return false;
    }
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      IPath projectPath = project.getLocation();
      IPath resourcePath = resource.getLocation();
      if (projectPath != null && projectPath.equals(resourcePath)) {
        return true;
      }
    }
    return false;
  }

  // Helper method to find the IgnoreNode entry that controls a given path
  private Map.Entry<String, IgnoreNode> findControllingIgnoreEntry(@NonNull String uri) {
    Map.Entry<String, IgnoreNode> result = null;
    for (Map.Entry<String, IgnoreNode> entry : ignoreNodeMap.entrySet()) {
      if (uri.startsWith(entry.getKey())) {
        // If this is the first match OR if we found a path closer in the hierarchy (longer path)
        return entry;
      }
    }

    return result;
  }

  private void addWatchedFileChangeListener() {
    WatchedFilesListener watchedFilesListener = new WatchedFilesListener();
    ResourcesPlugin.getWorkspace().addResourceChangeListener(watchedFilesListener,
        IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_DELETE);
  }

  private boolean shouldCollect(String uri, boolean isDirecotry) {
    if (StringUtils.isEmpty(uri)) {
      return false;
    }
    String extension = uri.substring(uri.lastIndexOf(".") + 1);
    // ignore binary files
    if (Constants.EXCLUDED_FILE_TYPE.contains(extension)) {
      return false;
    }

    Map.Entry<String, IgnoreNode> controllingEntry = findControllingIgnoreEntry(uri);
    if (controllingEntry == null) {
      return true;
    } else {
      // We found a controlling IgnoreNode
      String rootUri = controllingEntry.getKey();
      IgnoreNode controllingNode = controllingEntry.getValue();

      // Calculate relative path from the IgnoreNode's root directory
      String relativePath = uri.substring(rootUri.length());
      if (relativePath.startsWith("/")) {
        relativePath = relativePath.substring(1);
      }

      // Check if the file should be ignored
      return controllingNode.isIgnored(relativePath, isDirecotry) != IgnoreNode.MatchResult.IGNORED;
    }
  }

  private final class WatchedFilesListener implements IResourceChangeListener {
    @Override
    public void resourceChanged(IResourceChangeEvent event) {
      DidChangeCopilotWatchedFilesParams params = toDidChangeCopilotWatchedFilesParams(event);
      if (params == null || (params.getChanges() != null && params.getChanges().isEmpty())) {
        return;
      }
      // If shutting down, language server will be set to null, so ignore the event
      final CopilotLanguageServerConnection connection = CopilotCore.getPlugin().getCopilotLanguageServer();
      if (connection != null) {
        connection.didChangeWatchedFiles(params);
      }
    }

    private @Nullable DidChangeCopilotWatchedFilesParams toDidChangeCopilotWatchedFilesParams(IResourceChangeEvent e) {
      if (!isPostChangeEvent(e) && !isPreDeleteEvent(e)) {
        return null;
      }

      List<FileEvent> fileChanges = new ArrayList<>();

      if (isPostChangeEvent(e) && e.getDelta() != null) {
        collectFileChanges(e.getDelta(), fileChanges);
      } else if (isPreDeleteEvent(e) && e.getResource() != null) {
        IResource resource = e.getResource();
        if (resource.exists()) {
          addResourceDeletion(resource, fileChanges);
        }
      }

      fileChanges.removeIf(fileEvent -> fileEvent.getUri() == null);
      if (fileChanges.isEmpty()) {
        return null;
      }

      return new DidChangeCopilotWatchedFilesParams(PlatformUtils.getWorkspaceRootUri(), fileChanges);
    }

    private void collectFileChanges(IResourceDelta delta, List<FileEvent> changes) {
      // Process this delta node
      IResource resource = delta.getResource();
      if (resource == null || !resource.exists() && !isRemoveEvent(delta)) {
        return;
      }

      // For files, add the change if it's not ignored
      if (resource.getType() == IResource.FILE) {
        String uri = FileUtils.getResourceUri(resource);
        if (shouldCollect(uri, false)) {
          if (isAddEvent(delta)) {
            changes.add(createFileEvent(uri, FileChangeType.Created));
          } else if (isRemoveEvent(delta)) {
            changes.add(createFileEvent(uri, FileChangeType.Deleted));
          } else if ((delta.getFlags() & IResourceDelta.CONTENT) != 0) {
            changes.add(createFileEvent(uri, FileChangeType.Changed));
          }
        }
      }

      // Recursively process child deltas
      for (IResourceDelta childDelta : delta.getAffectedChildren()) {
        collectFileChanges(childDelta, changes);
      }
    }

    private void addResourceDeletion(IResource resource, List<FileEvent> changes) {
      // If it's a file, add it directly
      if (resource.getType() == IResource.FILE) {
        String uri = FileUtils.getResourceUri(resource);
        if (shouldCollect(uri, false)) {
          changes.add(createFileEvent(uri, FileChangeType.Deleted));
        }
        return;
      }

      // For containers, recursively process all children
      if (resource instanceof IContainer container) {
        try {
          for (IResource child : container.members()) {
            addResourceDeletion(child, changes);
          }
        } catch (CoreException ex) {
          CopilotCore.LOGGER.error("Error processing resource deletion", ex);
        }
      }
    }

    private boolean isPostChangeEvent(IResourceChangeEvent e) {
      return e.getType() == IResourceChangeEvent.POST_CHANGE;
    }

    private boolean isPreDeleteEvent(IResourceChangeEvent e) {
      return e.getType() == IResourceChangeEvent.PRE_DELETE;
    }

    private boolean isAddEvent(IResourceDelta delta) {
      return delta.getKind() == IResourceDelta.ADDED;
    }

    private boolean isRemoveEvent(IResourceDelta delta) {
      return delta.getKind() == IResourceDelta.REMOVED;
    }

    @Nullable
    private FileEvent createFileEvent(String uri, FileChangeType type) {
      if (uri == null) {
        return null;
      }
      FileEvent event = new FileEvent();
      event.setUri(uri);
      event.setType(type);
      return event;
    }
  }
}

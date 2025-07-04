package com.microsoft.copilot.eclipse.core.lsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jgit.ignore.FastIgnoreRule;
import org.eclipse.jgit.ignore.IgnoreNode;
import org.eclipse.jgit.util.StringUtils;
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
  private Map<IPath, IgnoreNode> gitignoreNodeMap;

  /**
   * For some unknown reason, 'copilot/watchedFiles' will be called multiple times. So we cached the file list to save
   * the calculation time.
   */
  private Set<String> files;

  /**
   * Constructor.
   */
  public WatchedFileManager() {
    gitignoreNodeMap = new LinkedHashMap<>();
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

    // Load ignore nodes
    if (params.isExcludeGitignoredFiles()) {
      List<IFile> gitignoreFiles = new ArrayList<>();
      for (IProject project : projects) {
        if (!project.isAccessible()) {
          continue;
        }
        gitignoreFiles.addAll(findGitignoreFiles(project));
      }

      // Sort gitignore files by their path to ensure the closest .gitignore is used first.
      gitignoreFiles.sort((f1, f2) -> {
        return f2.getLocation().segmentCount() - f1.getLocation().segmentCount();
      });

      loadGitignoreNodeMap(gitignoreFiles);
    }

    // collect watched files
    for (IProject project : projects) {
      if (!project.isAccessible()) {
        continue;
      }

      try {
        collectFiles(project);
      } catch (CoreException e) {
        CopilotCore.LOGGER.error("Error when collect files", e);
        return Collections.emptyList();
      }
    }

    return new ArrayList<>(files);
  }

  private List<IFile> findGitignoreFiles(IProject project) {
    final List<IFile> gitignoreFiles = new ArrayList<>();

    try {
      project.accept(new IResourceProxyVisitor() {
        @Override
        public boolean visit(IResourceProxy proxy) throws CoreException {
          if (proxy.getType() == IResource.FILE && proxy.getName().equals(GITIGNORE)) {
            gitignoreFiles.add((IFile) proxy.requestResource());
          }
          return true; // Continue visiting children
        }
      }, IResource.NONE);
    } catch (CoreException e) {
      CopilotCore.LOGGER.error("Error when finding gitignore files.", e);
    }

    return gitignoreFiles;
  }

  private void loadGitignoreNodeMap(List<IFile> gitignoreFiles) {
    for (IFile gitignoreFile : gitignoreFiles) {
      try {
        IgnoreNode ignoreNode = new IgnoreNode() {
          // This is to fix the issue that when a directory is ignored, the files under that directory are not ignored.
          @Override
          public @Nullable Boolean checkIgnored(String entryPath, boolean isDirectory) {
            for (int i = this.getRules().size() - 1; i > -1; i--) {
              FastIgnoreRule rule = this.getRules().get(i);
              // Enable relative path match when pathMatch is false.
              if (rule.isMatch(entryPath, isDirectory, false)) {
                return Boolean.valueOf(rule.getResult());
              }
            }
            return null;
          }
        };

        if (gitignoreFile.getParent() != null && gitignoreFile.getParent().getLocation() != null) {
          ignoreNode.parse(gitignoreFile.getContents());
          gitignoreNodeMap.put(gitignoreFile.getParent().getLocation(), ignoreNode);
        }
      } catch (IOException | CoreException e) {
        CopilotCore.LOGGER.error("Error when parse git ignore file: ", e);
      }
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
      String uri = FileUtils.getResourceUri(member);
      if (uri == null) {
        continue;
      }
      boolean isDirectory = member instanceof IContainer;
      if (isDirectory) {
        // Recursively process subdirectory
        collectFiles((IContainer) member);
      } else {
        // Add file to the list
        if (shouldCollect(member, false)) {
          files.add(uri);
        }
      }
    }
  }

  private boolean isInvalidToScan(IContainer container) {
    if (container == null || !container.exists()) {
      return true;
    }

    // Do not include .git content, this block list may need to expand per requirement.
    if (GIT.equals(container.getName())) {
      return true;
    }

    return false;
  }

  private void addWatchedFileChangeListener() {
    WatchedFilesListener watchedFilesListener = new WatchedFilesListener();
    ResourcesPlugin.getWorkspace().addResourceChangeListener(watchedFilesListener,
        IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_DELETE);
  }

  private boolean shouldCollect(IResource resource, boolean isDirectory) {
    if (resource == null || !resource.exists()) {
      return false;
    }

    String extension = resource.getFileExtension();
    if (!StringUtils.isEmptyOrNull(extension) && Constants.EXCLUDED_FILE_TYPE.contains(extension)) {
      return false;
    }

    for (Map.Entry<IPath, IgnoreNode> entry : gitignoreNodeMap.entrySet()) {
      IPath directoryPath = entry.getKey();
      if (!directoryPath.isPrefixOf(resource.getLocation())) {
        continue;
      }

      IgnoreNode ignoreNode = entry.getValue();
      IPath relativePath = resource.getLocation().makeRelativeTo(directoryPath);
      IgnoreNode.MatchResult matchResult = ignoreNode.isIgnored(relativePath.toString(), isDirectory);

      switch (matchResult) {
        case IGNORED:
          return false;
        case NOT_IGNORED:
          return true;
        case CHECK_PARENT:
        default:
      }
    }

    return true;
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
        if (shouldCollect(resource, false)) {
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
        if (shouldCollect(resource, false)) {
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

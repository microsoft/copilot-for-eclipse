package com.microsoft.copilot.eclipse.core.lsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.DidChangeCopilotWatchedFilesParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.GetWatchedFilesRequest;
import com.microsoft.copilot.eclipse.core.lsp.protocol.GetWatchedFilesResponse;
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
   * Batch size for reporting progress during file collection.
   */
  private static final int PROGRESS_BATCH_SIZE = 500;

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
   * Get the watched files with support for progress reporting.
   * If partialResultToken is provided, files will be sent in batches via $/progress notifications.
   *
   * @param params the request parameters
   * @return CompletableFuture with the final response
   */
  public synchronized CompletableFuture<GetWatchedFilesResponse> getWatchedFilesWithProgress(
      GetWatchedFilesRequest params) {
    if (files == null) {
      files = new LinkedHashSet<>();
      scanWorkspace(params);
    }

    final List<String> fileSnapshot = new ArrayList<>(files);

    Either<String, Integer> partialToken = params.getPartialResultToken();

    // If no partial result token, return all files synchronously
    if (partialToken == null) {
      return CompletableFuture.completedFuture(new GetWatchedFilesResponse(fileSnapshot));
    }

    // Send files in batches via progress notifications
    return CompletableFuture.supplyAsync(() -> {
      CopilotLanguageServerConnection connection = CopilotCore.getPlugin().getCopilotLanguageServer();
      if (connection == null) {
        return new GetWatchedFilesResponse(fileSnapshot);
      }

      List<String> batch = new ArrayList<>();
      for (String uri : fileSnapshot) {
        batch.add(uri);
        if (batch.size() >= PROGRESS_BATCH_SIZE) {
          emitProgressBatch(connection, partialToken, batch);
          batch.clear();
        }
      }
      if (!batch.isEmpty()) {
        emitProgressBatch(connection, partialToken, batch);
        batch.clear();
      }
      return new GetWatchedFilesResponse(Collections.emptyList());
    }).exceptionally(ex -> {
      CopilotCore.LOGGER.error("Error during watched files collection, returning empty response", ex);
      return new GetWatchedFilesResponse(Collections.emptyList());
    });
  }

  private void scanWorkspace(GetWatchedFilesRequest params) {
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

    for (IResource member : container.members()) {
      String uri = FileUtils.getResourceUri(member);
      if (uri == null) {
        continue;
      }
      boolean isDirectory = member instanceof IContainer;
      if (isDirectory) {
        collectFiles((IContainer) member);
      } else {
        if (shouldCollect(member, false)) {
          files.add(uri);
        }
      }
    }
  }

  private void emitProgressBatch(CopilotLanguageServerConnection connection, Either<String, Integer> token,
      List<String> batch) {
    if (batch == null || batch.isEmpty()) {
      return;
    }
    try {
      ProgressParams progressParams = new ProgressParams();
      progressParams.setToken(token);
      // Use a copy of the batch to avoid concurrency issues as the batch list is cleared by the caller
      progressParams.setValue(Either.forRight(new GetWatchedFilesResponse(new ArrayList<>(batch))));

      connection.sendProgressNotification(progressParams).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      CopilotCore.LOGGER.error("Interrupted while sending progress", e);
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to send progress notification (may be shutting down)", e);
    }
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

  private boolean isInvalidToScan(IContainer container) {
    if (container == null || !container.exists()) {
      return true;
    }

    if (container.isDerived() || container.isTeamPrivateMember()) {
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

    // Check if resource location is available
    IPath resourceLocation = resource.getLocation();
    if (resourceLocation == null) {
      return false;
    }

    if (resource.isDerived() || resource.isTeamPrivateMember()) {
      return false;
    }

    String extension = resource.getFileExtension();
    if (!StringUtils.isEmptyOrNull(extension) && Constants.EXCLUDED_CURRENT_FILE_TYPE.contains(extension)) {
      return false;
    }

    for (Map.Entry<IPath, IgnoreNode> entry : gitignoreNodeMap.entrySet()) {
      IPath directoryPath = entry.getKey();
      if (!directoryPath.isPrefixOf(resourceLocation)) {
        continue;
      }

      IgnoreNode ignoreNode = entry.getValue();
      IPath relativePath = resourceLocation.makeRelativeTo(directoryPath);
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
          } else if ((delta.getFlags() & IResourceDelta.ENCODING) != 0) {
            // File encoding changed - notify CLS to invalidate cache
            notifyEncodingChange(uri);
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

    /**
     * Notify the language server about encoding changes for specific files.
     * This triggers cache invalidation in CLS for the affected files.
     *
     * @param fileUri the URI of the file whose encoding changed
     */
    private void notifyEncodingChange(String fileUri) {
      CopilotLanguageServerConnection connection = CopilotCore.getPlugin().getCopilotLanguageServer();
      if (connection != null) {
        Map<String, Object> copilotSettings = Map.of(
            "encodingChanges", List.of(fileUri)
        );
        DidChangeConfigurationParams params = new DidChangeConfigurationParams();
        params.setSettings(Map.of("copilot", copilotSettings));
        connection.updateConfig(params);
      }
    }
  }
}

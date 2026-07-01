// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.WorkspaceFolder;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CustomizationFileInfo;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;
import com.microsoft.copilot.eclipse.core.utils.WorkspaceUtils;

/**
 * Default {@link ICustomizationFileService} backed by the language server's {@code copilot/custom*}
 * list requests. Each customization type is fetched and snapshotted independently.
 */
public class CustomizationFileService implements ICustomizationFileService {

  private final CopilotLanguageServerConnection lsConnection;

  private volatile Set<Path> promptFiles = Set.of();
  private volatile Set<Path> instructionFiles = Set.of();
  private volatile Set<Path> agentFiles = Set.of();
  private volatile Set<Path> skillFolders = Set.of();

  private volatile Set<Path> customizationFiles = Set.of();

  /**
   * Creates the service.
   *
   * @param lsConnection the language server connection used to issue the list requests
   */
  public CustomizationFileService(CopilotLanguageServerConnection lsConnection) {
    this.lsConnection = lsConnection;
  }

  @Override
  public Set<Path> getCustomizationFiles() {
    return customizationFiles;
  }

  @Override
  public Set<Path> getSkillFolders() {
    return skillFolders;
  }

  @Override
  public void refresh() {
    for (CustomizationType type : CustomizationType.values()) {
      refresh(type);
    }
  }

  @Override
  public void refresh(CustomizationType type) {
    List<WorkspaceFolder> workspaceFolders = WorkspaceUtils.listWorkspaceFolders();
    switch (type) {
      case SKILL -> toPaths(lsConnection.listCustomSkills(workspaceFolders)).thenAccept(paths -> {
        Set<Path> folders = new HashSet<>();
        for (Path skillFile : paths) {
          Path parent = skillFile.getParent();
          if (parent != null) {
            folders.add(parent);
          }
        }
        this.skillFolders = Set.copyOf(folders);
      });
      case PROMPT -> toPaths(lsConnection.listCustomPrompts(workspaceFolders)).thenAccept(paths -> {
        this.promptFiles = Set.copyOf(paths);
        rebuildCustomizationFiles();
      });
      case INSTRUCTION -> toPaths(lsConnection.listCustomInstructions(workspaceFolders)).thenAccept(paths -> {
        this.instructionFiles = Set.copyOf(paths);
        rebuildCustomizationFiles();
      });
      case AGENT -> toPaths(lsConnection.listCustomAgents(workspaceFolders)).thenAccept(paths -> {
        this.agentFiles = Set.copyOf(paths);
        rebuildCustomizationFiles();
      });
      default -> {
        // No other customization types.
      }
    }
  }

  private synchronized void rebuildCustomizationFiles() {
    Set<Path> all = new HashSet<>(promptFiles);
    all.addAll(instructionFiles);
    all.addAll(agentFiles);
    this.customizationFiles = Set.copyOf(all);
  }

  private CompletableFuture<List<Path>> toPaths(CompletableFuture<CustomizationFileInfo[]> future) {
    return future.thenApply(infos -> {
      List<Path> paths = new ArrayList<>();
      if (infos != null) {
        for (CustomizationFileInfo info : infos) {
          Path path = FileUtils.getLocalFilePath(info.uri());
          if (path != null) {
            paths.add(path);
          }
        }
      }
      return paths;
    }).exceptionally(ex -> {
      CopilotCore.LOGGER.error("Failed to list customization files", ex);
      return List.of();
    });
  }
}

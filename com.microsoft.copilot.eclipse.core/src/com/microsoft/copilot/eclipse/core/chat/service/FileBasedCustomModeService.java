package com.microsoft.copilot.eclipse.core.chat.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4j.WorkspaceFolder;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.CustomChatMode;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationModesParams;

/**
 * File-based implementation of ICustomModeService. Manages custom modes as .agent.md files under .github/agents in the
 * workspace using LSP.
 */
public class FileBasedCustomModeService implements ICustomModeService {
  private static final String AGENTS_DIR = ".github/agents";
  private static final String AGENT_FILE_EXTENSION = ".agent.md";

  /**
   * Get the path to the .github/agents directory for a specific workspace folder.
   */
  private Path getAgentsDirectory(WorkspaceFolder workspaceFolder) {
    try {
      URI uri = URI.create(workspaceFolder.getUri());
      Path folderPath = Paths.get(uri);
      return folderPath.resolve(AGENTS_DIR);
    } catch (Exception e) {
      throw new RuntimeException("Invalid workspace folder URI: " + workspaceFolder.getUri(), e);
    }
  }

  @Override
  public CompletableFuture<List<CustomChatMode>> loadCustomModes() {
    // Get workspace folders for LSP call
    List<WorkspaceFolder> workspaceFolders = LSPEclipseUtils.getWorkspaceFolders();
    ConversationModesParams params = new ConversationModesParams(workspaceFolders);

    // Call LSP to get modes from all projects
    CopilotLanguageServerConnection lspConnection = CopilotCore.getPlugin().getCopilotLanguageServer();
    if (lspConnection == null) {
      return CompletableFuture.completedFuture(new ArrayList<CustomChatMode>());
    }

    return lspConnection.listConversationModes(params).<List<CustomChatMode>>thenApply(conversationModes -> {
      if (conversationModes == null) {
        return new ArrayList<CustomChatMode>();
      }

      // Convert ConversationMode[] to List<CustomChatMode>
      // Merge modes from multiple projects by ID (deduplication)
      Map<String, CustomChatMode> mergedModes = new HashMap<>();

      for (ConversationMode mode : conversationModes) {
        if (mode == null || mode.isBuiltIn()) {
          continue; // Skip built-in modes
        }

        String id = mode.getId();
        if (id == null) {
          continue;
        }

        // Merge: keep first occurrence by ID
        if (!mergedModes.containsKey(id)) {
          CustomChatMode customMode = convertToCustomChatMode(mode);
          if (customMode != null) {
            mergedModes.put(id, customMode);
          }
        }
      }

      return new ArrayList<CustomChatMode>(mergedModes.values());
    }).exceptionally(ex -> {
      CopilotCore.LOGGER.error("Failed to load custom modes from LSP", ex);
      return new ArrayList<CustomChatMode>();
    });
  }

  /**
   * Convert LSP ConversationMode to CustomChatMode.
   */
  private CustomChatMode convertToCustomChatMode(ConversationMode mode) {
    try {
      String id = mode.getId();
      String name = mode.getName() != null ? mode.getName() : id;
      String description = mode.getDescription();
      List<String> tools = mode.getCustomTools() != null ? mode.getCustomTools() : new ArrayList<>();
      String model = mode.getModel();

      return new CustomChatMode(id, name, description, tools, model);
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to convert ConversationMode to CustomChatMode", e);
      return null;
    }
  }

  /**
   * Create a custom mode in a specific workspace folder.
   * Creates a default template file that the user can edit.
   */
  @Override
  public CompletableFuture<CustomChatMode> createCustomModeInWorkspaceFolder(WorkspaceFolder workspaceFolder, 
      String displayName) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        Path agentsDir = getAgentsDirectory(workspaceFolder);
        if (!Files.exists(agentsDir)) {
          Files.createDirectories(agentsDir);
        }

        String fileName = displayName.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "_");
        Path filePath = agentsDir.resolve(fileName + AGENT_FILE_EXTENSION);

        // Check if file already exists
        int counter = 1;
        while (Files.exists(filePath)) {
          filePath = agentsDir.resolve(fileName + "_" + counter + AGENT_FILE_EXTENSION);
          counter++;
        }

        // Create default template with YAML frontmatter and default instructions
        String defaultDescription = "Describe what this custom agent does and when to use it.";
        String defaultInstructions = "Write instructions for this custom agent. "
            + "These will guide Copilot on how to respond when this mode is selected.";
        
        String content = "---\n" 
            + "description: " + defaultDescription + "\n" 
            + "tools: []\n" 
            + "---\n" 
            + defaultInstructions + "\n";
        
        Files.writeString(filePath, content);

        // Use file URI as the ID to match LSP behavior
        String id = filePath.toUri().toString();
        return new CustomChatMode(id, displayName, defaultDescription);
      } catch (IOException e) {
        CopilotCore.LOGGER.error("Failed to create custom mode in workspace folder", e);
        throw new RuntimeException("Failed to create custom mode: " + e.getMessage(), e);
      }
    });
  }

  @Override
  public CompletableFuture<Void> deleteCustomMode(String id) {
    return CompletableFuture.runAsync(() -> {
      try {
        Path filePath = getAgentFilePath(id);
        if (Files.exists(filePath)) {
          Files.delete(filePath);
        }
      } catch (IOException e) {
        CopilotCore.LOGGER.error("Failed to delete custom mode", e);
        throw new RuntimeException("Failed to delete custom mode: " + e.getMessage(), e);
      }
    });
  }

  /**
   * Get the file path for a custom mode. Expects id to be a file URI from LSP.
   */
  private Path getAgentFilePath(String id) {
    try {
      return Paths.get(java.net.URI.create(id));
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to parse file URI: " + id, e);
      throw new RuntimeException("Invalid file URI: " + id, e);
    }
  }
}

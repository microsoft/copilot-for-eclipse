package com.microsoft.copilot.eclipse.core;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A class to hold all the public constants used in the GitHub Copilot core.
 */
public class Constants {

  private Constants() {
    // prevent instantiation
  }

  // Increment CURRENT_COPILOT_QUICK_START_VERSION will force the quick start to be shown once again
  public static final int CURRENT_COPILOT_QUICK_START_VERSION = 1;

  public static final String PLUGIN_ID = "com.microsoft.copilot.eclipse";
  public static final String AUTO_SHOW_COMPLETION = "enableAutoCompletions";
  public static final String ENABLE_STRICT_SSL = "enableStrictSsl";
  public static final String PROXY_KERBEROS_SP = "proxyKerberosSp";
  public static final String GITHUB_ENTERPRISE = "githubEnterprise";
  public static final String WORKSPACE_CONTEXT_ENABLED = "workspaceContextEnabled";
  public static final String MCP = "mcp";
  public static final String MCP_TOOLS_STATUS = "mcpToolsStatus";
  public static final String CUSTOM_INSTRUCTIONS_WORKSPACE = "customInstructionsWorkspace";
  public static final String CUSTOM_INSTRUCTIONS_WORKSPACE_ENABLED = "customInstructionsWorkspaceEnabled";
  public static final String GITHUB_COPILOT_URL = "http://github.com";
  @Deprecated
  public static final String QUICK_START_VERSION = "quickStartVersion";
  public static final String COPILOT_QUICK_START_VERSION = "copilotQuickStartVersion";
  public static final String LAST_USED_PLUGIN_VERSION = "lastUsedPluginVersion";
  public static final String CHAT_VIEW_ID = "com.microsoft.copilot.eclipse.ui.chat.ChatView";
  public static final String CHAT_CHANNEL = "chatProgress";
  // Base excluded file types shared by both
  // Copied from InelliJ, excluded file extension list
  // https://github.com/microsoft/copilot-intellij/blob/main/core/src/main/kotlin/com/github/copilot/chat/references/FileSearchService.kt
  private static final Set<String> BASE_EXCLUDED_FILE_TYPES = Set.of("tif", "tiff", "ico", "raw", "indd", "ai", "eps",
      "pdf", "bin", "exe", "dat", "dll", "so", "class", "jar", "app", "dmg", "iso", "img", "docx", "pptx", "xlsx",
      "mp3", "wav", "flac", "mp4", "avi", "mov");

  // Additional excluded file types for current file
  private static final Set<String> ADDITIONAL_EXCLUDED_FILE_TYPES = Set.of("svg");

  // Allowed image file extensions
  public static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp");

  // Map of image file extensions to their MIME types
  public static final Map<String, String> EXTENSION_TO_MIMETYPE = Map.of("png", "image/png", "jpg", "image/jpeg",
      "jpeg", "image/jpeg", "gif", "image/gif", "bmp", "image/bmp", "webp", "image/webp");

  public static final Set<String> EXCLUDED_REFERENCE_FILE_TYPE = BASE_EXCLUDED_FILE_TYPES;

  // Excluded file types for current file, combining base and additional and allowed image extensions
  public static final Set<String> EXCLUDED_CURRENT_FILE_TYPE = Set
      .of(Stream.concat(Stream.concat(BASE_EXCLUDED_FILE_TYPES.stream(), ADDITIONAL_EXCLUDED_FILE_TYPES.stream()),
          ALLOWED_IMAGE_EXTENSIONS.stream()).toArray(String[]::new));

}

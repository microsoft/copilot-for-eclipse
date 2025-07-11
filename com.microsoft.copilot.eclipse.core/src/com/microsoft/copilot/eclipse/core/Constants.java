package com.microsoft.copilot.eclipse.core;

import java.util.Set;

/**
 * A class to hold all the public constants used in the GitHub Copilot core.
 */
public class Constants {

  private Constants() {
    // prevent instantiation
  }

  // Increment CURRENT_QUICK_START_VERSION will force the quick start to be shown once again
  public static final int CURRENT_QUICK_START_VERSION = 1;

  public static final String PLUGIN_ID = "com.microsoft.copilot.eclipse";
  public static final String AUTO_SHOW_COMPLETION = "enableAutoCompletions";
  public static final String ENABLE_STRICT_SSL = "enableStrictSsl";
  public static final String PROXY_KERBEROS_SP = "proxyKerberosSp";
  public static final String GITHUB_ENTERPRISE = "githubEnterprise";
  public static final String WORKSPACE_CONTEXT_ENABLED = "workspaceContextEnabled";
  public static final String MCP = "mcp";
  public static final String MCP_TOOLS_STATUS = "mcpToolsStatus";
  public static final String GITHUB_COPILOT_URL = "http://github.com";
  public static final String QUICK_START_VERSION = "quickStartVersion";
  public static final String LAST_USED_PLUGIN_VERSION = "lastUsedPluginVersion";
  public static final String CHAT_VIEW_ID = "com.microsoft.copilot.eclipse.ui.chat.ChatView";
  public static final String CHAT_CHANNEL = "chatProgress";
  // Copied from InelliJ, excluded file extension list
  // https://github.com/microsoft/copilot-intellij/blob/main/core/src/main/kotlin/com/github/copilot/chat/references/FileSearchService.kt
  public static final Set<String> EXCLUDED_FILE_TYPE = Set.of("jpg", "jpeg", "png", "gif", "bmp", "tif", "tiff", "ico",
      "webp", "raw", "indd", "ai", "eps", "pdf", "bin", "exe", "dat", "dll", "so", "class", "jar", "app", "dmg", "iso",
      "img", "docx", "pptx", "xlsx", "mp3", "wav", "flac", "mp4", "avi", "mov");
}

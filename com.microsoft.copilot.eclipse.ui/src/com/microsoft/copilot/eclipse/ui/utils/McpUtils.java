package com.microsoft.copilot.eclipse.ui.utils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpRegistryAllowList;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpRegistryEntry;
import com.microsoft.copilot.eclipse.core.lsp.mcp.RegistryAccess;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.Icon;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NullParams;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.dialogs.mcp.Messages;
import com.microsoft.copilot.eclipse.ui.preferences.CopilotPreferenceInitializer;

/**
 * Utility class for handling MCP related operations.
 */
public class McpUtils {
  /**
   * Get the MCP allowlist from the Copilot Language Server connection. This method retrieves the allowlist which
   * contains the MCP registry entries and their access modes.
   *
   * @param copilotLanguageServerConnection The Copilot Language Server connection to use for retrieving the allowlist
   * @return CompletableFuture containing the MCP allowlist, or null if no connection is available
   */
  public static CompletableFuture<McpRegistryAllowList> getMcpAllowList(
      CopilotLanguageServerConnection copilotLanguageServerConnection) {
    // Early return with empty allowlist if no language server connection
    if (copilotLanguageServerConnection == null) {
      CopilotCore.LOGGER.error(new IllegalStateException("Copilot Language Server connection is not available."));
      return CompletableFuture.completedFuture(null);
    }

    // Retrieve MCP allowlist from the language server
    return copilotLanguageServerConnection.getMcpAllowlist(new NullParams());
  }

  /**
   * Get the MCP registry base URL from the allowlist. This method processes the allowlist to determine the appropriate
   * registry URL based on the access mode and local preferences.
   *
   * <p>URL selection logic based on admin settings:
   * <ol>
   * <li>If admin sets registry_only, the admin-provided registry URL should directly overwrite the local registry URL
   * setting.</li>
   * <li>If admin sets 'allow_all' and provides a registry URL, users can set any registry URL or reuse the
   * admin-provided registry URL.</li>
   * <li>If admin does not set any registry URL, users can set any registry URL in the IDE.</li>
   * </ol>
   *
   * @return The selected MCP registry URL, or an empty string if no valid URL is available
   */
  public static String parseMcpRegistryBaseUrlFromAllowList(McpRegistryAllowList allowList) {
    String localUrl = CopilotUi.getStringPreference(Constants.MCP_REGISTRY_URL,
        CopilotPreferenceInitializer.DEFAULT_MCP_REGISTRY_BASE_URL);
    String registryBaseUrl = extractBaseUrl(localUrl.replaceAll("/+$", ""));

    // If no allowlist data exists, fall back to local preference
    if (allowList == null || allowList.getMcpRegistries() == null || allowList.getMcpRegistries().isEmpty()) {
      return registryBaseUrl;
    }

    // Process first registry entry
    McpRegistryEntry entry = allowList.getMcpRegistries().get(0);
    RegistryAccess registryAccess = entry.getRegistryAccess();
    String allowlistBaseUrl = extractBaseUrl(entry.getUrl() != null ? entry.getUrl().replaceAll("/+$", "")
        : "");

    // Determine URL based on access mode (registry_only takes precedence)
    if (registryAccess == RegistryAccess.registry_only) {
      // Use only allowlist base URL
      if (StringUtils.isBlank(allowlistBaseUrl)) {
        SwtUtils.getDisplay().asyncExec(() -> {
          MessageDialog.openWarning(SwtUtils.getDisplay().getActiveShell(),
              Messages.mcpRegistryDialog_emptyUrlForRegistryOnly_title,
              Messages.mcpRegistryDialog_emptyUrlForRegistryOnly_msg);
        });
        return "";
      }
      return allowlistBaseUrl;
    } else if (RegistryAccess.allow_all == registryAccess) {
      // Prefer local URL, fallback to allowlist URL
      return StringUtils.isNotBlank(registryBaseUrl) ? registryBaseUrl : allowlistBaseUrl;
    }

    // Default fallback to local preference for unknown access modes
    return registryBaseUrl;
  }

  /**
   * Extracts base URL from full url in the ServerDetail. This will remove version suffix patterns like /v0/servers,
   * /v1/servers, /v0.1/servers, /v0.1.1/servers, etc.
   *
   * @param fullUrl The full URL string
   * @return The base URL without version suffix
   */
  public static String extractBaseUrl(String fullUrl) {
    if (fullUrl == null) {
      return null;
    }
    return fullUrl.replaceAll("/v\\d+(\\.\\d+)*/servers$", "");
  }

  /**
   * Gets the preferred icon URL from the given icon list. Prefers PNG format over other formats since SWT has better
   * support for raster images.
   *
   * @param icons the list of icons from an MCP server definition
   * @return the icon URL, or null if no suitable icon is found
   */
  public static String getPreferredIconUrl(List<Icon> icons) {
    if (icons == null || icons.isEmpty()) {
      return null;
    }

    for (Icon icon : icons) {
      if (icon != null && icon.src() != null && "image/png".equalsIgnoreCase(icon.mimeType())) {
        return icon.src();
      }
    }
    for (Icon icon : icons) {
      if (icon != null && icon.src() != null && !"image/svg+xml".equalsIgnoreCase(icon.mimeType())) {
        return icon.src();
      }
    }
    return null;
  }

  /**
   * Loads the server icon from the given URL, scaled to the requested size.
   *
   * <p>The caller is responsible for attaching the returned image to a widget and disposing it when the widget is
   * disposed.</p>
   *
   * @param iconUrl the icon URL to load; if blank, the returned future completes with the default MCP icon image
   * @param width desired icon width in pixels
   * @param height desired icon height in pixels
   * @return a future that completes with the loaded and scaled image, or {@code null} if the image cannot be loaded
   */
  public static CompletableFuture<Image> loadServerIcon(String iconUrl, int width, int height) {
    if (StringUtils.isBlank(iconUrl)) {
      return CompletableFuture.completedFuture(loadDefaultServerIcon());
    }

    return UiUtils.loadImageFromUrl(iconUrl, width, height)
        .exceptionally(e -> loadDefaultServerIcon());
  }

  /**
   * Builds the default MCP server icon image.
   *
   * @return the default MCP icon image, or {@code null} if it cannot be loaded
   */
  public static Image loadDefaultServerIcon() {
    return UiUtils.buildImageFromPngPath("/icons/mcp/mcp_default_icon.png");
  }
}

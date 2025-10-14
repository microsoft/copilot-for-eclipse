package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.signedcontent.SignedContentFactory;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.BackingStoreException;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.dialogs.McpApprovalDialog;
import com.microsoft.copilot.eclipse.ui.extensions.IMcpRegistrationProvider;

/**
 * Manager for the MCP registration extension point.
 */
public class McpExtensionPointManager {

  private static final String EXTENSION_POINT_ID = "com.microsoft.copilot.eclipse.ui.mcpRegistration";
  private static final String ELEMENT_PROVIDER = "provider";
  private static final String ATTRIBUTE_CLASS = "class";

  private String approvedExtMcpServers;
  private Map<String, McpRegistrationInfo> extMcpInfoMap = new HashMap<>(); // Key: Plugin-Id(Bundle)
  private Set<String> mcpRegTrustedPlugins = Set.of("com.microsoft.copilot.eclipse.ui",
      "com.microsoft.azuretools.azuremcp");

  private McpConfigService mcpConfigService;
  private Gson gson;

  /**
   * Constructor for McpExtensionPointManager.
   */
  public McpExtensionPointManager(McpConfigService mcpConfigService) {
    gson = new Gson();
    this.mcpConfigService = mcpConfigService;
    // TODO: enable it when group policy is ready
    if (PlatformUtils.isNightly() && CopilotCore.getPlugin().getFeatureFlags().isMcpContributionPointEnabled()) {
      initializeExtMcpRegistration();
    }
    IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    eventBroker.subscribe(CopilotEventConstants.TOPIC_DID_CHANGE_MCP_CONTRIBUTION_POINT_POLICY, event -> {
      Boolean enabled = (Boolean) event.getProperty(IEventBroker.DATA);
      if (enabled.booleanValue()) {
        initializeExtMcpRegistration();
      } else {
        extMcpInfoMap.clear();
        approvedExtMcpServers = null;
        persistExtMcpInfo(extMcpInfoMap);
        mcpConfigService.setNewExtMcpRegFound(false);
      }
    });
  }

  private synchronized void initializeExtMcpRegistration() {
    // Previously approved servers will be started during Plugin startup.
    Map<String, McpRegistrationInfo> persistedMcpContribs = loadPersistedMcpContribs();
    updateApprovedMcpServerString(persistedMcpContribs);

    // Run the heavy initialization work asynchronously, which has weak relation with Plugin startup.
    CompletableFuture.runAsync(() -> {
      doRegistration(persistedMcpContribs);
    });
  }

  private synchronized void doRegistration(Map<String, McpRegistrationInfo> persistedMcpContribs) {
    try {
      FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
      if (flags != null && !flags.isMcpEnabled()) {
        return;
      }
      loadMcpRegistrationExtensionPoint();
      detectChangesInMcpContribs(persistedMcpContribs);
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Error during EXT MCP registration initialization", e);
    }
  }

  private Map<String, McpRegistrationInfo> loadPersistedMcpContribs() {
    String existingRecord = CopilotUi.getPlugin().getPreferenceStore().getString(Constants.MCP_EXTENSION_POINT_CONTRIB);
    if (existingRecord == null || existingRecord.trim().isEmpty()) {
      return Collections.emptyMap();
    }

    TypeToken<Map<String, McpRegistrationInfo>> typeToken = new TypeToken<Map<String, McpRegistrationInfo>>() {
    };

    return gson.fromJson(existingRecord, typeToken.getType());
  }

  /**
   * Set the combined MCP servers json from all approved plug-ins.
   */
  private void updateApprovedMcpServerString(Map<String, McpRegistrationInfo> extMcpInfoMap) {
    if (extMcpInfoMap == null || extMcpInfoMap.isEmpty()) {
      return;
    }

    Map<String, Object> allServers = new HashMap<>();
    extMcpInfoMap.forEach((contributor, regInfo) -> {
      if (!regInfo.isApproved()) {
        return; // Skip unapproved plug-ins
      }

      Map<String, Object> servers = regInfo.getMcpServers();
      if (servers != null && !servers.isEmpty()) {
        // Merge all servers into the result map
        servers.forEach((serverName, serverValue) -> {
          String displayServerName = regInfo.getPluginDisplayName() + ": " + serverName;
          allServers.merge(displayServerName, serverValue,
              (existingValue, newValue) -> newValue != null ? newValue : existingValue);
        });
      }
    });

    Map<String, Object> result = new HashMap<>();
    result.put("servers", allServers);
    this.approvedExtMcpServers = gson.toJson(result);
  }

  /**
   * Load MCP registration from extension point.
   */
  private void loadMcpRegistrationExtensionPoint() {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint = registry.getExtensionPoint(EXTENSION_POINT_ID);
    if (extensionPoint == null) {
      return;
    }

    // Traverse all extensions/bundles.
    IExtension[] extensions = extensionPoint.getExtensions();
    for (IExtension extension : extensions) {
      String bundleName = extension.getContributor().getName();
      if (!mcpRegTrustedPlugins.contains(bundleName)) {
        CopilotCore.LOGGER.info("Plug-in: " + bundleName + " is not in the allowed list.");
        continue; // Skip untrusted plug-ins
      }

      Bundle bundle = Platform.getBundle(bundleName);
      if (bundle == null) {
        CopilotCore.LOGGER.error("Cannot find bundle: " + bundleName, null);
        continue; // Skip inactive plug-ins
      }
      String pluginDisplayName = getPluginDisplayName(bundle);

      Map<String, Object> mergedServers = new HashMap<>();
      boolean isTrusted = isMcpFromSignedBundle(bundle);
      boolean isApproved = false; // default value

      // Traverse all providers in this extension, merge with servers under the same bundle name
      IConfigurationElement[] configElements = extension.getConfigurationElements();
      for (IConfigurationElement element : configElements) {
        if (ELEMENT_PROVIDER.equals(element.getName())) {
          try {
            Object provider = element.createExecutableExtension(ATTRIBUTE_CLASS);
            if (provider instanceof IMcpRegistrationProvider mcpProvider) {
              CompletableFuture<String> mcpServersFuture = mcpProvider.getMcpServerConfigurations();

              try {
                String mcpServers = mcpServersFuture.get();
                Map<String, Object> configObject = gson.fromJson(mcpServers, Map.class);
                if (configObject != null && configObject.containsKey("servers")) {
                  Object serversObj = configObject.get("servers");
                  if (serversObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> servers = (Map<String, Object>) serversObj;
                    // Duplicate server name is not allowed, which will be overridden by the last one
                    mergedServers.putAll(servers);
                  }
                }
              } catch (JsonSyntaxException e) {
                CopilotCore.LOGGER.error("Invalid JSON from provider: " + element.getAttribute(ATTRIBUTE_CLASS), e);
              } catch (InterruptedException | ExecutionException e) {
                CopilotCore.LOGGER.error(
                    "Failed to get MCP server configurations from provider: " + element.getAttribute(ATTRIBUTE_CLASS),
                    e);
              }
            }
          } catch (CoreException e) {
            CopilotCore.LOGGER
                .error("Failed to get display info for provider: " + element.getAttribute(ATTRIBUTE_CLASS), e);
          }
        }
      }

      // Update registration info
      if (!mergedServers.isEmpty()) {
        extMcpInfoMap.put(bundleName, new McpRegistrationInfo(isTrusted, isApproved, pluginDisplayName, mergedServers));
      }
    }
  }

  /**
   * Get the human-readable plugin name from the bundle name. Falls back to bundle name if Bundle-Name header is not
   * available.
   */
  private String getPluginDisplayName(Bundle bundle) {
    String bundleVender = bundle.getHeaders().get("Bundle-Vendor");
    if (bundleVender != null && !bundleVender.trim().isEmpty()) {
      return bundleVender;
    }

    return bundle.getSymbolicName(); // Fallback to bundle name if display name is not available
  }

  // TODO: check if we need verify the cert
  private boolean isMcpFromSignedBundle(Bundle bundle) {
    // Obtain SignedContentFactory via OSGi service
    SignedContent signedContent = null;
    BundleContext ctx = FrameworkUtil.getBundle(McpConfigService.class).getBundleContext();
    if (ctx != null) {
      ServiceReference<SignedContentFactory> ref = ctx.getServiceReference(SignedContentFactory.class);
      if (ref != null) {
        SignedContentFactory factory = ctx.getService(ref);
        try {
          if (factory != null) {
            try {
              signedContent = factory.getSignedContent(bundle);
            } catch (Exception e) {
              CopilotCore.LOGGER.error("Failed to validate signature for " + bundle.getSymbolicName(), e);
            }
          }
        } finally {
          ctx.ungetService(ref);
        }
      }
    }

    return signedContent != null && signedContent.isSigned();
  }

  /**
   * Detect changes in MCP registration from extension point compared to the existing record.
   */
  private void detectChangesInMcpContribs(Map<String, McpRegistrationInfo> existingExtMcpInfoMap) {
    // No new registration if no registration from extension point
    if (extMcpInfoMap.isEmpty()) {
      return;
    }

    boolean newExtMcpRegFound = false;
    // extMcpInfoMap is not empty, and no existing record, all registrations are new
    if (existingExtMcpInfoMap == null || existingExtMcpInfoMap.isEmpty()) {
      newExtMcpRegFound = true;
    } else {
      // Compare each plugin's current MCP servers with the stored record
      for (Map.Entry<String, McpRegistrationInfo> entry : extMcpInfoMap.entrySet()) {
        String contributorName = entry.getKey();
        McpRegistrationInfo mcpRegistrationInfo = entry.getValue();
        McpRegistrationInfo storedInfo = existingExtMcpInfoMap.get(contributorName);
        if (storedInfo != null) {
          String storedMcpServersJson = storedInfo.getMcpServersAsJson();
          String currentMcpServersJson = mcpRegistrationInfo.getMcpServersAsJson();
          if (currentMcpServersJson.equals(storedMcpServersJson)) {
            mcpRegistrationInfo.setApproved(storedInfo.isApproved());
          } else {
            // we do not early break here, to make sure all new registrations are marked
            newExtMcpRegFound = true;
          }
        } else {
          newExtMcpRegFound = true;
        }
      }
    }

    if (newExtMcpRegFound) {
      mcpConfigService.setNewExtMcpRegFound(true);
    }
  }

  /**
   * Process MCP registration from extension point.
   */
  public String approveExtMcpRegistration() {
    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    if (extMcpInfoMap.isEmpty()) {
      MessageDialog.openInformation(shell, "", "No MCP server registration found");
      return null;
    }

    McpApprovalDialog dialog = new McpApprovalDialog(shell, extMcpInfoMap);
    dialog.open();

    mcpConfigService.setNewExtMcpRegFound(false); // Reset the flag after user approval
    updateApprovedMcpServerString(extMcpInfoMap);
    persistExtMcpInfo(extMcpInfoMap);
    return approvedExtMcpServers;
  }

  private void persistExtMcpInfo(Map<String, McpRegistrationInfo> extMcpInfoMap) {
    IPreferenceStore preferenceStore = CopilotUi.getPlugin().getPreferenceStore();
    String extMcpInfo = gson.toJson(extMcpInfoMap);
    preferenceStore.setValue(Constants.MCP_EXTENSION_POINT_CONTRIB, extMcpInfo);
    // Necessary for persistence
    try {
      InstanceScope.INSTANCE.getNode("com.microsoft.copilot.eclipse.ui").flush();
    } catch (BackingStoreException e) {
      CopilotCore.LOGGER.error("Failed to flush preferences to disk", e);
    }
  }

  public String getApprovedExtMcpServers() {
    return approvedExtMcpServers;
  }

  /**
   * Class to hold MCP registration information.
   */
  public static class McpRegistrationInfo {
    boolean isTrusted;
    boolean isApproved;
    String pluginDisplayName;
    Map<String, Object> mcpServers; // Changed from String to Map<String, Object>

    McpRegistrationInfo(boolean isTrusted, boolean isEnabled, String pluginDisplayName,
        Map<String, Object> mcpServers) {
      this.isTrusted = isTrusted;
      this.isApproved = isEnabled;
      this.pluginDisplayName = pluginDisplayName;
      this.mcpServers = mcpServers;
    }

    public boolean isTrusted() {
      return isTrusted;
    }

    public boolean isApproved() {
      return isApproved;
    }

    public void setApproved(boolean approved) {
      this.isApproved = approved;
    }

    public String getPluginDisplayName() {
      return pluginDisplayName;
    }

    public Map<String, Object> getMcpServers() {
      return mcpServers;
    }

    /**
     * Get MCP servers as JSON string for preview display purposes.
     *
     * @return JSON string representation of the MCP servers
     */
    public String getMcpServersAsJson() {
      if (mcpServers == null || mcpServers.isEmpty()) {
        return null;
      }
      Gson gson = new Gson();
      Map<String, Object> wrapper = new HashMap<>();
      wrapper.put("servers", mcpServers);
      return gson.toJson(wrapper);
    }
  }

  /**
   * Check if there is any MCP registration from extension point.
   */
  public boolean hasExtMcpRegistration() {
    return !extMcpInfoMap.isEmpty();
  }
}

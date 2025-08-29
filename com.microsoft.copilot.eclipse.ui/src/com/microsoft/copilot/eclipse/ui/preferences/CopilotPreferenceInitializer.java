package com.microsoft.copilot.eclipse.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.ui.CopilotUi;

/**
 * A class to initialize the default preferences for the plugin.
 */
public class CopilotPreferenceInitializer extends AbstractPreferenceInitializer {

  @Override
  public void initializeDefaultPreferences() {
    IPreferenceStore pref = CopilotUi.getPlugin().getPreferenceStore();
    pref.setDefault(Constants.AUTO_SHOW_COMPLETION, true);
    pref.setDefault(Constants.ENABLE_STRICT_SSL, true);
    pref.setDefault(Constants.PROXY_KERBEROS_SP, "");
    pref.setDefault(Constants.GITHUB_ENTERPRISE, "");
    pref.setDefault(Constants.WORKSPACE_CONTEXT_ENABLED, false);
    pref.setDefault(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE_ENABLED, false);
    pref.setDefault(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE, "");
    pref.setDefault(Constants.MCP, """
        {
          "servers": {
            // example 1: local mcp server
            // "local-mcp-server": {
            //   "type": "stdio",
            //   "command": "my-command",
            //   "args": [],
            //   "env": { "<KEY>": "<VALUE>" }
            // }
            // example 2: remote mcp server
            // "remote-mcp-server": {
            //   "url": "<URL>",
            //   "requestInit": {
            //     "headers": {
            //       "Authorization": "Bearer <TOKEN>"
            //     }
            //   }
            // }
          }
        }
         """);
    pref.setDefault(Constants.MCP_TOOLS_STATUS, "{}");

    IEclipsePreferences configPrefs = ConfigurationScope.INSTANCE.getNode(Constants.PLUGIN_ID);
    String autoShowWhatsNew = configPrefs.get(Constants.AUTO_SHOW_WHAT_IS_NEW, null);
    if (autoShowWhatsNew == null) {
      configPrefs.putBoolean(Constants.AUTO_SHOW_WHAT_IS_NEW, true);
    }
  }
}

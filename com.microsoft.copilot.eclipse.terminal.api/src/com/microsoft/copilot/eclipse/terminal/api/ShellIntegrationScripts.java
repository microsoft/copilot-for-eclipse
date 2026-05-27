// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.terminal.api;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Utility class for accessing the shell integration script.
 */
public final class ShellIntegrationScripts {

  /** OSC namespace used by Copilot shell integration markers. */
  public static final String OSC_NAMESPACE = "7775";

  /** Marker emitted when the prompt starts. */
  public static final String PROMPT_START_MARKER = "\u001b]" + OSC_NAMESPACE + ";A\u0007";

  /** Marker emitted when the prompt ends. */
  public static final String PROMPT_END_MARKER = "\u001b]" + OSC_NAMESPACE + ";B\u0007";

  /** Marker emitted when a command finishes without an exit code. */
  public static final String COMMAND_FINISH_MARKER = "\u001b]" + OSC_NAMESPACE + ";C\u0007";

  /** Marker prefix emitted when a command finishes with an exit code. */
  public static final String COMMAND_FINISH_MARKER_PREFIX = "\u001b]" + OSC_NAMESPACE + ";C;";

  /** Pattern matching Copilot OSC markers, including markers that lost ESC/BEL during terminal processing. */
  public static final String OSC_MARKER_PATTERN = buildOscMarkerPattern();

  private static final String SCRIPTS_PATH = "scripts/";
  private static final String BASH_SCRIPT = "copilot-bash-integration.sh";
  private static final String POWERSHELL_SCRIPT = "copilot-powershell-integration.ps1";

  private ShellIntegrationScripts() {
    // Utility class
  }

  private static String buildOscMarkerPattern() {
    return "(?:\u001B)?\\]" + OSC_NAMESPACE + ";[ABC](?:;[-]?\\d+)?(?:\u0007|\u001B\\\\)?";
  }

  /**
   * Gets the absolute file path to the Bash integration script.
   *
   * @return the absolute path to the Bash script, or null if not found
   */
  public static String getBashScriptPath() {
    return getScriptPath(BASH_SCRIPT);
  }

  /**
   * Gets the absolute file path to the PowerShell integration script.
   *
   * @return the absolute path to the PowerShell script, or null if not found
   */
  public static String getPowerShellScriptPath() {
    return getScriptPath(POWERSHELL_SCRIPT);
  }

  private static String getScriptPath(String scriptName) {
    try {
      Bundle bundle = FrameworkUtil.getBundle(ShellIntegrationScripts.class);
      if (bundle == null) {
        return null;
      }

      URL scriptUrl = FileLocator.find(bundle, new Path(SCRIPTS_PATH + scriptName));
      if (scriptUrl == null) {
        return null;
      }

      URL fileUrl = FileLocator.toFileURL(scriptUrl);
      if (fileUrl == null) {
        return null;
      }

      File scriptFile = new File(fileUrl.getPath());
      if (scriptFile.exists()) {
        return scriptFile.getAbsolutePath();
      }
    } catch (IOException e) {
      CopilotCore.LOGGER.error("Failed to locate shell integration script: " + scriptName, e);
    }
    return null;
  }
}

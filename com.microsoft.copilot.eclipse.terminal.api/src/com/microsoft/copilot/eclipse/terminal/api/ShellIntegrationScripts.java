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

  /**
   * The marker string output by the shell integration script after each command completes.
   * Uses OSC (Operating System Command) escape sequence format: ESC ] 7775 ; marker BEL
   * This is invisible in terminal output but can be detected programmatically.
   */
  public static final String SHELL_MARKER = "\u001b]7775;C\u0007";

  private static final String SCRIPTS_PATH = "scripts/";
  private static final String SH_SCRIPT = "copilot-sh-integration.sh";

  private ShellIntegrationScripts() {
    // Utility class
  }

  /**
   * Gets the absolute file path to the POSIX sh integration script.
   *
   * @return the absolute path to the sh script, or null if not found
   */
  public static String getShScriptPath() {
    try {
      Bundle bundle = FrameworkUtil.getBundle(ShellIntegrationScripts.class);
      if (bundle == null) {
        return null;
      }

      URL scriptUrl = FileLocator.find(bundle, new Path(SCRIPTS_PATH + SH_SCRIPT));
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
      CopilotCore.LOGGER.error("Failed to locate shell integration script: " + SH_SCRIPT, e);
    }
    return null;
  }
}

package com.microsoft.copilot.eclipse.core.lsp;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;
import org.osgi.framework.Bundle;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotCapabilities;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InitializationOptions;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NameAndVersion;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;

/**
 * Stream connection provider for the Copilot language server.
 */
public class LsStreamConnectionProvider extends ProcessStreamConnectionProvider {

  public static final String EDITOR_NAME = "Eclipse";

  public static final String EDITOR_PLUGIN_NAME = "GotHub Copilot for Eclipse";

  @Override
  public Object getInitializationOptions(@Nullable URI rootUri) {
    NameAndVersion editorInfo = new NameAndVersion(EDITOR_NAME, PlatformUtils.getEclipseVersion());
    Bundle bundle = CopilotCore.getPlugin().getBundle();
    String bundleVersion = bundle == null ? "unknown" : bundle.getVersion().toString();
    NameAndVersion editorPluginInfo = new NameAndVersion(EDITOR_PLUGIN_NAME, bundleVersion);
    CopilotCapabilities capabilities = new CopilotCapabilities(false, false);
    return new InitializationOptions(editorInfo, editorPluginInfo, capabilities);
  }

  @Override
  public void start() throws IOException {
    // load lsp binary
    // call normalize to remove any relative path components and avoid "FILE_PATH_TOO_LONG" error
    Path binary = findBinary().normalize();
    if (binary == null) {
      throw new IOException("Could not find the language server binary");
    }

    File executable = binary.toFile();
    if (!executable.canExecute()) {
      boolean canExecute = executable.setExecutable(true);
      if (!canExecute) {
        // TODO: throw error or handle it?
      }
    }
    List<String> commands = new LinkedList<>();
    commands.add(binary.toString());
    commands.add("--stdio");
    this.setCommands(commands);
    super.start();
  }

  private @Nullable Path findBinary() throws IOException {
    if (PlatformUtils.isMac() && PlatformUtils.isIntel64()) {
      return null;
    }

    Path binDir = findAgentBinaryDirectoryPath();
    if (binDir == null) {
      return null;
    }

    Path executable = null;
    if (PlatformUtils.isLinux()) {
      executable = binDir.resolve("linux-x64/copilot-language-server");
    } else if (PlatformUtils.isWindows()) {
      executable = binDir.resolve("win32-x64/copilot-language-server.exe");
    } else if (PlatformUtils.isMac()) {
      if (PlatformUtils.isArm64()) {
        executable = binDir.resolve("darwin-arm64/copilot-language-server");
      } else {
        executable = binDir.resolve("darwin-x64/copilot-language-server");
      }
    }

    return executable != null && Files.exists(executable) ? executable : null;
  }

  private @Nullable Path findAgentBinaryDirectoryPath() throws IOException {
    URL url = CopilotCore.getPlugin().getBundle().getEntry("copilot-agent/native");
    if (url == null) {
      return null;
    }

    try {
      return URIUtil.toFile(URIUtil.toURI(FileLocator.toFileURL(url))).toPath();
    } catch (URISyntaxException | IOException e) {
      // TODO: Log exception via telemetry.
      return null;
    }
  }

}

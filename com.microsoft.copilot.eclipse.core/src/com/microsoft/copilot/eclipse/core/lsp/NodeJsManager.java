// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.wildwebdeveloper.embedder.node.Activator;
import org.eclipse.wildwebdeveloper.embedder.node.CompressUtils;

/**
 * Utility class to manage Node.js installation.
 */
public class NodeJsManager {

  private NodeJsManager() {
    // Prevent instantiation
  }

  private static final String NODE_ROOT_DIRECTORY = ".node";

  private static final String MACOS_DSCL_SHELL_PREFIX = "UserShell: ";

  /**
   * The minimum required version of Node.js for Copilot Language Server.
   */
  private static final int REQUIRED_MINIMUM_VERSION = 22;

  private static final String NODE_NAME = "node";

  private static Properties cachedNodeJsInfoProperties;
  private static final Object EXPAND_LOCK = new Object();

  /**
   * Finds Node.js executable installed in following list of locations:
   * <ul>
   * <li>System property <code>org.eclipse.wildwebdeveloper.nodeJSLocation</code></li>
   * <li>Platform Install Location</li>
   * <li>Platform User Location</li>
   * <li>WWD Node bundle configuration location</li>
   * <li>OS dependent default installation path</li>
   * </ul>
   *
   * @return The file for Node.js executable or null if it cannot be installed.
   */
  public static File getNodeJsLocation() {
    File nodeJsLocation = getNodeJsFromSystemProperty();
    if (nodeJsLocation != null) {
      return nodeJsLocation;
    }

    nodeJsLocation = getNodeJsFromInfoProperties();
    if (nodeJsLocation != null) {
      return nodeJsLocation;
    }

    nodeJsLocation = getNodeJsFromWhich();
    if (nodeJsLocation != null) {
      return nodeJsLocation;
    }

    return getNodeFromDefaultLocation();

  }

  private static File getNodeJsFromSystemProperty() {
    String nodeJsLocation = System.getProperty("org.eclipse.wildwebdeveloper.nodeJSLocation");
    if (nodeJsLocation != null) {
      File nodejs = new File(nodeJsLocation);
      if (validateNodeVersion(nodejs)) {
        return nodejs;
      }
    }
    return null;
  }

  private static File getNodeJsFromInfoProperties() {
    Properties properties = getNodeJsInfoProperties();
    File nodeJsLocation = null;
    if (properties != null) {
      File nodePath = probeNodeJsExacutable(properties);
      if (nodePath != null && nodePath.exists() && nodePath.canRead() && nodePath.canExecute()) {
        File exe = new File(nodePath.getParent(), NODE_NAME);
        if (exe.canExecute()) {
          nodeJsLocation = exe;
        } else if (Platform.OS_WIN32.equals(Platform.getOS())) {
          exe = new File(nodePath.getParent(), NODE_NAME + ".exe");
          if (exe.canExecute()) {
            nodeJsLocation = exe;
          }
        }
      } else {
        try {
          File installationPath = probeNodeJsInstallLocation();
          if (installationPath != null) {
            nodePath = new File(installationPath, properties.getProperty("nodePath"));
            synchronized (EXPAND_LOCK) {
              if (!nodePath.exists() || !nodePath.canRead() || !nodePath.canExecute()) {
                CompressUtils.unarchive(FileLocator.find(Activator.getDefault().getBundle(),
                    new Path(properties.getProperty("archiveFile"))), installationPath);
              }
            }
            nodeJsLocation = nodePath;
          }
        } catch (IOException e) {
          Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        }
      }
    }

    if (validateNodeVersion(nodeJsLocation)) {
      return nodeJsLocation;
    }
    return null;
  }

  private static File getNodeJsFromWhich() {
    String[] paths = System.getenv("PATH").split(System.getProperty("path.separator"));
    for (String path : paths) {
      File exe = new File(path, NODE_NAME);
      if (validateNodeVersion(exe)) {
        return exe;
      }
    }

    String[] command = new String[] { "/bin/bash", "-c", "-l", "which " + NODE_NAME };
    if (Platform.getOS().equals(Platform.OS_WIN32)) {
      command = new String[] { "cmd", "/c", "where " + NODE_NAME };
    } else if (Platform.getOS().equals(Platform.OS_MACOSX)) {
      command = new String[] { getDefaultShellMac(), "-c", "-li", "which " + NODE_NAME };
    }
    String res = executeCommand(command);
    File nodeJsLocation = res != null ? new File(res) : null;
    if (validateNodeVersion(nodeJsLocation)) {
      return nodeJsLocation;
    }

    return null;
  }

  private static File getNodeFromDefaultLocation() {
    File nodeJsLocation = getDefaultNodePath();
    if (validateNodeVersion(nodeJsLocation)) {
      return nodeJsLocation;
    }
    return null;
  }

  private static Properties getNodeJsInfoProperties() {
    if (cachedNodeJsInfoProperties == null) {
      URL nodeJsInfo = FileLocator.find(Activator.getDefault().getBundle(), new Path("nodejs-info.properties"));
      if (nodeJsInfo != null) {
        try (InputStream infoStream = nodeJsInfo.openStream()) {
          Properties properties = new Properties();
          properties.load(infoStream);
          cachedNodeJsInfoProperties = properties;
        } catch (IOException e) {
          Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        }
      }
    }
    return cachedNodeJsInfoProperties;
  }

  private static final File probeNodeJsInstallLocation() {
    File[] nodeJsLocations = getOrderedInstallationLocations();
    for (File installationPath : nodeJsLocations) {
      if (probeDirectoryForInstallation(installationPath)) {
        return installationPath;
      }
    }
    return null;
  }

  private static final boolean probeDirectoryForInstallation(File directory) {
    if (directory == null) {
      return false;
    }
    if (directory.exists() && directory.isDirectory() && directory.canWrite() && directory.canExecute()) {
      return true;
    }
    return probeDirectoryForInstallation(directory.getParentFile());
  }

  private static final File probeNodeJsExacutable(Properties properties) {
    File[] nodeJsLocations = getOrderedInstallationLocations();
    for (File installationPath : nodeJsLocations) {
      File nodePath = getNodeJsExecutable(installationPath, properties);
      if (nodePath != null) {
        return nodePath;
      }
    }
    return null;
  }

  private static final File[] getOrderedInstallationLocations() {
    return new File[] { toFile(Platform.getInstallLocation(), NODE_ROOT_DIRECTORY), // Platform Install Location
        toFile(Platform.getUserLocation(), NODE_ROOT_DIRECTORY), // Platform User Location
        toFile(Platform.getStateLocation(Activator.getDefault().getBundle())) // Default
    };
  }

  private static final File toFile(Location location, String binDirectory) {
    File installLocation = location != null && location.getURL() != null ? new File(location.getURL().getFile()) : null;
    if (installLocation != null && binDirectory != null) {
      installLocation = new File(installLocation, binDirectory);
    }
    return installLocation;
  }

  private static final File toFile(IPath locationPath) {
    return locationPath != null ? locationPath.toFile() : null;
  }

  private static final File getNodeJsExecutable(File installationLocation, Properties properties) {
    if (installationLocation != null) {
      File nodePath = new File(installationLocation, properties.getProperty("nodePath"));
      if (nodePath.exists() && nodePath.canRead() && nodePath.canExecute()) {
        return nodePath;
      }
    }
    return null;
  }

  private static String getDefaultShellMac() {
    String[] command = { "/bin/bash", "-c", "-l", "dscl . -read ~/ UserShell" };
    String res = executeCommand(command);
    if (res == null || !res.startsWith(MACOS_DSCL_SHELL_PREFIX)) {
      Activator.getDefault().getLog().log(new Status(IStatus.ERROR,
          Activator.getDefault().getBundle().getSymbolicName(), "Cannot find default shell. Use '/bin/zsh' instead."));
      return "/bin/zsh"; // Default shell since macOS 10.15
    }
    res = res.substring(MACOS_DSCL_SHELL_PREFIX.length());
    return res;
  }

  private static File getDefaultNodePath() {
    //@formatter:off
    return new File(
        switch (Platform.getOS()) {
          case Platform.OS_MACOSX -> "/usr/local/bin/node";
          case Platform.OS_WIN32 -> "C:\\Program Files\\nodejs\\node.exe";
          default -> "/usr/bin/node";
        }
    );
    //@formatter:on
  }

  private static boolean validateNodeVersion(File nodeJsLocation) {
    if (nodeJsLocation == null || !nodeJsLocation.exists() || !nodeJsLocation.canExecute()) {
      return false;
    }
    String[] nodeVersionCommand = { nodeJsLocation.getAbsolutePath(), "-v" };
    String nodeVersion = executeCommand(nodeVersionCommand);

    if (nodeVersion == null) {
      return false;
    }

    // Parse version string (format is typically "v12.18.3")
    if (nodeVersion.startsWith("v")) {
      nodeVersion = nodeVersion.substring(1); // Remove 'v' prefix
    }

    try {
      String majorVersionStr = nodeVersion.split("\\.")[0];
      int majorVersion = Integer.parseInt(majorVersionStr);
      return majorVersion >= REQUIRED_MINIMUM_VERSION;
    } catch (Exception e) {
      Activator.getDefault().getLog().log(new Status(IStatus.ERROR,
          Activator.getDefault().getBundle().getSymbolicName(), "Failed to parse Node.js version: " + nodeVersion, e));
      return false;
    }
  }

  /**
   * Executes a command and returns the first line of its output.
   *
   * @param command The command and its arguments as a string array.
   * @return The first line of the command output, or null if execution failed
   */
  private static String executeCommand(String[] command) {
    String result = null;
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(Runtime.getRuntime().exec(command).getInputStream()))) {
      result = reader.readLine();
    } catch (IOException e) {
      Activator.getDefault().getLog()
          .log(new Status(IStatus.ERROR, Activator.getDefault().getBundle().getSymbolicName(), e.getMessage(), e));
    }
    return result;
  }
}

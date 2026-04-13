package com.microsoft.copilot.eclipse.ui.chat.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osgi.framework.Bundle;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpRuntimeLog;

/**
 * Console handler for MCP Runtime logs that writes to a dedicated Eclipse console.
 */
public class McpRuntimeLogger {

  private final String consoleName = "Copilot (MCP)";
  private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private final String uiPluginId = "com.microsoft.copilot.eclipse.ui";
  private final String iconPath = "icons/github_copilot.png";

  private MessageConsole mcpConsole;
  private MessageConsoleStream mcpConsoleStream;
  private IEventBroker eventBroker;
  private EventHandler mcpRuntimeLoggerEventHandler;

  /**
   * Constructor for the McpRuntimeLogger. Initializes the event handler to listen for MCP Runtime log events.
   */
  public McpRuntimeLogger() {
    mcpRuntimeLoggerEventHandler = event -> {
      Object data = event.getProperty(IEventBroker.DATA);
      if (data instanceof McpRuntimeLog mcpRuntimeLog) {
        println(mcpRuntimeLog);
      }
    };

    eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    if (eventBroker != null) {
      eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_MCP_RUNTIME_LOG, mcpRuntimeLoggerEventHandler);
    } else {
      CopilotCore.LOGGER.error(new IllegalStateException("Event broker is null"));
    }
  }

  /**
   * Get or create the MCP Runtime console.
   *
   * @return The message console
   */
  private MessageConsole getConsole() {
    if (mcpConsole == null) {
      IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();

      // Look for existing console
      for (IConsole existing : consoleManager.getConsoles()) {
        if (consoleName.equals(existing.getName())) {
          mcpConsole = (MessageConsole) existing;
          break;
        }
      }

      // If not found, create a new one
      if (mcpConsole == null) {
        ImageDescriptor icon = getIconImageDescriptor();
        mcpConsole = new MessageConsole(consoleName, icon);
        consoleManager.addConsoles(new IConsole[] { mcpConsole });
      }
    }

    return mcpConsole;
  }

  /**
   * Get the icon image descriptor from the UI plugin.
   *
   * @return ImageDescriptor for the console icon
   */
  private ImageDescriptor getIconImageDescriptor() {
    try {
      Bundle bundle = Platform.getBundle(uiPluginId);
      if (bundle != null) {
        return ImageDescriptor.createFromURL(
            FileLocator.find(bundle, new Path(iconPath), null));
      }
    } catch (Exception e) {
      // Fall back to null icon if there's any problem loading
    }
    return null;
  }

  /**
   * Get console output stream.
   *
   * @return Console message stream
   */
  private MessageConsoleStream getConsoleStream() {
    if (mcpConsoleStream == null) {
      mcpConsoleStream = getConsole().newMessageStream();
    }
    return mcpConsoleStream;
  }

  /**
   * Print a message to the MCP Runtime console.
   */
  public void println(McpRuntimeLog mcpRuntimeLog) {
    Objects.requireNonNull(mcpRuntimeLog, "McpRuntimeLog entry cannot be null");
    StringBuilder logBuilder = new StringBuilder();

    if (mcpRuntimeLog.getTime() != null) {
      LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(mcpRuntimeLog.getTime()),
          ZoneId.systemDefault());
      logBuilder.append('[').append(dateFormat.format(dateTime)).append("] ");
    }

    if (mcpRuntimeLog.getLevel() != null) {
      logBuilder.append('[').append(mcpRuntimeLog.getLevel()).append("] ");
    }

    if (mcpRuntimeLog.getServer() != null) {
      logBuilder.append("[Server: ").append(mcpRuntimeLog.getServer()).append("] ");
    }

    if (mcpRuntimeLog.getTool() != null) {
      logBuilder.append("[Tool: ").append(mcpRuntimeLog.getTool()).append("]");
    }

    logBuilder.append("\nMessage: " + mcpRuntimeLog.getMessage() + "\n");

    MessageConsoleStream stream = getConsoleStream();
    stream.println(logBuilder.toString());
  }
}
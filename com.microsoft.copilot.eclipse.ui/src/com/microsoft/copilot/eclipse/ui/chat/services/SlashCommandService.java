package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotAuthStatusListener;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationTemplate;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;

/**
 * Service for handling slash commands.
 */
public class SlashCommandService implements CopilotAuthStatusListener {
  private List<ConversationTemplate> templates = new  ArrayList<>();
  private HashSet<String> allCommands = new HashSet<>();
  // Exclude intelliJ sepcific slash commands
  private static final Set<String> EXCLUDED_COMMANDS = Set.of("help", "feedback");
  public static final String INIT_JOB_FAMILY = 
      "com.microsoft.copilot.eclipse.chat.services.SlashCommandService.initJob";
  private CopilotLanguageServerConnection lsConnection;
  private AuthStatusManager authStatusManager;

  /**
   * Constructor for the SlashCommandService.
   */
  public SlashCommandService(CopilotLanguageServerConnection lsConnection, AuthStatusManager authStatusManager) {
    this.authStatusManager = authStatusManager;
    this.lsConnection = lsConnection;
    this.authStatusManager.addCopilotAuthStatusListener(this);
    syncCommands(this.authStatusManager.getCopilotStatus());
  }

  private void initAsync() {
    final Runnable initRunnable = () -> {
      initConversationTemplates();
    };

    Job initJob = new Job("Initialize slash commands service") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        initRunnable.run();
        return Status.OK_STATUS;
      }

      @Override
      public boolean belongsTo(Object family) {
        return Objects.equals(INIT_JOB_FAMILY, family);
      }
    };
    initJob.setUser(false);
    initJob.schedule();
  }

  private boolean initConversationTemplates() {
    if (isTempaltesReady()) {
      return true;
    }

    try {
      ConversationTemplate[] rawTemplates = this.lsConnection.listConversationTemplates().get();
      for (ConversationTemplate template : rawTemplates) {
        if (template.getScopes().contains(CopilotScope.CHAT_PANEL) && !EXCLUDED_COMMANDS.contains(template.getId())) {
          templates.add(template);
          allCommands.add("/" + template.getId());
        }
      }
    } catch (InterruptedException | ExecutionException e) {
      CopilotCore.LOGGER.error(e);
      return false;
    }
    return true;
  }

  /**
   * Find a broken slash command in the given text.
   *
   * @param text the text
   * @return the start and end index of the broken slash command
   */
  public boolean isBrokenCommand(String text, int cursorPosition) {
    if (allCommands == null) {
      return false;
    }
    // Try to recover the text by adding a dot at the cursor position
    String recoveredText = "^" + text.substring(0, cursorPosition) + "." + text.substring(cursorPosition) + "$";
    for (String command : allCommands) {
      if (command.matches(recoveredText)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Find a slash command in the given text.
   *
   * @param text the text
   * @return the start and end index of the slash command
   */
  public boolean isCommand(String text) {
    if (allCommands == null) {
      return false;
    }
    return allCommands.contains(text);
  }

  public boolean isTempaltesReady() {
    return templates != null && templates.size() > 0;
  }

  public ConversationTemplate[] getTemplates() {
    return templates.toArray(new ConversationTemplate[0]);
  }

  @Override
  public void onDidCopilotStatusChange(CopilotStatusResult copilotStatusResult) {
    String status = copilotStatusResult.getStatus();
    syncCommands(status);
  }

  private void syncCommands(String status) {
    switch (status) {
      case CopilotStatusResult.OK:
        initAsync();
        break;
      default:
        allCommands.clear();
        templates.clear();
        break;
    }
  }

  /**
   * Dispose the service.
   */
  public void dispose() {
    this.authStatusManager.removeCopilotAuthStatusListener(this);
  }
}

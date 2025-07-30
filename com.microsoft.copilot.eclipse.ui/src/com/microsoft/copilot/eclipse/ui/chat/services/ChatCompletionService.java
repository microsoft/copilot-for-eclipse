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
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.core.IdeCapabilities;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationAgent;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationTemplate;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;

/**
 * Service for handling slash commands.
 */
public class ChatCompletionService implements CopilotAuthStatusListener {
  public static final String AGENT_MARK = "@";
  public static final String TEMPLATE_MARK = "/";

  private List<ConversationTemplate> templates = new ArrayList<>();
  private List<ConversationAgent> agents = new ArrayList<>();
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
  public ChatCompletionService(CopilotLanguageServerConnection lsConnection, AuthStatusManager authStatusManager) {
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

  private void initConversationTemplates() {
    if (isTempaltesReady() && isAgentsReady()) {
      return;
    }

    // Command: /***
    try {
      ConversationTemplate[] rawTemplates = this.lsConnection.listConversationTemplates().get();
      for (ConversationTemplate template : rawTemplates) {
        if (template.getScopes().contains(CopilotScope.CHAT_PANEL) && !EXCLUDED_COMMANDS.contains(template.getId())) {
          templates.add(template);
          allCommands.add(TEMPLATE_MARK + template.getId());
        }
      }
    } catch (InterruptedException | ExecutionException e) {
      CopilotCore.LOGGER.error(e);
    }

    // Command: @***
    try {
      ConversationAgent[] rawAgents = this.lsConnection.listConversationAgents().get();
      for (ConversationAgent agent : rawAgents) {
        String agentSlug = agent.getSlug();
        // @see ui.chat.ChatView#replaceWorkspaceCommand(String)
        if (agentSlug.equals("project")) {
          if (!FeatureFlags.isWorkspaceContextEnabled()) {
            continue;
          }

          agent.setSlug("workspace");
        }
        agents.add(agent);
        allCommands.add(AGENT_MARK + agent.getSlug());
      }
    } catch (InterruptedException | ExecutionException e) {
      CopilotCore.LOGGER.error(e);
    }
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
    String recoveredText = text.substring(0, cursorPosition) + "." + text.substring(cursorPosition);
    for (String command : allCommands) {
      if (matchesRecoveredCommand(recoveredText, command)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchesRecoveredCommand(String recovered, String command) {
    if (command.length() != recovered.length()) {
      return false;
    }
    int diffCount = 0;
    for (int i = 0; i < command.length(); i++) {
      if (command.charAt(i) != recovered.charAt(i)) {
        diffCount++;
        if (diffCount > 1) {
          return false;
        }
      }
    }
    return true;
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

  public boolean isAgentsReady() {
    return agents != null && agents.size() > 0;
  }

  public ConversationTemplate[] getTemplates() {
    return templates.toArray(new ConversationTemplate[0]);
  }

  public ConversationAgent[] getAgents() {
    return agents.toArray(new ConversationAgent[0]);
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
        agents.clear();
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

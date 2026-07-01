// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotAuthStatusListener;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.core.chat.service.ICustomizationFileService.CustomizationType;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationAgent;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationTemplate;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.TemplateSource;
import com.microsoft.copilot.eclipse.ui.utils.PreferencesUtils;

/**
 * Service for handling slash commands.
 */
public class ChatCompletionService implements CopilotAuthStatusListener {
  public static final String AGENT_MARK = "@";
  public static final String TEMPLATE_MARK = "/";

  private volatile List<ConversationTemplate> templates = List.of();
  private volatile List<ConversationAgent> agents = List.of();
  private volatile Set<String> allCommands = Set.of();
  // Exclude intelliJ sepcific slash commands
  private static final Set<String> EXCLUDED_COMMANDS = Set.of("help", "feedback");
  public static final String REFRESH_JOB_FAMILY =
      "com.microsoft.copilot.eclipse.chat.services.SlashCommandService.refreshJob";
  private CopilotLanguageServerConnection lsConnection;
  private AuthStatusManager authStatusManager;
  private IResourceChangeListener skillFileListener;
  private IEventBroker eventBroker;
  private EventHandler customPromptsChangedHandler;

  private static final String SKILL_FILE_NAME = "SKILL.md";
  private static final String PROMPT_FILE_SUFFIX = ".prompt.md";

  /**
   * Constructor for the SlashCommandService.
   */
  public ChatCompletionService(CopilotLanguageServerConnection lsConnection, AuthStatusManager authStatusManager) {
    this.authStatusManager = authStatusManager;
    this.lsConnection = lsConnection;
    this.authStatusManager.addCopilotAuthStatusListener(this);
    // TODO: Remove this listener once workspace-root is removed from workspaceFolders in CopilotLanguageClient as CLS
    // can watch the project prompt file change directly.
    this.skillFileListener = new SkillFileChangeListener();
    ResourcesPlugin.getWorkspace().addResourceChangeListener(skillFileListener, IResourceChangeEvent.POST_CHANGE);
    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    if (this.eventBroker != null) {
      // Templates only surface skills and prompts, so ignore instruction/agent changes.
      this.customPromptsChangedHandler = event -> {
        Object type = event.getProperty(IEventBroker.DATA);
        if (type == CustomizationType.SKILL || type == CustomizationType.PROMPT) {
          fetchAsync();
        }
      };
      this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_DID_CHANGE_CUSTOMIZATION_FILES,
          customPromptsChangedHandler);
    }
    syncCommands(this.authStatusManager.getCopilotStatus());
  }

  private void fetchAsync() {
    Job.getJobManager().cancel(REFRESH_JOB_FAMILY);

    Job refreshJob = new Job("Refresh slash commands service") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        initConversationTemplates(monitor);
        if (monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
      }

      @Override
      public boolean belongsTo(Object family) {
        return Objects.equals(REFRESH_JOB_FAMILY, family);
      }
    };
    refreshJob.setUser(false);
    refreshJob.schedule();
  }

  private void initConversationTemplates(IProgressMonitor monitor) {
    List<ConversationTemplate> newTemplates = new ArrayList<>();
    List<ConversationAgent> newAgents = new ArrayList<>();
    Set<String> newCommands = new HashSet<>();
    boolean skillsEnabled = PreferencesUtils.isSkillsEnabled();

    // Command: /***
    // Pass workspace folders so the language server returns workspace-specific
    // prompt files (.prompt.md) and skills (SKILL.md) alongside built-in templates.
    try {
      List<WorkspaceFolder> workspaceFolders = LSPEclipseUtils.getWorkspaceFolders();
      ConversationTemplate[] rawTemplates = this.lsConnection.listConversationTemplates(workspaceFolders).get();
      if (monitor.isCanceled()) {
        return;
      }
      for (ConversationTemplate template : rawTemplates) {
        if (!skillsEnabled && template.source() == TemplateSource.SKILL) {
          continue;
        }
        if (!EXCLUDED_COMMANDS.contains(template.id())) {
          newTemplates.add(template);
          newCommands.add(TEMPLATE_MARK + template.id());
        }
      }
    } catch (InterruptedException | ExecutionException e) {
      CopilotCore.LOGGER.error(e);
    }

    if (monitor.isCanceled()) {
      return;
    }

    // Command: @***
    try {
      ConversationAgent[] rawAgents = this.lsConnection.listConversationAgents().get();
      if (monitor.isCanceled()) {
        return;
      }
      for (ConversationAgent agent : rawAgents) {
        String agentSlug = agent.getSlug();
        // @see ui.chat.ChatView#replaceWorkspaceCommand(String)
        if (agentSlug.equals("project")) {
          if (!FeatureFlags.isWorkspaceContextEnabled()) {
            continue;
          }

          agent.setSlug("workspace");
        }
        newAgents.add(agent);
        newCommands.add(AGENT_MARK + agent.getSlug());
      }
    } catch (InterruptedException | ExecutionException e) {
      CopilotCore.LOGGER.error(e);
    }

    if (monitor.isCanceled()) {
      return;
    }

    // Atomically swap the cached data so readers always see a consistent snapshot.
    // Publish immutable snapshots so readers cannot accidentally mutate a live collection.
    this.templates = List.copyOf(newTemplates);
    this.agents = List.copyOf(newAgents);
    this.allCommands = Set.copyOf(newCommands);
  }

  /**
   * Returns templates filtered by the scope appropriate for the given chat mode. In Agent mode only {@code agent-panel}
   * scoped templates (including skills) are shown; in Ask mode only {@code chat-panel} scoped templates are shown.
   */
  public ConversationTemplate[] getFilteredTemplates(ChatMode chatMode) {
    String scope = chatMode == ChatMode.Agent ? CopilotScope.AGENT_PANEL : CopilotScope.CHAT_PANEL;
    return templates.stream().filter(t -> t.scopes() != null && t.scopes().contains(scope))
        .toArray(ConversationTemplate[]::new);
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
        fetchAsync();
        break;
      default:
        this.allCommands = Set.of();
        this.templates = List.of();
        this.agents = List.of();
        break;
    }
  }

  /**
   * Dispose the service.
   */
  public void dispose() {
    this.authStatusManager.removeCopilotAuthStatusListener(this);
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(skillFileListener);
    if (this.eventBroker != null && this.customPromptsChangedHandler != null) {
      this.eventBroker.unsubscribe(this.customPromptsChangedHandler);
    }
  }

  /**
   * Listens for workspace resource changes involving SKILL.md or .prompt.md files and triggers a template refresh when
   * such files are added, removed, or changed.
   *
   * <p>TODO: Remove this listener once workspace-root is removed from workspaceFolders in CopilotLanguageClient as CLS
   * can watch the project prompt file change directly.
   */
  private class SkillFileChangeListener implements IResourceChangeListener {
    @Override
    public void resourceChanged(IResourceChangeEvent event) {
      IResourceDelta delta = event.getDelta();
      if (delta == null) {
        return;
      }
      boolean[] needsRefresh = { false };
      try {
        delta.accept(childDelta -> {
          if (needsRefresh[0]) {
            return false;
          }
          if (!shouldVisitDelta(childDelta)) {
            return false;
          }
          if (isPromptOrSkillFileDelta(childDelta)) {
            needsRefresh[0] = true;
            return false;
          }
          return true;
        });
      } catch (CoreException e) {
        CopilotCore.LOGGER.error("Error visiting resource delta for skill file changes", e);
      }
      if (needsRefresh[0]) {
        fetchAsync();
      }
    }

    private boolean shouldVisitDelta(IResourceDelta delta) {
      IResource resource = delta.getResource();
      return resource != null && !resource.isDerived() && !resource.isTeamPrivateMember();
    }

    private boolean isPromptOrSkillFileDelta(IResourceDelta delta) {
      IResource resource = delta.getResource();
      if (resource.getType() != IResource.FILE || !isRelevantFileDelta(delta)) {
        return false;
      }

      String name = resource.getName();
      return SKILL_FILE_NAME.equals(name) || name.endsWith(PROMPT_FILE_SUFFIX);
    }

    private boolean isRelevantFileDelta(IResourceDelta delta) {
      int kind = delta.getKind();
      if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED) {
        return true;
      }
      return kind == IResourceDelta.CHANGED && (delta.getFlags() & IResourceDelta.CONTENT) != 0;
    }
  }
}

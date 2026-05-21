// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.confirmation;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationAction;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationActionScope;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationContent;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolConfirmationParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ToolAnnotations;
import com.microsoft.copilot.eclipse.ui.chat.Messages;

/**
 * Evaluates MCP tool confirmation requests against session and global approval lists.
 * Supports per-server and per-tool approval at both session and global scopes,
 * plus optional trust-annotation-based auto-approve for read-only tools.
 */
public class McpConfirmationHandler implements ConfirmationHandler {

  /** Action types for MCP tool confirmation decisions. */
  public enum Action {
    /** Allow a specific tool for the current session/conversation. */
    ACCEPT_TOOL_SESSION,
    /** Always allow a specific tool (persisted globally). */
    ACCEPT_TOOL_GLOBAL,
    /** Allow all tools from a server for the current session/conversation. */
    ACCEPT_SERVER_SESSION,
    /** Always allow all tools from a server (persisted globally). */
    ACCEPT_SERVER_GLOBAL
  }

  static final String META_SERVER_NAME = "serverName";
  static final String META_TOOL_KEY = "toolKey";

  private static final String SEPARATOR = "::";
  private static final Type STRING_LIST_TYPE =
      new TypeToken<List<String>>() {}.getType();

  private final IPreferenceStore preferenceStore;

  // Session-scoped in-memory storage keyed by conversationId.
  private final Map<String, Set<String>> approvedServers =
      Collections.synchronizedMap(new LinkedHashMap<>());
  private final Map<String, Set<String>> approvedTools =
      Collections.synchronizedMap(new LinkedHashMap<>());

  /**
   * Creates a new McpConfirmationHandler.
   *
   * @param preferenceStore the preference store for reading MCP auto-approve settings
   */
  public McpConfirmationHandler(IPreferenceStore preferenceStore) {
    this.preferenceStore = preferenceStore;
  }

  /**
   * When the auto-approval feature is disabled, MCP tools always prompt with
   * Allow Once / Skip only — no session or global approval buttons.
   * This matches IntelliJ's behavior where MCP ignores all rules when disabled.
   */
  @Override
  public ConfirmationResult evaluate(InvokeClientToolConfirmationParams params,
      String sessionConversationId, boolean isAutoApprovalEnabled) {
    if (!isAutoApprovalEnabled) {
      return evaluateAutoApprovalDisabled(params);
    }
    return evaluateAutoApprovalEnabled(params, sessionConversationId);
  }

  /**
   * Evaluates an MCP tool confirmation request. Check order:
   * 1. Session approved servers (by conversationId)
   * 2. Session approved tools (by conversationId, key = "server::tool")
   * 3. Global approved servers list
   * 4. Global approved tools list
   * 5. Trust annotations (readOnlyHint=true AND openWorldHint=false)
   * 6. Otherwise: needs confirmation
   */
  private ConfirmationResult evaluateAutoApprovalEnabled(
      InvokeClientToolConfirmationParams params,
      String sessionConversationId) {
    String serverName = extractServerName(params);
    String toolName = extractToolName(params);
    String serverLower = serverName != null
        ? serverName.toLowerCase(Locale.ROOT) : null;
    String toolKey = buildToolKey(serverLower, toolName);

    // 1. Session: server approved for this conversation
    if (serverLower != null) {
      Set<String> sessionServers =
          approvedServers.get(sessionConversationId);
      if (sessionServers != null
          && sessionServers.contains(serverLower)) {
        return ConfirmationResult.AUTO_APPROVED;
      }
    }

    // 2. Session: specific tool approved for this conversation
    if (toolKey != null) {
      Set<String> sessionTools =
          approvedTools.get(sessionConversationId);
      if (sessionTools != null && sessionTools.contains(toolKey)) {
        return ConfirmationResult.AUTO_APPROVED;
      }
    }

    // 3. Global: server in approved servers list
    if (serverLower != null) {
      List<String> globalServers = loadJsonList(
          Constants.AUTO_APPROVE_MCP_SERVERS);
      for (String s : globalServers) {
        if (s.toLowerCase(Locale.ROOT).equals(serverLower)) {
          return ConfirmationResult.AUTO_APPROVED;
        }
      }
    }

    // 4. Global: tool in approved tools list
    if (toolKey != null) {
      List<String> globalTools = loadJsonList(
          Constants.AUTO_APPROVE_MCP_TOOLS);
      for (String t : globalTools) {
        if (t.toLowerCase(Locale.ROOT).equals(toolKey)) {
          return ConfirmationResult.AUTO_APPROVED;
        }
      }
    }

    // 5. Trust annotations: read-only and not open-world
    if (preferenceStore.getBoolean(
        Constants.AUTO_APPROVE_TRUST_TOOL_ANNOTATIONS)) {
      ToolAnnotations annotations = params.getAnnotations();
      if (annotations != null
          && annotations.isReadOnlyHint()
          && !annotations.isOpenWorldHint()) {
        return ConfirmationResult.AUTO_APPROVED;
      }
    }

    // 6. Needs confirmation
    return ConfirmationResult.needsConfirmation(
        buildContent(params, serverName, toolName));
  }

  private ConfirmationResult evaluateAutoApprovalDisabled(
      InvokeClientToolConfirmationParams params) {
    String serverName = extractServerName(params);
    String toolName = extractToolName(params);
    return ConfirmationResult.needsConfirmation(
        buildContent(params, serverName, toolName, /* simplifiedOnly= */ true));
  }

  @Override
  public void cacheDecision(ConfirmationAction confirmAction,
      InvokeClientToolConfirmationParams params,
      String sessionConversationId) {
    String actionName = confirmAction.getMetadata()
        .get(ConfirmationAction.META_ACTION);
    if (actionName == null) {
      return;
    }
    Action type;
    try {
      type = Action.valueOf(actionName);
    } catch (IllegalArgumentException e) {
      return;
    }

    Map<String, String> meta = confirmAction.getMetadata();
    String serverName = meta.get(META_SERVER_NAME);
    String toolKey = meta.get(META_TOOL_KEY);

    switch (type) {
      case ACCEPT_TOOL_SESSION:
        if (toolKey != null) {
          synchronized (approvedTools) {
            ConfirmationHandler.evictOldestIfNeeded(approvedTools);
            approvedTools.computeIfAbsent(
                sessionConversationId,
                k -> ConcurrentHashMap.newKeySet())
                .add(toolKey.toLowerCase(Locale.ROOT));
          }
        }
        break;
      case ACCEPT_SERVER_SESSION:
        if (serverName != null) {
          synchronized (approvedServers) {
            ConfirmationHandler.evictOldestIfNeeded(approvedServers);
            approvedServers.computeIfAbsent(
                sessionConversationId,
                k -> ConcurrentHashMap.newKeySet())
                .add(serverName.toLowerCase(Locale.ROOT));
          }
        }
        break;
      case ACCEPT_TOOL_GLOBAL:
        if (toolKey != null) {
          addToGlobalList(Constants.AUTO_APPROVE_MCP_TOOLS, toolKey);
        }
        break;
      case ACCEPT_SERVER_GLOBAL:
        if (serverName != null) {
          addToGlobalList(Constants.AUTO_APPROVE_MCP_SERVERS, serverName);
        }
        break;
      default:
        break;
    }
  }

  @Override
  public void clearSession(String conversationId) {
    approvedServers.remove(conversationId);
    approvedTools.remove(conversationId);
  }

  private ConfirmationContent buildContent(
      InvokeClientToolConfirmationParams params,
      String serverName, String toolName) {
    return buildContent(params, serverName, toolName, false);
  }

  private ConfirmationContent buildContent(
      InvokeClientToolConfirmationParams params,
      String serverName, String toolName, boolean simplifiedOnly) {
    String toolKey = buildToolKey(
        serverName != null ? serverName.toLowerCase(Locale.ROOT) : null,
        toolName);

    List<ConfirmationAction> actions = new ArrayList<>();
    actions.add(ConfirmationAction.allowOnce(
        Messages.confirmation_action_allowOnce));

    if (!simplifiedOnly) {
      if (toolName != null && toolKey != null) {
        actions.add(action(Action.ACCEPT_TOOL_SESSION,
            NLS.bind(Messages.confirmation_action_allowNamesSession,
                "'" + toolName + "'"),
            ConfirmationActionScope.SESSION,
            Map.of(META_TOOL_KEY, toolKey)));
        actions.add(action(Action.ACCEPT_TOOL_GLOBAL,
            NLS.bind(Messages.confirmation_action_alwaysAllowNames,
                "'" + toolName + "'"),
            ConfirmationActionScope.GLOBAL,
            Map.of(META_TOOL_KEY, toolKey)));
      }

      if (serverName != null) {
        actions.add(action(Action.ACCEPT_SERVER_SESSION,
            NLS.bind(Messages.confirmation_action_allowServerSession,
                "'" + serverName + "'"),
            ConfirmationActionScope.SESSION,
            Map.of(META_SERVER_NAME, serverName)));
        actions.add(action(Action.ACCEPT_SERVER_GLOBAL,
            NLS.bind(Messages.confirmation_action_alwaysAllowServer,
                "'" + serverName + "'"),
            ConfirmationActionScope.GLOBAL,
            Map.of(META_SERVER_NAME, serverName)));
      }
    }

    actions.add(ConfirmationAction.skip(
        Messages.confirmation_action_skip));

    String title;
    if (toolName != null && serverName != null) {
      title = NLS.bind(Messages.confirmation_title_mcpTool,
          toolName, serverName);
    } else {
      title = params.getTitle() != null
          ? params.getTitle()
          : Messages.confirmation_title_mcpToolDefault;
    }

    return new ConfirmationContent(title, params.getMessage(), actions);
  }

  private static ConfirmationAction action(Action type, String label,
      ConfirmationActionScope scope, Map<String, String> extra) {
    Map<String, String> meta = new HashMap<>(extra);
    meta.put(ConfirmationAction.META_ACTION, type.name());
    return new ConfirmationAction(label, true, scope, meta, false);
  }

  private String extractServerName(
      InvokeClientToolConfirmationParams params) {
    Object input = params.getInput();
    if (input instanceof Map<?, ?> inputMap) {
      Object value = inputMap.get("mcpServerName");
      if (value instanceof String s && StringUtils.isNotBlank(s)) {
        return s;
      }
    }
    return null;
  }

  private String extractToolName(
      InvokeClientToolConfirmationParams params) {
    Object input = params.getInput();
    if (input instanceof Map<?, ?> inputMap) {
      Object value = inputMap.get("mcpToolName");
      if (value instanceof String s && StringUtils.isNotBlank(s)) {
        return s;
      }
    }
    return null;
  }

  private static String buildToolKey(String serverLower, String toolName) {
    if (serverLower == null || toolName == null) {
      return null;
    }
    return serverLower + SEPARATOR
        + toolName.toLowerCase(Locale.ROOT);
  }

  private List<String> loadJsonList(String preferenceKey) {
    String json = preferenceStore.getString(preferenceKey);
    if (StringUtils.isBlank(json) || "[]".equals(json.trim())) {
      return Collections.emptyList();
    }
    try {
      List<String> list = new Gson().fromJson(json, STRING_LIST_TYPE);
      return list != null ? list : Collections.emptyList();
    } catch (Exception e) {
      CopilotCore.LOGGER.error(
          "Failed to parse MCP auto-approve list: " + preferenceKey, e);
      return Collections.emptyList();
    }
  }

  private void addToGlobalList(String preferenceKey, String value) {
    List<String> current = new ArrayList<>(loadJsonList(preferenceKey));
    String lowerValue = value.toLowerCase(Locale.ROOT);
    for (String existing : current) {
      if (existing.toLowerCase(Locale.ROOT).equals(lowerValue)) {
        return; // already present
      }
    }
    current.add(value);
    preferenceStore.setValue(preferenceKey, new Gson().toJson(current));
  }

}

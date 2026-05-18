// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.confirmation;

import java.util.EnumMap;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;

import com.microsoft.copilot.eclipse.core.chat.ConfirmationAction;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationActionScope;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolConfirmationParams;

/**
 * Central entry point for auto-approve evaluation. Classifies each tool confirmation request
 * into a category using the toolType field provided by CLS, then dispatches to the registered
 * handler for that category. All session/global persist logic lives in each handler.
 */
public class ConfirmationService {

  /** Tool functional types matching CLS ToolFunctionalType enum values. */
  public enum ToolCategory {
    TERMINAL("terminal"),
    FILE_READ("file_read"),
    FILE_WRITE("file_write"),
    FILE_OPERATION("file_operation"),
    MCP_TOOL("mcp_tool"),
    SAFE_TOOL("safe_tool"),
    WEB("web"),
    UNKNOWN("unknown");

    private final String value;

    ToolCategory(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    /**
     * Resolves a CLS toolType string to a ToolCategory.
     */
    public static ToolCategory fromValue(String value) {
      if (value != null) {
        for (ToolCategory category : values()) {
          if (category.value.equals(value)) {
            return category;
          }
        }
      }
      return UNKNOWN;
    }
  }

  private final Map<ToolCategory, ConfirmationHandler> handlers =
      new EnumMap<>(ToolCategory.class);
  private final ConfirmationHandler fallbackHandler =
      new FallbackConfirmationHandler();
  private final IPreferenceStore preferenceStore;

  /**
   * Creates a new ConfirmationService.
   *
   * @param preferenceStore the preference store for reading auto-approve settings
   * @param attachedFileRegistry registry of user-attached context files
   */
  public ConfirmationService(IPreferenceStore preferenceStore,
      AttachedFileRegistry attachedFileRegistry) {
    this.preferenceStore = preferenceStore;
    handlers.put(ToolCategory.TERMINAL,
        new TerminalConfirmationHandler(preferenceStore));
    FileOperationConfirmationHandler fileHandler =
        new FileOperationConfirmationHandler(preferenceStore,
            attachedFileRegistry);
    handlers.put(ToolCategory.FILE_READ, fileHandler);
    handlers.put(ToolCategory.FILE_WRITE, fileHandler);
    handlers.put(ToolCategory.FILE_OPERATION, fileHandler);
  }

  /**
   * Evaluates whether a tool confirmation request should be auto-approved.
   *
   * @param params the confirmation request parameters
   * @param sessionConversationId the conversation ID for session-scoped lookups
   */
  public ConfirmationResult evaluate(
      InvokeClientToolConfirmationParams params,
      String sessionConversationId) {
    ToolCategory category = classify(params);
    if (category == ToolCategory.SAFE_TOOL) {
      return ConfirmationResult.AUTO_APPROVED;
    }

    ConfirmationHandler handler = handlers.get(category);
    if (handler == null) {
      handler = fallbackHandler;
    }
    return handler.evaluate(params, sessionConversationId);
  }

  /**
   * Caches the user's decision for future auto-approve lookups.
   *
   * @param action the user's selected action
   * @param params the original confirmation params
   * @param sessionConversationId the conversation ID for session storage
   */
  public void cacheDecision(ConfirmationAction action,
      InvokeClientToolConfirmationParams params,
      String sessionConversationId) {
    if (action == null || action.getScope() == null
        || action.getScope() == ConfirmationActionScope.ONCE) {
      return;
    }
    ToolCategory category = classify(params);
    ConfirmationHandler handler = handlers.get(category);
    if (handler != null) {
      handler.cacheDecision(action, params, sessionConversationId);
    }
  }

  /** Clears session-scoped approvals for a conversation across all handlers. */
  public void clearSession(String conversationId) {
    for (ConfirmationHandler handler : handlers.values()) {
      handler.clearSession(conversationId);
    }
  }

  ToolCategory classify(InvokeClientToolConfirmationParams params) {
    return ToolCategory.fromValue(extractToolType(params));
  }

  private String extractToolType(
      InvokeClientToolConfirmationParams params) {
    Object input = params.getInput();
    if (input instanceof Map<?, ?> inputMap) {
      Object toolType = inputMap.get("toolType");
      if (toolType instanceof String) {
        return (String) toolType;
      }
    }
    return null;
  }
}

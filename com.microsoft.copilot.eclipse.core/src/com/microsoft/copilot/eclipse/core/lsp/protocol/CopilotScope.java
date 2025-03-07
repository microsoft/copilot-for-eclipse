package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Scope constants for Copilot.
 * https://github.com/microsoft/copilot-client/blob/main/lib/src/conversation/promptTemplates.ts#L15
 */
public class CopilotScope {
  public static final String CHAT_PANEL = "chat-panel";
  // Scope targeting the editor, such as right click context.
  public static final String EDITOR = "editor";
  // Scope targeting the inline chat.
  public static final String INLINE = "inline";
  // Scope targeting code completions.
  public static final String COMPLETION = "completion";
}

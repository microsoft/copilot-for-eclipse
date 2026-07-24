// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import com.microsoft.copilot.eclipse.core.chat.ConfirmationAction;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationContent;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentToolCall;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ThinkingBlockData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ToolCallData;
import com.microsoft.copilot.eclipse.ui.chat.QuotaActions.QuotaAction;

/**
 * Factory for creating HTML block fragments used by {@link BrowserConversationWidget}.
 *
 * <p>Each method produces a self-contained HTML <code>DIV</code> block having an
 * {@code id} attribute. The widget inserts or updates these blocks in the browser DOM
 * using the generic JavaScript API for code block manipulation.
 */
public class ConversationHtmlBlockFactory {

  /**
   * DOM ID of the root chat container defined in {@code resources/html/chat-view.html}. New turn
   * containers and top-level blocks are appended as its children. Must stay in sync with the
   * {@code id} used in {@code chat-view.html}.
   */
  public static final String CHAT_CONTAINER_ID = "chat-container";

  private final Parser markdownParser;
  private final HtmlRenderer htmlRenderer;
  private String copyIconDataUri = "";
  private String insertIconDataUri = "";

  /** Creates a new factory with GFM tables and other extensions. */
  public ConversationHtmlBlockFactory() {
    List<Extension> extensions = List.of(
        TablesExtension.create(),
        TaskListItemsExtension.create(),
        StrikethroughExtension.create()
      );
    this.markdownParser = Parser.builder().extensions(extensions).build();
    this.htmlRenderer = HtmlRenderer.builder()
        .extensions(extensions)
        // prevent HTML injection in potentially malicious LLM-generated Markdown code
        .escapeHtml(true)
        // avoid potentially malicious URL schemes in LLM-generated Markdown code
        .sanitizeUrls(true)
        .build();
  }

  /**
   * Sets the data URIs for code block action button icons.
   *
   * @param copyIconUri base64 data URI for the copy icon
   * @param insertIconUri base64 data URI for the insert icon
   */
  public void setCodeBlockIcons(String copyIconUri, String insertIconUri) {
    this.copyIconDataUri = copyIconUri != null ? copyIconUri : "";
    this.insertIconDataUri = insertIconUri != null ? insertIconUri : "";
  }

  /**
   * User turn container ID: {@code turnId-user}. User and copilot turns share the same
   * server-assigned turn ID (workDoneToken), so each role gets a distinct container ID.
   */
  public static String userTurnContainerId(String turnId) {
    return turnId + "-user";
  }

  /**
   * Copilot turn container ID: {@code turnId-copilot}. Paired with
   * {@link #userTurnContainerId(String)} to avoid duplicate DOM IDs.
   */
  public static String copilotTurnContainerId(String turnId) {
    return turnId + "-copilot";
  }

  /** Content container ID, scoped by role: {@code turnId-user-content} or
   * {@code turnId-copilot-content}.
   */
  public static String contentBlockId(String turnId, boolean isCopilot) {
    String containerId = isCopilot
        ? copilotTurnContainerId(turnId) : userTurnContainerId(turnId);
    return containerId + "-content";
  }

  /** Sequential copilot child block ID: {@code turnId-N}. */
  public static String copilotChildBlockId(String turnId, int index) {
    return turnId + "-" + index;
  }

  /** Tool call block ID: {@code turnId-tc-N}. */
  public static String toolCallBlockId(String turnId, int index) {
    return turnId + "-tc-" + index;
  }

  /** Compacting status block ID: {@code turnId-compacting}. */
  public static String compactingBlockId(String turnId) {
    return turnId + "-compacting";
  }

  /** Model info block ID: {@code turnId-model-info}. */
  public static String modelInfoBlockId(String turnId) {
    return turnId + "-model-info";
  }

  /** Agent message block ID: {@code turnId-agent-msg-timestamp}. */
  public static String agentMessageBlockId(String turnId) {
    return turnId + "-agent-msg-" + System.currentTimeMillis();
  }

  /** Error turn block ID: {@code error-timestamp}. Not scoped to a turn (errors may be turnless). */
  public static String errorBlockId() {
    return "error-" + System.currentTimeMillis();
  }

  /** Streaming indicator block ID: {@code turnId-streaming}. */
  public static String streamingIndicatorId(String turnId) {
    return turnId + "-streaming";
  }

  /** Confirmation block ID: {@code turnId-confirm}. */
  public static String confirmationBlockId(String turnId) {
    return turnId + "-confirm";
  }

  /** Subagent block ID: {@code parentTurnId-subagent-toolCallId}. */
  public static String subagentBlockId(String parentTurnId, String toolCallId) {
    return parentTurnId + "-subagent-" + toolCallId;
  }

  /** Subagent content-area ID (where the nested subagent turn's child blocks are inserted). */
  public static String subagentContentAreaId(String parentTurnId, String toolCallId) {
    return subagentBlockId(parentTurnId, toolCallId) + "-content";
  }

  /**
   * Creates the outer turn container with header (avatar + name) and empty content div.
   */
  public String createTurnContainerHtmlBlock(String turnId, boolean isCopilot,
      String avatarDataUri, String displayName) {
    String containerId = isCopilot
        ? copilotTurnContainerId(turnId) : userTurnContainerId(turnId);
    String cssClass = isCopilot ? "turn turn-copilot" : "turn turn-user";
    String avatar = (avatarDataUri != null && !avatarDataUri.isEmpty())
        ? "<img class=\"turn-avatar\" src=\"%s\" alt=\"\"/>".formatted(escapeHtml(avatarDataUri))
        : "";
    return """
        <div id="%s" class="%s"><div class="turn-header">%s\
        <span class="turn-name">%s</span></div>\
        <div id="%s" class="turn-content"></div></div>"""
        .formatted(escapeHtml(containerId), cssClass, avatar,
            escapeHtml(displayName), escapeHtml(contentBlockId(turnId, isCopilot)));
  }

  /**
   * Creates a nested subagent block: a bordered card with a header (copilot avatar + title) and an
   * inner content area into which the subagent turn's child blocks are inserted. Mirrors the SWT
   * {@code SubagentTurnWidget} (copilot avatar + tool-call-derived title) and shares the
   * {@code subagent-message-block} CSS class for visual parity with the other message cards.
   */
  public String createSubagentBlockHtmlBlock(String blockId, String contentAreaId,
      String avatarDataUri, String title) {
    String avatar = StringUtils.isNotBlank(avatarDataUri)
        ? "<img class=\"subagent-avatar\" src=\"%s\" alt=\"\"/>".formatted(escapeHtml(avatarDataUri))
        : "";
    String titleText = escapeHtml(StringUtils.isNotBlank(title) ? title : "Subagent");
    return """
        <div id="%s" class="message-card subagent-message-block">\
        <div class="block-title">%s%s</div>\
        <div id="%s" class="subagent-content"></div></div>"""
        .formatted(escapeHtml(blockId), avatar, titleText, escapeHtml(contentAreaId));
  }

  /** Creates a collapsible thinking block (open by default during streaming, with spinner). */
  public String createThinkingHtmlBlock(String blockId, String thinkingText) {
    return """
        <div id="%s" class="thinking-block"><details open>\
        <summary><span class="thinking-spinner"></span>\
        <span class="thinking-chevron"></span>Thinking&#x2026;</summary>\
        <div class="thinking-body">%s</div></details></div>"""
        .formatted(escapeHtml(blockId), renderMarkdown(thinkingText));
  }

  /** Creates a sealed/completed thinking block (closed, no spinner, with optional title). */
  public String createSealedThinkingHtmlBlock(String blockId, String thinkingText, String title) {
    String summary = StringUtils.isNotBlank(title)
        ? renderMarkdownInline(title) : "Thinking&#x2026;";
    return """
        <div id="%s" class="thinking-block"><details>\
        <summary>%s<span class="thinking-chevron"></span>%s</summary>\
        <div class="thinking-body">%s</div></details></div>"""
        .formatted(escapeHtml(blockId), SvgIcons.get(SvgIcons.Icon.THINKING_BULB),
            summary, renderMarkdown(thinkingText));
  }

  /** Creates a restored thinking block (closed, no spinner, with optional title). */
  public String createRestoredThinkingHtmlBlock(String blockId, ThinkingBlockData data) {
    return createSealedThinkingHtmlBlock(blockId, data.getContent(), data.getTitle());
  }

  /** Creates a tool call status block from a live AgentToolCall. */
  public String createToolCallHtmlBlock(String blockId, AgentToolCall toolCall) {
    String progressMsg = toolCall.getProgressMessage();
    String progressSpan = (progressMsg != null && !progressMsg.isEmpty())
        ? " <span class=\"tool-call-progress\">%s</span>".formatted(escapeHtml(progressMsg))
        : "";
    return toolCallHtmlBlock(blockId, toolCall.getStatus(), toolCall.getName(), progressSpan);
  }

  /** Creates a tool call status block from persisted ToolCallData. */
  public String createRestoredToolCallHtmlBlock(String blockId, ToolCallData tc) {
    String progressMsg = tc.getProgressMessage();
    String progressSpan = StringUtils.isNotBlank(progressMsg)
        ? " <span class=\"tool-call-progress\">%s</span>".formatted(escapeHtml(progressMsg))
        : "";
    return toolCallHtmlBlock(blockId, tc.getStatus(), tc.getName(), progressSpan);
  }

  /**
   * Builds a tool-call status block. The {@code progressSpan} is a pre-rendered, optional
   * progress fragment (empty string when absent) so each caller keeps its own presence check.
   */
  private static String toolCallHtmlBlock(String blockId, String status, String name,
      String progressSpan) {
    return """
        <div id="%s" class="tool-call%s">%s \
        <span class="tool-call-name">%s</span>%s</div>"""
        .formatted(escapeHtml(blockId), toolCallStatusClass(status),
            toolCallIcon(status), escapeHtml(name), progressSpan);
  }

  /**
   * Returns the icon HTML for a tool call status. Running state shows animated dots;
   * completed shows a green checkmark; error shows a red cross.
   *
   * <p>Note: The "running" state is sent by the language server for in-progress tool calls.
   * Most tool calls complete quickly, so the running indicator may only flash briefly.
   */
  private static String toolCallIcon(String status) {
    if ("completed".equals(status)) {
      return """
          <span class="tool-call-icon tc-success">&#x2713;</span>""";
    } else if ("error".equals(status)) {
      return """
          <span class="tool-call-icon tc-error">&#x2717;</span>""";
    } else if ("cancelled".equals(status)) {
      return """
          <span class="tool-call-icon tc-cancelled">&#x2717;</span>""";
    }
    // running or unknown — spinning circle (same as thinking spinner)
    return """
        <span class="tool-call-icon tc-running">\
        <span class="thinking-spinner"></span></span>""";
  }

  private static String toolCallStatusClass(String status) {
    if ("completed".equals(status)) {
      return " tc-completed";
    } else if ("error".equals(status) || "cancelled".equals(status)) {
      return " tc-failed";
    }
    return "";
  }

  /** Creates a copilot response block with rendered Markdown content. */
  public String createCopilotReplyHtmlBlock(String blockId, String markdownText) {
    String renderedHtml = renderMarkdown(markdownText);
    return """
        <div id="%s" class="response">%s</div>""".formatted(escapeHtml(blockId), renderedHtml);
  }

  /** Creates a user request block with rendered Markdown content. */
  public String createUserRequestHtmlBlock(String blockId, String markdownText) {
    String renderedHtml = renderMarkdown(markdownText);
    return """
        <div id="%s" class="user-request">%s</div>""".formatted(escapeHtml(blockId), renderedHtml);
  }

  /** Creates an error message block (simple, no action buttons). */
  public String createErrorMessageHtmlBlock(String blockId, String errorMessage) {
    return createWarningMessageHtmlBlock(blockId, errorMessage, List.of());
  }

  /**
   * Creates a warning message block with an SVG warning icon, message text, and optional
   * action buttons. Used for quota-exceeded (402) errors and generic turn-level errors.
   *
   * @param blockId unique DOM element ID
   * @param message the warning/error message to display
   * @param actions quota actions to render as buttons; empty list for no buttons
   * @return self-contained HTML div block
   */
  public String createWarningMessageHtmlBlock(String blockId, String message,
      List<QuotaAction> actions) {
    String actionsBlock = "";
    if (actions != null && !actions.isEmpty()) {
      StringBuilder buttons = new StringBuilder();
      for (QuotaAction action : actions) {
        String cssClass = action.primary() ? "btn-confirm btn-primary" : "btn-confirm";
        buttons.append(createCopilotActionButtonHtml(cssClass, action.label(), "openLink",
            action.url(), action.tooltip()));
      }
      actionsBlock = "<div class=\"warning-actions\">%s</div>".formatted(buttons);
    }
    return """
        <div id="%s" class="warning-message"><div class="warning-content">%s\
        <span>%s</span></div>%s</div>"""
        .formatted(escapeHtml(blockId), SvgIcons.get(SvgIcons.Icon.WARNING),
            escapeHtml(message), actionsBlock);
  }

  /** Creates a standalone error turn block (for top-level errors). */
  public String createErrorTurnHtmlBlock(String blockId, String renderedContent) {
    return """
        <div id="%s" class="turn turn-error"><div class="turn-content">%s</div></div>"""
        .formatted(escapeHtml(blockId), renderedContent);
  }

  /** Creates a compacting status block. */
  public String createCompactingStatusHtmlBlock(String blockId) {
    return """
        <div id="%s" class="compacting-status">&#x23F3; Compacting&#x2026;</div>"""
        .formatted(escapeHtml(blockId));
  }

  /** Creates a model info footer block. */
  public String createModelInfoHtmlBlock(String blockId, String modelName,
      double billingMultiplier, String reasoningEffort) {
    String billing = (billingMultiplier > 0 && billingMultiplier != 1.0)
        ? " <span class=\"billing-multiplier\">(%sx)</span>".formatted(billingMultiplier)
        : "";
    String reasoning = "";
    if (reasoningEffort != null && !reasoningEffort.isEmpty()) {
      String capitalized = reasoningEffort.substring(0, 1).toUpperCase()
          + reasoningEffort.substring(1);
      reasoning = " <span class=\"reasoning-effort\">- %s</span>".formatted(escapeHtml(capitalized));
    }
    return """
        <div id="%s" class="model-info"><span class="model-name">%s</span>%s%s</div>"""
        .formatted(escapeHtml(blockId), escapeHtml(modelName), billing, reasoning);
  }

  /** Creates an agent message block (e.g., coding agent PR link). */
  public String createAgentMessageHtmlBlock(String blockId, String title,
      String description, String prLink) {
    String titleBlock = StringUtils.isNotBlank(title)
        ? "<div class=\"block-title\">%s%s</div>"
            .formatted(SvgIcons.get(SvgIcons.Icon.PULL_REQUEST), escapeHtml(title))
        : "";
    String descBlock = "";
    if (StringUtils.isNotBlank(description)) {
      // Match the StyledText AgentMessageWidget, which truncates the description
      // to the first 100 characters followed by an ellipsis.
      String reducedDescription = description.length() > 100
          ? description.substring(0, 100) + "..."
          : description;
      descBlock = "<p class=\"agent-message-desc\">%s</p>".formatted(escapeHtml(reducedDescription));
    }
    String actionsBlock = "";
    if (StringUtils.isNotBlank(prLink)) {
      actionsBlock = """
          <div class="agent-message-actions">\
          %s\
          %s</div>"""
          .formatted(
              createCopilotActionButtonHtml("btn-confirm btn-primary", "Open in Browser",
                  "openLink", prLink, null),
              createCopilotActionButtonHtml("btn-confirm", "Open Job List", "openJobList", "",
                  null));
    }
    return """
        <div id="%s" class="message-card agent-message">%s%s%s</div>"""
        .formatted(escapeHtml(blockId), titleBlock, descBlock, actionsBlock);
  }

  /** Creates a streaming indicator (animated dots) shown while waiting for copilot response. */
  public String createStreamingIndicatorHtmlBlock(String blockId) {
    return """
        <div id="%s" class="streaming-indicator">\
        <span class="dot"></span><span class="dot"></span><span class="dot"></span>\
        </div>""".formatted(escapeHtml(blockId));
  }

  /**
   * Creates an inline confirmation block with title, optional message, optional command panel,
   * and action buttons. Mimics the layout of the SWT {@code InvokeToolConfirmationDialog}.
   */
  @SuppressWarnings("unchecked")
  public String createConfirmationHtmlBlock(String blockId, ConfirmationContent content, Object input) {
    // Message
    String message = StringUtils.isNotBlank(content.getMessage())
        ? "<div class=\"confirmation-message\">%s</div>".formatted(escapeHtml(content.getMessage()))
        : "";

    // Command panel (extracted from input map)
    String commandPanel = "";
    String explanationPanel = "";
    if (input instanceof Map) {
      Map<String, Object> inputMap = (Map<String, Object>) input;
      Object command = inputMap.get("command");
      if (command != null) {
        commandPanel = "<div class=\"confirmation-command\">%s</div>"
            .formatted(escapeHtml(command.toString()));
      }
      Object explanation = inputMap.get("explanation");
      if (explanation != null) {
        explanationPanel = "<div class=\"confirmation-explanation\">%s</div>"
            .formatted(escapeHtml(explanation.toString()));
      }
    }

    // Action buttons: mirror the SWT split-dropdown layout. The primary accept action is
    // rendered as a button; any additional accept actions are moved into a caret-triggered
    // dropdown attached to it, and the dismiss action stays a separate button beside it.
    StringBuilder actionsHtml = new StringBuilder();
    List<ConfirmationAction> actions = content.getActions();
    if (actions != null && !actions.isEmpty()) {
      int primaryIndex = -1;
      int dismissIndex = -1;
      List<Integer> alternativeIndexes = new ArrayList<>();
      for (int i = 0; i < actions.size(); i++) {
        ConfirmationAction action = actions.get(i);
        if (!action.isAccept()) {
          dismissIndex = i;
        } else if (action.isPrimary() && primaryIndex < 0) {
          primaryIndex = i;
        } else {
          alternativeIndexes.add(i);
        }
      }
      // Fallback: if no accept action is flagged primary, promote the first accept action so
      // the confirmation card always stays actionable.
      if (primaryIndex < 0 && !alternativeIndexes.isEmpty()) {
        primaryIndex = alternativeIndexes.remove(0);
      }
      if (primaryIndex >= 0) {
        appendConfirmationSplitButton(actionsHtml, actions, primaryIndex, alternativeIndexes);
      }
      if (dismissIndex >= 0) {
        ConfirmationAction dismiss = actions.get(dismissIndex);
        actionsHtml.append("""
            <button class="btn-confirm" onclick="window.dismissToolAction()">%s</button>"""
            .formatted(escapeHtml(dismiss.getLabel())));
      }
    }

    return """
        <div id="%s" class="message-card confirmation-block">\
        <div class="block-title">%s%s</div>%s%s%s\
        <div class="confirmation-actions">%s</div></div>"""
        .formatted(escapeHtml(blockId), SvgIcons.get(SvgIcons.Icon.TERMINAL),
            escapeHtml(content.getTitle()), message, commandPanel, explanationPanel,
            actionsHtml.toString());
  }

  /**
   * Appends the primary accept action as a button. When alternative accept actions exist,
   * the button is wrapped in a split-button group with a caret that opens a dropdown menu
   * listing those alternatives; otherwise a plain primary button is emitted.
   */
  private void appendConfirmationSplitButton(StringBuilder html,
      List<ConfirmationAction> actions, int primaryIndex, List<Integer> alternativeIndexes) {
    ConfirmationAction primary = actions.get(primaryIndex);
    if (alternativeIndexes.isEmpty()) {
      html.append("""
          <button class="btn-confirm btn-primary" \
          onclick="window.acceptToolAction(%s)">%s</button>"""
          .formatted(primaryIndex, escapeHtml(primary.getLabel())));
      return;
    }
    html.append("""
        <div class="split-button">\
        <button class="btn-confirm btn-primary split-primary" \
        onclick="window.acceptToolAction(%s)">%s</button>\
        <button class="btn-confirm btn-primary split-caret" data-dropdown-toggle \
        aria-label="More actions" aria-haspopup="true" aria-expanded="false">&#9662;</button>\
        <div class="dropdown-menu" hidden>"""
        .formatted(primaryIndex, escapeHtml(primary.getLabel())));
    for (int idx : alternativeIndexes) {
      ConfirmationAction alt = actions.get(idx);
      html.append("""
          <button class="dropdown-item" onclick="window.acceptToolAction(%s)">%s</button>"""
          .formatted(idx, escapeHtml(alt.getLabel())));
    }
    html.append("</div></div>");
  }

  /** Renders Markdown to HTML and injects code block action buttons. */
  public String renderMarkdown(String markdown) {
    if (markdown == null || markdown.isEmpty()) {
      return "";
    }
    String html = htmlRenderer.render(markdownParser.parse(markdown));
    return injectCodeBlockButtons(html);
  }

  /**
   * Renders Markdown to HTML for an inline context such as a thinking block title inside a
   * {@code <summary>}. Strips a single wrapping {@code <p>...</p>} (which commonmark always emits
   * around a lone paragraph) so the result stays inline and does not break the summary layout.
   * Does not inject code block action buttons.
   */
  public String renderMarkdownInline(String markdown) {
    if (markdown == null || markdown.isEmpty()) {
      return "";
    }
    String html = htmlRenderer.render(markdownParser.parse(markdown)).strip();
    if (html.startsWith("<p>") && html.endsWith("</p>")) {
      html = html.substring("<p>".length(), html.length() - "</p>".length());
    }
    return html;
  }

  /**
   * Post-processes rendered HTML to inject Copy/Insert action buttons into code blocks.
   * Uses the platform icons loaded via {@link #setCodeBlockIcons}.
   */
  public String injectCodeBlockButtons(String html) {
    String copyImg = copyIconDataUri.isEmpty() ? ""
        : """
            <img src="%s" width="16" height="16" />""".formatted(copyIconDataUri);
    String insertImg = insertIconDataUri.isEmpty() ? ""
        : """
            <img src="%s" width="16" height="16" />""".formatted(insertIconDataUri);
    String buttons = """
        <div class="code-actions">\
        <button class="code-action-btn" data-action="copy" title="Copy to clipboard">%s</button>\
        <button class="code-action-btn" data-action="insert" title="Insert into editor">%s</button>\
        </div>""".formatted(copyImg, insertImg);
    return html.replace("</pre>", buttons + "</pre>");
  }

  private static String createCopilotActionButtonHtml(String cssClass, String label, String action,
      String param, String tooltip) {
    String titleAttr = StringUtils.isNotBlank(tooltip)
        ? " title=\"%s\"".formatted(escapeHtml(tooltip))
        : "";
    return """
        <button class="%s" data-copilot-action="%s" data-copilot-param="%s"%s>%s</button>"""
        .formatted(escapeHtml(cssClass), escapeHtml(action), escapeHtml(param), titleAttr,
            escapeHtml(label));
  }

  /** Escapes special HTML characters in the given text. Returns empty string for null. */
  public static String escapeHtml(String text) {
    if (text == null) {
      return "";
    }
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}

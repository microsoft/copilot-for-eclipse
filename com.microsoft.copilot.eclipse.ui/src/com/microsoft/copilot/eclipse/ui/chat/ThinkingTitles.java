// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microsoft.copilot.eclipse.core.lsp.protocol.GenerateThinkingTitleParams;
import com.microsoft.copilot.eclipse.core.persistence.ConversationPersistenceManager;

/**
 * Shared helpers for the "thinking" title feature used by both the browser renderer
 * ({@link BrowserConversationWidget}) and the StyledText renderer ({@link ThinkingBlock} /
 * {@link ThinkingTurnWidget}). Centralizes the standalone-bold-line title pattern, title
 * extraction, the {@link GenerateThinkingTitleParams} construction rule, and title persistence so
 * the two renderers cannot drift.
 */
public final class ThinkingTitles {

  /**
   * Matches a standalone {@code **Title**} line in thinking text: a bold span that occupies its own
   * line (start-of-text or after a newline, terminated by a newline or end-of-text). Group 1 is the
   * title text.
   */
  public static final Pattern TITLE_PATTERN =
      Pattern.compile("(?:^|\\n)\\*\\*([^*\\r\\n]+?)\\*\\*(?=\\r?\\n|$)");

  private ThinkingTitles() {
  }

  /**
   * Extracts the non-blank {@code **Title**} strings from {@code text} in document order.
   *
   * @param text the thinking text to scan; may be {@code null}
   * @return the trimmed, non-empty titles; never {@code null}
   */
  public static String[] extractTitles(String text) {
    List<String> titles = new ArrayList<>();
    if (text != null) {
      Matcher matcher = TITLE_PATTERN.matcher(text);
      while (matcher.find()) {
        String title = matcher.group(1).trim();
        if (!title.isEmpty()) {
          titles.add(title);
        }
      }
    }
    return titles.toArray(String[]::new);
  }

  /**
   * Builds the {@link GenerateThinkingTitleParams} for a title-generation request. The server
   * schema rejects null entries inside {@code extractedTitles}, so exactly one field is populated:
   * the extracted titles when present, otherwise the raw content.
   *
   * @param content the accumulated thinking content
   * @param titles the titles extracted from {@code content}; may be {@code null} or empty
   * @return params carrying either the titles or the content, never both
   */
  public static GenerateThinkingTitleParams buildTitleParams(String content, String[] titles) {
    boolean hasTitles = titles != null && titles.length > 0;
    return new GenerateThinkingTitleParams(hasTitles ? null : content, hasTitles ? titles : null);
  }

  /**
   * Persists a generated thinking-block {@code title}. No-op when {@code persistenceManager} or
   * {@code conversationId} is {@code null}.
   *
   * @param persistenceManager the persistence manager, or {@code null}
   * @param conversationId the conversation id, or {@code null}
   * @param turnId the turn id
   * @param thinkingBlockId the thinking block id
   * @param title the generated title
   */
  public static void persistTitle(ConversationPersistenceManager persistenceManager,
      String conversationId, String turnId, String thinkingBlockId, String title) {
    if (persistenceManager == null || conversationId == null) {
      return;
    }
    persistenceManager.updateThinkingBlockTitle(conversationId, turnId, thinkingBlockId, title);
  }
}

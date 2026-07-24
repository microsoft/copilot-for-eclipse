// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Normalizes user-facing error messages carried by {@link ChatProgressValue} so that both the SWT
 * ({@link ChatContentViewer}) and browser ({@link BrowserConversationWidget}) conversation widgets
 * display identical text.
 */
public final class ChatErrorMessages {

  /** Server reason string indicating the selected model is not supported. */
  private static final String REASON_MODEL_NOT_SUPPORTED = "model_not_supported";

  /**
   * Matches the trailing "| Request ID: ..." and "GitHub Request ID: ..." segments that the
   * language server appends to user-facing error messages.
   */
  private static final Pattern REQUEST_ID_SUFFIX = Pattern.compile(
      "\\s*\\|?\\s*(?:GitHub\\s+)?Request\\s+ID:\\s*\\S+\\.?", Pattern.CASE_INSENSITIVE);

  private ChatErrorMessages() {
  }

  /**
   * Resolves the display error message for a progress event: strips the trailing request-ID suffix
   * and maps the {@code model_not_supported} reason to a friendly message. Returns the original
   * message (which may be blank) when no normalization applies.
   *
   * @param value the progress event
   * @return the normalized message to display, possibly blank
   */
  public static String resolveDisplayMessage(ChatProgressValue value) {
    if (value == null) {
      return StringUtils.EMPTY;
    }
    String message = value.getErrorMessage();
    if (StringUtils.isNotEmpty(message)) {
      message = REQUEST_ID_SUFFIX.matcher(message).replaceAll(StringUtils.EMPTY).trim();
    }
    if (REASON_MODEL_NOT_SUPPORTED.equals(value.getErrorReason())) {
      message = Messages.chat_model_unsupported_message;
    }
    return message;
  }
}

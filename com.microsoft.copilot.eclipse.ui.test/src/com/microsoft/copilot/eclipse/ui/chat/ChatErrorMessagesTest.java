// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationError;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Unit tests for {@link ChatErrorMessages#resolveDisplayMessage}, which both conversation widgets
 * rely on to display identical, normalized error text.
 */
class ChatErrorMessagesTest {

  @Test
  void resolveDisplayMessage_nullValue_returnsEmpty() {
    assertEquals("", ChatErrorMessages.resolveDisplayMessage(null));
  }

  @Test
  void resolveDisplayMessage_stripsRequestIdSuffix() {
    assertEquals("Something went wrong.",
        ChatErrorMessages.resolveDisplayMessage(error("Something went wrong. | Request ID: abc-123", null)));
  }

  @Test
  void resolveDisplayMessage_stripsGitHubRequestIdSuffix() {
    assertEquals("Rate limit reached.",
        ChatErrorMessages.resolveDisplayMessage(error("Rate limit reached. GitHub Request ID: xyz.789.", null)));
  }

  @Test
  void resolveDisplayMessage_leavesPlainMessageUnchanged() {
    assertEquals("Plain error.", ChatErrorMessages.resolveDisplayMessage(error("Plain error.", null)));
  }

  @Test
  void resolveDisplayMessage_modelNotSupported_mapsToFriendlyMessage() {
    assertEquals(Messages.chat_model_unsupported_message,
        ChatErrorMessages.resolveDisplayMessage(error("raw model error", "model_not_supported")));
  }

  private static ChatProgressValue error(String message, String reason) {
    ConversationError error = new ConversationError();
    error.setMessage(message);
    error.setReason(reason);
    ChatProgressValue value = new ChatProgressValue();
    value.setConversationError(error);
    return value;
  }
}

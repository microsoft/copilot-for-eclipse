// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.ui.chat.ConversationUtils;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Handler for creating a new conversation in the chat view.
 */
public class NewConversationHandler extends CopilotHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    if (ConversationUtils.confirmEndChat()) {
      IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
      if (eventBroker != null) {
        eventBroker.post(CopilotEventConstants.TOPIC_CHAT_NEW_CONVERSATION, null);
        eventBroker.post(CopilotEventConstants.TOPIC_CHAT_HIDE_CHAT_HISTORY, null);

        // Reset the title to default
        Map<String, Object> titleData = new HashMap<>();
        titleData.put(IEventBroker.DATA, Messages.chat_topBanner_defaultChatTitle);
        eventBroker.post(CopilotEventConstants.TOPIC_CHAT_CONVERSATION_TITLE_UPDATED, titleData);
      }
    }
    return null;
  }
}

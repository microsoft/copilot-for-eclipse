package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;

/**
 * Handler for showing the chat history.
 */
public class ShowChatHistoryHandler extends CopilotHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    if (eventBroker != null) {
      eventBroker.post(CopilotEventConstants.TOPIC_CHAT_SHOW_CHAT_HISTORY, null);
    }
    return null;
  }
}

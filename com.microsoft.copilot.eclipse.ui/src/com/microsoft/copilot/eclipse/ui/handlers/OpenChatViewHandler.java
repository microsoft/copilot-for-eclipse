package com.microsoft.copilot.eclipse.ui.handlers;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.chat.ActionBar;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService;

/**
 * Handler for opening the chat view.
 */
public class OpenChatViewHandler extends CopilotHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    ChatView chatView = openChatView();
    if (chatView != null) {
      setUpParameters(event, chatView);
    }
    return null;
  }

  /**
   * Opens the chat view.
   */
  private ChatView openChatView() {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window != null) {
      IWorkbenchPage page = window.getActivePage();
      if (page != null) {
        try {
          ChatView view = (ChatView) page.showView(Constants.CHAT_VIEW_ID);
          if (view != null) {
            view.setFocus();
            return view;
          }
        } catch (PartInitException e) {
          CopilotCore.LOGGER.error("Failed to open chat view", e);
        }
        return null;
      }
    }

    return null;
  }

  /**
   * Sets up parameters for the chat view based on the execution event and forces the chat mode to "Ask".
   *
   * @param event the execution event containing parameters
   * @param chatView the chat view to set parameters on
   */
  private void setUpParameters(ExecutionEvent event, ChatView chatView) {
    String inputValue = event.getParameter(UiConstants.OPEN_CHAT_VIEW_INPUT_VALUE);
    if (StringUtils.isBlank(inputValue)) {
      return;
    }

    ChatServiceManager chatServiceManager = CopilotUi.getPlugin().getChatServiceManager();
    if (chatServiceManager == null) {
      return;
    }

    UserPreferenceService userPreferenceService = chatServiceManager.getUserPreferenceService();
    if (userPreferenceService == null) {
      return;
    }

    // Force chat mode to Ask if auto-send is enabled
    boolean autoSend = Boolean.parseBoolean(event.getParameter(UiConstants.OPEN_CHAT_VIEW_AUTO_SEND));
    if (autoSend) {
      userPreferenceService.setActiveChatMode(ChatMode.Ask.toString());
    }

    ActionBar actionBar = chatView.getActionBar();
    if (actionBar == null) {
      return;
    }

    actionBar.setInputTextViewerContent(inputValue);

    if (autoSend) {
      actionBar.handleSendMessage();
    }
  }
}

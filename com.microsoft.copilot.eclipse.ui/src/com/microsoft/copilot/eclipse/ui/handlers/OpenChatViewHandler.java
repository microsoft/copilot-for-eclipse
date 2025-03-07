package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Handler for opening the chat view.
 */
public class OpenChatViewHandler extends CopilotHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    openMyView();
    return null;
  }

  /**
   * Opens the chat view.
   */
  public void openMyView() {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window != null) {
      IWorkbenchPage page = window.getActivePage();
      if (page != null) {
        try {
          IViewPart view = page.showView(Constants.CHAT_VIEW_ID);
        } catch (PartInitException e) {
          CopilotCore.LOGGER.error("Failed to open chat view", e);
        }
      }
    }
  }
}

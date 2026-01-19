package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationMode.HandOff;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatFontService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;

/**
 * Widget that renders a handoff button for mode switching.
 */
public class HandoffButtonWidget extends Composite {
  private Label lblButtonText;
  private HandOff handoff;
  private ChatServiceManager chatServiceManager;
  private ChatFontService chatFontService;
  private ActionBar actionBar;

  /**
   * Creates a new HandoffButtonWidget.
   *
   * @param parent the parent composite
   * @param handoff the handoff configuration
   * @param chatServiceManager the chat service manager
   * @param chatFontService the chat font service for font updates
   * @param actionBar the action bar
   */
  public HandoffButtonWidget(Composite parent, HandOff handoff, ChatServiceManager chatServiceManager,
      ChatFontService chatFontService, ActionBar actionBar) {
    super(parent, SWT.BORDER);
    this.handoff = handoff;
    this.chatServiceManager = chatServiceManager;
    this.chatFontService = chatFontService;
    this.actionBar = actionBar;

    createWidget();
  }

  private void createWidget() {
    // Use GridLayout similar to AddContextButton
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 4;
    layout.marginHeight = 2;
    this.setLayout(layout);

    lblButtonText = new Label(this, SWT.NONE);
    lblButtonText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    lblButtonText.setText(handoff.getLabel());

    // Register for font updates
    if (chatFontService != null) {
      chatFontService.registerControl(lblButtonText);
    }

    MouseAdapter clickListener = new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        handleHandoffClick();
      }
    };

    // Add mouse listener to both the composite and label
    this.addMouseListener(clickListener);
    lblButtonText.addMouseListener(clickListener);
    this.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
  }

  private void handleHandoffClick() {
    try {
      String targetModeId = handoff.getAgent();
      String prompt = handoff.getPrompt();
      Boolean shouldSend = handoff.getSend();

      // Switch to target mode
      chatServiceManager.getUserPreferenceService().setActiveChatMode(targetModeId);

      // Populate input box with prompt
      if (prompt != null && !prompt.isEmpty()) {
        actionBar.setInputTextViewerContent(prompt);
      }

      // Auto-send if specified
      if (shouldSend != null && shouldSend) {
        actionBar.handleSendMessage();
      }
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Handoff action failed", e);
      MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
      messageBox.setText("Handoff Failed");
      messageBox.setMessage("Failed to switch to " + handoff.getAgent() + ": " + e.getMessage());
      messageBox.open();
    }
  }
}
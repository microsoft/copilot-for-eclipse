// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatFontService;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Widget that renders a handoff button for mode switching.
 */
public class HandoffButtonWidget extends Composite {
  private Label lblButtonText;
  private HandOff handoff;
  private ChatFontService chatFontService;

  /**
   * Creates a new HandoffButtonWidget.
   *
   * @param parent the parent composite
   * @param handoff the handoff configuration
   * @param chatFontService the chat font service for font updates
   */
  public HandoffButtonWidget(Composite parent, HandOff handoff, ChatFontService chatFontService) {
    super(parent, SWT.BORDER);
    this.handoff = handoff;
    this.chatFontService = chatFontService;

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
      Map<String, Object> parameters = new HashMap<>();
      String targetModeId = handoff.getAgent();
      if (StringUtils.isNotBlank(targetModeId)) {
        parameters.put(UiConstants.OPEN_CHAT_VIEW_MODE, targetModeId);
      }

      String prompt = handoff.getPrompt();
      if (StringUtils.isNotBlank(prompt)) {
        parameters.put(UiConstants.OPEN_CHAT_VIEW_INPUT_VALUE, prompt);
      }

      Boolean shouldSend = handoff.getSend();
      if (shouldSend != null) {
        parameters.put(UiConstants.OPEN_CHAT_VIEW_AUTO_SEND, shouldSend.toString());
      }

      UiUtils.executeCommandWithParameters(UiConstants.OPEN_CHAT_VIEW_COMMAND_ID,
          parameters.isEmpty() ? null : parameters);
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Handoff action failed", e);
      MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
      messageBox.setText("Handoff Failed");
      messageBox.setMessage("Failed to switch to " + handoff.getAgent() + ": " + e.getMessage());
      messageBox.open();
    }
  }
}

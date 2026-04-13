// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.core.chat.BuiltInChatMode;
import com.microsoft.copilot.eclipse.core.chat.BuiltInChatModeManager;
import com.microsoft.copilot.eclipse.core.chat.CustomChatMode;
import com.microsoft.copilot.eclipse.core.chat.CustomChatModeManager;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationMode.HandOff;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatFontService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;

/**
 * Container for displaying handoff buttons.
 */
public class HandoffContainer extends Composite {
  private ChatServiceManager chatServiceManager;
  private ChatFontService chatFontService;
  private ActionBar actionBar;
  private ChatView chatView;
  private List<HandoffButtonWidget> handoffButtons = new ArrayList<>();

  /**
   * Creates a new HandoffContainer.
   *
   * @param parent the parent composite
   * @param chatServiceManager the chat service manager
   * @param actionBar the action bar for handling mode switches
   * @param chatView the chat view for scrolling operations
   */
  public HandoffContainer(Composite parent, ChatServiceManager chatServiceManager, ActionBar actionBar,
      ChatView chatView) {
    super(parent, SWT.NONE);
    this.chatServiceManager = chatServiceManager;
    this.chatFontService = chatServiceManager.getChatFontService();
    this.actionBar = actionBar;
    this.chatView = chatView;

    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 10;
    layout.marginHeight = 3;
    layout.verticalSpacing = 5;
    this.setLayout(layout);
    this.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

    // Initially hidden
    this.setVisible(false);
    ((GridData) this.getLayoutData()).exclude = true;
  }

  /**
   * Hide the handoff container and update layout.
   */
  public void hide() {
    this.setVisible(false);
    GridData gd = (GridData) this.getLayoutData();
    if (gd != null) {
      gd.exclude = true;
    }
    requestLayout();
  }

  /**
   * Show handoff buttons based on the current mode and update their content.
   */
  public void show() {
    // Clear existing buttons
    clearHandoffs();

    // Get current mode and its handoffs
    List<HandOff> handoffs = getHandoffsForCurrentMode();

    if (handoffs.isEmpty()) {
      this.setVisible(false);
      ((GridData) this.getLayoutData()).exclude = true;
    } else {
      // Get current mode name for label
      String activeModeId = chatServiceManager.getUserPreferenceService().getActiveModeNameOrId();
      String modeName = activeModeId;

      // Try to get display name from built-in modes
      BuiltInChatMode builtInMode = BuiltInChatModeManager.INSTANCE.getBuiltInModeByDisplayName(activeModeId);
      if (builtInMode != null) {
        modeName = builtInMode.getDisplayName();
      } else {
        // Try custom modes
        CustomChatMode customMode = CustomChatModeManager.INSTANCE.getCustomModeById(activeModeId);
        if (customMode != null && customMode.getDisplayName() != null) {
          modeName = customMode.getDisplayName();
        }
      }

      // Add label in first row with secondary text styling
      Label handoffLabel = new Label(this, SWT.NONE);
      handoffLabel.setText(MessageFormat.format(Messages.handoffContainer_proceedFrom, modeName.toUpperCase()));
      handoffLabel.setData(CssConstants.CSS_CLASS_NAME_KEY, "text-secondary");
      GridData labelData = new GridData(SWT.FILL, SWT.CENTER, true, false);
      labelData.horizontalIndent = 0;
      handoffLabel.setLayoutData(labelData);

      // Register label for font updates
      if (chatFontService != null) {
        chatFontService.registerControl(handoffLabel);
      }

      // Create buttons container with RowLayout for horizontal wrapping
      RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
      rowLayout.wrap = true;
      rowLayout.marginWidth = 0;
      rowLayout.marginHeight = 0;
      rowLayout.spacing = 5;
      rowLayout.fill = false;
      rowLayout.marginLeft = 0;

      Composite buttonsContainer = new Composite(this, SWT.NONE);
      buttonsContainer.setLayout(rowLayout);
      buttonsContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

      // Create handoff buttons in the buttons container
      for (HandOff handoff : handoffs) {
        HandoffButtonWidget button = new HandoffButtonWidget(buttonsContainer, handoff, chatFontService);
        handoffButtons.add(button);
      }

      this.setVisible(true);
      ((GridData) this.getLayoutData()).exclude = false;
    }

    requestLayout();

    // Scroll to bottom only when handoffs are present
    if (chatView != null && !handoffs.isEmpty()) {
      chatView.scrollContentToBottom();
    }
  }

  private List<HandOff> getHandoffsForCurrentMode() {
    List<HandOff> handoffs = new ArrayList<>();

    // Check built-in mode
    String activeModeId = chatServiceManager.getUserPreferenceService().getActiveModeNameOrId();
    BuiltInChatMode builtInMode = BuiltInChatModeManager.INSTANCE.getBuiltInModeByDisplayName(activeModeId);
    if (builtInMode != null && builtInMode.getHandOffs() != null) {
      handoffs.addAll(builtInMode.getHandOffs());
    }

    CustomChatMode customMode = CustomChatModeManager.INSTANCE.getCustomModeById(activeModeId);
    if (customMode != null && customMode.getHandOffs() != null) {
      handoffs.addAll(customMode.getHandOffs());
    }

    return handoffs;
  }

  private void clearHandoffs() {
    for (HandoffButtonWidget button : handoffButtons) {
      button.dispose();
    }
    handoffButtons.clear();

    // Clear all children
    for (Control child : this.getChildren()) {
      child.dispose();
    }
  }
}
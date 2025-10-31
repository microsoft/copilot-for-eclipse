package com.microsoft.copilot.eclipse.ui.chat;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.dialogs.McpRegistryDialog;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.preferences.ByokPreferencePage;
import com.microsoft.copilot.eclipse.ui.preferences.ChatPreferencesPage;
import com.microsoft.copilot.eclipse.ui.preferences.CompletionsPreferencesPage;
import com.microsoft.copilot.eclipse.ui.preferences.CopilotPreferencesPage;
import com.microsoft.copilot.eclipse.ui.preferences.CustomInstructionPreferencePage;
import com.microsoft.copilot.eclipse.ui.preferences.GeneralPreferencesPage;
import com.microsoft.copilot.eclipse.ui.preferences.McpPreferencePage;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.AccessibilityUtils;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A custom widget that displays the top banner.
 */
public class TopBanner extends Composite {
  private Composite cmpActionArea;
  private Button btnNewConversation;
  private Image newChatIcon;
  private CLabel chatTitle;
  private LinkedHashSet<NewConversationListener> newConversationListeners = new LinkedHashSet<>();
  private Button chatHistoryButton;
  private Image chatHistoryIcon;
  private Button openPreferenceButton;
  private Image openPreferenceIcon;
  private Button mcpRegistryButton;
  private Image mcpRegistryIcon;
  private IEventBroker eventBroker;

  /**
   * Create the widget.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public TopBanner(Composite parent, int style) {
    super(parent, style);
    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);

    GridLayout gl = new GridLayout(2, false);
    gl.marginWidth = 0;
    gl.marginHeight = 4;
    gl.horizontalSpacing = 0;
    setLayout(gl);
    GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
    layoutData.minimumHeight = 40;
    setLayoutData(layoutData);
    setData(CssConstants.CSS_ID_KEY, "chat-top-banner");

    this.chatTitle = new CLabel(this, SWT.NONE);
    this.chatTitle.setText(Messages.chat_topBanner_defaultChatTitle);

    GridData labelGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    labelGridData.horizontalIndent = 10;
    this.chatTitle.setLayoutData(labelGridData);

    this.cmpActionArea = new Composite(this, SWT.NONE);
    GridLayout glGroupButton = new GridLayout(PlatformUtils.isNightly() ? 4 : 3, false);
    glGroupButton.marginWidth = 0;
    glGroupButton.marginHeight = 0;
    this.cmpActionArea.setLayout(glGroupButton);
    this.cmpActionArea.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));

    if (PlatformUtils.isNightly()) {
      this.mcpRegistryIcon = UiUtils.buildImageFromPngPath("/icons/chat/mcp_registry.png");
      this.mcpRegistryButton = UiUtils.createIconButton(this.cmpActionArea, SWT.PUSH | SWT.FLAT);
      this.mcpRegistryButton.setImage(this.mcpRegistryIcon);
      this.mcpRegistryButton.setToolTipText(Messages.chat_topBanner_mcpRegistry_Tooltip);
      this.mcpRegistryButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          McpRegistryDialog dialog = new McpRegistryDialog(getShell());
          dialog.open();
        }
      });
      AccessibilityUtils.addAccessibilityNameForUiComponent(this.mcpRegistryButton,
          Messages.chat_topBanner_mcpRegistry_Tooltip);
    }

    this.newChatIcon = UiUtils.buildImageFromPngPath("/icons/chat/new_chat.png");
    this.btnNewConversation = UiUtils.createIconButton(this.cmpActionArea, SWT.PUSH | SWT.FLAT);
    this.btnNewConversation.setImage(this.newChatIcon);
    this.btnNewConversation.setToolTipText(Messages.chat_topBanner_newConversationButton_Tooltip);
    this.btnNewConversation.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (ConversationUtils.confirmNewChat()) { // use shared utility
          notifyNewConversationListeners();
          updateTitle(Messages.chat_topBanner_defaultChatTitle);
          if (eventBroker != null) {
            eventBroker.post(CopilotEventConstants.TOPIC_CHAT_HIDE_CHAT_HISTORY, null);
          }
        }
      }
    });
    AccessibilityUtils.addAccessibilityNameForUiComponent(this.btnNewConversation,
        Messages.chat_topBanner_newConversationButton_Tooltip);

    this.chatHistoryIcon = UiUtils.buildImageFromPngPath("/icons/chat/chat_history.png");
    this.chatHistoryButton = UiUtils.createIconButton(this.cmpActionArea, SWT.PUSH | SWT.FLAT);
    this.chatHistoryButton.setImage(this.chatHistoryIcon);
    this.chatHistoryButton.setToolTipText(Messages.chat_topBanner_chatHistoryButton_Tooltip);
    this.chatHistoryButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (eventBroker != null) {
          eventBroker.post(CopilotEventConstants.TOPIC_CHAT_SHOW_CHAT_HISTORY, null);
        }
      }
    });
    AccessibilityUtils.addAccessibilityNameForUiComponent(this.chatHistoryButton,
        Messages.chat_topBanner_chatHistoryButton_Tooltip);

    this.openPreferenceIcon = UiUtils.buildImageFromPngPath("/icons/edit_preferences.png");
    this.openPreferenceButton = UiUtils.createIconButton(this.cmpActionArea, SWT.PUSH | SWT.FLAT);
    this.openPreferenceButton.setImage(this.openPreferenceIcon);
    this.openPreferenceButton.setToolTipText(Messages.menu_editPreferences);
    this.openPreferenceButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("com.microsoft.copilot.eclipse.commands.openPreferences.activePageId",
            CopilotPreferencesPage.ID);

        parameters.put("com.microsoft.copilot.eclipse.commands.openPreferences.pageIds",
            String.join(",", CopilotPreferencesPage.ID, GeneralPreferencesPage.ID, ChatPreferencesPage.ID,
                CompletionsPreferencesPage.ID, CustomInstructionPreferencePage.ID, McpPreferencePage.ID,
                ByokPreferencePage.ID));

        UiUtils.executeCommandWithParameters("com.microsoft.copilot.eclipse.commands.openPreferences", parameters);
      }
    });
    AccessibilityUtils.addAccessibilityNameForUiComponent(this.openPreferenceButton, Messages.menu_editPreferences);

    this.addPaintListener(new PaintListener() {
      @Override
      public void paintControl(PaintEvent e) {
        GC gc = e.gc;
        int borderWidth = 1;
        Color borderColor = CssConstants.getTopBannerBorderColor(getDisplay());
        gc.setForeground(borderColor);
        gc.setLineWidth(borderWidth);
        Rectangle bounds = parent.getClientArea();
        gc.drawLine(bounds.x, getBounds().height - 1, bounds.width, getBounds().height - 1);
      }
    });

    // Add dispose listener to clean up icons
    this.addDisposeListener(e -> {
      if (this.newConversationListeners != null) {
        this.newConversationListeners.clear();
      }
      if (this.newChatIcon != null && !this.newChatIcon.isDisposed()) {
        this.newChatIcon.dispose();
      }
      if (this.chatHistoryIcon != null && !this.chatHistoryIcon.isDisposed()) {
        this.chatHistoryIcon.dispose();
      }
      if (this.openPreferenceIcon != null && !this.openPreferenceIcon.isDisposed()) {
        this.openPreferenceIcon.dispose();
      }
      if (this.mcpRegistryIcon != null && !this.mcpRegistryIcon.isDisposed()) {
        this.mcpRegistryIcon.dispose();
      }
    });
  }

  /**
   * Update the title of the chat.
   *
   * @param title the new title
   */
  public void updateTitle(String title) {
    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      if (this.chatTitle == null || this.chatTitle.isDisposed()) {
        return;
      }
      if (StringUtils.isBlank(title)) {
        this.chatTitle.setText(Messages.chat_topBanner_defaultChatTitle);
      } else {
        this.chatTitle.setText(title);
      }
      this.chatTitle.requestLayout();
    }, this.chatTitle);
  }

  /**
   * Get the current chat title text.
   *
   * @return the current chat title text, or null if chatTitle is not available
   */
  public String getChatTitle() {
    if (chatTitle != null && !chatTitle.isDisposed()) {
      return chatTitle.getText();
    }
    return null;
  }

  /**
   * Add a new conversation listener.
   *
   * @param listener the listener
   */
  public void registerNewConversationListener(NewConversationListener listener) {
    this.newConversationListeners.add(listener);
  }

  /**
   * Remove a new conversation listener.
   *
   * @param listener the listener
   */
  public void unregisterNewConversationListener(NewConversationListener listener) {
    this.newConversationListeners.remove(listener);
  }

  /**
   * Notify new conversation listeners.
   */
  public void notifyNewConversationListeners() {
    for (NewConversationListener listener : this.newConversationListeners) {
      listener.onNewConversation();
    }
    IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    eventBroker.post(CopilotEventConstants.TOPIC_CHAT_NEW_CONVERSATION, null);
  }
}
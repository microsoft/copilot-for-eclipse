package com.microsoft.copilot.eclipse.ui.chat;

import java.util.LinkedHashSet;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * A custom widget that displays the top banner.
 */
public class TopBanner extends Composite {
  private CLabel chatTitle;
  private LinkedHashSet<NewConversationListener> newConversationListeners = new LinkedHashSet<>();
  private IEventBroker eventBroker;
  private EventHandler newConversationEventHandler;

  /**
   * Create the widget.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public TopBanner(Composite parent, int style) {
    super(parent, style);
    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    this.newConversationEventHandler = new EventHandler() {
      @Override
      public void handleEvent(Event event) {
        if (ConversationUtils.confirmEndChat()) {
          notifyNewConversationListeners();
          updateTitle(Messages.chat_topBanner_defaultChatTitle);
        }
      }
    };
    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_HIDE_CHAT_HISTORY, newConversationEventHandler);

    GridLayout gl = new GridLayout(1, false);
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
    registerControlForFontUpdates(this.chatTitle);

    GridData labelGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    labelGridData.horizontalIndent = 10;
    this.chatTitle.setLayoutData(labelGridData);

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

    // Add dispose listener to clean up listeners
    this.addDisposeListener(e -> {
      if (this.eventBroker != null && this.newConversationEventHandler != null) {
        this.eventBroker.unsubscribe(this.newConversationEventHandler);
      }
      if (this.newConversationListeners != null) {
        this.newConversationListeners.clear();
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

  /**
   * Registers a control for chat font updates via the centralized ChatFontService.
   *
   * @param control the control to register
   */
  private void registerControlForFontUpdates(org.eclipse.swt.widgets.Control control) {
    var chatServiceManager = CopilotUi.getPlugin().getChatServiceManager();
    if (chatServiceManager != null) {
      chatServiceManager.getChatFontService().registerControl(control);
    }
  }
}
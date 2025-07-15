package com.microsoft.copilot.eclipse.ui.chat;

import java.util.LinkedHashSet;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A custom widget that displays the top banner.
 */
public class TopBanner extends Composite {
  private Composite cmpActionArea;
  private Button btnNewConversation;
  private Image newChatIcon;
  private LinkedHashSet<NewConversationListener> newConversationListeners = new LinkedHashSet<>();

  /**
   * Create the widget.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public TopBanner(Composite parent, int style) {
    super(parent, style);
    GridLayout gl = new GridLayout(2, false);
    gl.marginWidth = 0;
    gl.marginHeight = 0;
    // Keep this to align with previous ChatView layout setting, Will remove it when adding ChatTitle.
    gl.marginTop = 10;
    setLayout(gl);
    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    this.cmpActionArea = new Composite(this, SWT.NONE);
    GridLayout glGroupButton = new GridLayout(3, false);
    glGroupButton.marginWidth = 0;
    glGroupButton.marginHeight = 0;
    this.cmpActionArea.setLayout(glGroupButton);
    this.cmpActionArea.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));

    this.newChatIcon = UiUtils.buildImageFromPngPath("/icons/chat/new_chat.png");
    this.btnNewConversation = UiUtils.createIconButton(this.cmpActionArea, SWT.PUSH | SWT.FLAT);
    this.btnNewConversation.setImage(this.newChatIcon);
    this.btnNewConversation.setToolTipText(Messages.chat_topBanner_newConversationButton_Tooltip);
    this.btnNewConversation.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        notifyNewConversationListeners();
      }
    });
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

  @Override
  public void dispose() {
    super.dispose();
    if (this.newConversationListeners != null) {
      this.newConversationListeners.clear();
    }
    if (this.newChatIcon != null) {
      this.newChatIcon.dispose();
    }
  }
}
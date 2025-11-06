package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.ui.chat.services.AvatarService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * A custom widget that displays a turn for the copilot.
 */
public class CopilotTurnWidget extends BaseTurnWidget {

  /**
   * Create the widget.
   */
  public CopilotTurnWidget(Composite parent, int style, ChatServiceManager serviceManager, String turnId) {
    super(parent, style, serviceManager, turnId, true, null);
  }

  @Override
  protected Image getAvatar(AvatarService avatarService) {
    return avatarService.getAvatarForCopilot();
  }

  @Override
  protected String getRoleName() {
    return Messages.chat_turnWidget_copilot;
  }

  @Override
  protected Label createAvatarLabel(Composite parent) {
    return new Label(parent, SWT.NONE);
  }

  @Override
  protected void createTextBlock() {
    this.currentTextBlock = new ChatMarkupViewer(this, SWT.MULTI | SWT.WRAP);
    StyledText styledText = this.currentTextBlock.getTextWidget();
    styledText.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
    styledText.setEditable(false);
  }
}

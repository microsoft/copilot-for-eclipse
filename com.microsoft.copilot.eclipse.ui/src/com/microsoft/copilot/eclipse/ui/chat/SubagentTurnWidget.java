package com.microsoft.copilot.eclipse.ui.chat;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentToolCall;
import com.microsoft.copilot.eclipse.ui.chat.services.AvatarService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;

/**
 * A turn widget for displaying subagent messages within a SubagentMessageBlock.
 * This widget doesn't show an avatar or role name, only the message content.
 */
public class SubagentTurnWidget extends BaseTurnWidget {

  /**
   * Create the widget.
   */
  public SubagentTurnWidget(Composite parent, int style, ChatServiceManager serviceManager, String turnId,
      AgentToolCall toolCall) {
    super(parent, style, serviceManager, turnId + "_subagent", true,
        getToolCallRoleName(toolCall));
  }

  /**
   * Extract role name from tool call.
   */
  private static String getToolCallRoleName(AgentToolCall toolCall) {
    if (toolCall != null) {
      if (StringUtils.isNotEmpty(toolCall.getProgressMessage())) {
        return toolCall.getProgressMessage();
      }
      if (StringUtils.isNotEmpty(toolCall.getName())) {
        return toolCall.getName();
      }
    }
    return "Subagent";
  }

  @Override
  protected Image getAvatar(AvatarService avatarService) {
    // Subagent uses the copilot avatar
    return avatarService.getAvatarForCopilot();
  }

  @Override
  protected String getRoleName() {
    // This is overridden by the constructor parameter
    return "Subagent";
  }

  @Override
  protected Label createAvatarLabel(Composite parent) {
    // Create a label for the avatar
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

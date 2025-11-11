package com.microsoft.copilot.eclipse.ui.chat;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.ui.chat.services.AvatarService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.ModelUtils;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

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

  /**
   * Render the model information for this Copilot turn. This method should be called after receiving ChatTurnResult or
   * ChatCreateResult.
   *
   * @param modelName the name of the model used
   * @param billingMultiplier the billing multiplier for the model
   */
  public void renderModelInfo(String modelName, double billingMultiplier) {
    if (modelName != null && !modelName.isEmpty()) {
      SwtUtils.invokeOnDisplayThreadAsync(() -> {
        if (footer == null || footer.isDisposed()) {
          createFooter();
        }
        if (StringUtils.isNotBlank(modelName)) {
          Label modelInfoLabel = new Label(footer, SWT.NONE);
          String formattedMultiplier = ModelUtils.formatBillingMultiplier(billingMultiplier);
          String displayText = String.format("%s - %s", modelName, formattedMultiplier);
          modelInfoLabel.setText(displayText);
          GridData labelGridData = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
          modelInfoLabel.setLayoutData(labelGridData);
          modelInfoLabel.setData(CssConstants.CSS_CLASS_NAME_KEY, "model-info-label");

          footer.requestLayout();
        }
      }, this);
    }
  }

  @Override
  protected void createFooter() {
    footer = new Composite(this, SWT.NONE);
    footer.setLayout(new GridLayout(1, false));
    footer.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
  }

}

package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.WrapLabel;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Widget to display a message when the user has no quota.
 */
public class WarnWidget extends Composite {
  private Image warnImage;
  private int buttonLeftMargin;
  private static final int MESSAGE_LEFT_MARGIN = 5;

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param message the message to display
   */
  public WarnWidget(Composite parent, int style, String message, int code) {
    super(parent, style | SWT.BORDER);
    setLayout(new GridLayout(1, true));
    setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    buildWarnLabelWithIcon(message);

    if (code == 402) {
      // Render the button based on the error code. See:
      // https://github.com/microsoft/copilot-client/blob/77f8f28e1a1e2efb51b6f92649bd9d085b8b64f5/lib/src/conversation/fetchPostProcessor.ts#L232-L248
      buildUpdatePlanButton();
    }
    parent.layout();
  }

  private void buildWarnLabelWithIcon(String message) {
    Composite composite = new Composite(this, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

    Label icon = new Label(composite, SWT.CENTER);
    warnImage = UiUtils.buildImageFromPngPath("/icons/message_warning.png");
    icon.setImage(warnImage);

    WrapLabel label = new WrapLabel(composite, SWT.LEFT);
    label.setText(message);
    label.setHorizontalIndent(MESSAGE_LEFT_MARGIN); // Indent the text to the right of the icon

    composite.layout();
    buttonLeftMargin += label.getLocation().x - icon.getLocation().x;
  }

  private void buildUpdatePlanButton() {
    Composite composite = new Composite(this, SWT.NONE);
    RowLayout layout = new RowLayout();
    layout.marginLeft = this.buttonLeftMargin; // Add margin to the left of the button to align with the message
    composite.setLayout(layout);

    Button updatePlanButton = new Button(composite, SWT.PUSH);
    updatePlanButton.setText(Messages.chat_noQuotaView_updatePlanButton);
    updatePlanButton.setToolTipText(Messages.chat_noQuotaView_updatePlanButton_Tooltip);
    updatePlanButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
        UiUtils.openLink(Messages.chat_noQuotaView_updatePlanLink);
      }
    });
  }

  @Override
  public void dispose() {
    super.dispose();
    if (warnImage != null) {
      warnImage.dispose();
    }
  }
}

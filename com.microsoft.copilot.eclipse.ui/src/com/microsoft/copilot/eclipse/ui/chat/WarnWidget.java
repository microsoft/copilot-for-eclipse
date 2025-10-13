package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Widget to display a message when the user has no quota.
 */
public class WarnWidget extends Composite {
  private int buttonLeftMargin;

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

    // Render the button based on the error code. See:
    // https://github.com/microsoft/copilot-client/blob/77f8f28e1a1e2efb51b6f92649bd9d085b8b64f5/lib/src/conversation/fetchPostProcessor.ts#L232-L248
    if (code == 402) {
      // TODO: This is just a temporary solution. We need compose a dialog to support any warn message once issue
      // https://github.com/microsoft/copilot-client/issues/405 is resolved.
      if (message.toLowerCase().contains("upgrade to copilot pro (30-day free trial)")) {
        buildUpdatePlanButton();
      }
    }
    parent.layout();
  }

  private void buildWarnLabelWithIcon(String message) {
    Composite composite = new Composite(this, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    composite.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, true, false));

    Label iconLabel = new Label(composite, SWT.TOP);
    Image warnImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
    iconLabel.setImage(warnImage);
    GridData iconGd = new GridData(SWT.LEFT, SWT.TOP, false, false);
    iconGd.verticalIndent = 4;
    iconLabel.setLayoutData(iconGd);
    buttonLeftMargin = warnImage.getBounds().width + iconGd.verticalIndent;
    iconLabel.addDisposeListener(e -> {
      if (warnImage != null && !warnImage.isDisposed()) {
        warnImage.dispose();
      }
    });

    ChatMarkupViewer textLabel = new ChatMarkupViewer(composite, SWT.LEFT | SWT.WRAP);
    StyledText styledText = textLabel.getTextWidget();
    styledText.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
    styledText.setEditable(false);
    textLabel.setMarkup(message);

    requestLayout();
  }

  private void buildUpdatePlanButton() {
    Composite composite = new Composite(this, SWT.NONE);
    RowLayout layout = new RowLayout(SWT.HORIZONTAL);
    layout.marginLeft = this.buttonLeftMargin; // Add margin to the left of the buttons to align with the message
    layout.spacing = 10;
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
    updatePlanButton.setData(CssConstants.CSS_CLASS_NAME_KEY, "btn-primary");
  }
}

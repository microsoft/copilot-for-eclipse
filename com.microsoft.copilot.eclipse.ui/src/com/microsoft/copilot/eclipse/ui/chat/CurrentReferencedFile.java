package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A special {@link ReferencedFile} that represents the current file being edited. This is used to represent the current
 * file in the chat view, when no file is selected.
 */
public class CurrentReferencedFile extends ReferencedFile {
  private static Image visibleImage = UiUtils.buildImageDescriptorFromPngPath("/icons/chat/eye.png").createImage();
  private static Image invisibleImage = UiUtils.buildImageDescriptorFromPngPath("/icons/chat/eye_closed.png")
      .createImage();

  private Label descriptionLabel;

  /**
   * Creates a new CurrentReferencedFile.
   */
  public CurrentReferencedFile(Composite parent) {
    super(parent, null);

    // change to 4 col layout
    GridLayout layout = new GridLayout(4, false);
    layout.marginWidth = 4;
    layout.marginHeight = 2;
    setLayout(layout);

    descriptionLabel = new Label(this, SWT.NONE);
    descriptionLabel.setText(Messages.chat_currentReferencedFile_description);
    descriptionLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
    descriptionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    descriptionLabel.moveAbove(lblClose);
    UiUtils.useParentBackground(descriptionLabel);
  }

  /**
   * update the visible icon.
   */
  public void updateCloseClickBtnIcon(boolean isCurrentFileVisible) {
    if (isCurrentFileVisible) {
      setCloseClickBtnIcon(visibleImage);
    } else {
      setCloseClickBtnIcon(invisibleImage);
    }
  }

  @Override
  public void setFile(IFile file) {
    super.setFile(file);
  }

}

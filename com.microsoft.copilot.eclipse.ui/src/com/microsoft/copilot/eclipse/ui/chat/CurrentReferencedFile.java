package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ReferencedFileService;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
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
    // No need to get supportVision here, as currentFile will not be an image file.
    super(parent, null, false);

    // change to 4 col layout
    GridLayout layout = new GridLayout(4, false);
    layout.marginWidth = 4;
    layout.marginHeight = 2;
    setLayout(layout);

    descriptionLabel = new Label(this, SWT.NONE);
    descriptionLabel.setText(Messages.chat_currentReferencedFile_description);
    descriptionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    descriptionLabel.moveAbove(lblClose);
    descriptionLabel.setData(CssConstants.CSS_CLASS_NAME_KEY, "text-secondary");
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
  public void setFile(IResource file) {
    super.setFile(file);
  }

  @Override
  protected void setCloseClickAction() {
    lblClose.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        ReferencedFileService referencedFileService = CopilotUi.getPlugin().getChatServiceManager()
            .getReferencedFileService();
        referencedFileService.toggleIsVisible();
      }
    });
  }

}

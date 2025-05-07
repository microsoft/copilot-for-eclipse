package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A widget that displays two buttons.
 */
public class ReferencedFile extends Composite {
  private Label lblfileIcon;
  private Label lblFileName;
  protected Label lblClose;
  private Image lblImage;
  private IFile file;

  // make it static to avoid creating multiple instances of the same label provider
  protected static WorkbenchLabelProvider labelProvider = new WorkbenchLabelProvider();

  /**
   * Creates a new TwinButton.
   */
  public ReferencedFile(Composite parent, IFile file) {
    super(parent, SWT.BORDER);
    GridLayout layout = new GridLayout(3, false);
    layout.marginWidth = 4;
    layout.marginHeight = 2;
    setLayout(layout);

    lblfileIcon = new Label(this, SWT.NONE);
    lblfileIcon.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    UiUtils.useParentBackground(this.lblfileIcon);
    lblFileName = new Label(this, SWT.NONE);
    lblFileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    UiUtils.useParentBackground(this.lblFileName);

    MouseAdapter mouseAdapter = new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        UiUtils.openInEditor(ReferencedFile.this.file);
      }
    };
    lblFileName.addMouseListener(mouseAdapter);
    lblfileIcon.addMouseListener(mouseAdapter);
    this.addMouseListener(mouseAdapter);

    lblClose = new Label(this, SWT.NONE);
    lblClose.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    lblImage = UiUtils.buildImageFromPngPath("/icons/close.png");
    UiUtils.useParentBackground(this.lblClose);

    setFile(file);
    UiUtils.useParentBackground(this);
    this.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
  }

  /**
   * Set the mouse click adapter for the close button.
   */
  public void setCloseClickAction(MouseAdapter adapter) {
    lblClose.addMouseListener(adapter);
  }

  /**
   * Set the icon for the close button.
   */
  protected void setCloseClickBtnIcon(Image image) {
    lblClose.setImage(image);
  }

  public IFile getFile() {
    return file;
  }

  /**
   * Set the file for this widget.
   */
  protected void setFile(@Nullable IFile file) {
    this.file = file;
    RowData layoutData = getLayoutData() == null ? new RowData() : (RowData) getLayoutData();
    if (file == null) {
      // Hide the file name label
      layoutData.exclude = true;
      layoutData.width = 0;
      layoutData.height = 0;
      setVisible(false);
    } else {
      lblFileName.setText(file.getName());
      Image image = labelProvider.getImage(file);
      if (image != null) {
        lblfileIcon.setImage(image);
      }
      lblClose.setImage(lblImage);

      // Show the file name label
      layoutData.exclude = false;
      layoutData.width = SWT.DEFAULT;
      layoutData.height = SWT.DEFAULT;
      setVisible(true);
    }
    setLayoutData(layoutData);
    ChatView chatView = UiUtils.getView(Constants.CHAT_VIEW_ID, ChatView.class);
    if (chatView != null) {
      chatView.layout(true, true);
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    lblImage.dispose();
  }

  /**
   * Dispose the label provider.
   */
  static void disposeLabelProvider() {
    labelProvider.dispose();
  }

}

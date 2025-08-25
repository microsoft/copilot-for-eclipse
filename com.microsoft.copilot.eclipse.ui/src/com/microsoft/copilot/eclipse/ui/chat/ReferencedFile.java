package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ReferencedFileService;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A widget that displays two buttons.
 */
public class ReferencedFile extends Composite {
  private Label lblfileIcon;
  private Label lblFileName;
  protected Label lblClose;
  private Image lblImage;
  private Image warningImage;
  private IResource file;
  private boolean isUnSupportedFile = false;

  // make it static to avoid creating multiple instances of the same label provider
  protected static WorkbenchLabelProvider labelProvider = new WorkbenchLabelProvider();

  /**
   * Creates a new TwinButton.
   */
  public ReferencedFile(Composite parent, IResource file, boolean isUnSupportedFile) {
    super(parent, SWT.BORDER);
    this.isUnSupportedFile = isUnSupportedFile;
    GridLayout layout = new GridLayout(3, false);
    layout.marginWidth = 4;
    layout.marginHeight = 2;
    setLayout(layout);

    lblfileIcon = new Label(this, SWT.NONE);
    lblfileIcon.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    lblFileName = new Label(this, SWT.NONE);
    lblFileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

    MouseAdapter mouseAdapter = new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        if (file instanceof IFile) {
          UiUtils.openInEditor((IFile) ReferencedFile.this.file);
        } else if (file instanceof IFolder) {
          UiUtils.revealInExplorer(ReferencedFile.this.file);
        }
      }
    };
    lblFileName.addMouseListener(mouseAdapter);
    lblfileIcon.addMouseListener(mouseAdapter);
    this.addMouseListener(mouseAdapter);

    lblClose = new Label(this, SWT.NONE);
    lblClose.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    setCloseClickAction();

    lblImage = UiUtils.buildImageFromPngPath("/icons/close.png");

    setFile(file);

    this.addDisposeListener(e -> {
      if (lblImage != null && !lblImage.isDisposed()) {
        lblImage.dispose();
      }
      if (warningImage != null && !warningImage.isDisposed()) {
        warningImage.dispose();
      }
    });
    this.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
  }

  /**
   * Set the mouse click adapter for the close button.
   */
  protected void setCloseClickAction() {
    lblClose.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        ReferencedFileService referencedFileService = CopilotUi.getPlugin().getChatServiceManager()
            .getReferencedFileService();
        referencedFileService.removeReferencedFile(file);
      }
    });
  }

  /**
   * Set the icon for the close button.
   */
  protected void setCloseClickBtnIcon(Image image) {
    lblClose.setImage(image);
  }

  public IResource getFile() {
    return file;
  }

  /**
   * Returns whether this file is unsupported by the current model.
   */
  public boolean isFileUnSupported() {
    return isUnSupportedFile;
  }

  /**
   * Set the file for this widget.
   */
  protected void setFile(@Nullable IResource file) {
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
      lblClose.setImage(lblImage);

      if (isUnSupportedFile) {
        setupUnsupportedFileDisplay();
      } else {
        setupNormalFileDisplay();
      }

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

  /**
   * Setup display for unsupported files (e.g., images without vision support).
   */
  private void setupUnsupportedFileDisplay() {
    // Set warning icon
    if (warningImage == null || warningImage.isDisposed()) {
      warningImage = UiUtils.buildImageFromPngPath("/icons/message_warning.png");
    }
    lblfileIcon.setImage(warningImage);
    // Set tooltip with model name
    String modelName = CopilotUi.getPlugin().getChatServiceManager().getUserPreferenceService().getActiveModel()
        .getModelName();
    String tooltipText = String.format(Messages.chat_referencedFile_noVision_tooltip, modelName);
    lblfileIcon.setToolTipText(tooltipText);
    lblFileName.setToolTipText(tooltipText);
    lblClose.setToolTipText(tooltipText);
    lblFileName.setData(CSSSWTConstants.CSS_ID_KEY, "not-supported-referenced-file-name");

    lblFileName.addPaintListener(e -> {
      String text = lblFileName.getText();
      Point textSize = e.gc.textExtent(text);

      int y = textSize.y / 2;
      e.gc.setLineWidth(1);
      e.gc.drawLine(0, y, textSize.x, y);
    });
  }

  /**
   * Setup display for normal supported files.
   */
  private void setupNormalFileDisplay() {
    Image image = labelProvider.getImage(file);
    if (image != null) {
      lblfileIcon.setImage(image);
    }
    lblFileName.setData(CSSSWTConstants.CSS_ID_KEY, "normal-referenced-file-name");
  }

  /**
   * Dispose the label provider.
   */
  static void disposeLabelProvider() {
    labelProvider.dispose();
  }

}

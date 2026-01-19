package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4j.Range;
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
  private Label selectionLabel;
  private Range currentSelection;

  /**
   * Creates a new CurrentReferencedFile.
   */
  public CurrentReferencedFile(Composite parent) {
    // No need to get supportVision here, as currentFile will not be an image file.
    super(parent, null, false);

    // change to 5 col layout to accommodate selection label
    GridLayout layout = new GridLayout(5, false);
    layout.marginWidth = 4;
    layout.marginHeight = 2;
    layout.horizontalSpacing = 0;
    setLayout(layout);

    // Set filename column with spacing from icon
    GridData fileNameData = new GridData(SWT.FILL, SWT.CENTER, false, true);
    fileNameData.horizontalIndent = 5;
    lblFileName.setLayoutData(fileNameData);

    // Selection label (e.g., ":10-20") - no gap with filename
    selectionLabel = new Label(this, SWT.NONE);
    selectionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    selectionLabel.moveAbove(lblClose);
    selectionLabel.setData(CssConstants.CSS_CLASS_NAME_KEY, "text-secondary");
    selectionLabel.setVisible(false);
    registerControlForFontUpdates(selectionLabel);

    descriptionLabel = new Label(this, SWT.NONE);
    descriptionLabel.setText(Messages.chat_currentReferencedFile_description);
    GridData descData = new GridData(SWT.FILL, SWT.CENTER, false, false);
    descData.horizontalIndent = 5;
    descriptionLabel.setLayoutData(descData);
    descriptionLabel.moveAbove(lblClose);
    descriptionLabel.setData(CssConstants.CSS_CLASS_NAME_KEY, "text-secondary");
    registerControlForFontUpdates(descriptionLabel);

    // Add spacing to close button to match default spacing
    GridData closeData = new GridData(SWT.FILL, SWT.CENTER, false, false);
    closeData.horizontalIndent = 5;
    lblClose.setLayoutData(closeData);
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

  /**
   * Set the current selection to display.
   */
  public void setSelection(@Nullable Range selection) {
    this.currentSelection = selection;
    updateSelectionLabel();
  }

  private void updateSelectionLabel() {
    if (selectionLabel.isDisposed()) {
      return;
    }

    if (currentSelection == null || isSinglePointSelection(currentSelection)) {
      selectionLabel.setText("");
      selectionLabel.setVisible(false);
      GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
      gridData.exclude = true;
      selectionLabel.setLayoutData(gridData);
    } else {
      // Show ":X-Y" (LSP lines are 0-based, display as 1-based)
      int startLine = currentSelection.getStart().getLine() + 1;
      int endLine = currentSelection.getEnd().getLine() + 1;
      String selectionText;
      if (startLine == endLine) {
        selectionText = ":" + startLine;
      } else {
        selectionText = ":" + startLine + "-" + endLine;
      }
      selectionLabel.setText(selectionText);
      // Create new GridData each time to ensure proper size computation
      GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
      gridData.exclude = false;
      selectionLabel.setLayoutData(gridData);
      selectionLabel.setVisible(true);
    }
    requestLayout();
  }

  /**
   * Check if the selection is just a cursor position (no actual text selected).
   */
  private boolean isSinglePointSelection(Range selection) {
    return selection.getStart().getLine() == selection.getEnd().getLine()
        && selection.getStart().getCharacter() == selection.getEnd().getCharacter();
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

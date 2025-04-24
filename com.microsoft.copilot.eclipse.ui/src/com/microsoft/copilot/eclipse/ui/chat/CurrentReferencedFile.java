package com.microsoft.copilot.eclipse.ui.chat;

import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPartReference;

import com.microsoft.copilot.eclipse.core.Constants;
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
  private IPartListener2 listener;
  private boolean isCurrentFileVisible = true;
  private IPartService partService;

  private Label descriptionLabel;

  /**
   * Creates a new CurrentReferencedFile.
   */
  public CurrentReferencedFile(Composite parent) {
    super(parent, null);
    IFile currentFile = UiUtils.getCurrentFile();
    if (needExcluded(currentFile)) {
      currentFile = null;
    }
    setFile(currentFile);
    updateCloseClickBtnIcon();
    setCloseClickAction(new MouseAdapter() {
      @Override
      public void mouseDown(org.eclipse.swt.events.MouseEvent e) {
        isCurrentFileVisible = !isCurrentFileVisible;
        updateCloseClickBtnIcon();
      }
    });
    this.listener = new IPartListener2() {
      @Override
      public void partActivated(IWorkbenchPartReference partRef) {
        if (partRef.getPart(false) instanceof IEditorPart) {
          IFile newFile = UiUtils.getCurrentFile();
          IFile file = CurrentReferencedFile.this.getFile();
          if (Objects.equals(newFile, file)) {
            return;
          }
          if (needExcluded(newFile)) {
            newFile = null;
          }
          CurrentReferencedFile.this.setFile(newFile);
          CurrentReferencedFile.this.updateCloseClickBtnIcon();
        }
      }

      @Override
      public void partClosed(IWorkbenchPartReference partRef) {
        IFile newFile = UiUtils.getCurrentFile();
        if (newFile == null) {
          CurrentReferencedFile.this.setFile(null);
        } else {
          CurrentReferencedFile.this.setFile(newFile);
        }
        CurrentReferencedFile.this.updateCloseClickBtnIcon();
      }
    };
    partService = UiUtils.getPartService();
    if (partService != null) {
      partService.addPartListener(listener);
    }

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

  private void updateCloseClickBtnIcon() {
    if (isCurrentFileVisible) {
      setCloseClickBtnIcon(visibleImage);
    } else {
      setCloseClickBtnIcon(invisibleImage);
    }
  }

  public boolean isCurrentFileVisible() {
    return isCurrentFileVisible;
  }

  /**
   * Returns true if the file needs to be excluded from 'Current file' reference in chat.
   */
  private boolean needExcluded(@Nullable IFile file) {
    if (file == null) {
      return true;
    }
    String fileExtension = file.getFileExtension();
    if (fileExtension == null) {
      return true;
    }
    return Constants.EXCLUDED_FILE_TYPE.contains(fileExtension);
  }

  @Override
  public void dispose() {
    super.dispose();
    if (partService != null) {
      partService.removePartListener(listener);
    }
  }
}

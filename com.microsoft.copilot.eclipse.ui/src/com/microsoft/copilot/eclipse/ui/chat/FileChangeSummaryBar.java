package com.microsoft.copilot.eclipse.ui.chat;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.chat.tools.FileToolService;
import com.microsoft.copilot.eclipse.ui.chat.tools.FileToolService.FileChangeProperty;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * FileChangeSummaryBar is a composite that displays the file change summary bar in the Copilot chat view. It is used to
 * show the summary of file changes in the Copilot chat view.
 */
public class FileChangeSummaryBar extends Composite {
  private FileToolService fileToolService;

  private FileChangeSummaryTitleBar titleBar;
  private ChangedFiles changedFiles;
  private Composite parent;
  private boolean enableButtons = false;

  /**
   * Constructs a new FileChangeSummaryBar with the given parent and style.
   *
   * @param parent the parent composite
   * @param style the style of the composite
   */
  public FileChangeSummaryBar(Composite parent, int style) {
    super(parent, style | SWT.BORDER);
    this.parent = parent;
    this.fileToolService = CopilotUi.getPlugin().getChatServiceManager().getFileToolService();
  }

  /**
   * Builds the file change summary bar for the given files.
   *
   * @param filesMap a map of files and their change status
   */
  public void buildSummaryBarFor(Map<IFile, FileChangeProperty> filesMap) {
    if (filesMap == null || isDisposed()) {
      return;
    }

    if (filesMap.isEmpty()) {
      this.fileToolService.onResolveAllChanges();
      return;
    }

    GridLayout gl = new GridLayout(1, false);
    setLayout(gl);
    gl.marginWidth = 0;
    gl.verticalSpacing = 0;
    gl.marginHeight = 0;
    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    if (titleBar != null) {
      titleBar.dispose();
    }
    titleBar = new FileChangeSummaryTitleBar(this, SWT.NONE, filesMap);
    titleBar.setLayout(new GridLayout(4, false));
    titleBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    if (changedFiles != null) {
      changedFiles.dispose();
    }
    this.changedFiles = new ChangedFiles(this, SWT.NONE, filesMap);

    this.setButtonStatus(this.enableButtons);
    requestLayout();
  }

  /**
   * Sets the button status (enabled/disabled) for the "Done", "Keep", and "Undo" buttons.
   *
   * @param enable true to enable the buttons, false to disable them
   */
  public void setButtonStatus(boolean enable) {
    this.enableButtons = enable;
    if (titleBar != null) {
      if (titleBar.doneButton != null) {
        titleBar.doneButton.setEnabled(enable);
      }
      if (titleBar.keepButton != null) {
        titleBar.keepButton.setEnabled(enable);
      }
      if (titleBar.undoButton != null) {
        titleBar.undoButton.setEnabled(enable);
      }
    }

    if (changedFiles != null) {
      changedFiles.setButtonStatus(enable);
    }
  }

  /**
   * Disposes of the FileChangeSummaryBar and its resources.
   */
  @Override
  public void dispose() {
    // Notify the parent node to release the FileChangeSummaryBar related resources
    if (titleBar != null) {
      titleBar.dispose();
      titleBar = null;
    }
    if (changedFiles != null) {
      changedFiles.dispose();
      changedFiles = null;
    }
    super.dispose();
    this.parent.layout(true, true);
  }

  class FileChangeSummaryTitleBar extends Composite {
    private Label titleLabel;
    private Button keepButton;
    private Button undoButton;
    private Button doneButton;

    public FileChangeSummaryTitleBar(Composite parent, int style, Map<IFile, FileChangeProperty> filesMap) {
      super(parent, style);
      GridLayout gl = new GridLayout(2, false);
      gl.marginWidth = 0;
      gl.horizontalSpacing = 0;
      gl.verticalSpacing = 0;
      gl.marginHeight = 0;
      setLayout(gl);
      setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

      // Create the title label with left alignment
      titleLabel = new Label(this, SWT.NONE);
      int fileChangeCount = filesMap.size();
      String titlePostfix = fileChangeCount > 1 ? " Files Changed" : " File Changed";
      titleLabel.setText(fileChangeCount + titlePostfix);
      GridData labelGridData = new GridData(SWT.BEGINNING, SWT.CENTER, true, false);
      titleLabel.setLayoutData(labelGridData);

      updateTitleBarButtons(filesMap.values().stream().filter(value -> !value.isHandled()).count() == 0);
    }

    private void updateTitleBarButtons(boolean isExecutionFinished) {
      // Dispose existing buttons
      if (doneButton != null) {
        doneButton.dispose();
        doneButton = null;
      }
      if (keepButton != null) {
        keepButton.dispose();
        keepButton = null;
      }
      if (undoButton != null) {
        undoButton.dispose();
        undoButton = null;
      }

      if (isExecutionFinished) {
        // Create the "Done" button
        doneButton = new Button(this, SWT.PUSH | SWT.FLAT);
        doneButton.setText("Done");
        GridData keepButtonGridData = new GridData(SWT.END, SWT.CENTER, false, false);
        doneButton.setLayoutData(keepButtonGridData);
        doneButton.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
            fileToolService.onResolveAllChanges();
          }
        });
      } else {
        // Create the "Keep" button
        keepButton = new Button(this, SWT.PUSH | SWT.FLAT);
        keepButton.setText("Keep");
        GridData keepButtonGridData = new GridData(SWT.END, SWT.CENTER, false, false);
        keepButton.setLayoutData(keepButtonGridData);
        keepButton.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
            fileToolService.onKeepAllChanges();
          }
        });

        // Create the "Undo" button
        undoButton = new Button(this, SWT.PUSH | SWT.FLAT);
        undoButton.setText("Undo");
        GridData undoButtonGridData = new GridData(SWT.END, SWT.CENTER, false, false);
        undoButton.setLayoutData(undoButtonGridData);
        undoButton.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
            fileToolService.onUndoAllChanges();
          }
        });
      }

      parent.layout(true, true);
    }
  }

  class ChangedFiles extends Composite {
    private final Composite contentArea;
    private List<FileRow> fileRows; // List to keep track of file rows

    public ChangedFiles(Composite parent, int style, Map<IFile, FileChangeProperty> filesMap) {
      super(parent, style);

      // Main layout
      GridLayout layout = new GridLayout(1, false);
      layout.marginWidth = 0;
      layout.marginHeight = 0;
      layout.verticalSpacing = 0;
      setLayout(layout);
      setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      // TODO: add scroll bar when list is too big, in vscode, it will have scroll bar when the size > 6
      contentArea = new Composite(this, SWT.NONE);
      contentArea.setLayout(new GridLayout(1, false));
      contentArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      // TODO: Should share a same instance with ReferencedFile
      WorkbenchLabelProvider labelProvider = new WorkbenchLabelProvider();
      fileRows = new LinkedList<>();
      for (IFile file : filesMap.keySet()) {
        if (file == null) {
          continue;
        }

        Image image = labelProvider.getImage(file);
        fileRows.add(new FileRow(contentArea, SWT.NONE, image, file, filesMap.get(file).isHandled()));
      }

      // Update layout
      contentArea.layout(true, true);
    }

    private void setButtonStatus(boolean enable) {
      for (FileRow fileRow : fileRows) {
        fileRow.setButtonStatus(enable);
      }
    }
  }

  /**
   * Represents a single file row with actions.
   */
  public class FileRow extends Composite {
    private Composite actionsArea;
    private Button keepButton;
    private Button undoButton;
    private Button removeButton;

    /**
     * Constructs a new FileRow.
     */
    public FileRow(Composite parent, int style, Image fileImage, IFile file, boolean fileIsHandled) {
      super(parent, style);

      GridLayout layout = new GridLayout(2, false);
      // set 1 here otherwise the mouse enter will not be triggered when
      // mouse is moving into the widget from the left border.
      layout.marginWidth = 1;
      layout.marginHeight = 0;
      setLayout(layout);
      setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

      // File information section (left)
      GridLayout fileInfoLayout = new GridLayout(3, false);
      fileInfoLayout.marginWidth = 0;
      fileInfoLayout.marginHeight = 0;
      Composite fileInfo = new Composite(this, SWT.NONE);
      fileInfo.setLayout(fileInfoLayout);
      fileInfo.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
      fileInfo.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseUp(MouseEvent e) {
          fileToolService.onViewDiff(file);
        }
      });

      // File icon
      Label iconLabel = new Label(fileInfo, SWT.NONE);
      iconLabel.setImage(fileImage);
      iconLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      iconLabel.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseUp(MouseEvent e) {
          fileToolService.onViewDiff(file);
        }
      });

      // File name (bold)
      Label nameLabel = new Label(fileInfo, SWT.NONE);
      nameLabel.setText(file.getName());
      nameLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      nameLabel.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseUp(MouseEvent e) {
          fileToolService.onViewDiff(file);
        }
      });

      // Make font bold
      FontData[] fontData = nameLabel.getFont().getFontData();
      for (FontData fd : fontData) {
        fd.setStyle(SWT.BOLD);
      }
      Font boldFont = new Font(getDisplay(), fontData);
      nameLabel.setFont(boldFont);
      nameLabel.addDisposeListener(e -> boldFont.dispose());

      // File path
      Label pathLabel = new Label(fileInfo, SWT.NONE);
      pathLabel.setText(file.getFullPath().toString());
      pathLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
      pathLabel.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseUp(MouseEvent e) {
          fileToolService.onViewDiff(file);
        }
      });

      // Actions section (right)
      actionsArea = new Composite(this, SWT.NONE);
      GridLayout actionsLayout = new GridLayout(4, false);
      actionsLayout.marginWidth = 0;
      actionsLayout.marginHeight = 0;
      actionsLayout.horizontalSpacing = 0;
      actionsArea.setLayout(actionsLayout);
      actionsArea.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

      // Create action buttons only when the file is not handled
      if (!fileIsHandled) {
        keepButton = UiUtils.createIconButton(actionsArea, SWT.PUSH | SWT.FLAT);
        Image keepImg = UiUtils.buildImageFromPngPath("/icons/chat/keep.png");
        this.addDisposeListener(e -> {
          if (keepImg != null && !keepImg.isDisposed()) {
            keepImg.dispose();
          }
        });
        keepButton.setImage(keepImg);
        keepButton.setToolTipText("Keep");
        GridData keepGridData = new GridData(SWT.END, SWT.CENTER, false, false);
        keepGridData.widthHint = keepImg.getImageData().width + 2 * UiConstants.BTN_PADDING;
        keepGridData.heightHint = keepImg.getImageData().height + 2 * UiConstants.BTN_PADDING;
        keepButton.setLayoutData(keepGridData);
        keepButton.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
            fileToolService.onKeepChange(file);
          }
        });

        undoButton = UiUtils.createIconButton(actionsArea, SWT.PUSH | SWT.FLAT);
        Image undoImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_UNDO);
        undoButton.setImage(undoImage);
        undoButton.setToolTipText("Undo");
        GridData undoGridData = new GridData(SWT.END, SWT.CENTER, false, false);
        undoGridData.widthHint = undoImage.getImageData().width + 2 * UiConstants.BTN_PADDING;
        undoGridData.heightHint = undoImage.getImageData().height + 2 * UiConstants.BTN_PADDING;
        undoButton.setLayoutData(undoGridData);
        undoButton.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
            fileToolService.onUndoChange(file);
          }
        });
      }

      removeButton = UiUtils.createIconButton(actionsArea, SWT.PUSH | SWT.FLAT);
      Image removeImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_REMOVE);
      removeButton.setImage(removeImage);
      removeButton.setToolTipText("Remove");
      GridData removeGridData = new GridData(SWT.END, SWT.CENTER, false, false);
      removeGridData.widthHint = removeImage.getImageData().width + 2 * UiConstants.BTN_PADDING;
      removeGridData.heightHint = removeImage.getImageData().height + 2 * UiConstants.BTN_PADDING;
      removeButton.setLayoutData(removeGridData);
      removeButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
          fileToolService.onRemoveFile(file);
        }
      });

      // Hide actions initially
      actionsArea.setVisible(false);

      // Handle hover events
      addMouseTrackListener(new MouseTrackAdapter() {
        @Override
        public void mouseEnter(MouseEvent e) {
          actionsArea.setVisible(true);
          layout(true, true);
        }

        @Override
        public void mouseExit(MouseEvent e) {
          // Check if mouse is still in our bounds before hiding
          Point cursorPos = getDisplay().getCursorLocation();
          Point widgetPos = toDisplay(0, 0);
          Rectangle bounds = getBounds();

          if (cursorPos.x <= widgetPos.x || cursorPos.x >= widgetPos.x + bounds.width || cursorPos.y <= widgetPos.y
              || cursorPos.y >= widgetPos.y + bounds.height) {
            actionsArea.setVisible(false);
            layout(true, true);
          }
        }
      });

      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseUp(MouseEvent e) {
          fileToolService.onViewDiff(file);
        }
      });
    }

    private void setButtonStatus(boolean enable) {
      if (keepButton != null) {
        keepButton.setEnabled(enable);
      }
      if (undoButton != null) {
        undoButton.setEnabled(enable);
      }
      if (removeButton != null) {
        removeButton.setEnabled(enable);
      }
    }
  }
}
package com.microsoft.copilot.eclipse.ui.chat;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatFontService;
import com.microsoft.copilot.eclipse.ui.chat.tools.FileToolService;
import com.microsoft.copilot.eclipse.ui.chat.tools.FileToolService.FileChangeProperty;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * FileChangeSummaryBar is a composite that displays the file change summary bar in the Copilot chat view. It is used to
 * show the summary of file changes in the Copilot chat view.
 */
public class FileChangeSummaryBar extends Composite {
  private FileToolService fileToolService;
  private ChatFontService chatFontService;
  private IStylingEngine stylingEngine;

  private FileChangeSummaryTitleBar titleBar;
  private ChangedFiles changedFiles;
  private Composite parent;
  private boolean enableButtons = false;
  private boolean isExpanded = true;

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
    this.chatFontService = CopilotUi.getPlugin().getChatServiceManager().getChatFontService();
    this.stylingEngine = PlatformUI.getWorkbench().getService(IStylingEngine.class);
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
   * Toggles the visibility of the changed files section.
   */
  private void toggleExpanded() {
    isExpanded = !isExpanded;
    if (changedFiles != null && !changedFiles.isDisposed()) {
      GridData layoutData = (GridData) changedFiles.getLayoutData();
      layoutData.exclude = !isExpanded;
      changedFiles.setVisible(isExpanded);
    }
    if (titleBar != null && !titleBar.isDisposed()) {
      titleBar.updateExpandCollapseState();
    }
    requestLayout();
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
    private Label expandIcon;
    private Image downArrowImage;
    private Image rightArrowImage;
    private Label titleLabel;
    private Button keepButton;
    private Button undoButton;
    private String changeFilesTitle;

    public FileChangeSummaryTitleBar(Composite parent, int style, Map<IFile, FileChangeProperty> filesMap) {
      super(parent, style);
      GridLayout gl = new GridLayout(3, false);
      gl.marginWidth = 0;
      gl.horizontalSpacing = 0;
      gl.verticalSpacing = 0;
      gl.marginHeight = 0;
      setLayout(gl);
      setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));

      // Create the expand/collapse icon
      expandIcon = new Label(this, SWT.NONE);
      expandIcon.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      expandIcon.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));

      // Create the title label with left alignment
      titleLabel = new Label(this, SWT.NONE);
      int fileChangeCount = filesMap.size();
      String titlePostfix = fileChangeCount > 1 ? Messages.fileChangeSummary_filesChanged
          : Messages.fileChangeSummary_fileChanged;
      changeFilesTitle = fileChangeCount + titlePostfix;
      titleLabel.setText(changeFilesTitle);
      GridData labelGridData = new GridData(SWT.BEGINNING, SWT.CENTER, true, false);
      titleLabel.setLayoutData(labelGridData);
      titleLabel.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
      chatFontService.registerControl(titleLabel);

      // Initialize icon and tooltips
      updateExpandCollapseState();

      // Add click listeners to toggle expansion
      MouseAdapter clickListener = new MouseAdapter() {
        @Override
        public void mouseUp(MouseEvent e) {
          toggleExpanded();
        }
      };
      expandIcon.addMouseListener(clickListener);
      titleLabel.addMouseListener(clickListener);
      addMouseListener(clickListener);

      updateTitleBarButtons();

      this.addDisposeListener(e -> {
        if (downArrowImage != null && !downArrowImage.isDisposed()) {
          downArrowImage.dispose();
        }
        if (rightArrowImage != null && !rightArrowImage.isDisposed()) {
          rightArrowImage.dispose();
        }
      });
    }

    /**
     * Updates the expand/collapse icon and tooltips based on the current expanded state. Sets appropriate icons and
     * tooltip messages for both the expand icon and title label.
     */
    private void updateExpandCollapseState() {
      if (expandIcon == null || expandIcon.isDisposed()) {
        return;
      }

      String tooltipMessage;
      if (isExpanded) {
        // Expanded state: show down arrow and collapse tooltip
        if (downArrowImage == null) {
          downArrowImage = UiUtils.buildImageFromPngPath("/icons/chat/down_arrow.png");
        }
        expandIcon.setImage(downArrowImage);
        tooltipMessage = NLS.bind(Messages.fileChangeSummary_collapseTooltip, changeFilesTitle);
      } else {
        // Collapsed state: show right arrow and expand tooltip
        if (rightArrowImage == null) {
          rightArrowImage = UiUtils.buildImageFromPngPath("/icons/chat/right_arrow.png");
        }
        expandIcon.setImage(rightArrowImage);
        tooltipMessage = NLS.bind(Messages.fileChangeSummary_expandTooltip, changeFilesTitle);
      }

      // Set the same tooltip for both icon, tile label, and the whole title bar
      expandIcon.setToolTipText(tooltipMessage);
      if (titleLabel != null && !titleLabel.isDisposed()) {
        titleLabel.setToolTipText(tooltipMessage);
      }
      if (this != null && !this.isDisposed()) {
        this.setToolTipText(tooltipMessage);
      }
    }

    private void updateTitleBarButtons() {
      // Dispose existing buttons
      if (keepButton != null) {
        keepButton.dispose();
        keepButton = null;
      }
      if (undoButton != null) {
        undoButton.dispose();
        undoButton = null;
      }

      // Create the "Keep" button
      keepButton = new Button(this, SWT.PUSH | SWT.FLAT);
      keepButton.setText(Messages.fileChangeSummary_keepButton);
      GridData keepButtonGridData = new GridData(SWT.END, SWT.CENTER, false, false);
      keepButton.setLayoutData(keepButtonGridData);
      keepButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
          fileToolService.onKeepAllChanges();
        }
      });
      chatFontService.registerControl(keepButton);

      // Create the "Undo" button
      undoButton = new Button(this, SWT.PUSH | SWT.FLAT);
      undoButton.setText(Messages.fileChangeSummary_undoButton);
      GridData undoButtonGridData = new GridData(SWT.END, SWT.CENTER, false, false);
      undoButton.setLayoutData(undoButtonGridData);
      undoButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
          fileToolService.onUndoAllChanges();
        }
      });
      chatFontService.registerControl(undoButton);

      parent.requestLayout();
    }
  }

  class ChangedFiles extends Composite {
    private static final int MAX_VISIBLE_FILES = 5;
    private final Composite contentArea;
    private final ScrolledComposite scrolledComposite;
    private List<FileRow> fileRows; // List to keep track of file rows

    public ChangedFiles(Composite parent, int style, Map<IFile, FileChangeProperty> filesMap) {
      super(parent, style);

      // Main layout
      GridLayout layout = new GridLayout(1, false);
      layout.marginWidth = 0;
      layout.marginHeight = 0;
      setLayout(layout);
      setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      // Count files
      long fileCount = filesMap.size();

      // Create ScrolledComposite if we have more than MAX_VISIBLE_FILES
      if (fileCount > MAX_VISIBLE_FILES) {
        scrolledComposite = new ScrolledComposite(this, SWT.V_SCROLL);
        GridLayout scrollLayout = new GridLayout(1, false);
        scrollLayout.marginWidth = 0;
        scrollLayout.marginHeight = 0;
        scrolledComposite.setLayout(scrollLayout);
        scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);

        contentArea = new Composite(scrolledComposite, SWT.NONE);
        GridLayout contentLayout = new GridLayout(1, false);
        contentLayout.marginWidth = 0;
        contentLayout.marginHeight = 0;
        contentArea.setLayout(contentLayout);

        scrolledComposite.setContent(contentArea);
      } else {
        scrolledComposite = null;
        contentArea = new Composite(this, SWT.NONE);
        GridLayout contentLayout = new GridLayout(1, false);
        contentLayout.marginWidth = 0;
        contentLayout.marginHeight = 0;
        contentArea.setLayout(contentLayout);
        contentArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      }

      // TODO: Should share a same instance with ReferencedFile
      WorkbenchLabelProvider labelProvider = new WorkbenchLabelProvider();
      fileRows = new LinkedList<>();
      for (IFile file : filesMap.keySet()) {
        if (file == null) {
          continue;
        }

        Image image = labelProvider.getImage(file);
        fileRows.add(new FileRow(contentArea, SWT.NONE, image, file));
      }

      // Update layout and calculate scroll height if needed
      contentArea.requestLayout();

      if (scrolledComposite != null && !fileRows.isEmpty()) {
        // Calculate the height of a single FileRow to estimate scroll area height
        FileRow firstRow = fileRows.get(0);
        int singleRowHeight = firstRow.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

        // Set the minimum height to show MAX_VISIBLE_FILES rows
        int scrollHeight = singleRowHeight * MAX_VISIBLE_FILES;

        // Set the scrolled composite's minimum size
        scrolledComposite.setMinHeight(contentArea.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);

        // Set layout data with height hint for the scrolled composite
        GridData scrollData = new GridData(SWT.FILL, SWT.FILL, true, false);
        scrollData.heightHint = scrollHeight;
        scrolledComposite.setLayoutData(scrollData);
      }
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
    private Label keepButton;
    private Label undoButton;

    /**
     * Constructs a new FileRow.
     */
    public FileRow(Composite parent, int style, Image fileImage, IFile file) {
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
      nameLabel.setToolTipText(file.getFullPath().toString());
      nameLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      nameLabel.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseUp(MouseEvent e) {
          fileToolService.onViewDiff(file);
        }
      });

      // Make font bold and register for font changes
      final Font[] boldFontHolder = new Font[1];
      Runnable applyBoldFont = () -> {
        if (nameLabel.isDisposed()) {
          return;
        }
        if (boldFontHolder[0] != null && !boldFontHolder[0].isDisposed()) {
          boldFontHolder[0].dispose();
        }
        boldFontHolder[0] = UiUtils.getBoldChatFont(getDisplay(), nameLabel.getFont());
        nameLabel.setFont(boldFontHolder[0]);
        nameLabel.requestLayout();
      };
      chatFontService.registerCallback(applyBoldFont);
      nameLabel.addDisposeListener(e -> {
        chatFontService.unregisterCallback(applyBoldFont);
        if (boldFontHolder[0] != null && !boldFontHolder[0].isDisposed()) {
          boldFontHolder[0].dispose();
        }
      });

      // File path
      CLabel pathLabel = new CLabel(fileInfo, SWT.NONE);
      pathLabel.setText(file.getFullPath().toString());
      pathLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
      pathLabel.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseUp(MouseEvent e) {
          fileToolService.onViewDiff(file);
        }
      });
      chatFontService.registerControl(pathLabel);

      // Actions section (right)
      actionsArea = new Composite(this, SWT.NONE);
      GridLayout actionsLayout = new GridLayout(2, false);
      actionsLayout.marginHeight = 0;
      actionsArea.setLayout(actionsLayout);
      GridData actionsData = new GridData(SWT.END, SWT.CENTER, false, false);
      actionsData.exclude = true;
      actionsArea.setLayoutData(actionsData);
      actionsArea.setVisible(false);

      keepButton = new Label(actionsArea, SWT.NONE);
      Image keepImg = UiUtils.buildImageFromPngPath("/icons/chat/keep.png");
      this.addDisposeListener(e -> {
        if (keepImg != null && !keepImg.isDisposed()) {
          keepImg.dispose();
        }
      });
      keepButton.setImage(keepImg);
      keepButton.setToolTipText(Messages.fileChangeSummary_keepButton);
      GridData keepGridData = new GridData(SWT.END, SWT.CENTER, false, false);
      keepButton.setLayoutData(keepGridData);
      keepButton.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseUp(MouseEvent e) {
          fileToolService.onKeepChange(file);
        }
      });

      undoButton = new Label(actionsArea, SWT.NONE);
      Image undoImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_UNDO);
      undoButton.setImage(undoImage);
      undoButton.setToolTipText(Messages.fileChangeSummary_undoButton);
      GridData undoGridData = new GridData(SWT.END, SWT.CENTER, false, false);
      undoButton.setLayoutData(undoGridData);
      undoButton.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseUp(MouseEvent e) {
          fileToolService.onUndoChange(file);
        }
      });

      // Hide actions initially
      actionsArea.setVisible(false);

      // Apply initial CSS ID
      setFileRowCssId(false);

      // Create hover listeners
      Listener enterListener = event -> setHoverEffect(true);
      Listener exitListener = event -> setHoverEffect(false);

      // Handle hover events - add listeners recursively to all controls
      addHoverListenersRecursively(this, enterListener, exitListener);

      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseUp(MouseEvent e) {
          fileToolService.onViewDiff(file);
        }
      });
    }

    /**
     * Sets the hover effect including CSS class and actions area visibility.
     *
     * @param isHover true if hovering, false otherwise
     */
    private void setHoverEffect(boolean isHover) {
      setFileRowCssId(isHover);
      setActionsAreaVisible(isHover);
    }

    /**
     * Sets the CSS ID on the FileRow and all its children based on hover state.
     *
     * @param isHover true if hovering, false otherwise
     */
    private void setFileRowCssId(boolean isHover) {
      String cssId = isHover ? "file-row-hover" : "file-row";
      applyCssIdRecursively(this, cssId);
    }

    /**
     * Recursively applies CSS ID to a control and all its children.
     */
    private void applyCssIdRecursively(Control control, String cssId) {
      control.setData(CssConstants.CSS_ID_KEY, cssId);
      if (stylingEngine != null) {
        stylingEngine.style(control);
      }
      if (control instanceof Composite composite) {
        for (Control child : composite.getChildren()) {
          applyCssIdRecursively(child, cssId);
        }
      }
    }

    /**
     * Recursively adds the hover listeners to all child controls.
     */
    private void addHoverListenersRecursively(Control control, Listener enterListener, Listener exitListener) {
      // Add listener to the control itself first
      control.addListener(SWT.MouseEnter, enterListener);
      control.addListener(SWT.MouseExit, exitListener);

      // Then recursively add to all children if it's a composite
      if (control instanceof Composite composite) {
        for (Control child : composite.getChildren()) {
          addHoverListenersRecursively(child, enterListener, exitListener);
        }
      }
    }

    /**
     * Sets the visibility of the actions area.
     *
     * @param visible true to show the actions area, false to hide it
     */
    private void setActionsAreaVisible(boolean visible) {
      if (actionsArea.isVisible() != visible) {
        GridData layoutData = (GridData) actionsArea.getLayoutData();
        layoutData.exclude = !visible;
        actionsArea.setVisible(visible);
        requestLayout();
      }
    }

    private void setButtonStatus(boolean enable) {
      if (keepButton != null) {
        keepButton.setEnabled(enable);
      }
      if (undoButton != null) {
        undoButton.setEnabled(enable);
      }
    }
  }
}
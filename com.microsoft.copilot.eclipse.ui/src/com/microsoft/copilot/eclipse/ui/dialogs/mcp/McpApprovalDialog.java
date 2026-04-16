// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.dialogs.mcp;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.VerticalRuler;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.ui.chat.services.McpExtensionPointManager.McpRegistrationInfo;
import com.microsoft.copilot.eclipse.ui.preferences.WrappableNoteLabel;
import com.microsoft.copilot.eclipse.ui.utils.TextMateUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Dialog for approving third-party MCP providers.
 */
public class McpApprovalDialog extends Dialog {

  private Map<String, McpRegistrationInfo> mcpRegInfoMap;

  private TableViewer contributorTableViewer;
  private SourceViewer mcpServersPreviewViewer;
  private Button approveButton;
  private Button denyButton;
  private Button approveAllButton;
  private Button denyAllButton;

  private String selectedContributor;

  // Images for status icons
  private Image approvedImage;
  private Image deniedImage;

  /**
   * Create the dialog.
   *
   * @param parentShell the parent shell
   * @param mcpRegInfoMap map of contributor name to McpRegistrationInfo
   */
  public McpApprovalDialog(Shell parentShell, Map<String, McpRegistrationInfo> mcpRegInfoMap) {
    super(parentShell);
    this.mcpRegInfoMap = mcpRegInfoMap;
    setShellStyle(getShellStyle() | SWT.RESIZE);
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(Messages.mcpApprovalDialog_title);
    loadImages(newShell);
  }

  private void loadImages(Shell shell) {
    approvedImage = UiUtils.buildImageFromPngPath("/icons/chat/keep.png");
    deniedImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
    shell.addDisposeListener(e -> {
      if (approvedImage != null && !approvedImage.isDisposed()) {
        approvedImage.dispose();
      }
    });
  }

  @Override
  protected Point getInitialSize() {
    return new Point(720, 550);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);
    GridLayoutFactory.swtDefaults().numColumns(1).margins(10, 10).spacing(5, 0).applyTo(container);

    // Description area
    createDescriptionArea(container);

    // Top area with table and buttons
    Composite topArea = new Composite(container, SWT.NONE);
    GridLayoutFactory.swtDefaults().numColumns(2).spacing(10, 5).applyTo(topArea);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(topArea);

    // Left panel - Contributors table
    createContributorsArea(topArea);

    // Right panel - Action buttons (vertical layout like Templates page)
    createActionButtonsArea(topArea);

    // Note area
    createNoteArea(container);

    // Bottom area - Preview and details
    createPreviewArea(container);

    // Initialize selection
    if (!mcpRegInfoMap.isEmpty()) {
      contributorTableViewer.getTable().select(0);
      updateSelection();
    } else {
      approveButton.setEnabled(false);
      denyButton.setEnabled(false);
      approveAllButton.setEnabled(false);
      denyAllButton.setEnabled(false);
    }

    return container;
  }

  private void createDescriptionArea(Composite parent) {
    WrappableNoteLabel descLabel = new WrappableNoteLabel(parent, "", Messages.mcpApprovalDialog_description);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(descLabel);
  }

  private void createContributorsArea(Composite parent) {
    contributorTableViewer = new TableViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL);
    Table table = contributorTableViewer.getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    GridDataFactory.fillDefaults().grab(true, false).hint(420, 80).applyTo(table);

    TableColumn statusColumn = new TableColumn(table, SWT.LEFT);
    statusColumn.setText(Messages.mcpApprovalDialog_column_status);
    statusColumn.setWidth(100);

    TableColumn nameColumn = new TableColumn(table, SWT.LEFT);
    nameColumn.setText(Messages.mcpApprovalDialog_column_pluginId);
    nameColumn.setWidth(200);

    TableColumn displayNameColumn = new TableColumn(table, SWT.LEFT);
    displayNameColumn.setText(Messages.mcpApprovalDialog_column_displayName);
    displayNameColumn.setWidth(150);

    TableColumn trustColumn = new TableColumn(table, SWT.LEFT);
    trustColumn.setText(Messages.mcpApprovalDialog_column_signedStatus);
    trustColumn.setWidth(100);

    contributorTableViewer.setContentProvider(new IStructuredContentProvider() {
      @Override
      public Object[] getElements(Object inputElement) {
        if (inputElement instanceof Map) {
          @SuppressWarnings("unchecked")
          Map<String, McpRegistrationInfo> map = (Map<String, McpRegistrationInfo>) inputElement;
          return map.keySet().toArray();
        }
        return new Object[0];
      }
    });

    contributorTableViewer.setLabelProvider(new ContributorLabelProvider());
    contributorTableViewer.setInput(mcpRegInfoMap);

    contributorTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        updateSelection();
      }
    });
  }

  private void createActionButtonsArea(Composite parent) {
    Composite buttonArea = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().numColumns(1).spacing(5, 5).applyTo(buttonArea);
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(buttonArea);

    approveButton = new Button(buttonArea, SWT.PUSH);
    approveButton.setText(Messages.mcpApprovalDialog_button_approve);
    GridDataFactory.swtDefaults().hint(90, SWT.DEFAULT).applyTo(approveButton);
    approveButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        approveSelected();
      }
    });

    denyButton = new Button(buttonArea, SWT.PUSH);
    denyButton.setText(Messages.mcpApprovalDialog_button_deny);
    GridDataFactory.swtDefaults().hint(90, SWT.DEFAULT).applyTo(denyButton);
    denyButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        denySelected();
      }
    });

    approveAllButton = new Button(buttonArea, SWT.PUSH);
    approveAllButton.setText(Messages.mcpApprovalDialog_button_approveAll);
    GridDataFactory.swtDefaults().hint(90, SWT.DEFAULT).applyTo(approveAllButton);
    approveAllButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        approveAll();
      }
    });

    denyAllButton = new Button(buttonArea, SWT.PUSH);
    denyAllButton.setText(Messages.mcpApprovalDialog_button_denyAll);
    GridDataFactory.swtDefaults().hint(90, SWT.DEFAULT).applyTo(denyAllButton);
    denyAllButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        denyAll();
      }
    });
  }

  private void createNoteArea(Composite parent) {
    WrappableNoteLabel noteLabel = new WrappableNoteLabel(parent, Messages.mcpApprovalDialog_note_prefix,
        Messages.mcpApprovalDialog_note_content);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(noteLabel);
  }

  private void createPreviewArea(Composite parent) {
    // Preview label
    WrappableNoteLabel previewLabel = new WrappableNoteLabel(parent, "", Messages.mcpApprovalDialog_preview_label);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(previewLabel);

    // Preview source viewer for JSON syntax highlighting
    mcpServersPreviewViewer = new SourceViewer(parent, new VerticalRuler(0),
        SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
    mcpServersPreviewViewer.setEditable(false);
    mcpServersPreviewViewer.configure(TextMateUtils.getConfiguration("json"));

    // Set up the document
    IDocument document = new Document();
    mcpServersPreviewViewer.setDocument(document);

    GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 200)
        .applyTo(mcpServersPreviewViewer.getControl());
  }

  private void updateSelection() {
    IStructuredSelection selection = (IStructuredSelection) contributorTableViewer.getSelection();
    if (!selection.isEmpty()) {
      selectedContributor = (String) selection.getFirstElement();
      updatePreview();
    } else {
      selectedContributor = null;
      clearPreview();
    }
  }

  private void updatePreview() {
    if (selectedContributor == null || mcpRegInfoMap.get(selectedContributor) == null) {
      clearPreview();
      return;
    }

    McpRegistrationInfo info = mcpRegInfoMap.get(selectedContributor);

    // Update MCP servers preview
    String mcpServers = info.getMcpServersAsJson();
    if (mcpServers != null && !mcpServers.trim().isEmpty()) {
      String formattedJson = formatJsonForDisplay(mcpServers);
      mcpServersPreviewViewer.getDocument().set(formattedJson);
    } else {
      mcpServersPreviewViewer.getDocument().set(Messages.mcpApprovalDialog_preview_empty);
    }

    // Enable/disable approve and deny buttons based on approval status
    if (info.isApproved()) {
      approveButton.setEnabled(false);
      denyButton.setEnabled(true);
    } else {
      approveButton.setEnabled(true);
      denyButton.setEnabled(false);
    }
  }

  /**
   * Formats JSON string to display in multiple lines with proper indentation.
   *
   * @param jsonString the JSON string to format
   * @return formatted JSON string with proper indentation
   */
  private String formatJsonForDisplay(String jsonString) {
    if (jsonString == null || jsonString.trim().isEmpty()) {
      return jsonString;
    }

    try {
      Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
      JsonElement jsonElement = JsonParser.parseString(jsonString);
      return gson.toJson(jsonElement);
    } catch (JsonSyntaxException e) {
      // If JSON is invalid, return as-is
      return jsonString;
    }
  }

  private void clearPreview() {
    mcpServersPreviewViewer.getDocument().set("");
    approveButton.setEnabled(false);
    denyButton.setEnabled(false);
  }

  private void approveSelected() {
    if (selectedContributor != null) {
      McpRegistrationInfo info = mcpRegInfoMap.get(selectedContributor);
      if (info != null) {
        int result = openWarningDialog();
        if (result == 0) {
          info.setApproved(true);
          contributorTableViewer.refresh();
          updatePreview();
        }
      }
    }
  }

  private void denySelected() {
    if (selectedContributor != null) {
      McpRegistrationInfo info = mcpRegInfoMap.get(selectedContributor);
      if (info != null) {
        info.setApproved(false);
        contributorTableViewer.refresh();
        updatePreview();
      }
    }
  }

  private void approveAll() {
    int result = openWarningDialog();
    if (result != 0) {
      return;
    }

    for (McpRegistrationInfo info : mcpRegInfoMap.values()) {
      info.setApproved(true);
    }
    contributorTableViewer.refresh();
    updatePreview();
  }

  private void denyAll() {
    for (McpRegistrationInfo info : mcpRegInfoMap.values()) {
      info.setApproved(false);
    }
    contributorTableViewer.refresh();
    updatePreview();
  }

  private int openWarningDialog() {
    return MessageDialog.open(MessageDialog.WARNING, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
        Messages.mcpApprovalDialog_warning_title, Messages.mcpApprovalDialog_warning_message, SWT.NONE,
        Messages.mcpApprovalDialog_warning_button_approve, Messages.mcpApprovalDialog_warning_button_cancel);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, Messages.mcpApprovalDialog_close_button, true);
  }

  /**
   * Label provider for the contributors table.
   */
  private class ContributorLabelProvider extends LabelProvider implements ITableLabelProvider {

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
      if (element instanceof String) {
        if (columnIndex == 0) { // Status column - show status icons
          String contributor = (String) element;
          McpRegistrationInfo info = mcpRegInfoMap.get(contributor);

          if (info != null && info.isApproved()) {
            return approvedImage;
          } else {
            return deniedImage;
          }
        }
      }

      return null;
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
      if (element instanceof String) {
        String contributor = (String) element;
        McpRegistrationInfo info = mcpRegInfoMap.get(contributor);

        switch (columnIndex) {
          case 0: // Status
            if (info != null && info.isApproved()) {
              return Messages.mcpApprovalDialog_status_approved;
            } else {
              return Messages.mcpApprovalDialog_status_denied;
            }
          case 1: // Plug-in Id
            return contributor;
          case 2: // Display Name
            return info != null && info.getPluginDisplayName() != null ? info.getPluginDisplayName() : "";
          case 3: // Trust
            return info != null && info.isTrusted() ? Messages.mcpApprovalDialog_signed_signed
                : Messages.mcpApprovalDialog_signed_unsigned;
          default:
            return "";
        }
      }

      return "";
    }
  }
}

package com.microsoft.copilot.eclipse.ui.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Preference page for GitHub Copilot MCP settings.
 */
public class McpPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  private static final int NOTE_LABEL_MARGIN = 20;
  private ControlListener controlListener;

  /**
   * Constructor.
   */
  public McpPreferencePage() {
    super(GRID);
  }

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());
  }

  @Override
  protected void createFieldEditors() {
    Composite parent = getFieldEditorParent();
    parent.setLayout(new GridLayout(1, true));
    var gl = new GridLayout(1, true);
    gl.marginTop = 2;
    gl.marginLeft = 2;

    GridDataFactory gdf = GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false);
    Group mcpGroup = new Group(parent, SWT.NONE);
    mcpGroup.setLayout(gl);
    gdf.applyTo(mcpGroup);
    mcpGroup.setText(Messages.preferences_page_mcp_settings);
    // add mcp field
    var mcpFieldContainer = new Composite(mcpGroup, SWT.NONE);
    mcpFieldContainer.setLayout(gl);
    mcpFieldContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    var mcpField = new StringFieldEditor(Constants.MCP, Messages.preferences_page_mcp, StringFieldEditor.UNLIMITED, 20,
        StringFieldEditor.VALIDATE_ON_KEY_STROKE, mcpFieldContainer);
    mcpField.getLabelControl(mcpFieldContainer).setToolTipText(Messages.preferences_page_mcp_tooltip);
    // @formatter:off
    mcpField.getLabelControl(mcpFieldContainer).setLayoutData(new GridData(
        SWT.LEFT, 
        SWT.TOP, 
        false, 
        false, 
        2, // The label-control will take up 2 column cells itself, so the text-control will be underneath it.
        1));
    // @formatter:on
    addField(mcpField);
    // add note to mcp field
    var mcpNoteComposite = new Composite(mcpGroup, SWT.NONE);
    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginLeft = -3;
    gridLayout.marginBottom = 1;
    mcpNoteComposite.setLayout(gridLayout);
    mcpNoteComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    var mcpNoteLabel = new Label(mcpNoteComposite, SWT.NONE);
    mcpNoteLabel.setText(Messages.preferences_page_note_text);
    FontData[] fontData = mcpNoteLabel.getFont().getFontData();
    for (FontData fd : fontData) {
      fd.setStyle(SWT.BOLD);
    }
    Font boldFont = new Font(parent.getDisplay(), fontData);
    mcpNoteLabel.setFont(boldFont);
    Label mcpNoteContentLabel = new Label(mcpNoteComposite, SWT.WRAP);
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    gd.widthHint = 400;
    mcpNoteContentLabel.setLayoutData(gd);
    mcpNoteContentLabel.setText(Messages.preferences_page_mcp_note_content);

    this.controlListener = new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        // resize the note label
        int width = McpPreferencePage.this.getFieldEditorParent().getSize().x - NOTE_LABEL_MARGIN;
        GridData mcpNoteContentGrid = new GridData(SWT.FILL, SWT.FILL, true, true);
        mcpNoteContentGrid.widthHint = width;
        mcpNoteContentLabel.setLayoutData(mcpNoteContentGrid);
        McpPreferencePage.this.getFieldEditorParent().layout();
      }
    };
    parent.addControlListener(controlListener);
    parent.addDisposeListener(e -> {
      if (boldFont != null && !boldFont.isDisposed()) {
        boldFont.dispose();
      }
      parent.removeControlListener(controlListener);
    });

  }

}

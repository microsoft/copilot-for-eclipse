package com.microsoft.copilot.eclipse.ui.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Chat preference page.
 */
public class ChatPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
  public static final String ID = "com.microsoft.copilot.eclipse.ui.preferences.ChatPreferencesPage";

  /**
   * Constructor.
   */
  public ChatPreferencesPage() {
    super(GRID);
  }

  @Override
  public void createFieldEditors() {
    Composite parent = getFieldEditorParent();
    parent.setLayout(new GridLayout(1, true));
    GridLayout gl = new GridLayout(1, true);
    gl.marginTop = 2;
    gl.marginLeft = 2;

    GridDataFactory gdf = GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false);

    Composite workspaceContextComposite = new Composite(parent, SWT.NONE);
    workspaceContextComposite.setLayout(gl);
    gdf.applyTo(workspaceContextComposite);
    BooleanFieldEditor workspaceContextField = new BooleanFieldEditor(Constants.WORKSPACE_CONTEXT_ENABLED,
        Messages.preferences_page_watched_files, SWT.WRAP, workspaceContextComposite);
    GridData workspaceContextFieldGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    workspaceContextFieldGridData.widthHint = 400;
    workspaceContextField.getDescriptionControl(workspaceContextComposite).setLayoutData(workspaceContextFieldGridData);

    addField(workspaceContextField);

    // add chat note using WrappableNoteLabel
    new WrappableNoteLabel(parent, Messages.preferences_page_note_prefix,
        Messages.preferences_page_watched_files_note_content);

    // Check if sub-agent is disabled by policy
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    boolean policyAllowsSubAgent = flags != null && flags.isClientPreviewFeatureEnabled()
        && flags.isSubAgentPolicyEnabled();

    if (!policyAllowsSubAgent) {
      Label disabledLabel = new Label(parent, SWT.WRAP);
      disabledLabel.setText(Messages.preferences_page_sub_agent_disabled_by_policy);
      GridData disabledLabelData = new GridData(SWT.FILL, SWT.FILL, true, false);
      disabledLabelData.widthHint = 400;
      disabledLabel.setLayoutData(disabledLabelData);
    } else {
      // Add sub-agent toggle
      Composite subAgentComposite = new Composite(parent, SWT.NONE);
      subAgentComposite.setLayout(gl);
      gdf.applyTo(subAgentComposite);
      BooleanFieldEditor subAgentField = new BooleanFieldEditor(Constants.SUB_AGENT_ENABLED,
          Messages.preferences_page_sub_agent, SWT.WRAP, subAgentComposite);
      GridData subAgentFieldGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
      subAgentFieldGridData.widthHint = 400;
      subAgentField.getDescriptionControl(subAgentComposite).setLayoutData(subAgentFieldGridData);

      addField(subAgentField);

      // add sub-agent note using WrappableNoteLabel
      new WrappableNoteLabel(parent, Messages.preferences_page_note_prefix,
          Messages.preferences_page_sub_agent_note_content);

      // Update control listener to handle field resizing only if subAgentField exists
      ControlListener controlListener = new ControlAdapter() {
        @Override
        public void controlResized(ControlEvent e) {
          // resize the workspace context field and sub-agent field descriptions
          ChatPreferencesPage pg = ChatPreferencesPage.this;
          int width = pg.getFieldEditorParent().getSize().x - 20;
          ((GridData) workspaceContextField.getDescriptionControl(workspaceContextComposite)
              .getLayoutData()).widthHint = width;
          ((GridData) subAgentField.getDescriptionControl(subAgentComposite).getLayoutData()).widthHint = width;
          pg.getFieldEditorParent().layout();
        }
      };
      parent.addControlListener(controlListener);
      parent.addDisposeListener(e -> {
        parent.removeControlListener(controlListener);
      });
      return;
    }

    // Add control listener to handle field resizing for workspace context only when subagent is disabled
    ControlListener controlListener = new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        // resize the workspace context field description
        ChatPreferencesPage pg = ChatPreferencesPage.this;
        int width = pg.getFieldEditorParent().getSize().x - 20;
        ((GridData) workspaceContextField.getDescriptionControl(workspaceContextComposite)
            .getLayoutData()).widthHint = width;
        pg.getFieldEditorParent().layout();
      }
    };
    parent.addControlListener(controlListener);
    parent.addDisposeListener(e -> {
      parent.removeControlListener(controlListener);
    });
  }

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());
  }

  @Override
  public boolean performOk() {
    boolean oldWorkspaceContextValue = getPreferenceStore().getBoolean(Constants.WORKSPACE_CONTEXT_ENABLED);

    boolean result = super.performOk();
    boolean newWorkspaceContextValue = getPreferenceStore().getBoolean(Constants.WORKSPACE_CONTEXT_ENABLED);
    if (oldWorkspaceContextValue ^ newWorkspaceContextValue) {
      boolean restart = MessageDialog.openQuestion(getShell(), Messages.preferences_page_restart_required,
          Messages.preferences_page_watched_files_restart_question);

      if (restart) {
        try {
          // Explicitly save the preferences to disk to ensure they persist across the restart
          // CopilotUi.getPlugin().savePluginPreferences() is deprecated, flush is recommended
          InstanceScope.INSTANCE.getNode(CopilotUi.getPlugin().getBundle().getSymbolicName()).flush();
        } catch (BackingStoreException e) {
          CopilotCore.LOGGER.error("Failed to save preference 'Enable workspace context'", e);
        }

        // Close the preference dialog properly before restarting
        getShell().getDisplay().asyncExec(() -> {
          // Using asyncExec ensures the preference dialog completes its current operations
          PlatformUI.getWorkbench().restart();
        });
      }
    }

    return result;
  }
}
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
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

    GridDataFactory gdf = GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false);

    Composite workspaceContextComposite = new Composite(parent, SWT.NONE);
    workspaceContextComposite.setLayout(new GridLayout(1, true));
    gdf.applyTo(workspaceContextComposite);
    BooleanFieldEditor workspaceContextField = new BooleanFieldEditor(Constants.WORKSPACE_CONTEXT_ENABLED,
        Messages.preferences_page_watched_files, SWT.WRAP,
        workspaceContextComposite);
    GridData workspaceContextFieldGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    workspaceContextFieldGridData.widthHint = 400;
    workspaceContextField.getDescriptionControl(workspaceContextComposite).setLayoutData(workspaceContextFieldGridData);

    addField(workspaceContextField);

    // add chat note using WrappableNoteLabel
    WrappableNoteLabel workspaceContextNote = new WrappableNoteLabel(parent,
        Messages.preferences_page_note_prefix + " ",
        Messages.preferences_page_watched_files_note_content);
    GridData workspaceContextNoteGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    workspaceContextNoteGridData.horizontalSpan = 2;
    workspaceContextNote.setLayoutData(workspaceContextNoteGridData);

    // add separator
    Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
    GridData separatorGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    separatorGridData.horizontalSpan = 2;
    separator.setLayoutData(separatorGridData);

    // Add sub-agent toggle
    Composite subAgentComposite = new Composite(parent, SWT.NONE);
    subAgentComposite.setLayout(new GridLayout(1, true));
    gdf.applyTo(subAgentComposite);
    // Check if sub-agent is disabled by policy
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    boolean policyAllowsSubAgent = flags != null && flags.isClientPreviewFeatureEnabled()
        && flags.isSubAgentPolicyEnabled();
    if (!policyAllowsSubAgent) {
      Composite disabledComposite = new Composite(subAgentComposite, SWT.NONE);
      GridLayout disabledCompositeLayout = new GridLayout(1, false);
      disabledCompositeLayout.marginWidth = 0;
      disabledCompositeLayout.marginHeight = 0;
      disabledComposite.setLayout(disabledCompositeLayout);
      disabledComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
      WrappableIconLink.createWithCustomizedImage(disabledComposite, "/icons/information.png",
          Messages.setting_managed_by_organization);
    }

    BooleanFieldEditor subAgentField = new BooleanFieldEditor(Constants.SUB_AGENT_ENABLED,
        Messages.preferences_page_sub_agent, SWT.WRAP, subAgentComposite);
    subAgentField.setEnabled(policyAllowsSubAgent, subAgentComposite);
    GridData subAgentFieldGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    subAgentFieldGridData.widthHint = 400;
    subAgentField.getDescriptionControl(subAgentComposite).setLayoutData(subAgentFieldGridData);
    addField(subAgentField);

    // add sub-agent note using WrappableNoteLabel
    WrappableNoteLabel subAgentNote = new WrappableNoteLabel(parent,
        Messages.preferences_page_note_prefix + " ",
        Messages.preferences_page_sub_agent_note_content);
    GridData subAgentNoteGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    subAgentNoteGridData.horizontalSpan = 2;
    subAgentNote.setLayoutData(subAgentNoteGridData);

    // add separator
    Label separator2 = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
    GridData separator2GridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    separator2GridData.horizontalSpan = 2;
    separator2.setLayoutData(separator2GridData);

    // Add Agent Max Requests field
    Composite agentMaxRequestsComposite = new Composite(parent, SWT.NONE);
    agentMaxRequestsComposite.setLayout(new GridLayout(1, true));
    gdf.applyTo(agentMaxRequestsComposite);

    IntegerFieldEditor agentMaxRequestsField = new IntegerFieldEditor(Constants.AGENT_MAX_REQUESTS,
        Messages.preferences_page_agent_max_requests, agentMaxRequestsComposite);
    agentMaxRequestsField.setValidRange(1, 500);
    agentMaxRequestsField.setErrorMessage(Messages.preferences_page_agent_max_requests_validation_error);
    addField(agentMaxRequestsField);

    WrappableNoteLabel agentMaxRequestsNote = new WrappableNoteLabel(parent,
        Messages.preferences_page_note_prefix + " ",
        Messages.preferences_page_agent_max_requests_desc);
    GridData agentMaxRequestsNoteGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    agentMaxRequestsNoteGridData.horizontalSpan = 2;
    agentMaxRequestsNote.setLayoutData(agentMaxRequestsNoteGridData);
  }

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());

    // Ensure run_subagent tool configuration is consistent with sub-agent preference
    // Only check if sub-agent is policy-enabled
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    boolean policyAllowsSubAgent = flags != null && flags.isClientPreviewFeatureEnabled()
        && flags.isSubAgentPolicyEnabled();

    if (policyAllowsSubAgent) {
      boolean subAgentEnabled = getPreferenceStore().getBoolean(Constants.SUB_AGENT_ENABLED);
      updateSubAgentToolConfiguration(subAgentEnabled);
    }
  }

  @Override
  public boolean performOk() {
    final boolean oldWorkspaceContextValue = getPreferenceStore().getBoolean(Constants.WORKSPACE_CONTEXT_ENABLED);

    // Check if sub-agent is policy-enabled before handling sub-agent preferences
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    boolean policyAllowsSubAgent = flags != null && flags.isClientPreviewFeatureEnabled()
        && flags.isSubAgentPolicyEnabled();

    boolean oldSubAgentValue = false;
    if (policyAllowsSubAgent) {
      oldSubAgentValue = getPreferenceStore().getBoolean(Constants.SUB_AGENT_ENABLED);
    }

    final boolean result = super.performOk();
    boolean newWorkspaceContextValue = getPreferenceStore().getBoolean(Constants.WORKSPACE_CONTEXT_ENABLED);

    boolean newSubAgentValue = false;
    if (policyAllowsSubAgent) {
      newSubAgentValue = getPreferenceStore().getBoolean(Constants.SUB_AGENT_ENABLED);
    }

    // Handle sub-agent preference change
    boolean isSubAgentChanged = policyAllowsSubAgent && (oldSubAgentValue ^ newSubAgentValue);
    if (isSubAgentChanged) {
      updateSubAgentToolConfiguration(newSubAgentValue);
    }

    boolean isWorkspaceContextChanged = oldWorkspaceContextValue ^ newWorkspaceContextValue;
    if (isWorkspaceContextChanged) {
      try {
        InstanceScope.INSTANCE.getNode(CopilotUi.getPlugin().getBundle().getSymbolicName()).flush();
      } catch (BackingStoreException e) {
        CopilotCore.LOGGER.error("Failed to save preference 'Enable workspace context'", e);
      }
    }

    if (isSubAgentChanged || isWorkspaceContextChanged) {
      boolean restart = MessageDialog.openQuestion(getShell(),
          Messages.preferences_page_restart_required,
          Messages.preferences_page_restart_question);

      if (restart) {
        getShell().getDisplay().asyncExec(() -> {
          PlatformUI.getWorkbench().restart();
        });
      }
    }

    return result;
  }

  /**
   * Updates the MCP tool configuration to include or exclude the run_subagent tool for agent mode based on the
   * sub-agent preference setting.
   *
   * @param subAgentEnabled true if sub-agent is enabled, false otherwise
   */
  private void updateSubAgentToolConfiguration(boolean subAgentEnabled) {
    try {
      // Load existing MCP tools mode status
      String existingJson = getPreferenceStore().getString(Constants.MCP_TOOLS_MODE_STATUS);

      // Parse existing configuration or create new one
      Map<String, Map<String, Map<String, Boolean>>> modeToolStatus;
      if (existingJson != null && !existingJson.trim().isEmpty()) {
        Type type = new TypeToken<Map<String, Map<String, Map<String, Boolean>>>>() {
        }.getType();
        modeToolStatus = new Gson().fromJson(existingJson, type);
      } else {
        modeToolStatus = new HashMap<>();
      }

      // Ensure agent-mode map exists
      if (!modeToolStatus.containsKey("agent-mode")) {
        modeToolStatus.put("agent-mode", new HashMap<>());
      }

      // Get or create the Built-in Tools server map
      Map<String, Map<String, Boolean>> agentModeTools = modeToolStatus.get("agent-mode");
      String builtInToolsKey = Messages.preferences_page_mcp_tools_builtin;
      if (!agentModeTools.containsKey(builtInToolsKey)) {
        agentModeTools.put(builtInToolsKey, new HashMap<>());
      }

      // Update the run_subagent tool status
      Map<String, Boolean> builtInTools = agentModeTools.get(builtInToolsKey);
      if (subAgentEnabled) {
        builtInTools.put("run_subagent", true);
      } else {
        builtInTools.remove("run_subagent");
      }

      // Save back to preferences
      String updatedJson = new Gson().toJson(modeToolStatus);
      getPreferenceStore().setValue(Constants.MCP_TOOLS_MODE_STATUS, updatedJson);

    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to update sub-agent tool configuration", e);
    }
  }
}
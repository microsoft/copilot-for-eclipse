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
  private static final int FIELD_WIDTH_HINT = 400;

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

    Composite workspaceContextComposite = createSectionComposite(parent, gdf);
    BooleanFieldEditor workspaceContextField = new BooleanFieldEditor(Constants.WORKSPACE_CONTEXT_ENABLED,
        Messages.preferences_page_watched_files, SWT.WRAP, workspaceContextComposite);
    applyFieldWidthHint(workspaceContextField, workspaceContextComposite);
    addField(workspaceContextField);

    addNote(parent, Messages.preferences_page_watched_files_note_content);
    addSeparator(parent);

    // Add sub-agent toggle
    Composite subAgentComposite = createSectionComposite(parent, gdf);
    boolean policyAllowsSubAgent = isPolicyAllowsSubAgent();
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
    applyFieldWidthHint(subAgentField, subAgentComposite);
    addField(subAgentField);

    addNote(parent, Messages.preferences_page_sub_agent_note_content);
    addSeparator(parent);

    // Add Enable Skills toggle
    Composite skillsComposite = createSectionComposite(parent, gdf);

    BooleanFieldEditor skillsField = new BooleanFieldEditor(Constants.ENABLE_SKILLS,
        Messages.preferences_page_skills_enabled, SWT.WRAP, skillsComposite);
    applyFieldWidthHint(skillsField, skillsComposite);
    addField(skillsField);

    addNote(parent, Messages.preferences_page_skills_enabled_note_content);
    addSeparator(parent);

    // Add Agent Max Requests field
    Composite agentMaxRequestsComposite = createSectionComposite(parent, gdf);

    IntegerFieldEditor agentMaxRequestsField = new IntegerFieldEditor(Constants.AGENT_MAX_REQUESTS,
        Messages.preferences_page_agent_max_requests, agentMaxRequestsComposite);
    agentMaxRequestsField.setValidRange(1, 500);
    agentMaxRequestsField.setErrorMessage(Messages.preferences_page_agent_max_requests_validation_error);
    addField(agentMaxRequestsField);

    addNote(parent, Messages.preferences_page_agent_max_requests_desc);
  }

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());

    // Ensure run_subagent tool configuration is consistent with sub-agent preference
    // Only check if sub-agent is policy-enabled
    if (isPolicyAllowsSubAgent()) {
      boolean subAgentEnabled = getPreferenceStore().getBoolean(Constants.SUB_AGENT_ENABLED);
      updateSubAgentToolConfiguration(subAgentEnabled);
    }
  }

  @Override
  public boolean performOk() {
    final boolean oldWorkspaceContextValue = getPreferenceStore().getBoolean(Constants.WORKSPACE_CONTEXT_ENABLED);

    // Check if sub-agent is policy-enabled before handling sub-agent preferences
    boolean policyAllowsSubAgent = isPolicyAllowsSubAgent();

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
      boolean restart = MessageDialog.openQuestion(getShell(), Messages.preferences_page_restart_required,
          Messages.preferences_page_restart_question);

      if (restart) {
        getShell().getDisplay().asyncExec(() -> {
          PlatformUI.getWorkbench().restart();
        });
      }
    }

    return result;
  }

  private Composite createSectionComposite(Composite parent, GridDataFactory gdf) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(1, true));
    gdf.applyTo(composite);
    return composite;
  }

  private void applyFieldWidthHint(BooleanFieldEditor field, Composite parent) {
    GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    gridData.widthHint = FIELD_WIDTH_HINT;
    field.getDescriptionControl(parent).setLayoutData(gridData);
  }

  private void addNote(Composite parent, String noteContent) {
    WrappableNoteLabel note = new WrappableNoteLabel(parent, Messages.preferences_page_note_prefix + " ", noteContent);
    GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gridData.horizontalSpan = 2;
    note.setLayoutData(gridData);
  }

  private void addSeparator(Composite parent) {
    Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
    GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gridData.horizontalSpan = 2;
    separator.setLayoutData(gridData);
  }

  private boolean isPolicyAllowsSubAgent() {
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    return flags != null && flags.isClientPreviewFeatureEnabled() && flags.isSubAgentPolicyEnabled();
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
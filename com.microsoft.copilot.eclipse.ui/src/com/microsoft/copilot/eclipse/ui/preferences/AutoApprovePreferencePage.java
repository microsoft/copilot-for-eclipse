// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.McpConfigService;

/**
 * Auto-Approve preference page for terminal and file operation auto-approval rules.
 */
public class AutoApprovePreferencePage extends PreferencePage
    implements IWorkbenchPreferencePage {

  public static final String ID =
      "com.microsoft.copilot.eclipse.ui.preferences.AutoApprovePreferencePage";

  private TerminalAutoApproveSection terminalSection;
  private FileOperationAutoApproveSection fileOperationSection;
  private McpAutoApproveSection mcpSection;
  private GlobalAutoApproveSection globalSection;

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());
    noDefaultAndApplyButton();
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite root = new Composite(parent, SWT.NONE);
    root.setLayout(new GridLayout(1, false));
    root.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    IPreferenceStore store = getPreferenceStore();

    terminalSection = new TerminalAutoApproveSection(root, SWT.NONE);

    IPreferenceStore store = getPreferenceStore();
    terminalSection.loadFromPreferences(store);

    fileOperationSection = new FileOperationAutoApproveSection(root, SWT.NONE);
    fileOperationSection.loadFromPreferences(store);

    mcpSection = new McpAutoApproveSection(root, SWT.NONE);
    globalSection = new GlobalAutoApproveSection(root, SWT.NONE);
    return root;
  }

  @Override
  public boolean performOk() {
    IPreferenceStore store = getPreferenceStore();
    terminalSection.saveToPreferences(store);
    fileOperationSection.saveToPreferences(store);
    mcpSection.saveToPreferences(store);
    globalSection.saveToPreferences(store);
    return true;
  }

  private void bindMcpConfigService() {
    ChatServiceManager chatServiceManager =
        CopilotUi.getPlugin().getChatServiceManager();
    if (chatServiceManager != null) {
      McpConfigService mcpConfigService =
          chatServiceManager.getMcpConfigService();
      if (mcpConfigService != null) {
        mcpConfigService.bindWithAutoApproveSection(mcpSection);
      }
    }
  }
}
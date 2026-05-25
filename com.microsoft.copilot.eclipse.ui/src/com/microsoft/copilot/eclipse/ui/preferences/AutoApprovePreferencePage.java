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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
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
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    if (flags != null && !flags.isAutoApprovalEnabled()) {
      return WrappableIconLink.createWithSharedImage(parent,
          PlatformUI.getWorkbench().getSharedImages()
              .getImage(ISharedImages.IMG_OBJS_INFO_TSK),
          Messages.preferences_page_auto_approve_disabled_by_organization);
    }

    Composite root = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    root.setLayout(layout);
    root.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

    IPreferenceStore store = getPreferenceStore();
    terminalSection = new TerminalAutoApproveSection(root, SWT.NONE);
    terminalSection.loadFromPreferences(store);

    fileOperationSection = new FileOperationAutoApproveSection(root, SWT.NONE);
    fileOperationSection.loadFromPreferences(store);

    mcpSection = new McpAutoApproveSection(root, SWT.NONE);
    mcpSection.loadFromPreferences(store);
    bindMcpConfigService();

    globalSection = new GlobalAutoApproveSection(root, SWT.NONE);
    globalSection.loadFromPreferences(store);

    root.addDisposeListener(e -> unbindMcpConfigService());

    return root;
  }

  @Override
  public boolean performOk() {
    if (terminalSection == null) {
      return true;
    }
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

  private void unbindMcpConfigService() {
    ChatServiceManager chatServiceManager =
        CopilotUi.getPlugin().getChatServiceManager();
    if (chatServiceManager != null) {
      McpConfigService mcpConfigService =
          chatServiceManager.getMcpConfigService();
      if (mcpConfigService != null) {
        mcpConfigService.unbindWithAutoApproveSection();
      }
    }
  }
}

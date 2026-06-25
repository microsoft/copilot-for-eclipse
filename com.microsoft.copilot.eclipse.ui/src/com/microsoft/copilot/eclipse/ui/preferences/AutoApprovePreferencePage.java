// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
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

    // Wrap the sections in a height-capped ScrolledComposite. The override on
    // computeSize bounds the height reported to the dialog's PageLayout so the
    // Preferences shell stays stable; the real (taller) content scrolls here.
    // This scroller is also the parent the section tables forward wheel events
    // to (see forwardVerticalMouseWheelToParentScrollerAtBoundary).
    ScrolledComposite scrolled = new ScrolledComposite(parent, SWT.V_SCROLL) {
      @Override
      public Point computeSize(int widthHint, int heightHint, boolean changed) {
        Point size = super.computeSize(widthHint, heightHint, changed);
        size.y = Math.min(size.y, PreferencePageUtils.STANDARD_CONTENT_HEIGHT);
        return size;
      }
    };
    scrolled.setExpandHorizontal(true);
    scrolled.setExpandVertical(true);
    scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    Composite root = new Composite(scrolled, SWT.NONE);
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    root.setLayout(layout);

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

    scrolled.setContent(root);
    scrolled.setMinSize(root.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    // Track width so wrapping labels reflow and only vertical scrolling occurs.
    scrolled.addListener(SWT.Resize, e -> {
      int width = scrolled.getClientArea().width;
      scrolled.setMinSize(root.computeSize(width, SWT.DEFAULT));
    });

    scrolled.addDisposeListener(e -> unbindMcpConfigService());

    return scrolled;
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

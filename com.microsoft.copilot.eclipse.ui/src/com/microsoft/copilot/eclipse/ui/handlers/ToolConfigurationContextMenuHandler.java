// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Context menu contribution handler for .agent.md files. Adds "Configure Tools..." menu item when right-clicking in an
 * .agent.md file.
 */
public class ToolConfigurationContextMenuHandler extends CompoundContributionItem {

  @Override
  protected IContributionItem[] getContributionItems() {
    try {
      if (!shouldShowContextMenu()) {
        return new IContributionItem[0];
      }

      List<IContributionItem> items = new ArrayList<>();

      // Add separator before our menu items
      items.add(new Separator("com.microsoft.copilot.eclipse.ui.toolConfig.start"));

      // Create submenu for Copilot tool configuration
      MenuManager submenu = new MenuManager("Copilot",
          UiUtils.buildImageDescriptorFromPngPath("/icons/github_copilot.png"),
          "com.microsoft.copilot.eclipse.ui.toolConfigMenu");

      // Add "Configure Tools..." command
      IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      CommandContributionItemParameter param = new CommandContributionItemParameter(window, null,
          "com.microsoft.copilot.eclipse.commands.configureTools", CommandContributionItem.STYLE_PUSH);
      param.label = "Configure Tools...";
      param.icon = UiUtils.buildImageDescriptorFromPngPath("/icons/chat/tools.png");

      submenu.add(new CommandContributionItem(param));
      items.add(submenu);

      // Add separator after our menu items
      items.add(new Separator("com.microsoft.copilot.eclipse.ui.toolConfig.end"));

      return items.toArray(IContributionItem[]::new);

    } catch (Exception e) {
      CopilotCore.LOGGER.error("Error creating tool configuration context menu", e);
      return new IContributionItem[0];
    }
  }

  /**
   * Check if the context menu should be shown. Returns true if we're in an .agent.md file.
   */
  private boolean shouldShowContextMenu() {
    try {
      IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (window == null) {
        return false;
      }

      IEditorPart activeEditor = window.getActivePage().getActiveEditor();
      if (!(activeEditor instanceof ITextEditor)) {
        return false;
      }

      ITextEditor textEditor = (ITextEditor) activeEditor;

      // Check if it's an .agent.md file
      IFile file = textEditor.getEditorInput().getAdapter(IFile.class);
      return file != null && UiUtils.isAgentFile(file);

    } catch (Exception e) {
      CopilotCore.LOGGER.error("Error checking if context menu should be shown", e);
      return false;
    }
  }
}
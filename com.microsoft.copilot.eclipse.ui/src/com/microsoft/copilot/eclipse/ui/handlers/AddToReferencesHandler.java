// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.ReferencedFileService;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.ResourceUtils;

/**
 * Handler to add selected files or folders to the chat references.
 */
public class AddToReferencesHandler extends AbstractHandler implements IElementUpdater {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    ReferencedFileService referencedFileService = getReferencedFileService();
    if (referencedFileService == null) {
      return null;
    }
    IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
    IStructuredSelection selection = getStructuredSelection(window);
    if (selection == null) {
      return null;
    }

    List<IResource> validResources = ResourceUtils.collectValidResources(selection);
    if (!validResources.isEmpty()) {
      referencedFileService.addReferencedFiles(validResources);
    }

    return null;
  }

  @Override
  public void updateElement(UIElement element, Map parameters) {
    if (element == null) {
      return;
    }
    IStructuredSelection selection = getStructuredSelection(
        element.getServiceLocator().getService(IWorkbenchWindow.class));
    if (selection == null) {
      element.setText(Messages.addToReference_addFile_title);
      return;
    }

    ReferencedFileService referencedFileService = getReferencedFileService();
    if (referencedFileService == null) {
      element.setText(Messages.addToReference_addFile_title);
      return;
    }

    ResourceUtils.SelectionStats stats = ResourceUtils.analyzeSelection(selection);

    // Set text based on counts
    if (stats.hasOnlyFiles()) {
      element.setText(Messages.addToReference_addFile_title);
    } else if (stats.hasOnlyFolders()) {
      element.setText(Messages.addToReference_addFolder_title);
    } else {
      element.setText(Messages.addToReference_addFile_title);
    }
  }

  private ReferencedFileService getReferencedFileService() {
    ChatServiceManager chatServiceManager = CopilotUi.getPlugin().getChatServiceManager();
    if (chatServiceManager == null) {
      return null;
    }
    return chatServiceManager.getReferencedFileService();
  }

  private IStructuredSelection getStructuredSelection(IWorkbenchWindow window) {
    if (window == null) {
      return null;
    }

    ISelection selection = window.getSelectionService().getSelection();
    if (selection instanceof IStructuredSelection structuredSelection && !structuredSelection.isEmpty()) {
      return structuredSelection;
    }

    return null;
  }
}

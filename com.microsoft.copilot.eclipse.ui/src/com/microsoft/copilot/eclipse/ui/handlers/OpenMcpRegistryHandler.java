// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.ui.dialogs.mcp.McpRegistryDialog;

/**
 * Handler for opening the MCP Registry dialog.
 */
public class OpenMcpRegistryHandler extends CopilotHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Display.getDefault().asyncExec(() -> {
      IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (activeWorkbenchWindow != null) {
        McpRegistryDialog dialog = new McpRegistryDialog(activeWorkbenchWindow.getShell());
        dialog.open();
      }
    });
    return null;
  }
}

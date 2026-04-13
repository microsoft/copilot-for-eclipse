// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.CustomChatMode;
import com.microsoft.copilot.eclipse.core.chat.CustomChatModeManager;
import com.microsoft.copilot.eclipse.ui.preferences.McpPreferencePage;
import com.microsoft.copilot.eclipse.ui.utils.PreferencesUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Command handler for the "Configure Tools..." command. Opens the MCP Preference Page with the specific mode selected.
 */
public class ConfigureToolsCommandHandler extends CopilotHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    try {
      IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
      if (activeEditor == null) {
        return null;
      }

      // Get the file being edited
      IEditorInput editorInput = activeEditor.getEditorInput();
      String modeId = null;

      if (editorInput instanceof IFileEditorInput) {
        IFileEditorInput fileInput = (IFileEditorInput) editorInput;
        IFile file = fileInput.getFile();
        modeId = extractModeIdFromFile(file);
      } else {
        // Handle external files
        try {
          Path filePath = Paths.get(editorInput.getToolTipText());
          String fileName = filePath.getFileName().toString();
          if (UiUtils.isAgentFile(fileName)) {
            // Look up the custom mode by matching the file path
            List<CustomChatMode> customModes = CustomChatModeManager.INSTANCE.getCustomModes();

            for (CustomChatMode mode : customModes) {
              String modeModeId = mode.getId();
              if (modeModeId != null && modeModeId.startsWith("file:")) {
                try {
                  Path modePath = Paths.get(java.net.URI.create(modeModeId));
                  if (filePath.equals(modePath)) {
                    modeId = modeModeId;
                    break;
                  }
                } catch (Exception ex) {
                  // Ignore invalid URIs
                }
              }
            }
          }
        } catch (Exception e) {
          CopilotCore.LOGGER.error("Failed to extract file path from editor input", e);
        }
      }

      // Open MCP preference page with the mode ID
      openToolConfigurationDialog(HandlerUtil.getActiveShell(event), modeId);

    } catch (Exception e) {
      CopilotCore.LOGGER.error("Error executing configure tools command", e);
      throw new ExecutionException("Failed to execute configure tools command", e);
    }

    return null;
  }

  /**
   * Extract mode ID from an IFile. For custom modes, we need to find the actual mode ID by matching the file path since
   * the ID comes from LSP and may have a different format than file.getLocationURI().
   *
   * @param file the file to extract mode ID from
   * @return the mode ID (file URI), or null if not found
   */
  private static String extractModeIdFromFile(IFile file) {
    if (file == null) {
      return null;
    }

    if (UiUtils.isAgentFile(file)) {
      // Get the file's absolute path
      Path filePath = Paths.get(file.getLocationURI());

      // Look up the custom mode by matching the file path
      try {
        List<CustomChatMode> customModes = CustomChatModeManager.INSTANCE.getCustomModes();

        for (CustomChatMode mode : customModes) {
          String modeId = mode.getId();
          if (modeId != null && modeId.startsWith("file:")) {
            try {
              Path modePath = Paths.get(java.net.URI.create(modeId));
              if (filePath.equals(modePath)) {
                return modeId;
              }
            } catch (Exception e) {
              // Ignore invalid URIs
            }
          }
        }
      } catch (Exception e) {
        CopilotCore.LOGGER.error("Failed to look up custom mode by file path", e);
      }
    }
    return null;
  }

  /**
   * Open the tool configuration dialog for a specific file. This is a public static method that can be called from code
   * mining or other UI components.
   *
   * <p>This method saves all dirty .agent.md files before opening the dialog to ensure
   * the language server reads the latest file contents when updating tool configurations.
   *
   * @param shell the parent shell
   * @param file the .agent.md file
   */
  public static void openToolConfigurationDialog(Shell shell, IFile file) {
    // Save all dirty .agent.md files before opening the preference page
    // This ensures the language server reads the latest content when tool status is updated
    saveAllDirtyAgentFiles();

    String modeId = extractModeIdFromFile(file);
    openToolConfigurationDialog(shell, modeId);
  }

  /**
   * Open the tool configuration dialog with a specific mode ID.
   *
   * @param shell the parent shell
   * @param modeId the mode ID to configure, or null for default
   */
  public static void openToolConfigurationDialog(Shell shell, String modeId) {
    try {
      PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(shell, McpPreferencePage.ID,
          PreferencesUtils.getAllPreferenceIds(), modeId);

      dialog.open();
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Error opening tool configuration dialog", e);
    }
  }

  /**
   * Save all open .agent.md files that have unsaved changes.
   * This ensures the language server reads the latest content when updating tool configurations.
   * This method must be called from the UI thread.
   */
  private static void saveAllDirtyAgentFiles() {
    for (IEditorPart editor : UiUtils.findAllOpenAgentFiles()) {
      if (editor != null && editor.isDirty()) {
        IWorkbenchPage page = editor.getSite().getPage();
        if (page != null) {
          page.saveEditor(editor, false);
        }
      }
    }
  }
}
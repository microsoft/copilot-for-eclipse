// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.tools.FileToolService;

/**
 * Utility class for conversation-related operations.
 */
public class ConversationUtils {

  /**
   * Dialog type for end chat confirmation.
   */
  private enum DialogType {
    END_CHAT(Messages.endChat_confirmationTitle, Messages.endChat_confirmationMessage);

    private final String title;
    private final String message;

    DialogType(String title, String message) {
      this.title = title;
      this.message = message;
    }

    public String getTitle() {
      return title;
    }

    public String getMessage() {
      return message;
    }
  }

  /**
   * Dialog result constants for better readability.
   */
  private static final int KEEP_CHANGES = 0;
  private static final int UNDO_CHANGES = 1;

  /**
   * Confirm ending the current chat session when there are unhandled file changes. If there are
   * no unhandled changes, returns true immediately. If there are unhandled changes, prompts the
   * user with options to keep or undo them. Returning false means the user cancelled the
   * operation.
   *
   * @return true if it's ok to proceed with ending the chat session; false if cancelled.
   */
  public static boolean confirmEndChat() {
    return confirmChatAction(DialogType.END_CHAT);
  }

  /**
   * Common logic for confirming chat actions when there are unhandled file changes.
   *
   * @param dialogType the type of dialog to show
   * @return true if it's ok to proceed; false if cancelled.
   */
  private static boolean confirmChatAction(DialogType dialogType) {
    FileToolService fileToolService = getFileToolService();
    if (fileToolService == null) {
      return true; // Fail open if services are not ready or no file service available
    }

    if (!hasUnhandledChanges(fileToolService)) {
      return true;
    }

    return showConfirmationDialog(dialogType, fileToolService);
  }

  /**
   * Gets the FileToolService instance.
   *
   * @return the FileToolService, or null if not available
   */
  private static FileToolService getFileToolService() {
    if (CopilotUi.getPlugin() == null || CopilotUi.getPlugin().getChatServiceManager() == null) {
      return null;
    }
    return CopilotUi.getPlugin().getChatServiceManager().getFileToolService();
  }

  /**
   * Checks if there are any unhandled file changes.
   *
   * @param fileToolService the file tool service
   * @return true if there are unhandled changes
   */
  private static boolean hasUnhandledChanges(FileToolService fileToolService) {
    return fileToolService.getChangedFiles().size() > 0;
  }

  /**
   * Shows the confirmation dialog and handles the user's response.
   *
   * @param dialogType the type of dialog to show
   * @param fileToolService the file tool service
   * @return true if the user chose to proceed; false if cancelled
   */
  private static boolean showConfirmationDialog(DialogType dialogType, FileToolService fileToolService) {
    int result = MessageDialog.open(
        MessageDialog.QUESTION,
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
        dialogType.getTitle(),
        dialogType.getMessage(),
        SWT.NONE,
        Messages.confirmDialog_keepChangesButton,
        Messages.confirmDialog_undoChangesButton);

    return handleDialogResult(result, fileToolService);
  }

  /**
   * Handles the result of the confirmation dialog.
   *
   * @param result the dialog result
   * @param fileToolService the file tool service
   * @return true if the user chose a valid option; false if cancelled
   */
  private static boolean handleDialogResult(int result, FileToolService fileToolService) {
    switch (result) {
      case KEEP_CHANGES:
        fileToolService.onKeepAllChanges();
        return true;
      case UNDO_CHANGES:
        fileToolService.onUndoAllChanges();
        return true;
      default:
        return false; // Close / Cancel
    }
  }

  /**
   * Checks if the given throwable is an instance of cancellation exception, either directly or wrapped in a
   * CompletionException.
   *
   * @param th the exception to check
   * @return true if it's a cancellation exception; false otherwise
   */
  public static boolean isConversationCancellationThrowable(Throwable th) {
    return th instanceof CancellationException
        || (th instanceof CompletionException && th.getCause() instanceof CancellationException);
  }
}

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult.ToolConfirmationResult;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Dialog to confirm tool execution.
 */
public class InvokeToolConfirmationDialog extends Composite {

  private CompletableFuture<LanguageModelToolConfirmationResult> toolConfirmationFuture;
  private String cancelMessage;
  private Font boldFont;

  /**
   * Create a new confirmation dialog for tool execution.
   *
   * @param parent The parent composite
   * @param title The title of the confirmation dialog
   * @param message The message to display
   * @param input The input object to pass to the tool
   */
  public InvokeToolConfirmationDialog(Composite parent, String title, String message, Object input) {
    super(parent, SWT.BORDER | SWT.WRAP);
    this.setLayout(new GridLayout(1, false));
    this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    createDialogContent(title, message, input);

    this.toolConfirmationFuture = new CompletableFuture<>();
  }

  private void createDialogContent(String title, String message, Object input) {
    // Title of the confirmation dialog
    Label titleLbl = new Label(this, SWT.LEFT | SWT.WRAP);
    titleLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    titleLbl.setText(title);
    this.boldFont = UiUtils.getBoldFont(this.getDisplay(), titleLbl.getFont());
    titleLbl.setFont(boldFont);
    UiUtils.useParentBackground(titleLbl);
    titleLbl.addDisposeListener(e -> {
      boldFont.dispose();
    });

    // Confirmation message of the confirmation dialog
    Label messageLbl = new Label(this, SWT.LEFT | SWT.WRAP);
    GridData messageGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
    messageLbl.setLayoutData(messageGridData);
    messageLbl.setText(message);
    UiUtils.useParentBackground(messageLbl);
    // More information about the tool invocation
    if (input != null) {
      // TODO: Improve the logic to show more information about the tool invocation when confirm with users. The
      // following code only works for the run in terminal tool.
      Map<String, Object> inputMap = (Map<String, Object>) input;
      if (inputMap.containsKey("command")) {
        Label commandLbl = new Label(this, SWT.LEFT | SWT.WRAP);
        commandLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        commandLbl.setText((String) inputMap.get("command"));
        commandLbl.setBackground(this.getParent().getBackground());
        this.cancelMessage = (String) inputMap.get("command");
      }

      if (inputMap.containsKey("explanation")) {
        Label explanationLbl = new Label(this, SWT.LEFT | SWT.WRAP);
        explanationLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        explanationLbl.setText((String) inputMap.get("explanation"));
        UiUtils.useParentBackground(explanationLbl);
      }
    }

    createButtons();
  }

  private void createButtons() {
    GridLayout actionLayout = new GridLayout(2, false);
    actionLayout.marginLeft = 0;
    actionLayout.marginRight = 0;
    actionLayout.marginWidth = 0;
    actionLayout.horizontalSpacing = 0;
    Composite actionArea = new Composite(this, SWT.NONE);
    actionArea.setLayout(actionLayout);
    actionArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    UiUtils.useParentBackground(actionArea);

    Button continueButton = new Button(actionArea, SWT.PUSH);
    continueButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    continueButton.setText("Continue");
    continueButton.addListener(SWT.Selection, e -> {
      this.toolConfirmationFuture.complete(new LanguageModelToolConfirmationResult(ToolConfirmationResult.ACCEPT));

      // Store parent reference before disposal
      Composite parent = this.getParent();
      this.dispose();
      // Check if parent is still valid before using it
      if (parent != null && !parent.isDisposed()) {
        parent.layout();
      }
    });

    Button cancelButton = new Button(actionArea, SWT.PUSH);
    cancelButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    cancelButton.setText("Cancel");
    cancelButton.addListener(SWT.Selection, e -> {
      cancelConfirmation();
    });
  }

  /**
   * Get the future that will be completed when the user makes a choice.
   *
   * @return CompletableFuture containing the result of user's choice
   */
  public CompletableFuture<LanguageModelToolConfirmationResult> getConfirmationFuture() {
    return toolConfirmationFuture;
  }

  /**
   * Cancels the current tool confirmation dialog programmatically. This has the same effect as clicking the Cancel
   * button in the confirmation dialog.
   */
  public void cancelConfirmation() {
    if (toolConfirmationFuture != null && !toolConfirmationFuture.isDone()) {
      toolConfirmationFuture.complete(new LanguageModelToolConfirmationResult(ToolConfirmationResult.DISMISS));

      // Store parent reference before disposal
      Composite parent = this.getParent();
      SwtUtils.invokeOnDisplayThread(() -> {
        // Render a tool invocation cancel message
        new AgentToolCancelLabel(this.getParent(), SWT.NONE, this.cancelMessage);
        this.dispose();
        // Check if parent is still valid before using it
        if (parent != null && !parent.isDisposed()) {
          parent.layout();
        }
      }, this);
    }
  }
}

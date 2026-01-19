package com.microsoft.copilot.eclipse.ui.chat;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult.ToolConfirmationResult;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Dialog to confirm tool execution.
 */
public class InvokeToolConfirmationDialog extends Composite {

  /**
   * The key for the explanation in the input map.
   */
  private static final String EXPLANATION_KEY = "explanation";

  /**
   * The key for the command in the input map.
   */
  private static final String COMMAND_KEY = "command";
  private CompletableFuture<LanguageModelToolConfirmationResult> toolConfirmationFuture;
  private String cancelMessage;
  private Label titleLbl;
  private Font boldFont;
  private Runnable titleFontChangeCallback;

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
    titleLbl = new Label(this, SWT.LEFT | SWT.WRAP);
    titleLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    titleLbl.setText(title);

    // Register for chat font updates via centralized service with callback for bold font
    titleFontChangeCallback = this::applyTitleFont;
    var chatServiceManager = CopilotUi.getPlugin().getChatServiceManager();
    if (chatServiceManager != null) {
      chatServiceManager.getChatFontService().registerCallback(titleFontChangeCallback);
    }

    titleLbl.addDisposeListener(e -> {
      if (this.boldFont != null) {
        this.boldFont.dispose();
      }
      if (titleFontChangeCallback != null && chatServiceManager != null) {
        chatServiceManager.getChatFontService().unregisterCallback(titleFontChangeCallback);
      }
    });

    // Confirmation message of the confirmation dialog
    Label messageLbl = new Label(this, SWT.LEFT | SWT.WRAP);
    GridData messageGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
    messageLbl.setLayoutData(messageGridData);
    messageLbl.setText(message);
    registerControlForFontUpdates(messageLbl);

    // More information about the tool invocation
    if (input != null) {
      // TODO: Improve the logic to show more information about the tool invocation when confirm with users. The
      // following code only works for the run in terminal tool.
      Map<String, Object> inputMap = (Map<String, Object>) input;
      if (inputMap.containsKey(COMMAND_KEY)) {
        // Create a scrollable container for the command text
        ScrolledComposite commandScroll = new ScrolledComposite(this, SWT.H_SCROLL);
        commandScroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        commandScroll.setExpandHorizontal(true);
        commandScroll.setExpandVertical(true);

        Label commandLbl = new Label(commandScroll, SWT.LEFT);
        String command = (String) inputMap.get(COMMAND_KEY);
        // Escape & characters that are followed by non-space characters, needed for SWT labels where & is used as a
        // mnemonic character
        String escapedCommand = command.replace("&", "&&");
        commandLbl.setText(escapedCommand);
        commandLbl.setData(CssConstants.CSS_CLASS_NAME_KEY, "bg-command-panel");
        this.cancelMessage = escapedCommand;
        registerControlForFontUpdates(commandLbl);

        commandScroll.setContent(commandLbl);
        commandScroll.addControlListener(new ControlAdapter() {
          @Override
          public void controlResized(ControlEvent e) {
            Point size = commandLbl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            commandLbl.setSize(size);
            commandScroll.setMinSize(size);
          }
        });
        // Initial size computation
        Point size = commandLbl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        commandLbl.setSize(size);
        commandScroll.setMinSize(size);
      }

      if (inputMap.containsKey(EXPLANATION_KEY)) {
        Label explanationLbl = new Label(this, SWT.LEFT | SWT.WRAP);
        explanationLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        explanationLbl.setText((String) inputMap.get(EXPLANATION_KEY));
        registerControlForFontUpdates(explanationLbl);
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
    actionLayout.marginHeight = 0;
    Composite actionArea = new Composite(this, SWT.NONE);
    actionArea.setLayout(actionLayout);
    actionArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

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
    continueButton.setData(CssConstants.CSS_CLASS_NAME_KEY, "btn-primary");
    registerControlForFontUpdates(continueButton);

    Button cancelButton = new Button(actionArea, SWT.PUSH);
    cancelButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    cancelButton.setText("Cancel");
    cancelButton.addListener(SWT.Selection, e -> {
      cancelConfirmation();
    });
    registerControlForFontUpdates(cancelButton);
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
        // Only show the cancel widget for special cases when the tool has a parameter "command" in the input map
        if (StringUtils.isNotEmpty(this.cancelMessage)) {
          new AgentToolCancelLabel(this.getParent(), SWT.NONE, this.cancelMessage);
        }
        this.dispose();
        // Check if parent is still valid before using it
        if (parent != null && !parent.isDisposed()) {
          parent.layout();
        }
      }, this);
    }
  }

  /**
   * Apply the chat font (bold) to the title label.
   */
  private void applyTitleFont() {
    if (titleLbl == null || titleLbl.isDisposed()) {
      return;
    }
    // Dispose old font if exists
    if (this.boldFont != null) {
      this.boldFont.dispose();
    }
    // Create bold version of the chat font (or fallback font)
    this.boldFont = UiUtils.getBoldChatFont(this.getDisplay(), titleLbl.getFont());
    titleLbl.setFont(this.boldFont);
    titleLbl.requestLayout();
  }

  /**
   * Registers a control for chat font updates via the centralized ChatFontService.
   *
   * @param control the control to register
   */
  private void registerControlForFontUpdates(org.eclipse.swt.widgets.Control control) {
    var chatServiceManager = CopilotUi.getPlugin().getChatServiceManager();
    if (chatServiceManager != null) {
      chatServiceManager.getChatFontService().registerControl(control);
    }
  }
}

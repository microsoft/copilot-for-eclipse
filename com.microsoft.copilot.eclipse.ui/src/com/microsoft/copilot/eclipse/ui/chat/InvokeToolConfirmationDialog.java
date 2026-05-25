// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import com.microsoft.copilot.eclipse.core.chat.ConfirmationAction;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationContent;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult.ToolConfirmationResult;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.swt.SplitDropdownButton;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Dialog to confirm tool execution. Renders a title, message, optional command
 * block, and action buttons driven by {@link ConfirmationContent}. The primary
 * action is shown as a {@link SplitDropdownButton} with secondary actions in
 * the dropdown menu.
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

  /**
   * The key for the action in the input map (used by debugger tool).
   */
  private static final String ACTION_KEY = "action";

  private CompletableFuture<LanguageModelToolConfirmationResult> toolConfirmationFuture;
  private String cancelMessage;
  private Label titleLbl;
  private Font boldFont;
  private Runnable titleFontChangeCallback;
  private ConfirmationContent confirmationContent;
  private ConfirmationAction selectedAction;

  /**
   * Create a new confirmation dialog driven by {@link ConfirmationContent}.
   *
   * @param parent the parent composite
   * @param content confirmation content with title, message, and action buttons
   * @param input the input object to pass to the tool
   */
  public InvokeToolConfirmationDialog(Composite parent,
      ConfirmationContent content, Object input) {
    super(parent, SWT.BORDER | SWT.WRAP);
    this.setLayout(new GridLayout(1, false));
    this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    this.confirmationContent = content;
    createDialogContent(content.getTitle(), content.getMessage(), input);

    this.toolConfirmationFuture = new CompletableFuture<>();
  }

  /**
   * Returns the action the user selected, or {@code null} if dismissed.
   */
  public ConfirmationAction getSelectedAction() {
    return selectedAction;
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
   * Cancels the current tool confirmation dialog programmatically. This has
   * the same effect as clicking the Cancel / Skip button.
   */
  public void cancelConfirmation() {
    if (toolConfirmationFuture != null && !toolConfirmationFuture.isDone()) {
      toolConfirmationFuture.complete(
          new LanguageModelToolConfirmationResult(ToolConfirmationResult.DISMISS));

      Composite parent = this.getParent();
      SwtUtils.invokeOnDisplayThreadAsync(() -> {
        if (parent != null && !parent.isDisposed()
            && StringUtils.isNotEmpty(this.cancelMessage)) {
          new AgentToolCancelLabel(parent, SWT.NONE, this.cancelMessage);
        }
        this.dispose();
        if (parent != null && !parent.isDisposed()) {
          parent.requestLayout();
        }
      }, this);
    }
  }

  // --------------- content creation ---------------

  private void createDialogContent(String title, String message,
      Object input) {
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

    Label messageLbl = new Label(this, SWT.LEFT | SWT.WRAP);
    messageLbl.setLayoutData(
        new GridData(SWT.FILL, SWT.FILL, true, false));
    messageLbl.setText(message != null ? message : "");
    registerControlForFontUpdates(messageLbl);

    createInputContent(input);
    createActionButtons();
  }

  @SuppressWarnings("unchecked")
  private void createInputContent(Object input) {
    if (input == null) {
      return;
    }
    Map<String, Object> inputMap = (Map<String, Object>) input;

    if (inputMap.containsKey(ACTION_KEY)) {
      createScrollableCommand(formatDebuggerInput(inputMap),
          SWT.H_SCROLL | SWT.V_SCROLL);
    } else if (inputMap.containsKey(COMMAND_KEY)) {
      createScrollableCommand((String) inputMap.get(COMMAND_KEY),
          SWT.H_SCROLL);
    }

    if (inputMap.containsKey(EXPLANATION_KEY)) {
      Label explanationLbl = new Label(this, SWT.LEFT | SWT.WRAP);
      explanationLbl.setLayoutData(
          new GridData(SWT.FILL, SWT.FILL, true, false));
      explanationLbl.setText((String) inputMap.get(EXPLANATION_KEY));
      registerControlForFontUpdates(explanationLbl);
    }
  }

  private void createScrollableCommand(String text, int scrollStyle) {
    ScrolledComposite commandScroll =
        new ScrolledComposite(this, scrollStyle);
    commandScroll.setLayoutData(
        new GridData(SWT.FILL, SWT.FILL, true, false));
    commandScroll.setExpandHorizontal(true);
    commandScroll.setExpandVertical(true);

    Label commandLbl = new Label(commandScroll, SWT.LEFT);
    String escapedCommand = text.replace("&", "&&");
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
    Point size = commandLbl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    commandLbl.setSize(size);
    commandScroll.setMinSize(size);
  }

  // --------------- action buttons with dropdown ---------------

  private void createActionButtons() {
    List<ConfirmationAction> actions = confirmationContent.getActions();

    ConfirmationAction primaryAction = null;
    ConfirmationAction dismissAction = null;
    List<ConfirmationAction> dropdownActions = new ArrayList<>();

    for (ConfirmationAction action : actions) {
      if (!action.isAccept()) {
        dismissAction = action;
      } else if (action.isPrimary()) {
        primaryAction = action;
      } else {
        dropdownActions.add(action);
      }
    }

    if (primaryAction == null) {
      return;
    }

    // Column count: primary dropdown button + dismiss
    Composite actionArea = newButtonArea(2);

    // --- primary dropdown button ---
    SplitDropdownButton primaryDropdown =
        new SplitDropdownButton(actionArea, SWT.PUSH);
    primaryDropdown.setText(primaryAction.getLabel());
    primaryDropdown.setShowArrow(!dropdownActions.isEmpty());
    primaryDropdown.setSeparatorColor(
        getDisplay().getSystemColor(SWT.COLOR_WHITE));

    Button primaryBtn = primaryDropdown.getButton();
    primaryBtn.setData(CssConstants.CSS_CLASS_NAME_KEY, "btn-primary");
    primaryBtn.setLayoutData(
        new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    registerControlForFontUpdates(primaryBtn);

    final ConfirmationAction primaryRef = primaryAction;
    primaryDropdown.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (e.detail == SWT.ARROW && !dropdownActions.isEmpty()) {
          Menu menu = new Menu(primaryBtn.getShell(), SWT.POP_UP);
          for (ConfirmationAction action : dropdownActions) {
            MenuItem item = new MenuItem(menu, SWT.PUSH);
            item.setText(action.getLabel());
            item.addListener(SWT.Selection,
                ev -> acceptAndDispose(action));
          }
          menu.addListener(SWT.Hide, ev -> {
            ev.display.asyncExec(menu::dispose);
          });
          Rectangle bounds = primaryBtn.getBounds();
          Point loc = primaryBtn.getParent()
              .toDisplay(bounds.x, bounds.y + bounds.height);
          menu.setLocation(loc);
          menu.setVisible(true);
        } else {
          acceptAndDispose(primaryRef);
        }
      }
    });

    // --- dismiss (skip) button ---
    Button dismissBtn = new Button(actionArea, SWT.PUSH);
    dismissBtn.setLayoutData(
        new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    dismissBtn.setText(
        dismissAction != null ? dismissAction.getLabel()
            : Messages.confirmation_action_skip);
    registerControlForFontUpdates(dismissBtn);

    final ConfirmationAction dismissRef = dismissAction;
    dismissBtn.addListener(SWT.Selection, e -> {
      this.selectedAction = dismissRef;
      cancelConfirmation();
    });
  }

  // --------------- helpers ---------------

  private Composite newButtonArea(int columns) {
    GridLayout layout = new GridLayout(columns, false);
    layout.marginLeft = 0;
    layout.marginRight = 0;
    layout.marginWidth = 0;
    layout.horizontalSpacing = 0;
    layout.marginHeight = 0;

    Composite area = new Composite(this, SWT.NONE);
    area.setLayout(layout);
    area.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    return area;
  }

  private void acceptAndDispose(ConfirmationAction action) {
    this.selectedAction = action;
    this.toolConfirmationFuture.complete(
        new LanguageModelToolConfirmationResult(
            ToolConfirmationResult.ACCEPT));

    Composite parent = this.getParent();
    this.dispose();
    if (parent != null && !parent.isDisposed()) {
      parent.requestLayout();
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

  /**
   * Formats the debugger tool input map into a readable string.
   *
   * @param inputMap the input parameters from the debugger tool
   * @return formatted string with all parameters
   */
  private String formatDebuggerInput(Map<String, Object> inputMap) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(inputMap);
  }
}

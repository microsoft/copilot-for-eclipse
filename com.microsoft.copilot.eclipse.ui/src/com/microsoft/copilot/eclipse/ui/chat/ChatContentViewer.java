package com.microsoft.copilot.eclipse.ui.chat;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.WorkDoneProgressKind;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Widget to display chat content.
 */
public class ChatContentViewer extends ScrolledComposite {

  private ChatServiceManager serviceManager;

  private Composite cmpContent;

  private Map<String, TurnWidget> turns;
  private Composite warnWidget;
  private Composite errorWidget;

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public ChatContentViewer(Composite parent, int style, ChatServiceManager serviceManager) {
    super(parent, style | SWT.V_SCROLL);
    this.setBackground(this.getParent().getBackground());
    this.cmpContent = new Composite(this, SWT.NONE);
    this.cmpContent.setBackground(this.getBackground());
    GridLayout gl = new GridLayout(1, true);
    gl.marginHeight = 0;
    gl.marginWidth = 0;
    this.cmpContent.setLayout(gl);
    this.cmpContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    this.setContent(this.cmpContent);

    this.setExpandHorizontal(true);
    this.setExpandVertical(true);
    this.setLayout(new GridLayout(1, true));
    this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    this.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        refreshScrollerLayout();
      }
    });

    this.turns = new HashMap<>();

    this.serviceManager = serviceManager;
  }

  /**
   * Should be called when user sends a message.
   */
  public void startNewTurn(String workDoneToken, String message) {
    TurnWidget turnWidget = new TurnWidget(cmpContent, SWT.NONE, this.serviceManager, workDoneToken, false);
    turns.put(workDoneToken, turnWidget);
    turnWidget.appendMessage(message);
    turnWidget.notifyTurnEnd();
    refreshScrollerLayout();
    scrollToBottom();
  }

  /**
   * Create a new turn.
   */
  public void createNewTurn(String workDoneToken, boolean isCopilot) {
    SwtUtils.invokeOnDisplayThread(() -> {
      TurnWidget turnWidget = new TurnWidget(cmpContent, SWT.NONE, this.serviceManager, workDoneToken, isCopilot);
      turns.put(workDoneToken, turnWidget);
    }, this);

  }

  /**
   * Process turn event.
   */
  public void processTurnEvent(ChatProgressValue value) {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (!turns.containsKey(value.getTurnId())) {
        CopilotCore.LOGGER.error(new IllegalStateException("turnId not found: " + value.getTurnId()));
        return;
      }
      TurnWidget turnWidget = turns.get(value.getTurnId());
      if (turnWidget == null) {
        CopilotCore.LOGGER.error(new IllegalStateException("TurnWidget is null when event comes."));
        return;
      }
      if (value.getKind() == WorkDoneProgressKind.report) {
        turnWidget.appendMessage(value.getReply());
      } else if (value.getKind() == WorkDoneProgressKind.end) {
        turnWidget.notifyTurnEnd();
      }
      refreshScrollerLayout();
      String message = value.getErrorMessage();
      String reason = value.getErrorReason();
      if (StringUtils.isNotEmpty(reason) && reason.equals("model_not_supported")) {
        // TODO: add enable button for better UX.
        message = Messages.chat_model_unsupported_message;
      }
      if (StringUtils.isNotEmpty(message)) {
        renderWarnMessageWithUpgradePlanButton(message, value.getCode());
      }
    }, this);
  }

  private void renderWarnMessageWithUpgradePlanButton(String errorMessage, int code) {
    if (this.warnWidget != null) {
      this.warnWidget.dispose();
    }
    this.warnWidget = new WarnWidget(cmpContent, SWT.BOTTOM, errorMessage, code);
    refreshScrollerLayout();
    scrollToBottom();
  }

  /**
   * Render error message banner on the chat content viewer.
   */
  public void renderErrorMessage(String errorMessage) {
    if (this.errorWidget != null) {
      this.errorWidget.dispose();
    }
    this.errorWidget = new ErrorWidget(cmpContent, SWT.BOTTOM, errorMessage);
    refreshScrollerLayout();
    scrollToBottom();
  }

  /**
   * Update the size of scrolled composite when there are content updates.
   */
  private void refreshScrollerLayout() {
    if (this.isDisposed()) {
      return;
    }

    Rectangle clientArea = this.getClientArea();
    Point containerSize = cmpContent.computeSize(clientArea.width, SWT.DEFAULT);
    this.setMinSize(containerSize);
    this.layout(true, true);
  }

  /**
   * Scroll to the bottom.
   */
  private void scrollToBottom() {
    ScrollBar verticalBar = this.getVerticalBar();
    if (verticalBar != null) {
      this.setOrigin(0, verticalBar.getMaximum());
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    for (TurnWidget turn : turns.values()) {
      turn.dispose();
    }
    turns.clear();
    if (this.warnWidget != null) {
      this.warnWidget.dispose();
    }
    if (this.errorWidget != null) {
      this.errorWidget.dispose();
    }
  }
}

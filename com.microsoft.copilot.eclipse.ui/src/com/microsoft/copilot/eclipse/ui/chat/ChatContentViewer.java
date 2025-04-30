package com.microsoft.copilot.eclipse.ui.chat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Widget to display chat content.
 */
public class ChatContentViewer extends ScrolledComposite {

  private ChatServiceManager serviceManager;

  private Composite cmpContent;

  private Map<String, BaseTurnWidget> turns;
  private Composite warnWidget;
  private Composite errorWidget;

  private BaseTurnWidget latestUserTurn;
  private BaseTurnWidget latestCopilotTurn;

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
    BaseTurnWidget turnWidget = createNewTurn(workDoneToken, false);
    turnWidget.appendMessage(message);
    turnWidget.notifyTurnEnd();

    refreshScrollerLayout();
    scrollToLatestUserTurn();
  }

  /**
   * Create a new turn.
   */
  public BaseTurnWidget createNewTurn(String workDoneToken, boolean isCopilot) {
    AtomicReference<BaseTurnWidget> ref = new AtomicReference<>();
    SwtUtils.invokeOnDisplayThread(() -> {
      BaseTurnWidget turnWidget;
      if (isCopilot) {
        turnWidget = new CopilotTurnWidget(cmpContent, SWT.NONE, this.serviceManager, workDoneToken);
        this.latestCopilotTurn = turnWidget;
      } else {
        turnWidget = new UserTurnWidget(cmpContent, SWT.NONE, this.serviceManager, workDoneToken);
        this.latestUserTurn = turnWidget;
        this.latestCopilotTurn = null;
      }
      ref.set(turnWidget);
      turns.put(workDoneToken, turnWidget);
    }, this);

    return ref.get();

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
      BaseTurnWidget turnWidget = turns.get(value.getTurnId());
      if (turnWidget == null) {
        CopilotCore.LOGGER.error(new IllegalStateException("TurnWidget is null when event comes."));
        return;
      }

      if (value.getKind() == WorkDoneProgressKind.report) {
        ChatServiceManager chatServiceManager = CopilotUi.getPlugin().getChatServiceManager();
        boolean isAgentMode = chatServiceManager != null
            && ChatMode.Agent.equals(chatServiceManager.getUserPreferenceService().getActiveChatMode());

        if (isAgentMode && value.getAgentRounds() != null && !value.getAgentRounds().isEmpty()) {
          // Handle agent mode responses
          if (value.getAgentRounds().get(0).getReply() != null) {
            turnWidget.appendMessage(value.getAgentRounds().get(0).getReply());
          }

          if (value.getAgentRounds().get(0).getToolCalls() != null
              && !value.getAgentRounds().get(0).getToolCalls().isEmpty()) {
            turnWidget.appendToolCallStatus(value.getAgentRounds().get(0).getToolCalls().get(0));
          }
        } else {
          // Handle chat mode responses
          turnWidget.appendMessage(value.getReply());
        }
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

  /**
   * Get an existed turn widget by turn ID.
   */
  public BaseTurnWidget getTurnWidget(String turnId) {
    return turns.get(turnId);
  }

  private void renderWarnMessageWithUpgradePlanButton(String errorMessage, int code) {
    if (this.warnWidget != null) {
      this.warnWidget.dispose();
    }
    this.warnWidget = new WarnWidget(cmpContent, SWT.BOTTOM, errorMessage, code);
    refreshScrollerLayout();
    scrollToLatestUserTurn();
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
    scrollToLatestUserTurn();
  }

  /**
   * Update the size of scrolled composite when there are content updates.
   */
  public void refreshScrollerLayout() {
    if (this.isDisposed()) {
      return;
    }

    Rectangle clientArea = this.getClientArea();
    Point containerSize = cmpContent.computeSize(clientArea.width, SWT.DEFAULT);

    // Use the default size as a fallback
    if (latestUserTurn == null) {
      this.setMinSize(containerSize);
      return;
    }

    Point userTurnSize = latestUserTurn.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    Point copilotTurnSize = latestCopilotTurn == null ? new Point(0, 0)
        : latestCopilotTurn.computeSize(SWT.DEFAULT, SWT.DEFAULT);

    // Calculate the content height, so that the latest user turn is able to be put at the top of the client area.
    int contentHeight = 0;
    int roundedHeight = userTurnSize.y + copilotTurnSize.y;
    if (roundedHeight < clientArea.height) {
      contentHeight = clientArea.height + containerSize.y - roundedHeight;
    } else {
      contentHeight = containerSize.y;
    }

    this.setMinHeight(contentHeight);
    this.setMinWidth(containerSize.x);
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

  /**
   * Scroll to the latest user turn. It will be put at the top of the client area.
   */
  private void scrollToLatestUserTurn() {
    // Scroll to the bottom as a fallback.
    if (latestUserTurn == null) {
      scrollToBottom();
      return;
    }

    // Wait for layout to complete to get accurate positions
    SwtUtils.invokeOnDisplayThread(() -> {
      Point turnLocation = latestUserTurn.getLocation();
      this.setOrigin(0, turnLocation.y);
    }, this);
  }

  @Override
  public void dispose() {
    super.dispose();
    for (BaseTurnWidget turn : turns.values()) {
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

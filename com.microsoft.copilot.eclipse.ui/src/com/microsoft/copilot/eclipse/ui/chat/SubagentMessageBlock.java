package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentToolCall;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;

/**
 * A composite widget that displays subagent messages in a contained block.
 * This widget acts as a visual container for subagent execution, separating
 * subagent messages from regular conversation flow.
 */
public class SubagentMessageBlock extends Composite {
  private ChatServiceManager serviceManager;
  private String turnId;
  private Composite contentArea;
  private AgentToolCall toolCall;
  
  // Track the current content widget for message processing
  private BaseTurnWidget currentSubagentTurnWidget;

  /**
   * Create the subagent message block.
   *
   * @param parent the parent composite
   * @param style the style
   * @param serviceManager the chat service manager
   * @param turnId the turn ID
   * @param toolCall the tool call that triggered the subagent
   */
  public SubagentMessageBlock(Composite parent, int style, ChatServiceManager serviceManager, String turnId,
      AgentToolCall toolCall) {
    super(parent, style | SWT.BORDER);
    this.serviceManager = serviceManager;
    this.turnId = turnId;
    this.toolCall = toolCall;
    
    // Set CSS class for styling
    this.setData(CssConstants.CSS_CLASS_NAME_KEY, "subagent-message-block");
    
    // Layout for the block
    GridLayout layout = new GridLayout(1, true);
    layout.marginWidth = 8;
    layout.marginHeight = 8;
    layout.verticalSpacing = 0;
    setLayout(layout);
    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    
    // Create content area
    contentArea = new Composite(this, SWT.NONE);
    GridLayout contentLayout = new GridLayout(1, true);
    contentLayout.marginWidth = 0;
    contentLayout.marginHeight = 0;
    contentLayout.verticalSpacing = 0;
    contentArea.setLayout(contentLayout);
    contentArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    
    // Create the internal turn widget to handle messages
    currentSubagentTurnWidget = new SubagentTurnWidget(contentArea, SWT.NONE, serviceManager, turnId, toolCall);
  }

  /**
   * Append a message to the subagent block.
   *
   * @param message the message to append
   */
  public void appendMessage(String message) {
    if (currentSubagentTurnWidget != null) {
      currentSubagentTurnWidget.appendMessage(message);
    }
  }

  /**
   * Append a tool call status to the subagent block.
   *
   * @param toolCall the tool call to append
   */
  public void appendToolCallStatus(AgentToolCall toolCall) {
    if (currentSubagentTurnWidget != null) {
      currentSubagentTurnWidget.appendToolCallStatus(toolCall);
    }
  }

  /**
   * Notify the end of the subagent turn.
   */
  public void notifyTurnEnd() {
    if (currentSubagentTurnWidget != null) {
      currentSubagentTurnWidget.notifyTurnEnd();
    }
  }

  /**
   * Get the internal turn widget.
   *
   * @return the subagent turn widget
   */
  public BaseTurnWidget getSubagentTurnWidget() {
    return currentSubagentTurnWidget;
  }
}

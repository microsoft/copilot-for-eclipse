// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.GenerateThinkingTitleParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.Thinking;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ThinkingBlockData;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Base class for turn widgets that support thinking blocks (Copilot and subagent turns).
 */
public abstract class ThinkingTurnWidget extends BaseTurnWidget {

  private ThinkingBlock currentBlock;
  private String conversationId;

  /**
   * The server-assigned turn ID for persistence. Needed because widget turnId may not match the
   * persistence key (e.g. SubagentTurnWidget uses toolCallId + suffix, not the server turn ID).
   */
  private String persistTurnId;

  /** Construct a turn widget that supports streaming thinking blocks. */
  protected ThinkingTurnWidget(Composite parent, int style, ChatServiceManager serviceManager, String turnId,
      String overrideRoleName) {
    super(parent, style, serviceManager, turnId, true, overrideRoleName);
  }

  @Override
  public ThinkingTurnWidget getActiveTurnWidget() {
    return (ThinkingTurnWidget) super.getActiveTurnWidget();
  }

  /**
   * Set the conversation ID and server-assigned turn ID for thinking-block persistence.
   * Sets on both this widget and the current active widget (if different) so that
   * sealThinking/cancel work regardless of which widget is active at call time.
   */
  public void setConversationContext(String conversationId, String persistTurnId) {
    this.conversationId = conversationId;
    this.persistTurnId = persistTurnId;
    ThinkingTurnWidget active = getActiveTurnWidget();
    if (active != null && active != this) {
      active.conversationId = conversationId;
      active.persistTurnId = persistTurnId;
    }
  }

  /**
   * Append a thinking stream fragment from the language server, routing to the active turn (parent or subagent).
   * Must be called on the UI thread.
   */
  public void appendThinking(Thinking thinking) {
    // Preserve whitespace-only thinking fragments; they can carry markdown boundaries between sections.
    if (thinking == null || StringUtils.isEmpty(thinking.text())) {
      return;
    }
    if (isDisposed()) {
      return;
    }
    ThinkingTurnWidget active = getActiveTurnWidget();
    if (active == null || active.isDisposed()) {
      return;
    }
    // Single source of truth: ThinkingBlock decides whether it can accept more thinking stream fragments (it can't
    // once sealed, completed, or cancelled). Any of those transitions must start a fresh block.
    if (active.currentBlock == null || active.currentBlock.isDisposed()
        || !active.currentBlock.isAcceptingThinkStream()) {
      active.currentBlock = new ThinkingBlock(active, SWT.NONE);
    }
    active.currentBlock.appendText(thinking.text());
  }

  /**
   * Seal the active thinking block and asynchronously fetch a title to finalize it.
   * Must be called on the UI thread. Requires {@link #setConversationContext} to have been called first.
   */
  public void sealThinking() {
    if (isDisposed()) {
      return;
    }
    ThinkingTurnWidget active = getActiveTurnWidget();
    if (active == null || active.isDisposed()) {
      return;
    }
    if (active.persistTurnId == null) {
      return;
    }
    ThinkingBlock target = active.currentBlock;
    // Skip when the block is no longer accepting thinking stream fragments (already sealed, completed, or cancelled)
    // so we don't fire a stale generateTitle request whose response would be discarded.
    if (target == null || target.isDisposed() || !target.isAcceptingThinkStream()) {
      return;
    }
    String content = target.getAccumulatedText();
    if (StringUtils.isBlank(content)) {
      // Nothing to title; leave the block alone (still in-progress) so a later thinking stream fragment can keep
      // streaming.
      return;
    }
    CopilotLanguageServerConnection ls = CopilotCore.getPlugin().getCopilotLanguageServer();
    target.markSealed();
    if (ls == null) {
      target.showCompleted();
      requestLayout();
      return;
    }
    String[] titles = target.getExtractedTitles();
    // Server schema rejects null entries inside extractedTitles, so we send one of the two fields, never both.
    boolean hasTitles = titles.length > 0;
    GenerateThinkingTitleParams params = new GenerateThinkingTitleParams(hasTitles ? null : content,
        hasTitles ? titles : null);
    String thinkingBlockId = target.getThinkingId();
    ls.generateThinkingTitle(params)
        .thenAccept(resp -> SwtUtils.invokeOnDisplayThread(() -> {
          if (isDisposed() || target.isDisposed() || target.isFinalized()) {
            return;
          }
          if (resp != null && StringUtils.isNotBlank(resp.title())) {
            target.setTitle(resp.title());
            persistThinkingTitle(active.conversationId, active.persistTurnId, thinkingBlockId, resp.title());
          }
          target.showCompleted();
          requestLayout();
        }, this))
        .exceptionally(ex -> {
          SwtUtils.invokeOnDisplayThreadAsync(() -> {
            if (!isDisposed() && !target.isDisposed() && !target.isFinalized()) {
              target.showCompleted();
              requestLayout();
            }
          }, this);
          return null;
        });
  }

  /** Returns the active thinking block ID for persistence context, or {@code null} if none exists. */
  public String getActiveThinkingBlockId() {
    ThinkingTurnWidget active = getActiveTurnWidget();
    if (active == null || active.isDisposed() || active.currentBlock == null || active.currentBlock.isDisposed()) {
      return null;
    }
    return active.currentBlock.getThinkingId();
  }

  @Override
  protected void onChatMessageCancelled() {
    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      if (isDisposed()) {
        return;
      }
      ThinkingTurnWidget active = getActiveTurnWidget();
      if (active == null || active.isDisposed()) {
        return;
      }
      if (active.currentBlock != null && !active.currentBlock.isDisposed()
          && !active.currentBlock.isFinalized()) {
        boolean cancelled = active.currentBlock.showCancelled();
        if (cancelled && active.persistTurnId != null) {
          active.cancelThinkingBlock(active.persistTurnId, active.currentBlock.getThinkingId());
        }
        active.requestLayout();
      }
    }, this);
  }

  private void cancelThinkingBlock(String cancelTurnId, String thinkingBlockId) {
    if (conversationId == null || serviceManager == null) {
      return;
    }
    serviceManager.getPersistenceManager()
        .cancelThinkingBlock(conversationId, cancelTurnId, thinkingBlockId);
  }

  private void persistThinkingTitle(String conversationId, String persistTurnId, String thinkingBlockId, String title) {
    if (conversationId == null || serviceManager == null) {
      return;
    }
    serviceManager.getPersistenceManager()
        .updateThinkingBlockTitle(conversationId, persistTurnId, thinkingBlockId, title);
  }

  /**
   * Restore a completed thinking block from persisted data. Creates a ThinkingBlock that is
   * already in the completed state with the given content and title.
   */
  public void restoreThinkingBlock(ThinkingBlockData data) {
    if (isDisposed() || data == null || StringUtils.isBlank(data.getContent())) {
      return;
    }
    ThinkingBlock block = new ThinkingBlock(this, SWT.NONE);
    block.appendText(data.getContent());
    if (data.isCancelled()) {
      block.showCancelled();
    } else {
      block.markSealed();
      if (StringUtils.isNotBlank(data.getTitle())) {
        block.setTitle(data.getTitle());
      }
      block.showCompleted();
    }
  }
}

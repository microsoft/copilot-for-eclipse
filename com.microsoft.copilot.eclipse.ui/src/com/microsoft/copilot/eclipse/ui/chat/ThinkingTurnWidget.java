// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.GenerateThinkingTitleParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.Thinking;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Base class for turn widgets that support thinking blocks (Copilot and subagent turns).
 */
public abstract class ThinkingTurnWidget extends BaseTurnWidget {

  private ThinkingBlock currentBlock;
  /** True once {@link #sealThinking()} has fired for {@link #currentBlock}; the next delta will start a new block. */
  private boolean sealed;

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
   * Append a thinking delta from the language server, routing to the active turn (parent or subagent).
   * Must be called on the UI thread.
   */
  public void appendThinking(Thinking thinking) {
    if (thinking == null || StringUtils.isBlank(thinking.text())) {
      return;
    }
    if (isDisposed()) {
      return;
    }
    ThinkingTurnWidget active = getActiveTurnWidget();
    if (active == null || active.isDisposed()) {
      return;
    }
    if (active.currentBlock == null || active.currentBlock.isDisposed() || active.sealed) {
      active.currentBlock = new ThinkingBlock(active, SWT.NONE);
      active.sealed = false;
    }
    active.currentBlock.appendText(thinking.text());
  }

  /**
   * Seal the active thinking block and asynchronously fetch a title to finalize it.
   * Must be called on the UI thread.
   */
  public void sealThinking() {
    if (isDisposed()) {
      return;
    }
    ThinkingTurnWidget active = getActiveTurnWidget();
    if (active == null || active.isDisposed()) {
      return;
    }
    ThinkingBlock target = active.currentBlock;
    // Skip when already sealed, or when a prior cancel has already finalized the block (so we don't fire a stale
    // generateTitle request whose response would be discarded).
    if (target == null || target.isDisposed() || active.sealed || target.isFinalized()) {
      return;
    }
    String content = target.getAccumulatedText();
    if (StringUtils.isBlank(content)) {
      // Nothing to title; leave the block alone (still in-progress) so a later delta can keep streaming.
      return;
    }
    active.sealed = true;
    String[] titles = target.getExtractedTitles();
    // Server schema rejects null entries inside extractedTitles, so we send one of the two fields, never both.
    boolean hasTitles = titles.length > 0;
    GenerateThinkingTitleParams params = new GenerateThinkingTitleParams(hasTitles ? null : content,
        hasTitles ? titles : null);
    CopilotCore.getPlugin().getCopilotLanguageServer().generateThinkingTitle(params)
        .thenAccept(resp -> SwtUtils.invokeOnDisplayThread(() -> {
          if (target.isDisposed() || target.isFinalized()) {
            return;
          }
          if (resp != null && StringUtils.isNotBlank(resp.title())) {
            target.showCompleted(resp.title());
          } else {
            // Title fetch failed: surface the cancelled visual state so the spinner does not run forever.
            target.showCancelled();
          }
          requestLayout();
        }, this));
  }

  @Override
  protected void onChatMessageCancelled() {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (currentBlock != null && !currentBlock.isDisposed() && !currentBlock.isFinalized()) {
        currentBlock.showCancelled();
        requestLayout();
      }
    }, this);
  }
}

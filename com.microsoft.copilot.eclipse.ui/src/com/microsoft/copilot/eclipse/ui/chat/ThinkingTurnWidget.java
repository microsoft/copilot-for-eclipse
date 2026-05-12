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
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Base class for turn widgets that support thinking blocks (Copilot and subagent turns).
 */
public abstract class ThinkingTurnWidget extends BaseTurnWidget {

  private ThinkingBlock currentBlock;

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
   * Append a thinking stream fragment from the language server, routing to the active turn (parent or subagent).
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
    if (ls == null) {
      target.showCancelled();
      requestLayout();
      return;
    }
    target.markSealed();
    String[] titles = target.getExtractedTitles();
    // Server schema rejects null entries inside extractedTitles, so we send one of the two fields, never both.
    boolean hasTitles = titles.length > 0;
    GenerateThinkingTitleParams params = new GenerateThinkingTitleParams(hasTitles ? null : content,
        hasTitles ? titles : null);
    ls.generateThinkingTitle(params)
        .thenAccept(resp -> SwtUtils.invokeOnDisplayThread(() -> {
          if (isDisposed() || target.isDisposed() || target.isFinalized()) {
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
    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      if (isDisposed()) {
        return;
      }
      if (currentBlock != null && !currentBlock.isDisposed() && !currentBlock.isFinalized()) {
        currentBlock.showCancelled();
        requestLayout();
      }
    }, this);
  }
}

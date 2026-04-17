// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.contextwindow;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ContextSizeInfo;
import com.microsoft.copilot.eclipse.ui.chat.BaseHoverPopup;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.ContextWindowBar;

/**
 * Popup shell that displays context window token usage breakdown. Layout and color follow
 * {@code ModelHoverContentProvider}.
 */
class ContextWindowPopup extends BaseHoverPopup {

  private final ContextWindowService contextWindowService;

  private Label tokenUsageLabel;
  private Label percentageLabel;
  private ContextWindowBar progressBar;
  private Label systemInstructionsValue;
  private Label toolDefinitionsValue;
  private Label messagesValue;
  private Label attachedFilesValue;
  private Label toolResultsValue;

  private ContextSizeInfo latestInfo;

  ContextWindowPopup(ContextWindowService service) {
    super();
    this.contextWindowService = service;
    this.contextWindowService.bindContextWindowPopup(this);
  }

  public void open(Control anchor) {
    this.anchor = anchor;
    if (isOpen()) {
      return;
    }
    ContextSizeInfo info = contextWindowService.getState();
    if (info == null) {
      return;
    }
    this.latestInfo = info;
    openPopup(anchor);
  }

  public void dispose() {
    close();
    contextWindowService.unbindContextWindowPopup(this);
  }

  @Override
  protected void populateContent(Composite parent) {
    addSectionHeader(parent, Messages.context_window_title, 0);

    Composite tokenRow = createRowComposite(parent);
    tokenUsageLabel = createSecondaryTextLabel(tokenRow, formatTokenRow(latestInfo));
    tokenUsageLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    percentageLabel = createSecondaryTextLabel(tokenRow,
        formatPercentage(latestInfo.utilizationPercentage()));
    percentageLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false));

    progressBar = new ContextWindowBar(parent, SWT.NONE);
    progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    progressBar.setPercentage((int) Math.round(latestInfo.utilizationPercentage()));

    addSeparator(parent, SECTION_SPACING);

    addSectionHeader(parent, Messages.context_window_system, SECTION_SPACING);
    systemInstructionsValue = addKeyValueRow(parent, Messages.context_window_system_instructions,
        percentageOf(latestInfo.systemPromptTokens(), latestInfo.totalTokenLimit()));
    toolDefinitionsValue = addKeyValueRow(parent, Messages.context_window_tool_definitions,
        percentageOf(latestInfo.toolDefinitionTokens(), latestInfo.totalTokenLimit()));

    addSeparator(parent, SECTION_SPACING);

    addSectionHeader(parent, Messages.context_window_user_context, SECTION_SPACING);
    messagesValue = addKeyValueRow(parent, Messages.context_window_messages,
        percentageOf(latestInfo.userMessagesTokens() + latestInfo.assistantMessagesTokens(),
            latestInfo.totalTokenLimit()));
    attachedFilesValue = addKeyValueRow(parent, Messages.context_window_files,
        percentageOf(latestInfo.attachedFilesTokens(), latestInfo.totalTokenLimit()));
    toolResultsValue = addKeyValueRow(parent, Messages.context_window_tool_results,
        percentageOf(latestInfo.toolResultsTokens(), latestInfo.totalTokenLimit()));
  }

  public void onContextSizeInfoChanged(ContextSizeInfo info) {
    if (info == null) {
      close();
      return;
    }
    updateLabels(info);
  }

  private void updateLabels(ContextSizeInfo info) {
    if (shell == null || shell.isDisposed()) {
      return;
    }
    setLabelText(tokenUsageLabel, formatTokenRow(info));
    setLabelText(percentageLabel, formatPercentage(info.utilizationPercentage()));
    setLabelText(systemInstructionsValue,
        percentageOf(info.systemPromptTokens(), info.totalTokenLimit()));
    setLabelText(toolDefinitionsValue,
        percentageOf(info.toolDefinitionTokens(), info.totalTokenLimit()));
    setLabelText(messagesValue,
        percentageOf(info.userMessagesTokens() + info.assistantMessagesTokens(),
            info.totalTokenLimit()));
    setLabelText(attachedFilesValue,
        percentageOf(info.attachedFilesTokens(), info.totalTokenLimit()));
    setLabelText(toolResultsValue,
        percentageOf(info.toolResultsTokens(), info.totalTokenLimit()));
    if (progressBar != null && !progressBar.isDisposed()) {
      progressBar.setPercentage((int) Math.round(info.utilizationPercentage()));
    }
    shell.requestLayout();
  }

  private static void setLabelText(Label label, String text) {
    if (label != null && !label.isDisposed()) {
      label.setText(text);
    }
  }

  private static String formatTokens(int count) {
    if (count >= 1000) {
      double k = count / 1000.0;
      return String.format("%.1fK", k);
    }
    return String.valueOf(count);
  }

  private static String formatPercentage(double pct) {
    if (pct == 0) {
      return "0%";
    }
    return String.format("%.1f%%", pct);
  }

  private static String formatTokenRow(ContextSizeInfo info) {
    return MessageFormat.format(Messages.context_window_tokens,
        formatTokens(info.totalUsedTokens()), formatTokens(info.totalTokenLimit()));
  }

  private static String percentageOf(int tokens, int totalLimit) {
    if (totalLimit <= 0) {
      return "0%";
    }
    double pct = (double) tokens / totalLimit * 100;
    return formatPercentage(pct);
  }
}

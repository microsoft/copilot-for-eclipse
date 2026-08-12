// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.quickfix;

import java.util.function.Consumer;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.microsoft.copilot.eclipse.ui.CopilotImages;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

class CopilotQuickFixProposal implements ICompletionProposal {
  private final String prompt;
  private final Consumer<String> openChat;

  CopilotQuickFixProposal(String prompt) {
    this(prompt, QuickFixProcessorSupport::openChat);
  }

  CopilotQuickFixProposal(String prompt, Consumer<String> openChat) {
    this.prompt = prompt;
    this.openChat = openChat;
  }

  @Override
  public void apply(IDocument document) {
    openChat.accept(prompt);
  }

  @Override
  public Point getSelection(IDocument document) {
    return null;
  }

  @Override
  public String getAdditionalProposalInfo() {
    return null;
  }

  @Override
  public String getDisplayString() {
    return Messages.quickFix_fixWithCopilot;
  }

  @Override
  public Image getImage() {
    return CopilotImages.getImage(CopilotImages.IMG_GITHUB_COPILOT);
  }

  @Override
  public IContextInformation getContextInformation() {
    return null;
  }
}

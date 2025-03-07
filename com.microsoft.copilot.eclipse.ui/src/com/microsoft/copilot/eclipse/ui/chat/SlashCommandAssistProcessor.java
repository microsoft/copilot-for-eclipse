package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationTemplate;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.SlashCommandService;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

class SlashCommandAssistProcessor implements IContentAssistProcessor {
  private TextViewer input;
  private ChatServiceManager chatServiceManager;

  public SlashCommandAssistProcessor(TextViewer input, ChatServiceManager chatServiceManager) {
    this.input = input;
    this.chatServiceManager = chatServiceManager;
  }

  class SlashCommandProposal implements ICompletionProposal, ICompletionProposalExtension6 {
    private String name;
    private String description;

    public SlashCommandProposal(String name, String description) {
      this.name = name;
      this.description = description;
    }

    @Override
    public void apply(IDocument document) {
      StyledText styledText = input.getTextWidget();
      // Implement apply method
      int offset = styledText.getCaretOffset();
      int start = UiUtils.getFirstWordIndex(document.get()).x;
      String newText = "/" + name;
      try {
        document.replace(start, offset - start, newText);
      } catch (BadLocationException e) {
        CopilotCore.LOGGER.error(e);
      }
      styledText.setStyleRange(new StyleRange(start, newText.length(), UiUtils.SLASH_COMMAND_FORGROUND_COLOR,
          UiUtils.SLASH_COMMAND_BACKGROUND_COLOR, SWT.BOLD));
      styledText.setCaretOffset(start + newText.length());
    }

    @Override
    public String getAdditionalProposalInfo() {
      return "";
    }

    @Override
    public IContextInformation getContextInformation() {
      return null;
    }

    @Override
    public String getDisplayString() {
      return "/" + name;
    }

    @Override
    public Image getImage() {
      return null;
    }

    @Override
    public Point getSelection(IDocument document) {
      return null;
    }

    @Override
    public StyledString getStyledDisplayString() {
      StyledString styledString = new StyledString();
      styledString.append("/" + name);
      styledString.append(" - " + description, StyledString.QUALIFIER_STYLER);
      return styledString;
    }
  }

  public ICompletionProposal createCompletionProposal(ConversationTemplate template) {
    ICompletionProposal proposal = new SlashCommandProposal(template.getId(), template.getDescription());
    return proposal;
  }

  public ICompletionProposal[] createCopilotCompletionProposals(String prefix) {
    java.util.List<ICompletionProposal> proposals = new java.util.ArrayList<>();
    SlashCommandService slashCommandService = chatServiceManager.getSlashCommandService();
    if (!slashCommandService.isTempaltesReady()) {
      return new ICompletionProposal[0];
    }
    ConversationTemplate[] templates = slashCommandService.getTemplates();
    for (ConversationTemplate template : templates) {
      if (prefix.isEmpty() || template.getId().startsWith(prefix)) {
        proposals.add(createCompletionProposal(template));
      }
    }
    return proposals.toArray(new ICompletionProposal[proposals.size()]);
  }

  @Override
  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
    // Provide your completion proposals here
    try {
      IDocument document = viewer.getDocument();
      int line = document.getLineOfOffset(offset);
      int lineStartOffset = document.getLineOffset(line);
      String lineText = document.get(lineStartOffset, offset - lineStartOffset).trim();

      // Check if the "/" is at the beginning of the line
      if (lineText.startsWith("/")) {
        return createCopilotCompletionProposals(lineText.substring(1));
      }
    } catch (BadLocationException e) {
      CopilotCore.LOGGER.error(e);
    }
    return new ICompletionProposal[0];
  }

  @Override
  public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
    return new IContextInformation[0];
  }

  @Override
  public char[] getCompletionProposalAutoActivationCharacters() {
    return new char[] { '/' };
  }

  @Override
  public char[] getContextInformationAutoActivationCharacters() {
    return new char[] { '/' };
  }

  @Override
  public IContextInformationValidator getContextInformationValidator() {
    return null;
  }

  @Override
  public String getErrorMessage() {
    return null;
  }
}

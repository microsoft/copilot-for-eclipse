package com.microsoft.copilot.eclipse.ui.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationAgent;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationTemplate;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatCompletionService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

class ChatAssistProcessor implements IContentAssistProcessor {
  private TextViewer input;
  private ChatServiceManager chatServiceManager;

  public ChatAssistProcessor(TextViewer input, ChatServiceManager chatServiceManager) {
    this.input = input;
    this.chatServiceManager = chatServiceManager;
  }

  class ChatCompletionProposal implements ICompletionProposal, ICompletionProposalExtension6 {
    private String triggerCharacter;
    private String name;
    private String description;

    public ChatCompletionProposal(String mark, String name, String description) {
      this.triggerCharacter = mark;
      this.name = name;
      this.description = description;
    }

    @Override
    public void apply(IDocument document) {
      StyledText styledText = input.getTextWidget();
      // Implement apply method
      int offset = styledText.getCaretOffset();
      int start = UiUtils.getFirstWordIndex(document.get()).x;
      String newText = triggerCharacter + name;
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
      return triggerCharacter + name;
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
      styledString.append(triggerCharacter + name);
      styledString.append(" - " + description, StyledString.QUALIFIER_STYLER);
      return styledString;
    }
  }

  public ICompletionProposal[] createCopilotCompletionTemplateProposals(String prefix) {
    List<ICompletionProposal> proposals = new ArrayList<>();
    ChatCompletionService commandService = chatServiceManager.getChatCompletionService();
    if (!commandService.isTempaltesReady()) {
      return new ICompletionProposal[0];
    }
    // So far no template supports agent mode.
    if (Objects.equals(chatServiceManager.getUserPreferenceService().getActiveChatMode(), ChatMode.Agent)) {
      return new ICompletionProposal[0];
    }
    ConversationTemplate[] templates = commandService.getTemplates();
    for (ConversationTemplate template : templates) {
      if (prefix.isEmpty() || template.getId().startsWith(prefix)) {
        proposals.add(new ChatCompletionProposal(ChatCompletionService.TEMPLATE_MARK, template.getId(),
            template.getDescription()));
      }
    }
    return proposals.toArray(new ICompletionProposal[proposals.size()]);
  }

  public ICompletionProposal[] createCopilotCompletionAgentProposals(String prefix) {
    List<ICompletionProposal> proposals = new ArrayList<>();
    ChatCompletionService commandService = chatServiceManager.getChatCompletionService();
    if (!commandService.isAgentsReady()) {
      return new ICompletionProposal[0];
    }
    // So far no template supports agent mode.
    if (Objects.equals(chatServiceManager.getUserPreferenceService().getActiveChatMode(), ChatMode.Agent)) {
      return new ICompletionProposal[0];
    }
    ConversationAgent[] agents = commandService.getAgents();
    for (ConversationAgent agent : agents) {
      if (prefix.isEmpty() || agent.getSlug().startsWith(prefix)) {
        proposals
            .add(new ChatCompletionProposal(ChatCompletionService.AGENT_MARK, agent.getSlug(), agent.getDescription()));
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

      // Check if the "/" are at the beginning of the line
      if (lineText.startsWith(ChatCompletionService.TEMPLATE_MARK)) {
        return createCopilotCompletionTemplateProposals(lineText.substring(1));
      }

      // Check if the "@" are at the beginning of the line
      if (lineText.startsWith(ChatCompletionService.AGENT_MARK)) {
        return createCopilotCompletionAgentProposals(lineText.substring(1));
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
    return new char[] { '/', '@' };
  }

  @Override
  public char[] getContextInformationAutoActivationCharacters() {
    return new char[] { '/', '@' };
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

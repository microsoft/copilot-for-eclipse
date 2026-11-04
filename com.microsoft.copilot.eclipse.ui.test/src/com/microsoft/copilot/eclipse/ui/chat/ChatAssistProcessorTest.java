// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationTemplate;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.core.lsp.protocol.TemplateSource;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatCompletionService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

class ChatAssistProcessorTest {

  @Test
  void testContentAssistAutoActivationUsesSlashOnly() throws ReflectiveOperationException {
    // The UI test bundle uses a separate OSGi classloader, so reflection is required for this package-private class.
    Class<?> processorClass = Class.forName("com.microsoft.copilot.eclipse.ui.chat.ChatAssistProcessor");
    Constructor<?> constructor = processorClass.getDeclaredConstructor(TextViewer.class, ChatServiceManager.class);
    constructor.setAccessible(true);
    IContentAssistProcessor processor = (IContentAssistProcessor) constructor.newInstance(null, null);

    assertArrayEquals(new char[] { '/' }, processor.getCompletionProposalAutoActivationCharacters());
    assertArrayEquals(new char[] { '/' }, processor.getContextInformationAutoActivationCharacters());
  }

  @Test
  void testComputeCompletionProposals_promptFileShowsAndInsertsName() {
    ConversationTemplate promptTemplate = new ConversationTemplate(
        "file:///c%3A/repo/.github/prompts/review.prompt.md", "review", "", "", List.of(CopilotScope.AGENT_PANEL),
        TemplateSource.PROMPT);
    ChatServiceManager chatServiceManager = mockChatServiceManager(promptTemplate);
    AtomicReference<String> displayString = new AtomicReference<>();
    AtomicReference<String> styledDisplayString = new AtomicReference<>();
    AtomicReference<String> insertedText = new AtomicReference<>();

    SwtUtils.invokeOnDisplayThread(() -> {
      Shell shell = new Shell(Display.getDefault());
      try {
        TextViewer viewer = new TextViewer(shell, SWT.NONE);
        Document document = new Document("/rev");
        viewer.setDocument(document);
        viewer.getTextWidget().setCaretOffset(document.getLength());

        ICompletionProposal[] proposals = createProcessor(viewer, chatServiceManager)
            .computeCompletionProposals(viewer, document.getLength());
        displayString.set(proposals[0].getDisplayString());
        styledDisplayString.set(((ICompletionProposalExtension6) proposals[0]).getStyledDisplayString().getString());
        proposals[0].apply(document);
        insertedText.set(document.get());
      } finally {
        shell.dispose();
      }
    });

    assertEquals("/review", displayString.get());
    assertEquals("/review", styledDisplayString.get());
    assertEquals("/review", insertedText.get());
  }

  private static ChatServiceManager mockChatServiceManager(ConversationTemplate... templates) {
    ChatServiceManager chatServiceManager = mock(ChatServiceManager.class);
    ChatCompletionService completionService = mock(ChatCompletionService.class);
    UserPreferenceService userPreferenceService = mock(UserPreferenceService.class);
    when(chatServiceManager.getChatCompletionService()).thenReturn(completionService);
    when(chatServiceManager.getUserPreferenceService()).thenReturn(userPreferenceService);
    when(userPreferenceService.getActiveChatMode()).thenReturn(ChatMode.Agent);
    when(completionService.isTempaltesReady()).thenReturn(true);
    when(completionService.getFilteredTemplates(ChatMode.Agent)).thenReturn(templates);
    return chatServiceManager;
  }

  private static IContentAssistProcessor createProcessor(TextViewer viewer, ChatServiceManager chatServiceManager) {
    try {
      Class<?> processorClass = Class.forName("com.microsoft.copilot.eclipse.ui.chat.ChatAssistProcessor");
      Constructor<?> constructor = processorClass.getDeclaredConstructor(TextViewer.class, ChatServiceManager.class);
      constructor.setAccessible(true);
      return (IContentAssistProcessor) constructor.newInstance(viewer, chatServiceManager);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}

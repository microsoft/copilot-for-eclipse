// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.lang.reflect.Constructor;

import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;

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
}

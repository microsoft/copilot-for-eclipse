// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.ui.chat.services.ChatCompletionService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatFontService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

@ExtendWith(MockitoExtension.class)
class ChatInputTextViewerTests {

  @Mock
  private ChatServiceManager mockChatServiceManager;

  @Mock
  private UserPreferenceService mockUserPreferenceService;

  @Mock
  private ChatCompletionService mockChatCompletionService;

  @Mock
  private ChatFontService mockChatFontService;

  private Display display;
  private Shell shell;
  private Composite parent;
  private ChatInputTextViewer chatInputTextViewer;

  @BeforeEach
  void setUp() {
    lenient().when(mockChatServiceManager.getUserPreferenceService()).thenReturn(mockUserPreferenceService);
    lenient().when(mockChatServiceManager.getChatCompletionService()).thenReturn(mockChatCompletionService);
    lenient().when(mockChatServiceManager.getChatFontService()).thenReturn(mockChatFontService);

    // Set up SWT components
    runOnDisplayThread(() -> {
      display = Display.getDefault();
      shell = new Shell(display);
      parent = new Composite(shell, SWT.NONE);

      // Create the viewer
      chatInputTextViewer = new ChatInputTextViewer(parent, mockChatServiceManager);
      chatInputTextViewer.setContentAssistProcessor(new ContentAssistant());
    });

  }

  @AfterEach
  void tearDown() {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
  }

  @Test
  void testConentCanBeKeptAfterRefresh() {
    SwtUtils.invokeOnDisplayThread(() -> {
      chatInputTextViewer.setContent("Test message");
      chatInputTextViewer.refresh();
      assertEquals("Test message", chatInputTextViewer.getContent());
    });
  }

  @Test
  void testUndoFunctionality() {
    SwtUtils.invokeOnDisplayThread(() -> {
      // Set initial content
      chatInputTextViewer.setContent("Initial text");
      assertEquals("Initial text", chatInputTextViewer.getContent());

      // Modify the content
      chatInputTextViewer.setContent("Modified text");
      assertEquals("Modified text", chatInputTextViewer.getContent());

      // Perform undo
      chatInputTextViewer.getUndoManager().undo();
      assertEquals("Initial text", chatInputTextViewer.getContent());
    });
  }

  @Test
  void testRedoFunctionality() {
    SwtUtils.invokeOnDisplayThread(() -> {
      // Set initial content
      chatInputTextViewer.setContent("Initial text");
      assertEquals("Initial text", chatInputTextViewer.getContent());

      // Modify the content
      chatInputTextViewer.setContent("Modified text");
      assertEquals("Modified text", chatInputTextViewer.getContent());

      // Perform undo
      chatInputTextViewer.getUndoManager().undo();
      assertEquals("Initial text", chatInputTextViewer.getContent());

      // Perform redo
      chatInputTextViewer.getUndoManager().redo();
      assertEquals("Modified text", chatInputTextViewer.getContent());
    });
  }

  @Test
  void testArrowUpMovesWithinWrappedLineInsteadOfSwitchingHistory() {
    runOnDisplayThread(() -> {
      StyledText textWidget = chatInputTextViewer.getTextWidget();
      textWidget.setSize(150, 300);
      String longLine = "word ".repeat(100);
      chatInputTextViewer.setContent(longLine);
      int caretOffset = textWidget.getCaretOffset();

      pressKey(SWT.ARROW_UP);

      assertEquals(longLine, chatInputTextViewer.getContent());
      assertNotEquals(caretOffset, textWidget.getCaretOffset());
      verify(mockUserPreferenceService, never()).getPreviousInput(anyString());
    });
  }

  @Test
  void testArrowDownMovesWithinWrappedLineInsteadOfSwitchingHistory() {
    runOnDisplayThread(() -> {
      StyledText textWidget = chatInputTextViewer.getTextWidget();
      textWidget.setSize(150, 300);
      String longLine = "word ".repeat(100);
      chatInputTextViewer.setContent(longLine);
      textWidget.setSelection(0);

      pressKey(SWT.ARROW_DOWN);

      assertEquals(longLine, chatInputTextViewer.getContent());
      assertNotEquals(0, textWidget.getCaretOffset());
      verify(mockUserPreferenceService, never()).getNextInput();
    });
  }

  @Test
  void testArrowKeysMoveBetweenLinesInsteadOfSwitchingHistory() {
    runOnDisplayThread(() -> {
      StyledText textWidget = chatInputTextViewer.getTextWidget();
      textWidget.setSize(150, 300);
      String text = "first\nsecond\nthird";
      chatInputTextViewer.setContent(text);

      pressKey(SWT.ARROW_UP);
      assertEquals(1, textWidget.getLineAtOffset(textWidget.getCaretOffset()));

      textWidget.setSelection(0);
      pressKey(SWT.ARROW_DOWN);
      assertEquals(1, textWidget.getLineAtOffset(textWidget.getCaretOffset()));

      assertEquals(text, chatInputTextViewer.getContent());
      verify(mockUserPreferenceService, never()).getPreviousInput(anyString());
      verify(mockUserPreferenceService, never()).getNextInput();
    });
  }

  @Test
  void testArrowUpOnFirstLineSwitchesToPreviousInput() {
    runOnDisplayThread(() -> {
      when(mockUserPreferenceService.getPreviousInput("current")).thenReturn("older");
      chatInputTextViewer.getTextWidget().setSize(150, 300);
      chatInputTextViewer.setContent("current");

      pressKey(SWT.ARROW_UP);

      assertEquals("older", chatInputTextViewer.getContent());
    });
  }

  @Test
  void testArrowDownOnLastLineSwitchesToNextInput() {
    runOnDisplayThread(() -> {
      when(mockUserPreferenceService.getNextInput()).thenReturn("newer");
      chatInputTextViewer.getTextWidget().setSize(150, 300);
      chatInputTextViewer.setContent("current");

      pressKey(SWT.ARROW_DOWN);

      assertEquals("newer", chatInputTextViewer.getContent());
    });
  }

  // The workbench's syncExec swallows exceptions, so rethrow them on the test thread
  private void runOnDisplayThread(Runnable runnable) {
    AtomicReference<Throwable> failure = new AtomicReference<>();
    SwtUtils.invokeOnDisplayThread(() -> {
      try {
        runnable.run();
      } catch (Throwable t) {
        failure.set(t);
      }
    });
    if (failure.get() instanceof Error error) {
      throw error;
    }
    if (failure.get() instanceof RuntimeException exception) {
      throw exception;
    }
  }

  private void pressKey(int keyCode) {
    Event event = new Event();
    event.keyCode = keyCode;
    chatInputTextViewer.getTextWidget().notifyListeners(SWT.KeyDown, event);
  }
}
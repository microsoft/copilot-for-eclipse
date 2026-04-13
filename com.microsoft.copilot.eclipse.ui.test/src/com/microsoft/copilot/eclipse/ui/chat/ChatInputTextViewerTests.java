package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

@ExtendWith(MockitoExtension.class)
class ChatInputTextViewerTests {

  @Mock
  private ChatServiceManager mockChatServiceManager;

  private Display display;
  private Shell shell;
  private Composite parent;
  private ChatInputTextViewer chatInputTextViewer;

  @BeforeEach
  void setUp() {
    // Set up SWT components
    SwtUtils.invokeOnDisplayThread(() -> {
      display = Display.getDefault();
      shell = new Shell(display);
      parent = new Composite(shell, SWT.NONE);

      // Create the viewer
      chatInputTextViewer = new ChatInputTextViewer(parent, mockChatServiceManager);
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
}
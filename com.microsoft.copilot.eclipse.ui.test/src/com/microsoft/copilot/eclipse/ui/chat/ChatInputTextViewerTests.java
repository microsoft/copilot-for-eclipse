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
      if (chatInputTextViewer != null) {
        chatInputTextViewer.dispose();
      }
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
}
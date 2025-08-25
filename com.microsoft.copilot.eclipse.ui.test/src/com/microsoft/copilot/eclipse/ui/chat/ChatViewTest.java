package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.McpConfigService;
import com.microsoft.copilot.eclipse.ui.chat.services.ReferencedFileService;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

@ExtendWith(MockitoExtension.class)
class ChatViewTest {

  private Shell shell;
  private Composite parent;
  private ChatView chatView;

  @Mock
  private ChatServiceManager mockChatServiceManager;
  @Mock
  private UserPreferenceService mockUserPreferenceService;
  @Mock
  private AuthStatusManager mockAuthStatusManager;
  @Mock
  private ReferencedFileService mockReferencedFileService;
  @Mock
  private McpConfigService mockMcpConfigService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    SwtUtils.invokeOnDisplayThread(() -> {
      setupSwtComponents();
      setupChatView();
    });
    setupMocks();
  }

  private void setupSwtComponents() {
    shell = new Shell(Display.getDefault());
    parent = new Composite(shell, SWT.NONE);
  }

  private void setupChatView() {
    chatView = new ChatView();

    setFieldValue(chatView, "parent", parent);
    setFieldValue(chatView, "chatServiceManager", mockChatServiceManager);
  }

  private void setupMocks() {
    when(mockChatServiceManager.getAuthStatusManager()).thenReturn(mockAuthStatusManager);
    when(mockChatServiceManager.getReferencedFileService()).thenReturn(mockReferencedFileService);
    when(mockChatServiceManager.getUserPreferenceService()).thenReturn(mockUserPreferenceService);
  }

  private void setFieldValue(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field " + fieldName, e);
    }
  }

  @AfterEach
  void tearDown() {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
  }
}
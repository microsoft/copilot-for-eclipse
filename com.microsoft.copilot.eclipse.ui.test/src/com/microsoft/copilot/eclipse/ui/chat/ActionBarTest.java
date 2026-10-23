// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.contextwindow.ContextWindowService;
import com.microsoft.copilot.eclipse.ui.chat.services.AgentToolService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatFontService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.McpConfigService;
import com.microsoft.copilot.eclipse.ui.chat.services.ModelService;
import com.microsoft.copilot.eclipse.ui.chat.services.ReferencedFileService;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService;
import com.microsoft.copilot.eclipse.ui.chat.tools.JavaDebuggerToolAdapter;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

@ExtendWith(MockitoExtension.class)
class ActionBarTest {

  @Mock
  private UserPreferenceService mockUserPreferenceService;
  @Mock
  private ModelService mockModelService;
  @Mock
  private ChatServiceManager mockChatServiceManager;
  @Mock
  private ReferencedFileService mockReferencedFileService;
  @Mock
  private CopilotUi mockCopilotUi;
  @Mock
  private McpConfigService mockMcpConfigService;
  @Mock
  private AgentToolService mockAgentToolService;
  @Mock
  private ChatFontService mockChatFontService;
  @Mock
  private ContextWindowService mockContextWindowService;

  private Shell shell;
  private ActionBar actionBar;
  private MockedStatic<CopilotUi> mockedCopilotUi;
  private FeatureFlags featureFlags;
  private boolean previewFeaturesEnabled;

  @BeforeEach
  void setUp() {
    featureFlags = CopilotCore.getPlugin().getFeatureFlags();
    previewFeaturesEnabled = featureFlags.isClientPreviewFeatureEnabled();
    featureFlags.setClientPreviewFeatureEnabled(true);

    mockedCopilotUi = mockStatic(CopilotUi.class);
    mockedCopilotUi.when(CopilotUi::getPlugin).thenReturn(mockCopilotUi);
    lenient().when(mockCopilotUi.getChatServiceManager()).thenReturn(mockChatServiceManager);
    lenient().when(mockChatServiceManager.getUserPreferenceService()).thenReturn(mockUserPreferenceService);
    lenient().when(mockChatServiceManager.getModelService()).thenReturn(mockModelService);
    lenient().when(mockChatServiceManager.getReferencedFileService()).thenReturn(mockReferencedFileService);
    lenient().when(mockChatServiceManager.getMcpConfigService()).thenReturn(mockMcpConfigService);
    lenient().when(mockChatServiceManager.getAgentToolService()).thenReturn(mockAgentToolService);
    lenient().when(mockChatServiceManager.getChatFontService()).thenReturn(mockChatFontService);
    lenient().when(mockChatServiceManager.getContextWindowService()).thenReturn(mockContextWindowService);
    lenient().when(mockAgentToolService.getTool(JavaDebuggerToolAdapter.TOOL_NAME)).thenReturn(null);
    lenient().when(mockUserPreferenceService.getActiveChatMode()).thenReturn(ChatMode.Ask);

    runOnDisplayThread(() -> {
      shell = new Shell(Display.getDefault());
      actionBar = new ActionBar(shell, SWT.NONE, mockChatServiceManager);
    });
  }

  @AfterEach
  void tearDown() {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
    mockedCopilotUi.close();
    featureFlags.setClientPreviewFeatureEnabled(previewFeaturesEnabled);
  }

  @Test
  void testUpdateButtonsLayout_noModelWithText_sendButtonsDisabled() {
    AtomicReference<Boolean> sendEnabled = new AtomicReference<>();
    AtomicReference<Boolean> delegateEnabled = new AtomicReference<>();
    runOnDisplayThread(() -> {
      actionBar.setInputTextViewerContent("hello");

      // Recreates the send buttons, e.g. on chat mode switch
      actionBar.updateButtonsLayout();
      sendEnabled.set(getButton("btnMsgToggle").getEnabled());
      delegateEnabled.set(getButton("sendToJobButton").getEnabled());
    });

    assertFalse(sendEnabled.get(), "Send must be disabled without a model");
    assertFalse(delegateEnabled.get(), "Delegate must be disabled without a model");
  }

  // The workbench's syncExec swallows exceptions, so rethrow them on the test thread
  private static void runOnDisplayThread(Runnable runnable) {
    AtomicReference<Throwable> error = new AtomicReference<>();
    SwtUtils.invokeOnDisplayThread(() -> {
      try {
        runnable.run();
      } catch (Throwable t) {
        error.set(t);
      }
    });
    assertNull(error.get(), () -> "Unexpected error on display thread: " + error.get());
  }

  private Button getButton(String fieldName) {
    try {
      Field field = ActionBar.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      return (Button) field.get(actionBar);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to read " + fieldName, e);
    }
  }
}

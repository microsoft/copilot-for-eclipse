// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.AvatarService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatFontService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Shared SWT and service fixture for turn widget tests.
 */
abstract class BaseTurnWidgetTestSupport {

  protected static final String TURN_ID = "turn-1";

  protected Shell shell;

  @Mock
  protected ChatServiceManager mockChatServiceManager;
  @Mock
  private AvatarService mockAvatarService;
  @Mock
  private ChatFontService mockChatFontService;

  private MockedStatic<CopilotUi> copilotUiMock;

  @BeforeEach
  void setUpBaseTurnWidget() {
    lenient().when(mockChatServiceManager.getAvatarService()).thenReturn(mockAvatarService);
    lenient().when(mockChatServiceManager.getChatFontService()).thenReturn(mockChatFontService);
    lenient().when(mockAvatarService.getAvatarForCopilot()).thenReturn(null);

    SwtUtils.invokeOnDisplayThread(() -> {
      shell = new Shell(Display.getDefault());
      copilotUiMock = mockStatic(CopilotUi.class);
      CopilotUi mockPlugin = mock(CopilotUi.class);
      copilotUiMock.when(CopilotUi::getPlugin).thenReturn(mockPlugin);
      lenient().when(mockPlugin.getChatServiceManager()).thenReturn(mockChatServiceManager);
    });
  }

  @AfterEach
  void tearDownBaseTurnWidget() {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (copilotUiMock != null) {
        copilotUiMock.close();
        copilotUiMock = null;
      }
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.core.commands.ExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.completion.SuggestionUpdateManager;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.completion.CompletionManager;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;
import com.microsoft.copilot.eclipse.ui.handlers.AcceptFullSuggestionHandler;

@ExtendWith(MockitoExtension.class)
class AcceptFullSuggestionHandlerTests {

  @Test
  void testIsNotEnabledWhenNoCompletionIsAvailable() {
    CopilotUi mockedUi = mock(CopilotUi.class);
    EditorsManager mockedManager = mock(EditorsManager.class);
    CompletionManager mockedCompletionManager = mock(CompletionManager.class);
    SuggestionUpdateManager mockedSuggestionUpdateManager = mock(SuggestionUpdateManager.class);
    when(mockedCompletionManager.getSuggestionUpdateManager()).thenReturn(mockedSuggestionUpdateManager);
    when(mockedUi.getEditorsManager()).thenReturn(mockedManager);
    when(mockedManager.getActiveCompletionManager()).thenReturn(mockedCompletionManager);
    when(mockedSuggestionUpdateManager.getCurrentItem()).thenReturn(null);

    AcceptFullSuggestionHandler handler = new AcceptFullSuggestionHandler();

    try (MockedStatic<CopilotUi> mockedStatic = mockStatic(CopilotUi.class)) {
      mockedStatic.when(CopilotUi::getPlugin).thenReturn(mockedUi);
      assertFalse(handler.isEnabled());
    }
  }

  @Test
  void testAcceptionNotifiedWhenCompletionIsAccepted() throws ExecutionException {
    CopilotLanguageServerConnection mockedConnection = mock(CopilotLanguageServerConnection.class);
    when(mockedConnection.notifyAccepted(any())).thenReturn(null);
    CopilotCore mockedCore = mock(CopilotCore.class);
    when(mockedCore.getCopilotLanguageServer()).thenReturn(mockedConnection);

    CompletionItem item = new CompletionItem("uuid", "text", null, "displayText", null, 0);
    CompletionManager mockedCompletionManager = mock(CompletionManager.class);
    doNothing().when(mockedCompletionManager).acceptSuggestion(any());
    SuggestionUpdateManager mockedSuggestionUpdateManager = mock(SuggestionUpdateManager.class);
    when(mockedSuggestionUpdateManager.getCurrentItem()).thenReturn(item);
    when(mockedCompletionManager.getSuggestionUpdateManager()).thenReturn(mockedSuggestionUpdateManager);
    EditorsManager mockedManager = mock(EditorsManager.class);
    when(mockedManager.getActiveCompletionManager()).thenReturn(mockedCompletionManager);
    CopilotUi mockedUi = mock(CopilotUi.class);
    when(mockedUi.getEditorsManager()).thenReturn(mockedManager);

    AcceptFullSuggestionHandler handler = new AcceptFullSuggestionHandler();

    try (MockedStatic<CopilotUi> mockedStatic = mockStatic(CopilotUi.class);
        MockedStatic<CopilotCore> mockedStaticCore = mockStatic(CopilotCore.class)) {
      mockedStatic.when(CopilotUi::getPlugin).thenReturn(mockedUi);
      mockedStaticCore.when(CopilotCore::getPlugin).thenReturn(mockedCore);

      handler.execute(null);

      verify(mockedConnection).notifyAccepted(any());
    }
  }

}

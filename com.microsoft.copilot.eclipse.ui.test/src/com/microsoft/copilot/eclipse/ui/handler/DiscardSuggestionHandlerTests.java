package com.microsoft.copilot.eclipse.ui.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.completion.SuggestionUpdateManager;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.completion.CompletionManager;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;
import com.microsoft.copilot.eclipse.ui.handlers.DiscardSuggestionHandler;

@ExtendWith(MockitoExtension.class)
class DiscardSuggestionHandlerTests {

  @Test
  void testRejectionNotifiedWhenCompletionIsDiscarded() throws ExecutionException {
    CopilotLanguageServerConnection mockedConnection = mock(CopilotLanguageServerConnection.class);
    when(mockedConnection.notifyRejected(any())).thenReturn(null);
    CopilotCore mockedCore = mock(CopilotCore.class);
    when(mockedCore.getCopilotLanguageServer()).thenReturn(mockedConnection);

    SuggestionUpdateManager updateManager = mock(SuggestionUpdateManager.class);
    when(updateManager.getUuids()).thenReturn(List.of("uuid"));
    CompletionManager mockedCompletionManager = mock(CompletionManager.class);
    doNothing().when(mockedCompletionManager).clearGhostTexts();
    when(mockedCompletionManager.getSuggestionUpdateManager()).thenReturn(updateManager);
    EditorsManager mockedManager = mock(EditorsManager.class);
    when(mockedManager.getActiveCompletionManager()).thenReturn(mockedCompletionManager);
    CopilotUi mockedUi = mock(CopilotUi.class);
    when(mockedUi.getEditorsManager()).thenReturn(mockedManager);

    DiscardSuggestionHandler handler = new DiscardSuggestionHandler();

    try (MockedStatic<CopilotUi> mockedStatic = mockStatic(CopilotUi.class);
        MockedStatic<CopilotCore> mockedStaticCore = mockStatic(CopilotCore.class)) {
      mockedStatic.when(CopilotUi::getPlugin).thenReturn(mockedUi);
      mockedStaticCore.when(CopilotCore::getPlugin).thenReturn(mockedCore);

      handler.execute(null);

      verify(mockedConnection).notifyRejected(any());
    }
  }

}

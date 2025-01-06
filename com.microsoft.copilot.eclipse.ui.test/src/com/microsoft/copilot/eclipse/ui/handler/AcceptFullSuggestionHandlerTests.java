package com.microsoft.copilot.eclipse.ui.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.microsoft.copilot.eclipse.core.completion.CompletionCollection;
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
    when(mockedUi.getEditorsManager()).thenReturn(mockedManager);
    when(mockedManager.getActiveCompletionManager()).thenReturn(mockedCompletionManager);
    when(mockedCompletionManager.hasCompletion()).thenReturn(false);

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

    CompletionCollection completions = new CompletionCollection(
        List.of(new CompletionItem("uuid", "text", null, "displayText", null, 0)), "uri");
    CompletionManager mockedCompletionManager = mock(CompletionManager.class);
    doNothing().when(mockedCompletionManager).acceptFullSuggestion();
    when(mockedCompletionManager.getCompletions()).thenReturn(completions);
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

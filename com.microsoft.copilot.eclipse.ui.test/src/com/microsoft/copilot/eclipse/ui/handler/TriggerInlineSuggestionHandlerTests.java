package com.microsoft.copilot.eclipse.ui.handler;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.core.commands.ExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.completion.CompletionManager;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;
import com.microsoft.copilot.eclipse.ui.handlers.TriggerInlineSuggestionHandler;

@ExtendWith(MockitoExtension.class)
class TriggerInlineSuggestionHandlerTests {

  @Test
  void testTriggerCompletionInvocation() throws ExecutionException {
    CopilotUi mockedUi = mock(CopilotUi.class);
    EditorsManager mockedManager = mock(EditorsManager.class);
    CompletionManager mockedCompletionManager = mock(CompletionManager.class);
    when(mockedUi.getEditorsManager()).thenReturn(mockedManager);
    when(mockedManager.getActiveCompletionManager()).thenReturn(mockedCompletionManager);
    doNothing().when(mockedCompletionManager).triggerCompletion();

    TriggerInlineSuggestionHandler handler = new TriggerInlineSuggestionHandler();

    try (MockedStatic<CopilotUi> mockedStatic = mockStatic(CopilotUi.class)) {
      mockedStatic.when(CopilotUi::getPlugin).thenReturn(mockedUi);
      handler.execute(null);
      verify(mockedCompletionManager, times(1)).triggerCompletion();
    }
  }
}

package com.microsoft.copilot.eclipse.ui.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.completion.CompletionHandler;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;
import com.microsoft.copilot.eclipse.ui.handlers.TriggerInlineSuggestionHandler;

@ExtendWith(MockitoExtension.class)
class TriggerInlineSuggestionHandlerTests {

  @Test
  void testIsEnabledWhenNoCompletionIsAvailable() {
    CopilotUi mockedUi = mock(CopilotUi.class);
    EditorsManager mockedManager = mock(EditorsManager.class);
    CompletionHandler mockedHandler = mock(CompletionHandler.class);
    when(mockedUi.getEditorsManager()).thenReturn(mockedManager);
    when(mockedManager.getActiveCompletionHandler()).thenReturn(mockedHandler);
    when(mockedHandler.hasCompletion()).thenReturn(false);

    TriggerInlineSuggestionHandler handler = new TriggerInlineSuggestionHandler();

    try (MockedStatic<CopilotUi> mockedStatic = mockStatic(CopilotUi.class)) {
      mockedStatic.when(CopilotUi::getPlugin).thenReturn(mockedUi);
      assertTrue(handler.isEnabled());
    }
  }
}

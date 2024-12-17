package com.microsoft.copilot.eclipse.ui.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.microsoft.copilot.eclipse.ui.handlers.DiscardSuggestionHandler;

@ExtendWith(MockitoExtension.class)
class DiscardSuggestionHandlerTests {

  @Test
  void testIsNotEnabledWhenNoCompletionIsAvailable() {
    CopilotUi mockedUi = mock(CopilotUi.class);
    EditorsManager mockedManager = mock(EditorsManager.class);
    CompletionHandler mockedHandler = mock(CompletionHandler.class);
    when(mockedUi.getEditorsManager()).thenReturn(mockedManager);
    when(mockedManager.getActiveCompletionHandler()).thenReturn(mockedHandler);
    when(mockedHandler.hasCompletion()).thenReturn(false);

    DiscardSuggestionHandler handler = new DiscardSuggestionHandler();

    try (MockedStatic<CopilotUi> mockedStatic = mockStatic(CopilotUi.class)) {
      mockedStatic.when(CopilotUi::getPlugin).thenReturn(mockedUi);
      assertFalse(handler.isEnabled());
    }
  }

}

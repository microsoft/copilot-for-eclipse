package com.microsoft.copilot.eclipse.ui.handler;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.chat.ActionBar;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService;
import com.microsoft.copilot.eclipse.ui.handlers.OpenChatViewHandler;

@ExtendWith(MockitoExtension.class)
public class OpenChatViewHandlerTests {

  @Mock
  private ExecutionEvent mockEvent;

  @Mock
  private ChatView mockChatView;

  @Mock
  private ActionBar mockActionBar;

  @Mock
  private IWorkbench mockWorkbench;

  @Mock
  private IWorkbenchWindow mockWindow;

  @Mock
  private IWorkbenchPage mockPage;

  @Mock
  private CopilotUi mockCopilotUi;

  @Mock
  private ChatServiceManager mockChatServiceManager;

  @Mock
  private UserPreferenceService mockUserPreferenceService;

  private OpenChatViewHandler handler;

  @BeforeEach
  void setUp() {
    handler = new OpenChatViewHandler();
  }

  private void setupCopilotUiMocks(MockedStatic<CopilotUi> mockedCopilotUi) {
    mockedCopilotUi.when(() -> CopilotUi.getPlugin()).thenReturn(mockCopilotUi);
    when(mockCopilotUi.getChatServiceManager()).thenReturn(mockChatServiceManager);
    when(mockChatServiceManager.getUserPreferenceService()).thenReturn(mockUserPreferenceService);
  }

  @Test
  void testExecute_SuccessfullyOpensAndConfiguresChatView() throws ExecutionException, PartInitException {
    try (MockedStatic<PlatformUI> mockedPlatformUI = Mockito.mockStatic(PlatformUI.class);
         MockedStatic<CopilotUi> mockedCopilotUi = Mockito.mockStatic(CopilotUi.class)) {
      
      setupCopilotUiMocks(mockedCopilotUi);
      
      // Setup mocks for successful chat view opening
      mockedPlatformUI.when(() -> PlatformUI.getWorkbench()).thenReturn(mockWorkbench);
      when(mockWorkbench.getActiveWorkbenchWindow()).thenReturn(mockWindow);
      when(mockWindow.getActivePage()).thenReturn(mockPage);
      when(mockPage.showView(Constants.CHAT_VIEW_ID)).thenReturn(mockChatView);
      when(mockChatView.getActionBar()).thenReturn(mockActionBar);
      
      // Setup parameters
      when(mockEvent.getParameter(UiConstants.OPEN_CHAT_VIEW_INPUT_VALUE))
          .thenReturn("Test message");
      when(mockEvent.getParameter(UiConstants.OPEN_CHAT_VIEW_AUTO_SEND))
          .thenReturn("true");

      // Execute
      Object result = handler.execute(mockEvent);

      // Verify the full execution flow
      assertNull(result);
      verify(mockChatView, times(1)).setFocus();
      verify(mockActionBar, times(1)).setInputTextViewerContent("Test message");
      verify(mockActionBar, times(1)).handleSendMessage();
      verify(mockUserPreferenceService, times(1)).setActiveChatMode(ChatMode.Ask.displayName());
    }
  }

  @Test
  void testExecute_WithInputValueButNoAutoSend() throws ExecutionException, PartInitException {
    try (MockedStatic<PlatformUI> mockedPlatformUI = Mockito.mockStatic(PlatformUI.class);
         MockedStatic<CopilotUi> mockedCopilotUi = Mockito.mockStatic(CopilotUi.class)) {
      
      setupCopilotUiMocks(mockedCopilotUi);
      
      // Setup mocks for successful chat view opening
      mockedPlatformUI.when(() -> PlatformUI.getWorkbench()).thenReturn(mockWorkbench);
      when(mockWorkbench.getActiveWorkbenchWindow()).thenReturn(mockWindow);
      when(mockWindow.getActivePage()).thenReturn(mockPage);
      when(mockPage.showView(Constants.CHAT_VIEW_ID)).thenReturn(mockChatView);
      when(mockChatView.getActionBar()).thenReturn(mockActionBar);
      
      // Setup parameters - no auto send
      when(mockEvent.getParameter(UiConstants.OPEN_CHAT_VIEW_INPUT_VALUE))
          .thenReturn("Test message");
      when(mockEvent.getParameter(UiConstants.OPEN_CHAT_VIEW_AUTO_SEND))
          .thenReturn("false");

      // Execute
      Object result = handler.execute(mockEvent);

      // Verify the execution flow
      assertNull(result);
      verify(mockChatView, times(1)).setFocus();
      verify(mockActionBar, times(1)).setInputTextViewerContent("Test message");
      verify(mockActionBar, never()).handleSendMessage();
      verify(mockUserPreferenceService, times(0)).setActiveChatMode("Ask");
    }
  }

  @Test
  void testExecute_NoParameters() throws ExecutionException, PartInitException {
    try (MockedStatic<PlatformUI> mockedPlatformUI = Mockito.mockStatic(PlatformUI.class);
         MockedStatic<CopilotUi> mockedCopilotUi = Mockito.mockStatic(CopilotUi.class)) {
      
      // Setup mocks for successful chat view opening
      mockedPlatformUI.when(() -> PlatformUI.getWorkbench()).thenReturn(mockWorkbench);
      when(mockWorkbench.getActiveWorkbenchWindow()).thenReturn(mockWindow);
      when(mockWindow.getActivePage()).thenReturn(mockPage);
      when(mockPage.showView(Constants.CHAT_VIEW_ID)).thenReturn(mockChatView);
      
      // Setup parameters as null/empty
      when(mockEvent.getParameter(UiConstants.OPEN_CHAT_VIEW_INPUT_VALUE))
          .thenReturn(null);

      // Execute
      Object result = handler.execute(mockEvent);

      // Verify the execution flow
      assertNull(result);
      verify(mockChatView, times(1)).setFocus();
      verify(mockActionBar, never()).setInputTextViewerContent(any());
      verify(mockActionBar, never()).handleSendMessage();
      verify(mockUserPreferenceService, times(0)).setActiveChatMode("Ask");
    }
  }

  @Test
  void testExecute_NoActiveWorkbenchWindow() throws ExecutionException {
    try (MockedStatic<PlatformUI> mockedPlatformUI = Mockito.mockStatic(PlatformUI.class);
         MockedStatic<CopilotUi> mockedCopilotUi = Mockito.mockStatic(CopilotUi.class)) {
      
    	mockedCopilotUi.when(() -> CopilotUi.getPlugin()).thenReturn(mockCopilotUi);
      
      // Setup mocks with no active window
      mockedPlatformUI.when(() -> PlatformUI.getWorkbench()).thenReturn(mockWorkbench);
      when(mockWorkbench.getActiveWorkbenchWindow()).thenReturn(null);

      // Execute
      Object result = handler.execute(mockEvent);

      // Verify execution handles null window gracefully
      assertNull(result);
      verify(mockChatView, never()).setFocus();
      verify(mockUserPreferenceService, never()).setActiveChatMode(any());
    }
  }

  @Test
  void testExecute_NoActivePage() throws ExecutionException {
    try (MockedStatic<PlatformUI> mockedPlatformUI = Mockito.mockStatic(PlatformUI.class);
         MockedStatic<CopilotUi> mockedCopilotUi = Mockito.mockStatic(CopilotUi.class)) {
      
      // Setup mocks with no active page
      mockedPlatformUI.when(() -> PlatformUI.getWorkbench()).thenReturn(mockWorkbench);
      when(mockWorkbench.getActiveWorkbenchWindow()).thenReturn(mockWindow);
      when(mockWindow.getActivePage()).thenReturn(null);

      // Execute
      Object result = handler.execute(mockEvent);

      // Verify execution handles null page gracefully
      assertNull(result);
      verify(mockChatView, never()).setFocus();
      verify(mockUserPreferenceService, never()).setActiveChatMode(any());
    }
  }

  @Test
  void testExecute_ChatViewHasNoActionBar() throws ExecutionException, PartInitException {
    try (MockedStatic<PlatformUI> mockedPlatformUI = Mockito.mockStatic(PlatformUI.class);
         MockedStatic<CopilotUi> mockedCopilotUi = Mockito.mockStatic(CopilotUi.class)) {

      setupCopilotUiMocks(mockedCopilotUi);

      // Setup mocks for successful chat view opening but no action bar
      mockedPlatformUI.when(() -> PlatformUI.getWorkbench()).thenReturn(mockWorkbench);
      when(mockWorkbench.getActiveWorkbenchWindow()).thenReturn(mockWindow);
      when(mockWindow.getActivePage()).thenReturn(mockPage);
      when(mockPage.showView(Constants.CHAT_VIEW_ID)).thenReturn(mockChatView);
      when(mockChatView.getActionBar()).thenReturn(null);
      
      // Setup parameters
      when(mockEvent.getParameter(UiConstants.OPEN_CHAT_VIEW_INPUT_VALUE))
          .thenReturn("Test message");

      // Execute
      Object result = handler.execute(mockEvent);

      // Verify execution handles null action bar gracefully
      assertNull(result);
      verify(mockChatView, times(1)).setFocus();
      verify(mockActionBar, never()).setInputTextViewerContent(any());
      verify(mockActionBar, never()).handleSendMessage();
      verify(mockUserPreferenceService, times(0)).setActiveChatMode("Ask");
    }
  }

  @Test
  void testExecute_SwitchesFromAgentToAskMode() throws ExecutionException, PartInitException {
    try (MockedStatic<PlatformUI> mockedPlatformUI = Mockito.mockStatic(PlatformUI.class);
         MockedStatic<CopilotUi> mockedCopilotUi = Mockito.mockStatic(CopilotUi.class)) {
      
      setupCopilotUiMocks(mockedCopilotUi);
      
      // Setup mocks for successful chat view opening
      mockedPlatformUI.when(() -> PlatformUI.getWorkbench()).thenReturn(mockWorkbench);
      when(mockWorkbench.getActiveWorkbenchWindow()).thenReturn(mockWindow);
      when(mockWindow.getActivePage()).thenReturn(mockPage);
      when(mockPage.showView(Constants.CHAT_VIEW_ID)).thenReturn(mockChatView);
      when(mockChatView.getActionBar()).thenReturn(mockActionBar);
      
      // Setup parameters - no auto send
      when(mockEvent.getParameter(UiConstants.OPEN_CHAT_VIEW_INPUT_VALUE))
          .thenReturn("Test message");
      when(mockEvent.getParameter(UiConstants.OPEN_CHAT_VIEW_AUTO_SEND))
          .thenReturn("true");

      // Execute
      Object result = handler.execute(mockEvent);

      // Verify that the mode was switched from Agent to Ask
      assertNull(result);
      verify(mockChatView, times(1)).setFocus();
      verify(mockUserPreferenceService, times(1)).setActiveChatMode("Ask");
    }
  }
}

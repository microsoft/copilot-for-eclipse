// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.chat.BuiltInChatMode;
import com.microsoft.copilot.eclipse.core.chat.BuiltInChatModeManager;
import com.microsoft.copilot.eclipse.core.chat.InputNavigation;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

  @Mock
  private CopilotLanguageServerConnection mockLsConnection;

  @Mock
  private AuthStatusManager mockAuthStatusManager;

  @Mock
  private BuiltInChatModeManager mockBuiltInChatModeManager;

  private UserPreferenceService userPreferenceService;
  private final AtomicReference<List<BuiltInChatMode>> builtInModes = new AtomicReference<>(List.of());

  @BeforeEach
  void setUp() {
    when(mockAuthStatusManager.isSignedIn()).thenReturn(false);
    when(mockBuiltInChatModeManager.getBuiltInModes()).thenAnswer(invocation -> builtInModes.get());
  }

  @AfterEach
  void tearDown() {
    if (userPreferenceService != null) {
      userPreferenceService.dispose();
    }
  }

  @Test
  void testAuthStatusChangedEventHandler_UserSignsOut_ClearsUserPreferenceCache() {
    // Arrange
    userPreferenceService = new UserPreferenceService(mockLsConnection, mockAuthStatusManager,
        mockBuiltInChatModeManager);

    // Set up initial state with input navigation
    setInputNavigationForService(new InputNavigation());
    assertNotNull(getInputNavigationFromService(), "Input navigation should be set initially");

    // Get the auth status changed event handler
    EventHandler authHandler = getAuthStatusChangedEventHandler();
    assertNotNull(authHandler, "Auth status changed event handler should be available");

    Event signOutEvent = createAuthStatusEvent(CopilotStatusResult.NOT_SIGNED_IN);

    // Act
    authHandler.handleEvent(signOutEvent);

    // Assert
    assertNull(getInputNavigationFromService(), "Input navigation should be cleared when user signs out");
    verify(mockBuiltInChatModeManager).clearModes();
  }

  @Test
  void testAuthStatusChangedEventHandler_SignOutThenSignIn() {
    // Arrange
    when(mockBuiltInChatModeManager.reloadModes()).thenReturn(CompletableFuture.completedFuture(null));
    userPreferenceService = new UserPreferenceService(mockLsConnection, mockAuthStatusManager,
        mockBuiltInChatModeManager);

    EventHandler authHandler = getAuthStatusChangedEventHandler();
    assertNotNull(authHandler, "Auth status changed event handler should be available");

    Event signOutEvent = createAuthStatusEvent(CopilotStatusResult.NOT_SIGNED_IN);
    Event signInEvent = createAuthStatusEvent(CopilotStatusResult.OK, "test-user");

    // Act - Sign out then sign in
    authHandler.handleEvent(signOutEvent);
    assertNull(getInputNavigationFromService(), "Input navigation should be null after sign out");

    authHandler.handleEvent(signInEvent);
    InputNavigation initialNavigation = new InputNavigation(List.of("input1", "input2"));
    setInputNavigationForService(initialNavigation);
    assertNotNull(getInputNavigationFromService(), "Input navigation should be set initially");

    // Assert - After sign in, input navigation should be restored
    assertNotNull(getInputNavigationFromService(), "Input navigation should be restored after sign in");
    assertEquals("input2", getInputNavigationFromService().getLatestInput(), "Input navigation should be restored");
    verify(mockBuiltInChatModeManager).clearModes();
    verify(mockBuiltInChatModeManager).reloadModes();
  }

  @Test
  void testAuthStatusChangedEventHandler_UserSignsIn_ReloadsBuiltInModesWithoutBlocking() {
    // Arrange
    CompletableFuture<Void> pendingReload = new CompletableFuture<>();
    when(mockBuiltInChatModeManager.reloadModes()).thenReturn(pendingReload);
    userPreferenceService = new UserPreferenceService(mockLsConnection, mockAuthStatusManager,
        mockBuiltInChatModeManager);

    EventHandler authHandler = getAuthStatusChangedEventHandler();
    assertNotNull(authHandler, "Auth status changed event handler should be available");

    Event signInEvent = createAuthStatusEvent(CopilotStatusResult.OK, "test-user");

    // Act - Simulate user sign in
    assertTimeoutPreemptively(Duration.ofSeconds(1), () -> authHandler.handleEvent(signInEvent));

    // Assert
    assertFalse(pendingReload.isDone(), "The event handler should not wait for the mode reload");
    verify(mockBuiltInChatModeManager).reloadModes();

    builtInModes.set(List.of(createBuiltInMode("Agent")));
    pendingReload.complete(null);
    assertArrayEquals(new String[] { "Agent" }, getAvailableChatModesFromObservable());
  }

  /**
   * Helper method to create an auth status changed event
   */
  private Event createAuthStatusEvent(String status) {
    return createAuthStatusEvent(status, null);
  }

  /**
   * Helper method to create an auth status changed event with user
   */
  private Event createAuthStatusEvent(String status, String user) {
    CopilotStatusResult statusResult = new CopilotStatusResult();
    statusResult.setStatus(status);
    if (user != null) {
      statusResult.setUser(user);
    }

    Map<String, Object> eventProperties = new HashMap<>();
    eventProperties.put(IEventBroker.DATA, statusResult);
    return new Event(CopilotEventConstants.TOPIC_AUTH_STATUS_CHANGED, eventProperties);
  }

  private BuiltInChatMode createBuiltInMode(String name) {
    ConversationMode mode = new ConversationMode();
    mode.setId(name);
    mode.setName(name);
    mode.setKind(name);
    mode.setBuiltIn(true);
    mode.setDescription(name + " mode");
    return new BuiltInChatMode(mode);
  }

  /**
   * Helper method to access private authStatusChangedEventHandler field for
   * testing
   */
  private EventHandler getAuthStatusChangedEventHandler() {
    try {
      Field field = UserPreferenceService.class.getDeclaredField("authStatusChangedEventHandler");
      field.setAccessible(true);
      return (EventHandler) field.get(userPreferenceService);
    } catch (Exception e) {
      throw new RuntimeException("Failed to access authStatusChangedEventHandler field", e);
    }
  }

  private String[] getAvailableChatModesFromObservable() {
    AtomicReference<String[]> availableModes = new AtomicReference<>();
    SwtUtils.invokeOnDisplayThread(() -> {
      try {
        Field field = UserPreferenceService.class.getDeclaredField("chatModeObservable");
        field.setAccessible(true);
        Object observable = field.get(userPreferenceService);
        availableModes.set((String[]) observable.getClass().getMethod("getValue").invoke(observable));
      } catch (Exception e) {
        throw new RuntimeException("Failed to read chatModeObservable", e);
      }
    });
    return availableModes.get();
  }

  /**
   * Helper method to access private inputNavigation field for testing
   */
  private InputNavigation getInputNavigationFromService() {
    try {
      Field field = UserPreferenceService.class.getDeclaredField("inputNavigation");
      field.setAccessible(true);
      return (InputNavigation) field.get(userPreferenceService);
    } catch (Exception e) {
      throw new RuntimeException("Failed to access inputNavigation field", e);
    }
  }

  /**
   * Helper method to set private inputNavigation field for testing
   */
  private void setInputNavigationForService(InputNavigation inputNavigation) {
    try {
      Field field = UserPreferenceService.class.getDeclaredField("inputNavigation");
      field.setAccessible(true);
      field.set(userPreferenceService, inputNavigation);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set inputNavigation field", e);
    }
  }
}

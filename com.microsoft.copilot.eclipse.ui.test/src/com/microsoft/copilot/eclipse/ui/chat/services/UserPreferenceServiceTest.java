package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.microsoft.copilot.eclipse.core.chat.InputNavigation;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

  @Mock
  private CopilotLanguageServerConnection mockLsConnection;

  @Mock
  private AuthStatusManager mockAuthStatusManager;

  private UserPreferenceService userPreferenceService;

  @BeforeEach
  void setUp() {
    when(mockAuthStatusManager.isSignedIn()).thenReturn(false);
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
    userPreferenceService = new UserPreferenceService(mockLsConnection, mockAuthStatusManager);

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
  }

  @Test
  void testAuthStatusChangedEventHandler_SignOutThenSignIn() {
    // Arrange
    userPreferenceService = new UserPreferenceService(mockLsConnection, mockAuthStatusManager);

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
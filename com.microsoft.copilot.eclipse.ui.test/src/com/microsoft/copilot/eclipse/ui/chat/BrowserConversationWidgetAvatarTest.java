// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.ui.chat.services.AvatarService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Tests the avatar/user-name resolution in {@link BrowserConversationWidget}
 * ({@code getUserAvatarUri}, {@code getUserDisplayName}, {@code getCopilotDisplayName}, and the
 * Copilot avatar), which delegate to the shared {@link AvatarService} via the
 * {@link ChatServiceManager}.
 *
 * <p>The widget requires a non-null {@link ChatServiceManager}; production always injects one, so
 * these tests supply a mock and assert the widget delegates every avatar/name lookup to it.
 */
class BrowserConversationWidgetAvatarTest {

  private static final String SERVICE_USER_DATA_URI = "data:image/png;base64,USERAVATAR";
  private static final String SERVICE_COPILOT_DATA_URI = "data:image/png;base64,COPILOTAVATAR";
  private static final String SERVICE_COPILOT_NAME = "GitHub Copilot (service)";

  private Shell shell;

  @BeforeEach
  void setUp() {
    Display.getDefault().syncExec(() -> shell = new Shell(Display.getDefault()));
  }

  @AfterEach
  void tearDown() {
    Display.getDefault().syncExec(() -> {
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
  }

  @Test
  void constructor_nullServiceManager_throws() {
    Display.getDefault().syncExec(() ->
        assertThrows(NullPointerException.class,
            () -> new BrowserConversationWidget(shell, null),
            "A null service manager is rejected because production always injects one"));
  }

  @Test
  void getUserAvatarUri_delegatesToAvatarService() {
    withWidget(serviceManager("octocat"), widget ->
        assertEquals(SERVICE_USER_DATA_URI, invokeString(widget, "getUserAvatarUri"),
            "The user avatar comes from AvatarService.getAvatarForCurrentUserAsDataUri()"));
  }

  @Test
  void getUserDisplayName_delegatesToAvatarService() {
    withWidget(serviceManager("octocat"), widget ->
        assertEquals("octocat", invokeString(widget, "getUserDisplayName"),
            "The display name comes from AvatarService.getUserName()"));
  }

  @Test
  void getUserDisplayName_blankUser_returnsFallbackFromService() {
    // getUserName is responsible for the fallback; here the service returns it.
    withWidget(serviceManager(Messages.chat_turnWidget_user), widget ->
        assertEquals(Messages.chat_turnWidget_user, invokeString(widget, "getUserDisplayName"),
            "A blank user resolves to the shared user label via getUserName"));
  }

  @Test
  void getCopilotDisplayName_delegatesToAvatarService() {
    withWidget(serviceManager("octocat"), widget ->
        assertEquals(SERVICE_COPILOT_NAME, invokeString(widget, "getCopilotDisplayName"),
            "The Copilot display name comes from AvatarService.getCopilotName()"));
  }

  @Test
  void copilotAvatar_sourcedFromAvatarService() {
    withWidget(serviceManager("octocat"), widget ->
        assertEquals(SERVICE_COPILOT_DATA_URI, readStringField(widget, "copilotAvatarDataUri"),
            "The Copilot avatar comes from AvatarService.getAvatarForCopilotAsDataUri()"));
  }

  /**
   * Builds a mock {@link ChatServiceManager} whose {@link AvatarService} returns sentinel data URIs
   * and display names.
   */
  private static ChatServiceManager serviceManager(String resolvedUserName) {
    AvatarService avatarService = mock(AvatarService.class);
    when(avatarService.getAvatarForCurrentUserAsDataUri()).thenReturn(SERVICE_USER_DATA_URI);
    when(avatarService.getAvatarForCopilotAsDataUri()).thenReturn(SERVICE_COPILOT_DATA_URI);
    when(avatarService.getUserName()).thenReturn(resolvedUserName);
    when(avatarService.getCopilotName()).thenReturn(SERVICE_COPILOT_NAME);
    ChatServiceManager manager = mock(ChatServiceManager.class);
    when(manager.getAvatarService()).thenReturn(avatarService);
    return manager;
  }

  private void withWidget(ChatServiceManager serviceManager,
      java.util.function.Consumer<BrowserConversationWidget> body) {
    AtomicReference<BrowserConversationWidget> widgetRef = new AtomicReference<>();
    try {
      Display.getDefault().syncExec(() -> {
        BrowserConversationWidget widget = new BrowserConversationWidget(shell, serviceManager);
        widgetRef.set(widget);
        body.accept(widget);
      });
    } finally {
      Display.getDefault().syncExec(() -> {
        BrowserConversationWidget widget = widgetRef.get();
        if (widget != null && !widget.isDisposed()) {
          widget.dispose();
        }
      });
    }
  }

  private static String invokeString(BrowserConversationWidget widget, String methodName) {
    try {
      Method method = BrowserConversationWidget.class.getDeclaredMethod(methodName);
      method.setAccessible(true);
      return (String) method.invoke(widget);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String readStringField(BrowserConversationWidget widget, String fieldName) {
    try {
      Field field = BrowserConversationWidget.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      return (String) field.get(widget);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}

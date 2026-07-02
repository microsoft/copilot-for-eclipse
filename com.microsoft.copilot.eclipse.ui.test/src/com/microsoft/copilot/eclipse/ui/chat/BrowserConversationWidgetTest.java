// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.microsoft.copilot.eclipse.ui.chat.services.AvatarService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;

/**
 * Basic platform integration test for {@link BrowserConversationWidget}.
 * Verifies that the widget can be created and disposed without error in a real
 * Eclipse platform environment (OSGi bundles active, Display available).
 */
class BrowserConversationWidgetTest {

  private Shell shell;
  private BrowserConversationWidget widget;

  @BeforeEach
  void setUp() {
    Display display = Display.getDefault();
    display.syncExec(() -> {
      shell = new Shell(display);
      widget = new BrowserConversationWidget(shell, mockServiceManager());
    });
  }

  /**
   * Builds a mock {@link ChatServiceManager} whose {@link AvatarService} returns non-null avatar
   * data URIs and display names, mirroring the non-null service manager the widget receives in
   * production.
   */
  private static ChatServiceManager mockServiceManager() {
    AvatarService avatarService = Mockito.mock(AvatarService.class);
    Mockito.when(avatarService.getAvatarForCopilotAsDataUri()).thenReturn("");
    Mockito.when(avatarService.getAvatarForCurrentUserAsDataUri()).thenReturn("");
    Mockito.when(avatarService.getUserName()).thenReturn("User");
    Mockito.when(avatarService.getCopilotName()).thenReturn("GitHub Copilot");
    ChatServiceManager manager = Mockito.mock(ChatServiceManager.class);
    Mockito.when(manager.getAvatarService()).thenReturn(avatarService);
    return manager;
  }

  @AfterEach
  void tearDown() {
    Display.getDefault().syncExec(() -> {
      if (widget != null && !widget.isDisposed()) {
        widget.dispose();
      }
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
  }

  @Test
  void widgetCreatesSuccessfully() {
    assertNotNull(widget);
    assertNotNull(widget.getControl());
    assertFalse(widget.isDisposed());
  }

  @Test
  void widgetDisposesCleanly() {
    // SWT widgets must be disposed on the UI thread, the way real callers do.
    assertDoesNotThrow(() -> Display.getDefault().syncExec(() -> widget.dispose()));
  }
}


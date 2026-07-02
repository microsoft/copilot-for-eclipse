// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Characterization tests for {@link AvatarService} covering today's {@link Image}-based avatar
 * retrieval used by the StyledText renderer ({@code UserTurnWidget} / {@code CopilotTurnWidget}).
 *
 * <p>These tests lock in the current behavior before the data-URI refactor: the Copilot avatar is a
 * bundled default, a blank user resolves to the bundled default user avatar, and a cache hit returns
 * the cached image without scheduling a download. The mocked {@link AuthStatusManager} returns a
 * blank user so the constructor's warm-up never triggers a network download (hermetic/offline).
 */
class AvatarServiceTest {

  private AuthStatusManager mockAuth;
  private AvatarService service;

  @BeforeEach
  void setUp() {
    mockAuth = mock(AuthStatusManager.class);
    when(mockAuth.getUserName()).thenReturn("");
    Display.getDefault().syncExec(() -> service = new AvatarService(mockAuth));
  }

  @AfterEach
  void tearDown() {
    Display.getDefault().syncExec(() -> {
      if (service != null) {
        service.dispose();
      }
    });
  }

  @Test
  void getAvatarForCopilot_returnsNonNullBundledImage() {
    Image copilot = service.getAvatarForCopilot();
    assertNotNull(copilot, "Copilot avatar should be the bundled default image");
    assertFalse(copilot.isDisposed(), "Copilot avatar should not be disposed");
  }

  @Test
  void getUserName_blankUser_returnsDefaultLabel() {
    assertEquals(Messages.chat_turnWidget_user, service.getUserName(),
        "A blank user name should resolve to the shared default user label");
  }

  @Test
  void getUserName_signedIn_returnsUserName() {
    when(mockAuth.getUserName()).thenReturn("octocat");
    assertEquals("octocat", service.getUserName(),
        "A non-blank user name should be returned as-is");
  }

  @Test
  void getCopilotName_returnsCopilotLabel() {
    assertEquals(Messages.chat_turnWidget_copilot, service.getCopilotName(),
        "The Copilot name should be the shared Copilot label");
  }

  @Test
  void getAvatarForCurrentUser_blankUser_returnsNonNullDefault() {
    AtomicReference<Image> first = new AtomicReference<>();
    AtomicReference<Image> second = new AtomicReference<>();
    Display display = Display.getDefault();
    display.syncExec(() -> {
      first.set(service.getAvatarForCurrentUser(display));
      second.set(service.getAvatarForCurrentUser(display));
    });
    assertNotNull(first.get(), "Blank user should resolve to the bundled default user avatar");
    assertSame(first.get(), second.get(), "Blank user should always return the same default image");
  }

  @Test
  void getAvatar_blankUser_returnsDefaultUserImage() {
    AtomicReference<Image> blank = new AtomicReference<>();
    AtomicReference<Image> nullUser = new AtomicReference<>();
    Display display = Display.getDefault();
    display.syncExec(() -> {
      blank.set(service.getAvatar(display, ""));
      nullUser.set(service.getAvatar(display, null));
    });
    assertNotNull(blank.get(), "Blank user should return the default user image");
    assertSame(blank.get(), nullUser.get(), "Blank and null user should return the same default image");
  }

  @Test
  void getAvatar_cacheHit_returnsCachedImageWithoutScheduling() throws Exception {
    Display display = Display.getDefault();
    AtomicReference<Image> result = new AtomicReference<>();
    AtomicReference<Image> seeded = new AtomicReference<>();
    display.syncExec(() -> {
      Image cached = service.getAvatarForCopilot();
      seeded.set(cached);
      seedAvatarCache("octocat", cached);
      result.set(service.getAvatar(display, "octocat"));
    });
    assertSame(seeded.get(), result.get(), "A cache hit should return the cached image");
    assertFalse(jobsContain("octocat"), "A cache hit must not schedule a download job");
  }

  @Test
  void getAvatarForCopilotAsDataUri_returnsPngDataUri() {
    String dataUri = service.getAvatarForCopilotAsDataUri();
    assertNotNull(dataUri, "Copilot avatar data URI should not be null");
    assertTrue(dataUri.startsWith("data:image/png;base64,"), "Copilot avatar should be a PNG data URI");
  }

  @Test
  void getAvatarForCurrentUserAsDataUri_blankUser_returnsDefaultUserDataUri() {
    String dataUri = service.getAvatarForCurrentUserAsDataUri();
    assertEquals(readDefaultUserDataUri(), dataUri, "Blank user should resolve to the default user data URI");
  }

  @Test
  void getAvatarAsDataUri_blankUser_returnsDefaultUserDataUri() {
    assertEquals(readDefaultUserDataUri(), service.getAvatarAsDataUri(""),
        "Blank user should resolve to the default user data URI");
    assertEquals(readDefaultUserDataUri(), service.getAvatarAsDataUri(null),
        "Null user should resolve to the default user data URI");
  }

  @Test
  void getAvatarAsDataUri_cacheHit_returnsCachedDataUriWithoutScheduling() throws Exception {
    String cachedUri = "data:image/png;base64,CACHED";
    seedAvatarDataUriCache("octocat", cachedUri);
    assertEquals(cachedUri, service.getAvatarAsDataUri("octocat"),
        "A cache hit should return the cached data URI");
    assertFalse(jobsContain("octocat"), "A cache hit must not schedule a download job");
  }

  private String readDefaultUserDataUri() {
    try {
      Field field = AvatarService.class.getDeclaredField("defaultUserAvatarDataUri");
      field.setAccessible(true);
      return (String) field.get(service);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private void seedAvatarCache(String user, Image image) {
    try {
      Field field = AvatarService.class.getDeclaredField("avatarCache");
      field.setAccessible(true);
      ((Map<String, Image>) field.get(service)).put(user, image);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private void seedAvatarDataUriCache(String user, String dataUri) {
    try {
      Field field = AvatarService.class.getDeclaredField("avatarDataUriCache");
      field.setAccessible(true);
      ((Map<String, String>) field.get(service)).put(user, dataUri);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private boolean jobsContain(String user) throws Exception {
    Field field = AvatarService.class.getDeclaredField("jobs");
    field.setAccessible(true);
    return ((Map<String, ?>) field.get(service)).containsKey(user);
  }
}

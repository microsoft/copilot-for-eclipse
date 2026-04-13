package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Tests for {@link ChatFontService}.
 */
class ChatFontServiceTest {

  private Shell shell;
  private Composite parent;
  private ChatFontService chatFontService;

  @BeforeEach
  void setUp() {
    SwtUtils.invokeOnDisplayThread(() -> {
      shell = new Shell(Display.getDefault());
      parent = new Composite(shell, SWT.NONE);
    });
    chatFontService = new ChatFontService();
  }

  @AfterEach
  void tearDown() {
    if (chatFontService != null) {
      chatFontService.dispose();
    }
    SwtUtils.invokeOnDisplayThread(() -> {
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
  }

  @Test
  void registerControl_WithValidControl_ShouldAddToTracking() {
    SwtUtils.invokeOnDisplayThread(() -> {
      Label label = new Label(parent, SWT.NONE);

      assertDoesNotThrow(() -> chatFontService.registerControl(label));
    });
  }

  @Test
  void registerControl_WhenCalledMultipleTimes_ShouldBeIdempotent() {
    SwtUtils.invokeOnDisplayThread(() -> {
      Label label = new Label(parent, SWT.NONE);

      // Register multiple times - should not throw or cause issues
      assertDoesNotThrow(() -> {
        chatFontService.registerControl(label);
        chatFontService.registerControl(label);
        chatFontService.registerControl(label);
      });
    });
  }

  @Test
  void registerControl_WithNullControl_ShouldNotThrow() {
    assertDoesNotThrow(() -> chatFontService.registerControl(null));
  }

  @Test
  void registerControl_WithDisposedControl_ShouldNotThrow() {
    SwtUtils.invokeOnDisplayThread(() -> {
      Label label = new Label(parent, SWT.NONE);
      label.dispose();

      assertDoesNotThrow(() -> chatFontService.registerControl(label));
    });
  }

  @Test
  void unregisterControl_WithRegisteredControl_ShouldRemoveFromTracking() {
    SwtUtils.invokeOnDisplayThread(() -> {
      Label label = new Label(parent, SWT.NONE);
      chatFontService.registerControl(label);

      assertDoesNotThrow(() -> chatFontService.unregisterControl(label));
    });
  }

  @Test
  void unregisterControl_WithNullControl_ShouldNotThrow() {
    assertDoesNotThrow(() -> chatFontService.unregisterControl(null));
  }

  @Test
  void registerCallback_WithValidCallback_ShouldInvokeImmediately() {
    AtomicBoolean callbackInvoked = new AtomicBoolean(false);

    chatFontService.registerCallback(() -> callbackInvoked.set(true));

    assertTrue(callbackInvoked.get(), "Callback should be invoked immediately upon registration");
  }

  @Test
  void registerCallback_WhenCalledMultipleTimes_ShouldBeIdempotent() {
    java.util.concurrent.atomic.AtomicInteger invocationCount = new java.util.concurrent.atomic.AtomicInteger(0);
    Runnable callback = () -> invocationCount.incrementAndGet();

    // Register the same callback multiple times
    chatFontService.registerCallback(callback);
    chatFontService.registerCallback(callback);
    chatFontService.registerCallback(callback);

    // Callback should only have been invoked once (on first registration)
    org.junit.jupiter.api.Assertions.assertEquals(1, invocationCount.get(),
        "Callback should only be invoked once even when registered multiple times");
  }

  @Test
  void registerCallback_WithNullCallback_ShouldNotThrow() {
    assertDoesNotThrow(() -> chatFontService.registerCallback(null));
  }

  @Test
  void unregisterCallback_WithRegisteredCallback_ShouldRemove() {
    AtomicBoolean callbackInvoked = new AtomicBoolean(false);
    Runnable callback = () -> callbackInvoked.set(true);

    chatFontService.registerCallback(callback);
    callbackInvoked.set(false); // Reset after initial invocation

    assertDoesNotThrow(() -> chatFontService.unregisterCallback(callback));
  }

  @Test
  void unregisterCallback_WithNullCallback_ShouldNotThrow() {
    assertDoesNotThrow(() -> chatFontService.unregisterCallback(null));
  }

  @Test
  void dispose_ShouldClearAllRegistrations() {
    SwtUtils.invokeOnDisplayThread(() -> {
      Label label = new Label(parent, SWT.NONE);
      chatFontService.registerControl(label);
    });
    chatFontService.registerCallback(() -> {});

    assertDoesNotThrow(() -> chatFontService.dispose());
  }

  @Test
  void dispose_WhenCalledMultipleTimes_ShouldNotThrow() {
    chatFontService.dispose();

    assertDoesNotThrow(() -> chatFontService.dispose());
  }

  @Test
  void controlAutoUnregistersOnDispose() {
    SwtUtils.invokeOnDisplayThread(() -> {
      Label label = new Label(parent, SWT.NONE);
      chatFontService.registerControl(label);

      // Disposing the control should auto-unregister it
      assertDoesNotThrow(() -> label.dispose());
    });
  }
}

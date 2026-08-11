// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.Accessible;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.accessibility.AccessibleListener;
import org.eclipse.swt.custom.StyledText;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Verifies accessible names exposed by turn content controls.
 */
@ExtendWith(MockitoExtension.class)
class BaseTurnWidgetAccessibilityTest extends BaseTurnWidgetTestSupport {

  @Test
  void appendMessage_multipleMarkupBlocks_haveUniqueAccessibleNames() {
    AtomicReference<List<String>> accessibleNamesRef = new AtomicReference<>();
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      widget.appendMessage("First explanation\n```java\nint value = 1;\n```\nSecond explanation\n");

      accessibleNamesRef.set(Arrays.stream(widget.getChildren())
          .filter(StyledText.class::isInstance)
          .map(StyledText.class::cast)
          .map(BaseTurnWidgetAccessibilityTest::getAccessibleName)
          .toList());
    });
    assertEquals(List.of("GitHub Copilot message 1", "GitHub Copilot message 2"), accessibleNamesRef.get());
  }

  @SuppressWarnings("unchecked")
  private static String getAccessibleName(StyledText text) {
    Accessible accessible = text.getAccessible();
    try {
      Field listenersField = Accessible.class.getDeclaredField("accessibleListeners");
      listenersField.setAccessible(true);
      List<AccessibleListener> listeners = (List<AccessibleListener>) listenersField.get(accessible);
      AccessibleEvent event = new AccessibleEvent(accessible);
      for (AccessibleListener listener : listeners) {
        listener.getName(event);
      }
      return event.result;
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}

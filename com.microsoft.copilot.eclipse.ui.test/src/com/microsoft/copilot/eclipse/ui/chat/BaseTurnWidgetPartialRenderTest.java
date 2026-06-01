// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Field;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.AvatarService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatFontService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Verifies that {@link BaseTurnWidget#appendMessage} eagerly renders trailing partial lines via
 * {@code renderPartialBuffer}, while deferring rendering for code-fence prefixes and inside code
 * blocks where fence detection requires a complete line.
 */
@ExtendWith(MockitoExtension.class)
class BaseTurnWidgetPartialRenderTest {

  private static final String TURN_ID = "turn-1";

  private Shell shell;
  private MockedStatic<CopilotUi> copilotUiMock;
  private CopilotUi mockPlugin;

  @Mock
  private ChatServiceManager mockChatServiceManager;
  @Mock
  private AvatarService mockAvatarService;
  @Mock
  private ChatFontService mockChatFontService;

  @BeforeEach
  void setUp() {
    lenient().when(mockChatServiceManager.getAvatarService()).thenReturn(mockAvatarService);
    lenient().when(mockChatServiceManager.getChatFontService()).thenReturn(mockChatFontService);
    lenient().when(mockAvatarService.getAvatarForCopilot()).thenReturn(null);

    SwtUtils.invokeOnDisplayThread(() -> {
      shell = new Shell(Display.getDefault());
      copilotUiMock = mockStatic(CopilotUi.class);
      mockPlugin = mock(CopilotUi.class);
      copilotUiMock.when(CopilotUi::getPlugin).thenReturn(mockPlugin);
      lenient().when(mockPlugin.getChatServiceManager()).thenReturn(mockChatServiceManager);
    });
  }

  @AfterEach
  void tearDown() {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (copilotUiMock != null) {
        copilotUiMock.close();
        copilotUiMock = null;
      }
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
  }

  @Test
  void appendMessage_partialLineWithoutNewline_rendersImmediately() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      widget.appendMessage("Hello world");

      ChatMarkupViewer viewer = getMarkupViewer(widget);
      assertNotNull(viewer, "Partial text without newline should be rendered eagerly to a text block");
      assertTrue(viewer.getTextWidget().getText().contains("Hello world"),
          "Expected partial text to be visible, got: '" + viewer.getTextWidget().getText() + "'");
    });
  }

  @Test
  void appendMessage_partialAfterCompleteLine_rendersBothCommittedAndPartial() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      // First chunk: a complete line plus a trailing partial fragment without a newline.
      widget.appendMessage("First line\nSecond ");

      ChatMarkupViewer viewer = getMarkupViewer(widget);
      assertNotNull(viewer, "Text block should be created");
      String rendered = viewer.getTextWidget().getText();
      assertTrue(rendered.contains("First line"),
          "Committed line should be rendered, got: '" + rendered + "'");
      assertTrue(rendered.contains("Second"),
          "Trailing partial fragment should be rendered eagerly, got: '" + rendered + "'");

      // Append more text completing the partial line — final render must show full content.
      widget.appendMessage("line\n");
      rendered = viewer.getTextWidget().getText();
      assertTrue(rendered.contains("First line"),
          "First line should still be present after appending more text, got: '" + rendered + "'");
      assertTrue(rendered.contains("Second line"),
          "Completed second line should be rendered, got: '" + rendered + "'");
    });
  }

  @Test
  void appendMessage_partialIsTripleBacktickFence_defersRendering() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      // A confirmed code-fence prefix — must wait for the newline before deciding whether
      // to render markup or open a code block. Otherwise the literal backticks would flash
      // into the markup viewer.
      widget.appendMessage("```");

      assertNull(getField(widget, "currentTextBlock"),
          "Text block must not be created for a confirmed fence prefix");
      assertNull(getField(widget, "currentCodeBlock"),
          "Code block must not be created until the fence newline is received");
    });
  }

  @Test
  void appendMessage_partialIsTripleBacktickWithLanguage_defersRendering() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      widget.appendMessage("```java");

      assertNull(getField(widget, "currentTextBlock"),
          "Text block must not be created while a fence-with-language is still being streamed");
      assertNull(getField(widget, "currentCodeBlock"),
          "Code block must not be created until the fence newline is received");
    });
  }

  @Test
  void appendMessage_partialIsSingleBacktick_defersRendering() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      // A lone backtick could grow into ``` on the next chunk — defer rendering.
      widget.appendMessage("`");

      assertNull(getField(widget, "currentTextBlock"),
          "Text block must not be created for an ambiguous single backtick");
    });
  }

  @Test
  void appendMessage_partialIsDoubleBacktick_defersRendering() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      widget.appendMessage("``");

      assertNull(getField(widget, "currentTextBlock"),
          "Text block must not be created for an ambiguous double backtick");
    });
  }

  @Test
  void appendMessage_partialBacktickFollowedByText_rendersImmediately() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      // A single backtick followed by non-backtick content is inline code, not a fence —
      // render eagerly so the user sees the streamed token immediately.
      widget.appendMessage("`code");

      ChatMarkupViewer viewer = getMarkupViewer(widget);
      assertNotNull(viewer, "Inline-code partial should be rendered eagerly");
      assertTrue(viewer.getTextWidget().getText().contains("code"),
          "Inline-code content should be visible, got: '" + viewer.getTextWidget().getText() + "'");
    });
  }

  @Test
  void appendMessage_partialResolvesIntoFence_doesNotLeaveStaleText() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      // First send some markdown so a text block exists.
      widget.appendMessage("intro text\n");
      ChatMarkupViewer viewer = getMarkupViewer(widget);
      assertNotNull(viewer);
      assertTrue(viewer.getTextWidget().getText().contains("intro text"));

      // Then start streaming a fence — must NOT append "```" into the markup viewer.
      widget.appendMessage("```");
      assertTrue(!viewer.getTextWidget().getText().contains("```"),
          "Fence prefix must not leak into the markup viewer, got: '" + viewer.getTextWidget().getText() + "'");

      // Complete the fence: the code block should open and the markup viewer should not gain
      // the fence characters.
      widget.appendMessage("java\n");
      assertNotNull(getField(widget, "currentCodeBlock"),
          "Code block must open once the fence newline is received");
      assertTrue(!viewer.getTextWidget().getText().contains("```"),
          "Fence characters must never appear in the markup viewer");
    });
  }

  @Test
  void appendMessage_partialInsideCodeBlock_isNotRenderedAsMarkup() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      // Open a code block.
      widget.appendMessage("```java\n");
      assertNotNull(getField(widget, "currentCodeBlock"),
          "Code block should be open after the opening fence line");
      assertNull(getField(widget, "currentTextBlock"),
          "Text block should not exist while we are inside a code block");

      // Stream a partial code line without a newline — the partial must NOT be rendered
      // to the markup viewer, since partial code text has to go through the source viewer
      // once a complete line arrives.
      widget.appendMessage("int x = 1;");

      assertNull(getField(widget, "currentTextBlock"),
          "Partial text inside a code block must not create a markup text block");
    });
  }

  @Test
  void appendMessage_emptyString_doesNotRender() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      widget.appendMessage("");

      assertNull(getField(widget, "currentTextBlock"),
          "Empty message must be a no-op and must not create a text block");
      StringBuilder buffer = (StringBuilder) getField(widget, "messageBuffer");
      assertEquals(0, buffer.length(), "Empty message must not accumulate into the buffer");
    });
  }

  private static ChatMarkupViewer getMarkupViewer(BaseTurnWidget widget) {
    Object textBlock = getField(widget, "currentTextBlock");
    return textBlock instanceof ChatMarkupViewer markup ? markup : null;
  }

  private static Object getField(Object target, String name) {
    Class<?> cls = target.getClass();
    while (cls != null) {
      try {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
      } catch (NoSuchFieldException e) {
        cls = cls.getSuperclass();
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    throw new RuntimeException("Field '" + name + "' not found on " + target.getClass());
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.BuiltInChatMode;
import com.microsoft.copilot.eclipse.core.chat.BuiltInChatModeManager;
import com.microsoft.copilot.eclipse.core.chat.CustomChatMode;
import com.microsoft.copilot.eclipse.core.chat.CustomChatModeManager;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatCompletionService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A custom TextViewer for the chat input area with configurable hints messages.
 */
public class ChatInputTextViewer extends UndoableTextViewer implements PaintListener {
  private static final int MAX_INPUT_ROWS = 5;

  private Composite parent;
  private Consumer<String> sendMessageHandler;
  private ChatCompletionService chatCompletionService;
  private UserPreferenceService userPreferenceService;
  private ChatServiceManager chatServiceManager;
  private ContentAssistant contentAssistant;

  private boolean caretLineOffsetChanged = false;
  private int lastCursorLineOffset = 0;

  private Color placeholderColor;

  /**
   * Constructs a new ChatInputTextViewer.
   *
   * @param parent the parent composite
   * @param chatServiceManager the chat service manager to access services
   */
  public ChatInputTextViewer(Composite parent, ChatServiceManager chatServiceManager) {
    super(parent, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
    this.parent = parent;
    this.chatServiceManager = chatServiceManager;
    this.userPreferenceService = chatServiceManager.getUserPreferenceService();
    this.chatCompletionService = chatServiceManager.getChatCompletionService();
    this.init();
  }

  public void setSendMessageHandler(Consumer<String> handler) {
    this.sendMessageHandler = handler;
  }

  public String getContent() {
    return this.getDocument().get();
  }

  /**
   * Sets the content of the text viewer.
   *
   * @param content the content to set
   */
  public void setContent(String content) {
    this.getDocument().set(content);
    this.getTextWidget().setSelection(content.length());
  }

  @Override
  public void paintControl(PaintEvent e) {
    String content = this.getContent();
    if (StringUtils.isNotEmpty(content)) {
      return;
    }
    e.gc.setForeground(placeholderColor);
    StyledText styledText = this.getTextWidget();
    e.gc.drawString(getPlaceholderText(), styledText.getLeftMargin(), styledText.getTopMargin(), true);
  }

  private void init() {
    this.setEditable(true);
    this.addTextListener(this::onTextChanged);

    StyledText tvw = this.getTextWidget();
    tvw.setLayout(new GridLayout(1, false));
    tvw.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    tvw.setAlwaysShowScrollBars(false);

    tvw.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(org.eclipse.swt.events.ModifyEvent e) {
        SwtUtils.invokeOnDisplayThread(tvw::redraw, tvw);
      }
    });

    // Add a traverse listener to handle Tab and Shift+Tab properly.
    // Otherwise, it will also insert an unexpected tab character as a side effect.
    tvw.addTraverseListener(e -> {
      if (e.detail == SWT.TRAVERSE_TAB_PREVIOUS || e.detail == SWT.TRAVERSE_TAB_NEXT) {
        e.doit = true;
      }
    });

    tvw.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        onKeyPressed(e);
      }
    });

    this.placeholderColor = CssConstants.getInputPlaceHolderColor(tvw.getDisplay());
    tvw.addPaintListener(this);
    SwtUtils.invokeOnDisplayThread(tvw::redraw, tvw);

    // new document after styled text redraw to avoid a redundant line being added to the document.
    this.setDocument(new Document());

    // Configure undo manager to enable undo/redo functionality (Ctrl+Z/Ctrl+Y)
    this.configureUndoManager();

    // Register callback for chat font updates - set font and refresh layout when font changes
    Runnable fontChangeCallback = () -> {
      StyledText textWidget = this.getTextWidget();
      if (textWidget != null && !textWidget.isDisposed()) {
        UiUtils.applyChatFont(textWidget);
        refreshHeightLayout();
      }
    };
    this.chatServiceManager.getChatFontService().registerCallback(fontChangeCallback);

    // Unregister callback on dispose
    tvw.addDisposeListener(e -> {
      this.chatServiceManager.getChatFontService().unregisterCallback(fontChangeCallback);
    });
  }

  private void clearFormat(int start, int end) {
    this.getTextWidget().setStyleRange(new StyleRange(start, end - start, null, null, SWT.NORMAL));
  }

  private void onTextChanged(TextEvent event) {
    // End compound change on newline to enable line-by-line undo and
    // intentionally skip refreshHeightLayout() to avoid layout shaking
    // when only line breaks are inserted (see comment in refreshHeightLayout()).
    if (isInsertLineBreakOnly(event)) {
      endCompoundChange();
      return;
    }

    // End compound change on whitespace for word-by-word undo
    String text = event.getText();
    if (text != null && text.length() == 1 && Character.isWhitespace(text.charAt(0))) {
      endCompoundChange();
    }

    refreshHeightLayout();
  }

  private boolean isInsertLineBreakOnly(TextEvent event) {
    String text = event.getText();
    return text != null && (text.equals("\n") || text.equals("\r\n"));
  }

  private void refreshHeightLayout() {
    StyledText tvw = this.getTextWidget();
    // If the width is not initialized, use SWT.DEFAULT to compute the size
    // otherwise, swt will think that each line can only have one character.
    int widthHint = tvw.getSize().x == 0 ? SWT.DEFAULT : tvw.getSize().x;
    Point size = tvw.computeSize(widthHint, SWT.DEFAULT);
    GridData gd = (GridData) tvw.getLayoutData();
    gd.heightHint = Math.min(tvw.getLineHeight() * MAX_INPUT_ROWS, size.y);
    // TODO: An very interesting bug here, if we call layout(true, true), even no changes,
    // The width of welcome view will become shorter and shorter, may investigate it later
    ChatInputTextViewer.this.parent.getParent().getParent().layout(true, false);
  }

  private void onKeyPressed(KeyEvent e) {
    // Handle undo/redo key events
    if (handleUndoRedoKeyEvent(e)) {
      return;
    }

    // Handle Shift+Tab to trigger focus traversal event for accessibility
    if (e.keyCode == SWT.TAB && (e.stateMask & SWT.SHIFT) != 0) {
      e.doit = false;
      this.getTextWidget().traverse(SWT.TRAVERSE_TAB_PREVIOUS);
      return;
    }

    String text = this.getContent();
    // check the caret status so that we know if this is moving caret through multiple lines, or it's a switching
    this.updateCaretLineOffsetStatus();
    if (handleArrowKeyEvent(e, text)) {
      // caret status need update since arrow key event may change the position via switching input history
      this.updateCaretLineOffsetStatus();
      return;
    }
    // If current char is not line break, it means assistant pop up is visible and assistant listener handle it
    // In this case, users just want to select a command, so we should not handle it here
    if ((e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) && isLineBreakInCaret()) {
      // If Shift+Enter is pressed, or the content is empty, insert new line
      if (isShiftHolded(e) || StringUtils.isBlank(text)) {
        // KeyEvent is later than TextEvent.
        // Skipped refreshing Enter in TextEvent in onTextChanged() to avoid layout-shaking issue.
        // Refresh the layout for real new line here in KeyEvent.
        refreshHeightLayout();
        e.doit = true; // Allow the new line
      } else {
        // Users press Enter to send message, so we should remove the line break
        removeLineBreak();
        handleSendMessage();
      }
      return;
    }
    Point firstWordIndex = UiUtils.getFirstWordIndex(text);
    int begin = firstWordIndex.x;
    int end = firstWordIndex.y;
    if (!isCaretInRange(begin, end)) {
      // user is not modifying the first word, no need to highlight the command
      return;
    }
    clearFormat(0, text.length());
    String firstWord = text.substring(begin, end);
    if (e.keyCode == SWT.BS
        && chatCompletionService.isBrokenCommand(firstWord, this.getTextWidget().getCaretOffset() - begin)) {
      try {
        getDocument().replace(begin, end - begin, StringUtils.EMPTY);
      } catch (BadLocationException ex) {
        CopilotCore.LOGGER.error(ex);
      }
      return;
    }
    // we may need to highlight the command if user removed leading character before a command
    // user is typing
    if (chatCompletionService.isCommand(firstWord)) {
      this.getTextWidget().setStyleRange(new StyleRange(begin, end - begin, UiUtils.SLASH_COMMAND_FORGROUND_COLOR,
          UiUtils.SLASH_COMMAND_BACKGROUND_COLOR, SWT.BOLD));
      return;
    }
  }

  /**
   * Return true if the event is handled, false otherwise.
   */
  private boolean handleArrowKeyEvent(KeyEvent e, String text) {
    if (this.caretLineOffsetChanged || this.isProposalPopupActive()) {
      return false;
    }
    if (e.keyCode == SWT.ARROW_UP) {
      String lastInput = userPreferenceService.getPreviousInput(text);
      if (StringUtils.isNotBlank(lastInput)) {
        this.setContent(lastInput);
      }
      return true;
    } else if (e.keyCode == SWT.ARROW_DOWN) {
      String nextInput = userPreferenceService.getNextInput();
      if (StringUtils.isNotBlank(nextInput)) {
        this.setContent(nextInput);
        return true;
      }
    } else {
      // if it's not about navigating input history, reset the cursor, so that next time
      // when user press up arrow, it will get the latest input from history.
      userPreferenceService.resetInputHistoryCursor();
    }
    return false;
  }

  private boolean isLineBreakInCaret() {
    // for both /r/n of windows and /n of Linux, the current character is LF, so just need to check it
    String currentChar = getCurrentChar();
    return StringUtils.equals(currentChar, StringUtils.LF);
  }

  private boolean isCaretInRange(int begin, int end) {
    int caretOffset = this.getTextWidget().getCaretOffset();
    return caretOffset >= begin && caretOffset <= end;
  }

  private String getCurrentChar() {
    StyledText tvw = this.getTextWidget();
    int caretOffset = tvw.getCaretOffset() - 1;
    return caretOffset < tvw.getCharCount() && caretOffset >= 0 ? tvw.getTextRange(caretOffset, 1) : null;
  }

  private boolean isShiftHolded(KeyEvent e) {
    return (e.stateMask & SWT.SHIFT) != 0;
  }

  private void removeLineBreak() {
    StyledText tvw = this.getTextWidget();
    int caretOffset = tvw.getCaretOffset() - 1;
    String text = this.getContent();
    // Check for \r\n (Windows style)
    if (caretOffset > 0 && caretOffset < text.length() && text.charAt(caretOffset) == '\n'
        && text.charAt(caretOffset - 1) == '\r') {
      // Remove both \r and \n
      tvw.replaceTextRange(caretOffset - 1, 2, StringUtils.EMPTY);
    } else if (caretOffset >= 0 && caretOffset < text.length() && text.charAt(caretOffset) == '\n') {
      // Remove single \n (Unix/Linux style)
      tvw.replaceTextRange(caretOffset, 1, StringUtils.EMPTY);
    }
  }

  private void handleSendMessage() {
    resetCaretLineOffsetStatus();
    Optional.ofNullable(this.sendMessageHandler).ifPresent(handler -> handler.accept(this.getContent()));
  }

  private String getPlaceholderText() {
    switch (userPreferenceService.getActiveChatMode()) {
      case Agent:
        // Get the active mode name or ID from observable (not from disk to avoid stale data)
        String activeModeId = userPreferenceService.getActiveModeNameOrId();

        // Check if a custom mode is active and use its description as placeholder
        if (CustomChatModeManager.INSTANCE.isCustomMode(activeModeId)) {
          CustomChatMode customMode = CustomChatModeManager.INSTANCE.getCustomModeById(activeModeId);
          if (customMode != null && StringUtils.isNotBlank(customMode.getDescription())) {
            return customMode.getDescription();
          }
        }

        // Check if a built-in mode (Agent/Plan) is active and use its description as placeholder
        BuiltInChatMode builtInMode = BuiltInChatModeManager.INSTANCE.getBuiltInModeByDisplayName(activeModeId);
        if (builtInMode != null && StringUtils.isNotBlank(builtInMode.getDescription())) {
          return builtInMode.getDescription();
        }

        return Messages.chat_actionBar_initialContentForAgent;
      case Ask:
      default:
        return Messages.chat_actionBar_initialContent;
    }
  }

  private void updateCaretLineOffsetStatus() {
    StyledText textWidget = this.getTextWidget();
    int caretOffset = textWidget.getCaretOffset();
    int offset = textWidget.getLineAtOffset(caretOffset);
    if (lastCursorLineOffset != offset) {
      lastCursorLineOffset = offset;
      caretLineOffsetChanged = true;
    } else {
      caretLineOffsetChanged = false;
    }
  }

  private void resetCaretLineOffsetStatus() {
    lastCursorLineOffset = 0;
    caretLineOffsetChanged = false;
  }

  public void setContentAssistProcessor(ContentAssistant ca) {
    contentAssistant = ca;
  }

  /**
   * Checks if the proposal popup is currently active. We use reflection instead of extends ContentAssistant to expose
   * the method. Because using the latter
   */
  private boolean isProposalPopupActive() {
    try {
      // Use reflection to call the isProposalPopupActive method from ContentAssistant
      Method method = ContentAssistant.class.getDeclaredMethod("isProposalPopupActive");
      method.setAccessible(true);
      return (boolean) method.invoke(contentAssistant);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      CopilotCore.LOGGER.error("Failed to call isProposalPopupActive via reflection", e);
      return false; // Default to false if reflection fails
    }
  }
}
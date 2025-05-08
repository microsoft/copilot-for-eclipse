package com.microsoft.copilot.eclipse.ui.chat;

import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextViewer;
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
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.SlashCommandService;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

class ChatInputTextViewer extends TextViewer implements PaintListener {
  private static final int MAX_INPUT_ROWS = 5;

  private Composite parent;
  private Consumer<String> sendMessageHandler;
  private ChatServiceManager chatServiceManager;

  /**
   * Whether the color resource should be disposed. When the color is fetched from the jface registry, it should not be
   * disposed.
   */
  private boolean needDisposeColorResource;
  private Color placeholderColor;

  public ChatInputTextViewer(Composite parent, ChatServiceManager chatServiceManager) {
    super(parent, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
    this.parent = parent;
    this.chatServiceManager = chatServiceManager;
    this.init();
  }

  public void setSendMessageHandler(Consumer<String> handler) {
    this.sendMessageHandler = handler;
  }

  public String getContent() {
    if (this.getDocument() == null) {
      this.setDocument(new Document());
    }
    return this.getDocument().get();
  }

  public void setContent(String content) {
    this.getDocument().set(content);
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
    tvw.setBackground(tvw.getParent().getBackground());
    tvw.setLayout(new GridLayout(1, false));
    tvw.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    tvw.setAlwaysShowScrollBars(false);

    tvw.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(org.eclipse.swt.events.ModifyEvent e) {
        SwtUtils.invokeOnDisplayThread(tvw::redraw, tvw);
      }
    });

    tvw.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        onKeyPressed(e);
      }
    });

    initializePlaceHolderColor();
    tvw.addPaintListener(this);
    SwtUtils.invokeOnDisplayThread(tvw::redraw, tvw);
  }

  private void initializePlaceHolderColor() {
    Color color = SwtUtils.getRegisteredInlineAnnotationColor(this.getTextWidget().getDisplay());
    if (color == null) {
      needDisposeColorResource = true;
      placeholderColor = SwtUtils.getDefaultGhostTextColor(this.getTextWidget().getDisplay());
    } else {
      needDisposeColorResource = false;
      placeholderColor = color;
    }
  }

  private void clearFormat(int start, int end) {
    this.getTextWidget().setStyleRange(new StyleRange(start, end - start, null, null, SWT.NORMAL));
  }

  private void onTextChanged(TextEvent event) {
    // Skip refreshing Enter-'\n' in TextEvent to avoid layout-shaking issue.
    if (isInsertLineBreakOnly(event)) {
      return;
    }

    refreshHeightLayout();
  }

  private boolean isInsertLineBreakOnly(TextEvent event) {
    String text = event.getText();
    return text != null && (text.equals("\n") || text.equals("\r\n"));
  }

  private void refreshHeightLayout() {
    StyledText tvw = ChatInputTextViewer.this.getTextWidget();
    Point size = tvw.computeSize(tvw.getSize().x, SWT.DEFAULT);
    GridData gd = (GridData) tvw.getLayoutData();
    gd.heightHint = Math.min(tvw.getLineHeight() * MAX_INPUT_ROWS, size.y);
    // TODO: An very interesting bug here, if we call layout(true, true), even no changes,
    // The width of welcome view will become shorter and shorter, may investigate it later
    ChatInputTextViewer.this.parent.getParent().layout(true, false);
  }

  private void onKeyPressed(KeyEvent e) {
    String text = this.getContent();
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
    SlashCommandService slashCommandService = chatServiceManager.getSlashCommandService();
    if (e.keyCode == SWT.BS
        && slashCommandService.isBrokenCommand(firstWord, this.getTextWidget().getCaretOffset() - begin)) {
      try {
        getDocument().replace(begin, end - begin, StringUtils.EMPTY);
      } catch (BadLocationException ex) {
        CopilotCore.LOGGER.error(ex);
      }
      return;
    }
    // we may need to highlight the command if user removed leading character before a command
    // user is typing
    if (slashCommandService.isCommand(firstWord)) {
      this.getTextWidget().setStyleRange(new StyleRange(begin, end - begin, UiUtils.SLASH_COMMAND_FORGROUND_COLOR,
          UiUtils.SLASH_COMMAND_BACKGROUND_COLOR, SWT.BOLD));
      return;
    }
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
    Optional.ofNullable(this.sendMessageHandler).ifPresent(handler -> handler.accept(this.getContent()));
  }

  private String getPlaceholderText() {
    switch (chatServiceManager.getUserPreferenceService().getActiveChatMode()) {
      case Agent:
        return Messages.chat_actionBar_initialContentForAgent;
      case Ask:
      default:
        return Messages.chat_actionBar_initialContent;
    }
  }

  public void dispose() {
    if (needDisposeColorResource && placeholderColor != null && !placeholderColor.isDisposed()) {
      placeholderColor.dispose();
    }
  }
}

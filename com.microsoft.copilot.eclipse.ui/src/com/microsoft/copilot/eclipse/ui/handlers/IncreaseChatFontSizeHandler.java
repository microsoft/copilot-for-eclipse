package com.microsoft.copilot.eclipse.ui.handlers;

import java.util.Arrays;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.swt.graphics.FontData;

import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handler for increasing the chat view font size.
 */
public class IncreaseChatFontSizeHandler extends CopilotHandler {
  private static final int FONT_SIZE_INCREMENT = 1;

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    FontRegistry fontRegistry = UiUtils.getThemeFontRegistry();
    if (fontRegistry == null) {
      return null;
    }

    FontData[] originalFontData = fontRegistry.getFontData(UiUtils.CHAT_FONT_ID);
    if (originalFontData == null || originalFontData.length == 0) {
      return null;
    }

    // Create copies and increase font height for all font data entries
    FontData[] fontData = Arrays.stream(originalFontData)
        .map(fd -> new FontData(fd.getName(), fd.getHeight() + FONT_SIZE_INCREMENT, fd.getStyle()))
        .toArray(FontData[]::new);

    // Update the font registry, which will trigger property change events
    fontRegistry.put(UiUtils.CHAT_FONT_ID, fontData);

    return null;
  }
}

package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.swt.graphics.FontData;

import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handler for decreasing the chat view font size.
 */
public class DecreaseChatFontSizeHandler extends CopilotHandler {
  private static final int FONT_SIZE_DECREMENT = 1;
  private static final int MINIMUM_FONT_SIZE = 6;

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

    // Check if we can decrease (don't go below minimum)
    boolean canDecrease = false;
    for (FontData fd : originalFontData) {
      if (fd.getHeight() - FONT_SIZE_DECREMENT >= MINIMUM_FONT_SIZE) {
        canDecrease = true;
        break;
      }
    }
    if (!canDecrease) {
      return null;
    }

    // Create copies and decrease font height for all font data entries
    FontData[] fontData = new FontData[originalFontData.length];
    for (int i = 0; i < originalFontData.length; i++) {
      int newHeight = Math.max(originalFontData[i].getHeight() - FONT_SIZE_DECREMENT, MINIMUM_FONT_SIZE);
      fontData[i] = new FontData(originalFontData[i].getName(),
          newHeight,
          originalFontData[i].getStyle());
    }

    // Update the font registry, which will trigger property change events
    fontRegistry.put(UiUtils.CHAT_FONT_ID, fontData);

    return null;
  }
}

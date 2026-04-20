// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.swt;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Constants for CSS styling in the Eclipse UI.
 */
public class CssConstants {

  private CssConstants() {
    // Prevent instantiation
  }

  /**
   * Key value for setting and getting the CSS class name of a widget.
   *
   * @see org.eclipse.swt.widgets.Widget#getData(String)
   * @see org.eclipse.swt.widgets.Widget#setData(String, Object)
   */
  public static final String CSS_CLASS_NAME_KEY = "org.eclipse.e4.ui.css.CssClassName";

  /**
   * Key value for setting and getting the CSS ID of a widget.
   *
   * @see org.eclipse.swt.widgets.Widget#getData(String)
   * @see org.eclipse.swt.widgets.Widget#setData(String, Object)
   */
  public static final String CSS_ID_KEY = "org.eclipse.e4.ui.css.id";

  /**
   * Get the placeholder color for input fields based on the current theme.
   */
  public static Color getInputPlaceHolderColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 164, 164, 164);
    }
    return new Color(display, 128, 128, 128);
  }

  /**
   * Get the border color for UI elements based on the current theme.
   */
  public static Color getBorderColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 68, 68, 68);
    }
    return new Color(display, 216, 216, 216);
  }

  /**
   * Get the separator color for dropdown popup groups based on the current theme.
   */
  public static Color getSeparatorColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 68, 68, 68);
    }
    return new Color(display, 234, 234, 234);
  }

  /**
   * Get the button focus background color based on the current theme.
   */
  public static Color getButtonFocusBgColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 64, 64, 64);
    }
    return new Color(display, 232, 232, 232);
  }

  /**
   * Get the background color for dropdown popup based on the current theme.
   */
  public static Color getPopupBgColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 30, 31, 34);
    }
    return new Color(display, 255, 255, 255);
  }

  /**
   * Returns the color for the "Powerful" model picker category badge.
   */
  public static Color getCategoryPowerfulColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 116, 167, 220); // #74A7DC
    }
    return new Color(display, 15, 108, 189); // #0F6CBD
  }

  /**
   * Returns the color for the "Versatile" model picker category badge.
   */
  public static Color getCategoryVersatileColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 222, 225, 229); // #DEE1E5
    }
    return new Color(display, 97, 97, 97); // #616161
  }

  /**
   * Returns the color for the "Lightweight" model picker category badge.
   */
  public static Color getCategoryLightweightColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 110, 185, 110); // #6EB96E
    }
    return new Color(display, 16, 124, 16); // #107C10
  }

  /**
   * Get the focused background color for dropdown popup items based on the current theme.
   */
  public static Color getPopupItemFocusBgColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 24, 71, 133);
    }
    return new Color(display, 212, 226, 255);
  }

  /**
   * Get the focus border color for widgets.
   */
  public static Color getWidgetFocusBorderColor(Display display) {
    return new Color(display, 55, 134, 246);
  }

  /**
   * Get the border color for the currently selected/focused item in list-like widgets (chat history, popup menus).
   */
  public static Color getSelectedItemBorderColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 54, 109, 145);
    }
    return new Color(display, 48, 107, 152);
  }

  /**
   * Returns the background color used to highlight replace text for next edit suggestions.
   */
  public static Color getNesReplaceBackground(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 36, 3, 2);
    }
    return new Color(display, 250, 222, 222);
  }

  /**
   * Returns the background color used to highlight insert text for next edit suggestions.
   */
  public static Color getNesInsertBackground(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 43, 45, 35);
    }
    return new Color(display, 225, 250, 227);
  }

  /**
   * Returns the highlight color used to highlight replace text for next edit suggestions.
   */
  public static Color getNesReplaceHighlight(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 64, 27, 29);
    } else {
      return new Color(display, 255, 161, 161);
    }
  }

  /**
   * Returns the highlight color used to highlight insert text for next edit suggestions.
   */
  public static Color getNesInsertHighlight(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 68, 75, 43);
    }
    return new Color(display, 108, 190, 114);
  }

  /**
   * Returns the border color for the NES bottom bar.
   */
  public static Color getNesBottomBarBorderColor(Display display) {
    return new Color(display, 53, 132, 241);
  }

  /**
   * Returns the color for the filled portion of the context size donut.
   */
  public static Color getDonutFilledColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 223, 225, 229); // #DFE1E5
    }
    return new Color(display, 108, 112, 126); // #6C707E
  }

  /**
   * Returns the warning color for the filled portion when utilisation is high (>= 90%).
   */
  public static Color getDonutWarningColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 242, 197, 92); // #F2C55C
    }
    return new Color(display, 255, 175, 15); // #FFAF0F
  }

  /**
   * Returns the color for the track portion of the context size donut.
   */
  public static Color getDonutTrackColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 111, 115, 122); // #6F737A
    }
    return new Color(display, 223, 225, 229); // #DFE1E5
  }

  /**
   * Returns the active (blue) fill color for the usage bar.
   */
  public static Color getUsageBarActiveColor(Display display) {
    return new Color(display, 53, 116, 240);
  }

  /**
   * Returns the approaching (yellow) fill color for the usage bar.
   */
  public static Color getQuotaApproachingColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 242, 197, 92); // #F2C55C
    }
    return new Color(display, 255, 175, 15); // #FFAF0F
  }

  /**
   * Returns the exhausted (red) fill color for the usage bar.
   */
  public static Color getQuotaExhaustedColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 224, 81, 81); // #E05151
    }
    return new Color(display, 229, 87, 101); // #E55765
  }

  /**
   * Returns the remaining (gray) track color for the usage bar.
   */
  public static Color getUsageBarRemainingColor(Display display) {
    return new Color(display, 223, 225, 229);
  }

  /**
   * Returns the text color used in the chat top banner.
   */
  public static Color getTopBannerTextColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 164, 164, 164); // #A4A4A4
    }
    return new Color(display, 128, 128, 128); // #808080
  }

  /**
   * Returns the active color for the quota pie chart border and fill.
   */
  public static Color getQuotaPieActiveColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 188, 187, 187); // #BCBBBB
    }
    return new Color(display, 128, 128, 128); // #808080
  }
}

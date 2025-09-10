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
   * Get the border color for the top banner based on the current theme.
   */
  public static Color getTopBannerBorderColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 100, 100, 100);
    }
    return new Color(display, 216, 216, 216);
  }

  /**
   * Get the button focus background color based on the current theme.
   */
  public static Color getButtonFocusBgColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 100, 100, 100);
    }
    return new Color(display, 216, 216, 216);
  }

  /**
   * Get the button border color.
   */
  public static Color getButtonBorderColor(Display display) {
    return new Color(display, 55, 134, 246);
  }

  /**
   * Get the Windows Chat History current item border color based on the current theme: #306B98 (light) or #366D91
   * (dark).
   */
  public static Color getWindowsChatHistoryCurrentItemBorderColor(Display display) {
    if (UiUtils.isDarkTheme()) {
      return new Color(display, 54, 109, 145);
    }
    return new Color(display, 48, 107, 152);
  }

}

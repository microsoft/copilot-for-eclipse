// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.osgi.framework.Bundle;

/**
 * Unit tests for {@link UiUtils#isDarkTheme()} and {@link UiUtils#isDark(RGB)}.
 *
 * <p>Theme detection resolves the active e4 CSS theme reflectively through the theme bundle's own
 * class loader, so no compile-time reference to the friend-restricted
 * {@code org.eclipse.e4.ui.css.swt.theme} package is required in production. These tests drive that
 * logic through mocked {@link PlatformUI} and {@link Platform} statics, covering both the case where
 * e4 CSS theming is available and the graceful fallback to the widget-background luminance when it
 * is not. The pure luma decision is exercised directly via {@link UiUtils#isDark(RGB)}.
 */
class UiUtilsThemeTest {

  private static final String THEME_BUNDLE = "org.eclipse.e4.ui.css.swt.theme";
  private static final String DARK_THEME_ID = "org.eclipse.e4.ui.css.theme.e4_dark";
  private static final String LIGHT_THEME_ID = "org.eclipse.e4.ui.css.theme.e4_default";

  // ---------------------------------------------------------------------------
  // Theming available: active theme id resolved via reflection.
  // ---------------------------------------------------------------------------

  @Test
  void isDarkTheme_activeThemeIsDark_returnsTrue() throws Exception {
    try (MockedStatic<PlatformUI> platformUi = mockStatic(PlatformUI.class);
        MockedStatic<Platform> platform = mockStatic(Platform.class)) {
      IWorkbench workbench = givenThemingAvailable(platformUi, platform);
      givenActiveTheme(workbench, DARK_THEME_ID);

      assertTrue(UiUtils.isDarkTheme());
    }
  }

  @Test
  void isDarkTheme_activeThemeIsLight_returnsFalse() throws Exception {
    try (MockedStatic<PlatformUI> platformUi = mockStatic(PlatformUI.class);
        MockedStatic<Platform> platform = mockStatic(Platform.class)) {
      IWorkbench workbench = givenThemingAvailable(platformUi, platform);
      givenActiveTheme(workbench, LIGHT_THEME_ID);

      assertFalse(UiUtils.isDarkTheme());
    }
  }

  @Test
  void isDarkTheme_activeThemeIdMixedCase_returnsTrue() throws Exception {
    try (MockedStatic<PlatformUI> platformUi = mockStatic(PlatformUI.class);
        MockedStatic<Platform> platform = mockStatic(Platform.class)) {
      IWorkbench workbench = givenThemingAvailable(platformUi, platform);
      givenActiveTheme(workbench, "com.example.Solarized.DARK.Theme");

      assertTrue(UiUtils.isDarkTheme());
    }
  }

  // ---------------------------------------------------------------------------
  // Theming unavailable: fall back to the actual widget-background luminance
  // (never the stale persisted themeid, which may not reflect the real appearance).
  // ---------------------------------------------------------------------------

  @Test
  void isDarkTheme_activeThemeNull_fallsBackToBackground() throws Exception {
    try (MockedStatic<PlatformUI> platformUi = mockStatic(PlatformUI.class);
        MockedStatic<Platform> platform = mockStatic(Platform.class)) {
      IWorkbench workbench = givenThemingAvailable(platformUi, platform);
      givenActiveTheme(workbench, null);

      assertEquals(actualWidgetBackgroundIsDark(), UiUtils.isDarkTheme());
    }
  }

  @Test
  void isDarkTheme_themeBundleAbsent_fallsBackToBackground() {
    try (MockedStatic<PlatformUI> platformUi = mockStatic(PlatformUI.class);
        MockedStatic<Platform> platform = mockStatic(Platform.class)) {
      platform.when(() -> Platform.getBundle(THEME_BUNDLE)).thenReturn(null);

      assertEquals(actualWidgetBackgroundIsDark(), UiUtils.isDarkTheme());
    }
  }

  @Test
  void isDarkTheme_themeServiceNull_fallsBackToBackground() throws Exception {
    try (MockedStatic<PlatformUI> platformUi = mockStatic(PlatformUI.class);
        MockedStatic<Platform> platform = mockStatic(Platform.class)) {
      IWorkbench workbench = givenThemingAvailable(platformUi, platform);
      doReturn(null).when(workbench).getService(IThemeEngine.class);

      assertEquals(actualWidgetBackgroundIsDark(), UiUtils.isDarkTheme());
    }
  }

  @Test
  void isDarkTheme_reflectionFailure_fallsBackToBackgroundWithoutThrowing() throws Exception {
    try (MockedStatic<PlatformUI> platformUi = mockStatic(PlatformUI.class);
        MockedStatic<Platform> platform = mockStatic(Platform.class)) {
      IWorkbench workbench = mock(IWorkbench.class);
      platformUi.when(PlatformUI::getWorkbench).thenReturn(workbench);

      Bundle themeBundle = mock(Bundle.class);
      doThrow(new ClassNotFoundException()).when(themeBundle)
          .loadClass("org.eclipse.e4.ui.css.swt.theme.IThemeEngine");
      platform.when(() -> Platform.getBundle(THEME_BUNDLE)).thenReturn(themeBundle);

      assertEquals(actualWidgetBackgroundIsDark(), UiUtils.isDarkTheme());
    }
  }

  // ---------------------------------------------------------------------------
  // Pure luma decision (ITU-R BT.601). No SWT Display required.
  // ---------------------------------------------------------------------------

  @Test
  void isDark_black_returnsTrue() {
    assertTrue(UiUtils.isDark(new RGB(0, 0, 0)));
  }

  @Test
  void isDark_white_returnsFalse() {
    assertFalse(UiUtils.isDark(new RGB(255, 255, 255)));
  }

  @Test
  void isDark_belowMidGray_returnsTrue() {
    // luma = 100 (< 128) -> dark
    assertTrue(UiUtils.isDark(new RGB(100, 100, 100)));
  }

  @Test
  void isDark_aboveMidGray_returnsFalse() {
    // luma = 160 (>= 128) -> light
    assertFalse(UiUtils.isDark(new RGB(160, 160, 160)));
  }

  @Test
  void isDark_appliesPerceptualWeighting() {
    // Pure green is perceived as bright (luma ~= 149.7 -> light), while pure blue is perceived as
    // dark (luma ~= 29.1 -> dark), even though a plain RGB average would rank them identically.
    assertFalse(UiUtils.isDark(new RGB(0, 255, 0)));
    assertTrue(UiUtils.isDark(new RGB(0, 0, 255)));
  }

  // ---------------------------------------------------------------------------
  // Helpers.
  // ---------------------------------------------------------------------------

  private static IWorkbench givenThemingAvailable(MockedStatic<PlatformUI> platformUi,
      MockedStatic<Platform> platform) throws ClassNotFoundException {
    IWorkbench workbench = mock(IWorkbench.class);
    platformUi.when(PlatformUI::getWorkbench).thenReturn(workbench);

    Bundle themeBundle = mock(Bundle.class);
    doReturn(IThemeEngine.class).when(themeBundle)
        .loadClass("org.eclipse.e4.ui.css.swt.theme.IThemeEngine");
    doReturn(ITheme.class).when(themeBundle)
        .loadClass("org.eclipse.e4.ui.css.swt.theme.ITheme");
    platform.when(() -> Platform.getBundle(THEME_BUNDLE)).thenReturn(themeBundle);
    return workbench;
  }

  private static void givenActiveTheme(IWorkbench workbench, String themeId) {
    IThemeEngine themeEngine = mock(IThemeEngine.class);
    doReturn(themeEngine).when(workbench).getService(IThemeEngine.class);

    ITheme activeTheme = null;
    if (themeId != null) {
      activeTheme = mock(ITheme.class);
      when(activeTheme.getId()).thenReturn(themeId);
    }
    when(themeEngine.getActiveTheme()).thenReturn(activeTheme);
  }

  /**
   * Reads the real widget-background color the same way the production fallback does, so the
   * theming-unavailable tests can assert that {@link UiUtils#isDarkTheme()} delegates to the
   * background luminance regardless of the concrete theme of the test environment.
   */
  private static boolean actualWidgetBackgroundIsDark() {
    Display display = Display.getDefault();
    RGB[] holder = new RGB[1];
    display.syncExec(() ->
        holder[0] = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getRGB());
    return UiUtils.isDark(holder[0]);
  }
}

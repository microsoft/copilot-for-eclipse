package com.microsoft.copilot.eclipse.ui.utils;

import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.widgets.Control;

/**
 * Utility class for adding accessibility properties to UI components.
 */
public class AccessibilityUtils {

  /**
   * Adds an accessibility name to the given UI component.
   */
  public static void addAccessibilityNameForUiComponent(Control control, String name) {
    addAccessibilityPropertiesForUiComponent(control, name, null);
  }

  /**
   * Adds an accessibility description to the given UI component.
   */
  public static void addAccessibilityDescriptionForUiComponent(Control control, String description) {
    addAccessibilityPropertiesForUiComponent(control, null, description);
  }

  /**
   * Adds accessibility name and description to the given UI component.
   */
  public static void addAccessibilityPropertiesForUiComponent(Control control, String name, String description) {
    control.getAccessible().addAccessibleListener(new AccessibleAdapter() {
      @Override
      public void getName(AccessibleEvent e) {
        e.result = name;
      }

      public void getDescription(AccessibleEvent e) {
        e.result = description;
      }
    });
  }
}

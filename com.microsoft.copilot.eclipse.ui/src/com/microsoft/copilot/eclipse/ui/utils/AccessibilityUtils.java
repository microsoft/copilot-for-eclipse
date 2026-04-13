package com.microsoft.copilot.eclipse.ui.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.microsoft.copilot.eclipse.ui.swt.CssConstants;

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

  /**
   * Adds focus border styling to a Composite widget. When the widget gains focus via keyboard (Tab), a colored border
   * is drawn around it to provide a visual hint. The border is not shown when focus is gained via mouse click. Also
   * enables tab traversal for the widget.
   *
   * @param composite the composite widget to add focus border styling and tab traversal to
   */
  public static void addFocusBorderToComposite(Composite composite) {
    final boolean[] showFocusBorder = { false };
    final boolean[] mousePressed = { false };

    // Enable tab traversal
    composite.addTraverseListener(e -> {
      if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
        e.doit = true;
      }
    });

    // Track mouse press to distinguish keyboard vs mouse focus
    composite.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        mousePressed[0] = true;
      }
    });

    composite.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        // Defer the border decision to allow mouseDown event to be processed first,
        // since focusGained fires before mouseDown when clicking
        composite.getDisplay().asyncExec(() -> {
          if (composite.isDisposed() || !composite.isFocusControl()) {
            return;
          }
          if (!mousePressed[0]) {
            // Traverse up to find parent ScrolledComposite and ensure focused widget is visible,
            // only auto scroll when focus is gained via keyboard
            Composite parent = composite.getParent();
            while (parent != null) {
              if (parent instanceof ScrolledComposite scrolledComposite) {
                scrolledComposite.showControl(composite);
                break;
              }
              parent = parent.getParent();
            }
          }
          // Only show border if focus was gained via keyboard (not mouse click)
          showFocusBorder[0] = !mousePressed[0];
          mousePressed[0] = false;
          composite.redraw();
        });
      }

      @Override
      public void focusLost(FocusEvent e) {
        showFocusBorder[0] = false;
        mousePressed[0] = false;
        composite.redraw();
      }
    });

    composite.addPaintListener(e -> {
      if (showFocusBorder[0]) {
        Rectangle clientArea = composite.getClientArea();
        Color focusBorderColor = CssConstants.getWidgetFocusBorderColor(composite.getDisplay());
        e.gc.setForeground(focusBorderColor);
        e.gc.setLineWidth(1);
        e.gc.drawRectangle(0, 0, clientArea.width - 1, clientArea.height - 1);
      }
    });
  }
}

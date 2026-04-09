package com.microsoft.copilot.eclipse.ui.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

class QuotaWarningNotificationPopupTest {

  private static final String WARNING_MESSAGE = "You have used 90% of your quota.";

  private Shell shell;
  private Composite parent;

  @BeforeEach
  void setUp() {
    SwtUtils.invokeOnDisplayThread(() -> {
      shell = new Shell(Display.getDefault());
      parent = new Composite(shell, SWT.NONE);
    });
  }

  @AfterEach
  void tearDown() {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
  }

  @Test
  void testCreateNotificationTitle_UsesLocalizedTitleText() {
    SwtUtils.invokeOnDisplayThread(() -> {
      QuotaWarningNotificationPopup popup = new QuotaWarningNotificationPopup(Display.getDefault(), WARNING_MESSAGE,
          1000);
      Image icon = new Image(Display.getDefault(), 1, 1);

      try (MockedStatic<UiUtils> uiUtilsMock = Mockito.mockStatic(UiUtils.class)) {
        uiUtilsMock.when(() -> UiUtils.buildImageFromPngPath("/icons/github_copilot.png")).thenReturn(icon);

        Control title = invokeCreateNotificationTitle(popup, parent);
        Label titleLabel = findLabelByText((Composite) title, Messages.quotaWarning_title);

        assertNotNull(titleLabel);
        assertEquals(Messages.quotaWarning_title, titleLabel.getText());
      } finally {
        if (!icon.isDisposed()) {
          icon.dispose();
        }
      }
    });
  }

  @Test
  void testCreateNotificationContent_UsesMessageAndBudgetAction() {
    SwtUtils.invokeOnDisplayThread(() -> {
      QuotaWarningNotificationPopup popup = new QuotaWarningNotificationPopup(Display.getDefault(), WARNING_MESSAGE,
          1000);

      try (MockedStatic<UiUtils> uiUtilsMock = Mockito.mockStatic(UiUtils.class)) {
        uiUtilsMock.when(() -> UiUtils.openLink(UiConstants.MANAGE_COPILOT_OVERAGE_URL)).thenReturn(true);

        Composite content = invokeCreateNotificationContent(popup, parent, WARNING_MESSAGE);
        Label messageLabel = findLabelByText(content, WARNING_MESSAGE);
        Button closeButton = findButtonByText(content, Messages.quotaWarning_closeButton);
        Button increaseBudgetButton = findButtonByText(content, Messages.quotaWarning_increaseBudgetButton);

        assertNotNull(messageLabel);
        assertNotNull(closeButton);
        assertNotNull(increaseBudgetButton);

        increaseBudgetButton.notifyListeners(SWT.Selection, new Event());

        uiUtilsMock.verify(() -> UiUtils.openLink(UiConstants.MANAGE_COPILOT_OVERAGE_URL));
      }
    });
  }

  private Control invokeCreateNotificationTitle(QuotaWarningNotificationPopup popup, Composite titleParent) {
    try {
      Method method = QuotaWarningNotificationPopup.class.getDeclaredMethod("createNotificationTitle",
          Composite.class);
      method.setAccessible(true);
      return (Control) method.invoke(popup, titleParent);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Failed to invoke createNotificationTitle", e);
    }
  }

  private Composite invokeCreateNotificationContent(QuotaWarningNotificationPopup popup, Composite contentParent,
      String message) {
    try {
      Method method = QuotaWarningNotificationPopup.class.getDeclaredMethod("createNotificationContent",
          Composite.class, String.class);
      method.setAccessible(true);
      return (Composite) method.invoke(popup, contentParent, message);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Failed to invoke createNotificationContent", e);
    }
  }

  private Label findLabelByText(Composite parentControl, String text) {
    for (Control child : parentControl.getChildren()) {
      if (child instanceof Label label && text.equals(label.getText())) {
        return label;
      }
      if (child instanceof Composite composite) {
        Label nestedLabel = findLabelByText(composite, text);
        if (nestedLabel != null) {
          return nestedLabel;
        }
      }
    }
    return null;
  }

  private Button findButtonByText(Composite parentControl, String text) {
    for (Control child : parentControl.getChildren()) {
      if (child instanceof Button button && text.equals(button.getText())) {
        return button;
      }
      if (child instanceof Composite composite) {
        Button nestedButton = findButtonByText(composite, text);
        if (nestedButton != null) {
          return nestedButton;
        }
      }
    }
    return null;
  }
}
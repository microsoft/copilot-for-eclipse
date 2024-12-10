package com.microsoft.copilot.eclipse.ui.utils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Utilities for SWT. *
 */
public class SwtUtils {

  private SwtUtils() {
    // prevent instantiation
  }

  /**
   * Invokes the given runnable on the display thread.
   */
  public static void invokeOnDisplayThread(Runnable runnable) {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
    if (windows != null && windows.length > 0) {
      Display display = windows[0].getShell().getDisplay();
      display.syncExec(runnable);
    } else {
      runnable.run();
    }
  }

  /**
   * Invokes the given runnable on the display thread.
   *
   * @param runnable the runnable to invoke
   * @param control the control used for the display
   */
  public static void invokeOnDisplayThread(Runnable runnable, Control control) {
    if (Objects.isNull(control)) {
      invokeOnDisplayThread(runnable);
    } else {
      Display display = control.getDisplay();
      if (display.getThread() == Thread.currentThread()) {
        runnable.run();
      } else {
        display.syncExec(runnable);
      }
    }
  }

  /**
   * Get the active editor part from workbench.
   */
  @Nullable
  public static IEditorPart getActiveEditorPart() {
    AtomicReference<IEditorPart> ref = new AtomicReference<>();
    invokeOnDisplayThread(() -> {
      IWorkbench workbench = PlatformUI.getWorkbench();
      IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
      if (window != null) {
        ref.set(window.getActivePage().getActiveEditor());
      }
    });
    return ref.get();
  }
}

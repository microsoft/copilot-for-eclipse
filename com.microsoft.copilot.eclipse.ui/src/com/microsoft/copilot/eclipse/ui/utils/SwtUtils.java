package com.microsoft.copilot.eclipse.ui.utils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

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

  /**
   * This method retrieves the active workbench window from the event and then gets the shell associated with that
   * window. It is more specific to the Eclipse framework and is typically used in handlers for commands or actions
   * within the Eclipse environment.
   *
   * @throws ExecutionException if the active workbench window cannot be retrieved from the event.
   */
  public static Shell getShellFromEvent(ExecutionEvent event) throws ExecutionException {
    return HandlerUtil.getActiveWorkbenchWindowChecked(event).getShell();
  }

  /**
   * Get current display.
   */
  public static Display getDisplay() {
    Display display = Display.getCurrent();
    if (display == null) {
      display = Display.getDefault();
    }
    return display;
  }

  /**
   * Check if the given text viewer is editable.
   */
  public static boolean isEditable(ITextViewer textViewer) {
    AtomicReference<Boolean> ref = new AtomicReference<>();
    invokeOnDisplayThread(() -> {
      if (textViewer != null) {
        ref.set(textViewer.isEditable());
      }
    });
    return ref.get();
  }
}

package com.microsoft.copilot.eclipse.ui.utils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyledText;
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

  /**
   * Redraw the block ghost texts at the given model offset. This function can be used when the model offset if out of
   * the text editor's visible range.
   */
  public static void redrawBlockLineAtModelOffset(ITextViewer textViewer, int modelOffset) {
    StyledText styledText = textViewer.getTextWidget();
    invokeOnDisplayThread(() -> {
      int widgetOffset = UiUtils.modelOffset2WidgetOffset(textViewer, modelOffset);
      if (widgetOffset < 0) {
        // Due to the model offset flicker, when the function block is collapsed, the widget offset may be negative in
        // the middle state. In this case, we will abort the redraw and the redraw will be triggered again when the
        // model offset flicker back to the correct value.
        return;
      }

      int line = styledText.getLineAtOffset(widgetOffset);
      // Block ghost text always starts at the beginning of the line.
      int x = styledText.getLeftMargin();
      int y = styledText.getLinePixel(line);
      int height = styledText.getLineHeight(line);

      // If only use styledText.getClientArea().width, when the ghost text is out of the editor's view, it will cause
      // the rendering issue. So we need to add the horizontal scroll offset that out of the editor's view as well.
      int width = styledText.getClientArea().width + styledText.getHorizontalPixel();
      int blockGhostTextFirstLine = Math.min(line + 1, styledText.getLineCount());

      // Clear the line vertical indent (the empty background)
      if (blockGhostTextFirstLine != styledText.getLineCount()
          && styledText.getLineVerticalIndent(blockGhostTextFirstLine) > 0) {
        height += styledText.getLineVerticalIndent(blockGhostTextFirstLine);
        styledText.setLineVerticalIndent(blockGhostTextFirstLine, 0);
      }

      styledText.redraw(x, y, width, height, true);
    }, styledText);
  }
}

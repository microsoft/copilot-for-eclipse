package com.microsoft.copilot.eclipse.ui.utils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
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

  private static final String INLINE_ANNOTATION_COLOR_KEY = "org.eclipse.ui.editors.inlineAnnotationColor";
  private static final int DEFAULT_GHOST_TEXT_SCALE = 128;

  /**
   * Invokes the given runnable on the display thread.
   */
  public static void invokeOnDisplayThread(Runnable runnable) {
    Display currentDisplay = Display.getCurrent();
    if (currentDisplay != null) {
      runnable.run();
      return;
    }

    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
    if (windows != null && windows.length > 0) {
      Shell shell = windows[0].getShell();
      if (shell != null && !shell.isDisposed()) {
        Display display = shell.getDisplay();
        display.syncExec(runnable);
        return;
      }
    }

    Display.getDefault().syncExec(runnable);
  }

  /**
   * Invokes the given runnable on the display thread.
   *
   * @param runnable the runnable to invoke
   * @param control the control used for the display
   */
  public static void invokeOnDisplayThread(Runnable runnable, Control control) {
    if (Objects.isNull(control) || control.isDisposed()) {
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
   * Invokes the given runnable on the display thread asynchronously.
   *
   * @param runnable the runnable to invoke
   */
  public static void invokeOnDisplayThreadAsync(Runnable runnable) {
    Display currentDisplay = Display.getCurrent();
    if (currentDisplay != null) {
      runnable.run();
      return;
    }

    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
    if (windows != null && windows.length > 0) {
      Shell shell = windows[0].getShell();
      if (shell != null && !shell.isDisposed()) {
        Display display = shell.getDisplay();
        display.asyncExec(runnable);
        return;
      }
    }

    Display.getDefault().asyncExec(runnable);
  }

  /**
   * Get the active editor part from workbench.
   */
  @Nullable
  public static IEditorPart getActiveEditorPart() {
    AtomicReference<IEditorPart> ref = new AtomicReference<>();
    invokeOnDisplayThread(() -> {
      IWorkbenchPage page = UiUtils.getActivePage();
      if (page != null) {
        ref.set(page.getActiveEditor());
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
    AtomicReference<Boolean> ref = new AtomicReference<>(false);
    invokeOnDisplayThread(() -> {
      if (textViewer != null) {
        ref.set(textViewer.isEditable());
      }
    });
    return ref.get();
  }

  /**
   * Redraw the block ghost texts at the given model offset. If forceRedraw is false, redraw will only be triggered when
   * the model offset if out of the text editor's visible range.
   */
  public static void redrawBlockLineAtModelOffset(ITextViewer textViewer, int modelOffset, boolean forceRedraw) {
    if (textViewer == null || textViewer.getDocument() == null) {
      return;
    }

    if (modelOffset < 0 || modelOffset >= textViewer.getDocument().getLength()) {
      return;
    }

    StyledText styledText = textViewer.getTextWidget();
    int widgetOffset = UiUtils.modelOffset2WidgetOffset(textViewer, modelOffset);

    if (widgetOffset < 0) {
      // Due to the model offset flicker, when the function block is collapsed, the widget offset may be negative in
      // the middle state. In this case, we will abort the redraw and the redraw will be triggered again when the
      // model offset flicker back to the correct value.
      return;
    }
    if (forceRedraw || isWidgetOffsetOutOfTextEditorVisibleRange(textViewer, widgetOffset)) {
      invokeOnDisplayThread(() -> {
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

  /**
   * Check if the widget offset is out of the text editor's visible range.
   */
  public static boolean isWidgetOffsetOutOfTextEditorVisibleRange(ITextViewer textViewer, int widgetOffset) {
    StyledText styledText = textViewer.getTextWidget();
    AtomicReference<Boolean> ref = new AtomicReference<>();
    invokeOnDisplayThread(() -> {
      try {
        // Get the caret line number
        int lineNumber = styledText.getLineAtOffset(widgetOffset);

        // Check vertical boundaries
        int topIndex = textViewer.getTopIndex();
        int bottomIndex = textViewer.getBottomIndex();
        if (lineNumber < topIndex || lineNumber > bottomIndex) {
          ref.set(true);
          return;
        }

        // Check horizontal boundaries
        int horizontalPixel = styledText.getHorizontalPixel();
        int clientWidth = styledText.getClientArea().width;
        int offsetX = styledText.getLocationAtOffset(widgetOffset).x;
        ref.set(offsetX < horizontalPixel || offsetX > (horizontalPixel + clientWidth));
      } catch (IllegalArgumentException e) {
        ref.set(true);
      }
    }, styledText);
    return ref.get();
  }

  /**
   * Get the registered inline annotation color.
   */
  @Nullable
  public static Color getRegisteredInlineAnnotationColor(Display display) {
    ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
    if (colorRegistry == null) {
      return null;
    }
    return colorRegistry.get(INLINE_ANNOTATION_COLOR_KEY);
  }

  /**
   * Get the default ghost text color.
   */
  public static Color getDefaultGhostTextColor(Display display) {
    return new Color(display, new RGB(DEFAULT_GHOST_TEXT_SCALE, DEFAULT_GHOST_TEXT_SCALE, DEFAULT_GHOST_TEXT_SCALE));
  }

  /**
   * Copy the given text to the clipboard.
   */
  public static void copyToClipboard(Control control, String text) {
    invokeOnDisplayThread(() -> {
      Clipboard clipboard = new Clipboard(Display.getDefault());
      clipboard.setContents(new Object[] { text }, new Transfer[] { TextTransfer.getInstance() });
      clipboard.dispose();
    }, control);
  }
}

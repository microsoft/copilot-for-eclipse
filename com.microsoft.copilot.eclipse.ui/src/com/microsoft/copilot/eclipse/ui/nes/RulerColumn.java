package com.microsoft.copilot.eclipse.ui.nes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.AbstractRulerColumn;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationHoverExtension;
import org.eclipse.jface.text.source.ILineRange;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.rulers.IContributedRulerColumn;
import org.eclipse.ui.texteditor.rulers.RulerColumnDescriptor;

import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Ruler column to display Next Edit Suggestion icons.
 */
public class RulerColumn extends AbstractRulerColumn implements IContributedRulerColumn {

  private static final int COLUMN_PADDING = 2;
  private ITextViewer viewer;
  private StyledText text;
  private Image icon;
  private ITextEditor editor;
  private RulerColumnDescriptor descriptor;
  private Rectangle lastIconBounds;
  private boolean enableIconRendering = true; // Flag to control whether to render icon
  private static final List<RulerColumn> liveColumns = Collections.synchronizedList(new ArrayList<>());

  private final class SuggestionAnnotationHover implements IAnnotationHover, IAnnotationHoverExtension {
    @Override
    public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
      ILineRange range = getHoverLineRange(sourceViewer, lineNumber);
      if (range == null) {
        return null;
      }
      return "Copilot Next Edit Suggestion\n" + "- Tab: Accept suggestion\n" + "- Esc: Dismiss suggestion";
    }

    @Override
    public Object getHoverInfo(ISourceViewer sourceViewer, ILineRange lineRange, int visibleNumberOfLines) {
      return "Copilot Next Edit Suggestion\n" + "- Tab: Accept suggestion\n" + "- Esc: Dismiss suggestion";
    }

    @Override
    public ILineRange getHoverLineRange(ISourceViewer viewer, int lineNumber) {
      if (text == null || text.isDisposed() || RulerColumn.this.viewer == null) {
        return null;
      }

      // Check if icon rendering is enabled
      if (!enableIconRendering) {
        return null;
      }

      RenderManager manager = resolveNesRenderManager();
      if (manager == null) {
        return null;
      }

      int suggestionLine = manager.getSuggestionLine();
      if (suggestionLine == -1) {
        return null;
      }

      // Need to match the exact line for this function will be called for each line in the hover area.
      if (lineNumber != suggestionLine) {
        return null;
      }

      return new LineRange(suggestionLine, 1);
    }

    @Override
    public IInformationControlCreator getHoverControlCreator() {
      return null;
    }

    @Override
    public boolean canHandleMouseCursor() {
      return false;
    }
  }

  /**
   * Constructor.
   */
  public RulerColumn() {
    ensureIcon();
    setHover(new SuggestionAnnotationHover());
  }

  @Override
  public Control createControl(CompositeRuler parentRuler, Composite parentControl) {
    this.viewer = parentRuler.getTextViewer();
    this.text = viewer.getTextWidget();
    Control control = super.createControl(parentRuler, parentControl);
    control.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        // Only handle left mouse button (e.button: 1=left, 2=middle, 3=right)
        if (e.button != 1) {
          return;
        }
        if (!isOnIcon(e.x, e.y)) {
          return;
        }
        RenderManager manager = resolveNesRenderManager();
        if (manager != null && !control.isDisposed()) {
          Point displayPoint = control.toDisplay(e.x, e.y);
          Point textPoint = text.toControl(displayPoint);
          manager.openActionMenu(textPoint.x, textPoint.y);
          // Temporarily capture mouse events to ensure the action menu receives initial events properly.
          // And otherwise parent controls may intercept those events leading to unexpected behavior.
          // We need to release the capture asynchronously after the menu is fully initialized and ready
          // to handle mouse events on its own, preventing event handling conflicts.
          control.setCapture(true);
          SwtUtils.invokeOnDisplayThreadAsync(() -> {
            if (!control.isDisposed()) {
              control.setCapture(false);
            }
          }, control);
        }
      }
    });
    control.addDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent e) {
        disposeResources();
      }
    });
    return control;
  }

  @Override
  protected void paintLine(GC gc, int modelLine, int widgetLine, int linePixel, int lineHeight) {
    gc.setBackground(getControl().getBackground());
    gc.fillRectangle(0, linePixel, getWidth(), lineHeight);

    Rectangle bounds = getSuggestionIconBounds(modelLine, widgetLine);
    if (bounds == null) {
      return;
    }
    lastIconBounds = bounds;
    
    // Get original icon dimensions for scaling source
    Rectangle iconBounds = icon.getBounds();
    
    // Draw icon with scaling from original size to actual drawn bounds
    gc.drawImage(icon, 0, 0, iconBounds.width, iconBounds.height, 
                 bounds.x, bounds.y, bounds.width, bounds.height);
  }

  private boolean isOnIcon(int x, int y) {
    Rectangle bounds = getSuggestionIconBounds(-1, -1);
    if (bounds == null) {
      return false;
    }

    // getSuggestionIconBounds already calculated the exact drawn bounds
    return x >= bounds.x && x <= bounds.x + bounds.width 
        && y >= bounds.y && y <= bounds.y + bounds.height;
  }

  /**
   * Gets the actual drawn bounds of the suggestion icon (including scaling and centering).
   * Returns the precise rectangle where the icon is drawn, accounting for line height constraints.
   *
   * @param expectedModelLine the expected model line number, or -1 to skip check
   * @param expectedWidgetLine the expected widget line number, or -1 to skip check
   * @return the actual drawn icon bounds, or null if no icon should be drawn
   */
  private Rectangle getSuggestionIconBounds(int expectedModelLine, int expectedWidgetLine) {
    if (text == null || text.isDisposed()) {
      return null;
    }
    if (viewer == null) {
      return null;
    }
    if (icon == null || icon.isDisposed()) {
      return null;
    }
    // Check if icon rendering is enabled
    if (!enableIconRendering) {
      return null;
    }

    RenderManager manager = resolveNesRenderManager();
    if (manager == null) {
      return null;
    }

    int suggestionLine = manager.getSuggestionLine();
    if (suggestionLine == -1) {
      return null;
    }

    // Check expected model line and widget line to match exact suggestion line
    if (expectedModelLine != -1 && expectedModelLine != suggestionLine) {
      return null;
    }

    int widgetLine = UiUtils.modelLine2WidgetLine(viewer, suggestionLine);
    if (widgetLine == -1) {
      return null;
    }

    if (expectedWidgetLine != -1 && expectedWidgetLine != widgetLine) {
      return null;
    }

    int linePixel = text.getLinePixel(widgetLine);
    int lineHeight = text.getLineHeight(widgetLine);

    // Get actual icon dimensions
    Rectangle iconBounds = icon.getBounds();
    int iconWidth = iconBounds.width;
    int iconHeight = iconBounds.height;

    // Scale to fit within line height if needed, maintaining aspect ratio
    int actualHeight = Math.min(iconHeight, lineHeight - 2);
    int actualWidth = iconWidth;
    if (actualHeight < iconHeight) {
      actualWidth = (iconWidth * actualHeight) / iconHeight;
    }

    // Get the available space for icon
    int iconSize = Math.max(iconBounds.width, iconBounds.height);
    
    // Center the icon within the column and line
    int drawX = Math.max(1, (getWidth() - iconSize) / 2);
    int offsetX = (iconSize - actualWidth) / 2;
    int offsetY = (actualHeight < iconSize) ? (iconSize - actualHeight) / 2 : 0;
    int drawY = linePixel + Math.max(0, (lineHeight - actualHeight) / 2);

    return new Rectangle(drawX + offsetX, drawY + offsetY, actualWidth, actualHeight);
  }

  private void ensureIcon() {
    if (icon == null || icon.isDisposed()) {
      ImageDescriptor desc = UiUtils.buildImageDescriptorFromPngPath("/icons/chat/gutter-arrow.png");
      if (desc != null) {
        icon = desc.createImage(true);

        // Update column width based on actual icon size
        if (icon != null && !icon.isDisposed()) {
          Rectangle bounds = icon.getBounds();
          int iconSize = Math.max(bounds.width, bounds.height);
          int requiredWidth = iconSize + COLUMN_PADDING;
          if (getWidth() != requiredWidth) {
            setWidth(requiredWidth);
          }
        }
      }
    }
  }

  private void disposeResources() {
    if (icon != null && !icon.isDisposed()) {
      icon.dispose();
    }
    icon = null;
    RenderManager manager = resolveNesRenderManager();
    if (manager != null) {
      manager.detachColumn(this);
    }
  }

  /**
   * Requests a layout update for the ruler column.
   */
  public void requestLayout() {
    requestLayout(true);
  }

  /**
   * Requests a layout update for the ruler column.
   *
   * @param enableRendering whether to enable icon rendering
   */
  public void requestLayout(boolean enableRendering) {
    this.enableIconRendering = enableRendering;
    Control c = getControl();
    if (c != null && !c.isDisposed()) {
      if (lastIconBounds != null) {
        // Clear the last icon area by filling with background color, In case Eclipse doesn't clear it properly.
        GC gc = new GC(c);
        try {
          gc.setBackground(c.getBackground());
          gc.fillRectangle(lastIconBounds);
        } finally {
          gc.dispose();
        }
        lastIconBounds = null;
      }
      c.redraw();
      c.update();
    }
  }

  /**
   * Clear indentation area in ruler column by manually filling with background color. This is needed because
   * indentation area doesn't correspond to any text line, the icon in indentation area will not be cleared when redraw
   * is called.
   */
  public void clearIndentationArea(int widgetLine, int height) {
    Control c = getControl();
    if (c == null || c.isDisposed() || text == null || text.isDisposed()) {
      return;
    }
    if (widgetLine < 0 || height <= 0) {
      return;
    }

    int linePixel = text.getLinePixel(widgetLine);
    int lineHeight = text.getLineHeight(widgetLine);

    // Fill the indentation area (from line bottom to line bottom + height)
    GC gc = new GC(c);
    try {
      gc.setBackground(c.getBackground());
      gc.fillRectangle(0, linePixel + lineHeight, getWidth(), height);
    } finally {
      gc.dispose();
    }
  }

  /**
   * Gets the list of all live RulerColumn instances.
   *
   * @return List of live RulerColumn instances
   */
  public static List<RulerColumn> getLiveColumns() {
    synchronized (liveColumns) {
      return new ArrayList<>(liveColumns);
    }
  }

  public ITextEditor getTextEditor() {
    return editor;
  }

  @Override
  public RulerColumnDescriptor getDescriptor() {
    return descriptor;
  }

  @Override
  public void setDescriptor(RulerColumnDescriptor descriptor) {
    this.descriptor = descriptor;
  }

  @Override
  public void setEditor(ITextEditor editor) {
    this.editor = editor;
  }

  @Override
  public ITextEditor getEditor() {
    return editor;
  }

  @Override
  public void columnCreated() {
    RenderManager manager = resolveNesRenderManager();
    if (manager != null) {
      manager.attachColumn(this);
    }
    liveColumns.add(this);
  }

  @Override
  public void columnRemoved() {
    RenderManager manager = resolveNesRenderManager();
    if (manager != null) {
      manager.detachColumn(this);
    }
    liveColumns.remove(this);
  }

  private RenderManager resolveNesRenderManager() {
    EditorsManager mgr = CopilotUi.getPlugin().getEditorsManager();
    if (mgr == null) {
      return null;
    }
    if (this.editor != null) {
      return mgr.getNesRenderManager(this.editor);
    }
    return null;
  }
}

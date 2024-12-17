package com.microsoft.copilot.eclipse.ui.utils;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;

/**
 * Utilities for Eclipse UI.
 */
public class UiUtils {

  private UiUtils() {
    // prevent instantiation
  }

  /**
   * Resizes the icon at the given path to the given width and height. Icon size is 16x16 by default, which is the
   * recommended size for toolbar icons. For more details: https://eclipse-platform.github.io/ui-best-practices/#toolbar
   */
  public static ImageDescriptor resizeIcon(String path, int width, int height) {
    ImageLoader loader = new ImageLoader();
    ImageData[] imageDataArray = loader.load(UiUtils.class.getResourceAsStream(path));
    if (imageDataArray.length > 0) {
      ImageData imageData = imageDataArray[0].scaledTo(width, height);
      Image image = new Image(Display.getDefault(), imageData);
      return ImageDescriptor.createFromImage(image);
    }
    return null;
  }

  /**
   * Gets the caret offset of the given text viewer.
   */
  public static int getCaretOffset(ITextViewer textViewer) {
    final AtomicInteger ref = new AtomicInteger(0);
    StyledText styledText = textViewer.getTextWidget();
    SwtUtils.invokeOnDisplayThread(() -> {
      int offset = styledText.getCaretOffset();
      ref.set(offset);
    }, styledText);
    return ref.get();
  }

}

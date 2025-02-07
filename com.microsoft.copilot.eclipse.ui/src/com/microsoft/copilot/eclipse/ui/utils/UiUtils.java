package com.microsoft.copilot.eclipse.ui.utils;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;

/**
 * Utilities for Eclipse UI.
 */
public class UiUtils {

  private UiUtils() {
    // prevent instantiation
  }

  /**
   * Gets the URI of the file opened in the given text editor.
   */
  @Nullable
  public static URI getUriFromTextEditor(ITextEditor editor) {
    IEditorInput input = editor.getEditorInput();
    if (input instanceof IFileEditorInput fileInput) {
      IFile file = fileInput.getFile();
      return file.getLocationURI();
    }

    return null;
  }

  /**
   * Gets the file opened in the given text editor.
   */
  @Nullable
  public static IFile getFileFromEditor(ITextEditor editor) {
    IEditorInput input = editor.getEditorInput();
    if (input instanceof IFileEditorInput fileInput) {
      return fileInput.getFile();
    }
    return null;
  }

  /**
   * Open the given link in a new browser page.
   */
  public static boolean openLink(String link) {
    String encodedUrl = PlatformUtils.escapeSpaceInUrl(link);
    IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
    try {
      IWebBrowser browser = browserSupport.createBrowser(IWorkbenchBrowserSupport.AS_EXTERNAL, null, null, null);
      browser.openURL(new URI(encodedUrl).toURL());
    } catch (Exception e) {
      CopilotCore.LOGGER.error(e);
      return false;
    }
    return true;
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
   * Returns the widget offset that corresponds to the given offset in the viewer's input document or <code>-1</code> if
   * there is no such offset.
   */
  public static int modelOffset2WidgetOffset(ITextViewer textViewer, int offset) {
    return textViewer instanceof ITextViewerExtension5 extension ? extension.modelOffset2WidgetOffset(offset) : offset;
  }

  /**
   * Returns the offset of the viewer's input document that corresponds to the given widget offset or <code>-1</code> if
   * there is no such offset.
   */
  public static int widgetOffset2ModelOffset(ITextViewer textViewer, int offset) {
    return textViewer instanceof ITextViewerExtension5 extension ? extension.widgetOffset2ModelOffset(offset) : offset;
  }

  /**
   * Builds an image descriptor from a PNG file at the given path.
   */
  public static ImageDescriptor buildImageDescriptorFromPngPath(String path) {
    return ImageDescriptor.createFromURL(UiUtils.class.getResource(path));
  }

  /**
   * Refreshes the elements of the command with the given ID.
   */
  public static void refreshCopilotMenu() {
    ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
    if (commandService != null) {
      commandService.refreshElements("com.microsoft.copilot.eclipse.commands.showStatusBarMenu", null);
    }
  }
}

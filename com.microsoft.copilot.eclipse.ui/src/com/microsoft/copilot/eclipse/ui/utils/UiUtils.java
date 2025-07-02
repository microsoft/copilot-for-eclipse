package com.microsoft.copilot.eclipse.ui.utils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.chat.tools.FileToolBase.EditableFileCompareInput;

/**
 * Utilities for Eclipse UI.
 */
public class UiUtils {

  public static final String HAIR_SPACE = "\u200A";
  private static final int MAX_SPACE_TO_ADD = 500;

  private UiUtils() {
    // prevent instantiation
  }

  /**
   * Get the active workbench page.
   */
  @Nullable
  public static IWorkbenchPage getActivePage() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    if (window != null) {
      return window.getActivePage();
    }
    return null;
  }

  public static final Color SLASH_COMMAND_FORGROUND_COLOR = new Color(48, 108, 170);
  public static final Color SLASH_COMMAND_BACKGROUND_COLOR = new Color(228, 244, 255);

  /**
   * Returns the file that is currently opened in the editor.
   */
  @Nullable
  public static IFile getCurrentFile() {
    IWorkbenchPage page = getActivePage();
    if (page == null) {
      return null;
    }

    return getFileFromEditorPart(page.getActiveEditor());
  }

  /**
   * Return the IFile from the given editor part.
   */
  @Nullable
  public static IFile getFileFromEditorPart(IEditorPart editor) {
    if (editor == null) {
      return null;
    }

    IEditorInput input = editor.getEditorInput();
    IFile file = ResourceUtil.getFile(input);
    if (file != null) {
      return file;
    }

    // Check editor type and retrieve file accordingly
    if (editor instanceof ITextEditor textEditor) {
      return getFileFromTextEditor(textEditor);
    } else if (editor instanceof CompareEditor compareEditor) {
      return getFileFromCompareEditor(compareEditor);
    }
    return null;
  }

  /**
   * Gets the file opened in the given text editor.
   */
  @Nullable
  public static IFile getFileFromTextEditor(ITextEditor editor) {
    IEditorInput input = editor.getEditorInput();
    if (input instanceof IFileEditorInput fileInput) {
      return fileInput.getFile();
    }
    return ResourceUtil.getFile(input);
  }

  /**
   * Gets the relative file opened in the given compare editor.
   */
  @Nullable
  public static IFile getFileFromCompareEditor(CompareEditor editor) {
    IEditorInput input = editor.getEditorInput();
    Object compareResult = ((CompareEditorInput) input).getCompareResult();
    if (compareResult instanceof ICompareInput compareInput) {
      ITypedElement left = compareInput.getLeft();
      if (left instanceof EditableFileCompareInput leftNode) {
        return leftNode.getFile();
      }
    }
    return null;
  }

  /**
   * Opens the given file in an editor.
   */
  public static IEditorPart openInEditor(IFile file) {
    if (file == null || !file.exists()) {
      CopilotCore.LOGGER.error(new IllegalArgumentException("Cannot open editor: file is null or doesn't exist"));
      return null;
    }

    try {
      IWorkbenchPage page = getActivePage();
      if (page != null) {
        return IDE.openEditor(page, file);
      }
    } catch (PartInitException e) {
      CopilotCore.LOGGER.error(e);
    }
    return null;
  }

  /**
   * Opens the file in the editor.
   */
  public static List<IFile> getOpenedFiles() {
    IWorkbenchPage page = getActivePage();
    if (page == null) {
      return new ArrayList<>();
    }
    IEditorReference[] editorReferences = page.getEditorReferences();
    ArrayList<IFile> files = new ArrayList<>();
    if (editorReferences == null) {
      return files;
    }
    for (IEditorReference editorRef : editorReferences) {
      IEditorInput input;
      try {

        input = editorRef.getEditorInput();
      } catch (PartInitException e) {
        CopilotCore.LOGGER.error(e);
        continue;
      }
      if (input instanceof IFileEditorInput fileEditorInput) {
        IFile file = fileEditorInput.getFile();
        files.add(file);
      }
    }
    return files;
  }

  /**
   * Returns the part service.
   */
  public static IPartService getPartService() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    if (workbench == null) {
      return null;
    }
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    if (window == null) {
      return null;
    }
    return window.getPartService();
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
   * Resizes the given image to the given width and height.
   */
  public static Image resizeImage(Display display, Image originalImage, int width, int height) {
    ImageData originalData = originalImage.getImageData();
    ImageData resizedData = originalData.scaledTo(width, height);
    return new Image(display, resizedData);
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
   * Builds an image from a PNG file at the given path.
   */
  public static Image buildImageFromPngPath(String path) {
    return buildImageDescriptorFromPngPath(path).createImage();
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

  /**
   * Returns the index of the first word in the given text.
   */
  public static Point getFirstWordIndex(String text) {
    int start = 0;
    int len = text.length();
    while (start < len && Character.isWhitespace(text.charAt(start))) {
      start++;
    }
    int end = start;
    while (end < len && !Character.isWhitespace(text.charAt(end))) {
      end++;
    }
    return new Point(start, end);
  }

  /**
   * Returns the theme color with the given ID.
   * 
   */
  public static Color getThemeColor(String colorId) {
    return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry().get(colorId);
  }

  /**
   * Returns the view with the given ID and type.
   *
   * @param viewId the ID of the view
   * @param viewType the type of the view
   * @return the view or <code>null</code> if the view is not found
   */
  @Nullable
  public static <T> T getView(String viewId, Class<T> viewType) {
    IWorkbenchPage page = getActivePage();
    if (page == null) {
      return null;
    }
    IViewPart view = page.findView(viewId);
    if (view == null) {
      return null;
    }
    return viewType.cast(view);
  }

  /**
   * Create a button only with an icon. As Button is NOT intended to be subclassed, use a factory method to create a
   * custom button.
   */
  public static Button createIconButton(Composite parent, int style) {
    Button result = new Button(parent, style);
    result.setBackground(parent.getBackground());
    result.addPaintListener(e -> {
      // Prevent the default border from being drawn
      e.gc.setBackground(result.getBackground());
      Rectangle bounds = result.getBounds();
      e.gc.fillRectangle(0, 0, bounds.width, bounds.height);

      // Draw the button's image if it has one
      Image image = result.getImage();
      if (image != null) {
        Rectangle imgBounds = image.getBounds();
        int x = (bounds.width - imgBounds.width) / 2;
        int y = (bounds.height - imgBounds.height) / 2;
        int oldAlpha = e.gc.getAlpha();
        if (!result.isEnabled()) {
          e.gc.setAlpha(oldAlpha / 2);
        }
        e.gc.drawImage(image, x, y);
        e.gc.setAlpha(oldAlpha);
      }
    });

    result.addFocusListener(new org.eclipse.swt.events.FocusAdapter() {
      private Color background = result.getBackground();

      @Override
      public void focusGained(org.eclipse.swt.events.FocusEvent e) {
        background = result.getBackground();
        result.setBackground(getThemeColor(UiConstants.HOVER_BACKGROUND));
      }

      @Override
      public void focusLost(org.eclipse.swt.events.FocusEvent e) {
        result.setBackground(background);
      }
    });

    result.addMouseTrackListener(new org.eclipse.swt.events.MouseTrackAdapter() {
      private Color background = result.getBackground();

      @Override
      public void mouseEnter(org.eclipse.swt.events.MouseEvent e) {
        background = result.getBackground();
        result.setBackground(getThemeColor(UiConstants.HOVER_BACKGROUND));
      }

      @Override
      public void mouseExit(org.eclipse.swt.events.MouseEvent e) {
        result.setBackground(background);
      }
    });
    return result;
  }

  /**
   * Returns a bold version of the given font.
   */
  public static Font getBoldFont(Display display, Font originalFont) {
    FontData[] fontData = originalFont.getFontData();
    for (int i = 0; i < fontData.length; i++) {
      fontData[i].setStyle(SWT.BOLD);
    }
    return new Font(display, fontData);
  }

  /**
   * Sets the background of the given composite to the background of its parent. As eclipse css may overwrite the result
   * of setBackground, this method will also add a paint listener to keep the background color consistent.
   */
  public static void useParentBackground(Control control) {
    if (control.getParent() == null) {
      return;
    }
    control.setData(UiConstants.USE_PARENT_BACKGROUND, true);
    control.setBackground(getParentColor(control));

    control.addPaintListener(e -> {
      Color currentParentColor = getParentColor(control);
      if (!currentParentColor.equals(control.getBackground())) {
        if (control instanceof Composite) {
          e.gc.setBackground(currentParentColor);
          Rectangle bounds = control.getBounds();
          e.gc.fillRectangle(0, 0, bounds.width, bounds.height);
        } else {
          control.getDisplay().asyncExec(() -> {
            if (!control.isDisposed()) {
              control.setBackground(currentParentColor);
              control.redraw();
            }
          });
        }
      }
    });
  }

  private static Color getParentColor(Control control) {
    Composite parent = control.getParent();
    while (parent != null) {
      // get the nearest parent that did not use parent background
      Object data = parent.getData(UiConstants.USE_PARENT_BACKGROUND);
      if (!Objects.equals(data, Boolean.TRUE)) {
        return parent.getBackground();
      }
      parent = parent.getParent();
    }
    return control.getBackground();
  }

  /**
   * Executes a command with the given parameters.
   *
   * @param commandId the command ID
   * @param parameters the parameters to pass to the command
   */
  public static void executeCommandWithParameters(String commandId, Map<String, Object> parameters) {
    ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
    Command command = commandService.getCommand(commandId);
    ParameterizedCommand paramCommand = ParameterizedCommand.generateCommand(command, parameters);
    IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);

    try {
      handlerService.executeCommand(paramCommand, null);
    } catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e) {
      CopilotCore.LOGGER.error(e);
    }
  }

  /**
   * Return the text aligned to the maxWidth with the given fineTuneCharacter and suffix.
   *
   * @param gc the graphics context used to measure text width
   * @param originalText the original text to align
   * @param fineTuneCharacter the character to use for fine-tuning the alignment
   * @param suffix the suffix to append to each line
   * @param initialSpaceNumber the number of initial spaces to add
   * @param maxWidth the maximum width of the aligned text
   */
  public static String getAlignedText(GC gc, String originalText, String fineTuneCharacter, String suffix,
      int initialSpaceNumber, int maxWidth) {
    String result = originalText + fineTuneCharacter.repeat(initialSpaceNumber) + suffix;
    // On some platforms, the textExtent of "foo" and "foofoo" does not scale linearly.
    // Typically, "foofoo" is shorter than the width of "foo x 2" but the exact behavior varies across different
    // operating systems. Therefore, we start with spacesToAdd and adjust it incrementally or decreasingly until we find
    // a width that is at least maxWidth.
    int initialWidth = gc.textExtent(result).x;
    if (initialWidth > maxWidth) {
      for (int i = initialSpaceNumber - 1; i > 0; i--) {
        result = originalText + fineTuneCharacter.repeat(i) + suffix;
        if (gc.textExtent(result).x <= maxWidth) {
          break;
        }
      }
    } else if (initialWidth < maxWidth) {
      for (int i = initialSpaceNumber + 1; i < initialSpaceNumber + MAX_SPACE_TO_ADD; i++) {
        result = originalText + fineTuneCharacter.repeat(i) + suffix;
        if (gc.textExtent(result).x >= maxWidth) {
          break;
        }
      }
    }
    return result;
  }
}

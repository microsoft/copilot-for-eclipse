package com.microsoft.copilot.eclipse.ui.utils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;
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
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.service.prefs.Preferences;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.chat.tools.FileToolBase.EditableFileCompareInput;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;

/**
 * Utilities for Eclipse UI.
 */
public class UiUtils {

  public static final String HAIR_SPACE = "\u200A";
  private static final int MAX_SPACE_TO_ADD = 500;
  private static final List<String> REVEAL_IN_EXPLORER_VIEW_IDS = List.of("org.eclipse.ui.navigator.ProjectExplorer",
      "org.eclipse.jdt.ui.PackageExplorer");

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
   * Returns the active editor part.
   *
   * @return the active editor part or null if no editor is active
   */
  @Nullable
  public static IEditorPart getActiveEditor() {
    IWorkbenchPage page = getActivePage();
    if (page == null) {
      return null;
    }
    return page.getActiveEditor();
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
   * Opens an E4 part (view) by its ID and activates it.
   *
   * @param partId the ID of the part to open
   * @return true if the part was successfully opened and activated, false otherwise
   */
  public static boolean openE4Part(String partId) {
    try {
      EPartService partService = PlatformUI.getWorkbench().getService(EPartService.class);
      if (partService != null) {
        MPart part = partService.showPart(partId, EPartService.PartState.VISIBLE);
        if (part != null) {
          partService.activate(part);
          return true;
        }
      }
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to open E4 part: " + partId, e);
    }
    return false;
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
   * Converts a model (document) offset to a widget line number.
   *
   * @param viewer the text viewer
   * @param modelOffset the model offset
   * @return the widget line number, or -1 if the offset is not visible or invalid
   */
  public static int modelOffset2WidgetLine(ITextViewer viewer, int modelOffset) {
    if (viewer == null) {
      return -1;
    }
    StyledText text = viewer.getTextWidget();
    if (text == null || text.isDisposed()) {
      return -1;
    }

    int widgetOffset = modelOffset2WidgetOffset(viewer, modelOffset);
    if (widgetOffset < 0 || widgetOffset >= text.getCharCount()) {
      return -1;
    }
    return text.getLineAtOffset(widgetOffset);
  }

  /**
   * Convert model line to widget line. Returns -1 if line is folded.
   *
   * @param viewer the text viewer
   * @param modelLine the model line number
   * @return the widget line number, or -1 if folded
   */
  public static int modelLine2WidgetLine(ITextViewer viewer, int modelLine) {
    if (viewer instanceof ITextViewerExtension5 ext5) {
      return ext5.modelLine2WidgetLine(modelLine);
    }
    return modelLine;
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
   * Font ID for the Copilot Chat view font.
   */
  public static final String CHAT_FONT_ID = "com.microsoft.copilot.eclipse.ui.chat.font";

  /**
   * Returns the font for the Copilot Chat view from the theme.
   *
   * @return the chat view font, or null if not available
   */
  @Nullable
  public static Font getChatFont() {
    try {
      return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry().get(CHAT_FONT_ID);
    } catch (NullPointerException e) {
      return null;
    }
  }

  /**
   * Returns the font registry from the current theme, or null if not available.
   *
   * @return the font registry, or null if not available
   */
  @Nullable
  public static FontRegistry getThemeFontRegistry() {
    try {
      return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry();
    } catch (NullPointerException e) {
      return null;
    }
  }

  /**
   * Applies the chat font to the given control.
   *
   * @param control the control to apply the font to
   */
  public static void applyChatFont(Control control) {
    Font chatFont = getChatFont();
    if (chatFont != null && control != null && !control.isDisposed()) {
      control.setFont(chatFont);
    }
  }

  /**
   * Returns true if Eclipse is currently using a dark theme.
   *
   * @return true if dark theme is active, false otherwise
   */
  public static boolean isDarkTheme() {
    Preferences preferences = InstanceScope.INSTANCE.getNode("org.eclipse.e4.ui.css.swt.theme");
    String themeCssUri = preferences.get("themeid", "");
    if (themeCssUri.toLowerCase().contains("dark")) {
      return true;
    }
    return false;
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
    final boolean[] mouseEntered = new boolean[1];
    result.addPaintListener(e -> {
      Rectangle bounds = result.getBounds();
      e.gc.fillRectangle(0, 0, bounds.width, bounds.height);

      // Draw focus indicator border for accessibility
      if (result.isFocusControl() && !mouseEntered[0]) {
        Color focusIndicatorColor = CssConstants.getWidgetFocusBorderColor(result.getDisplay());
        e.gc.setForeground(focusIndicatorColor);
        e.gc.setLineWidth(1);
        e.gc.drawRectangle(0, 0, bounds.width - 1, bounds.height - 1);
      }

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

    result.addMouseTrackListener(new org.eclipse.swt.events.MouseTrackAdapter() {
      private Color background = result.getBackground();
      private Color hoverBackground = CssConstants.getButtonFocusBgColor(result.getDisplay());

      @Override
      public void mouseEnter(org.eclipse.swt.events.MouseEvent e) {
        // The above background initialization will not take the css color, so here we need to re-fetch
        // the color when the mouse enters.
        background = result.getBackground();
        result.setBackground(hoverBackground);
        mouseEntered[0] = true;
      }

      @Override
      public void mouseExit(org.eclipse.swt.events.MouseEvent e) {
        result.setBackground(background);
        mouseEntered[0] = false;
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
   * Returns a bold version of the chat font for the Copilot Chat view.
   * Falls back to a bold version of the provided fallback font if chat font is not available.
   * The caller is responsible for disposing the returned font.
   *
   * @param display the display to create the font on
   * @param fallbackFont the font to use if chat font is not available
   * @return a new bold Font (caller must dispose)
   */
  public static Font getBoldChatFont(Display display, Font fallbackFont) {
    Font baseFont = getChatFont();
    if (baseFont == null) {
      baseFont = fallbackFont;
    }
    return getBoldFont(display, baseFont);
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
    String result = originalText + fineTuneCharacter.repeat(Math.max(0, initialSpaceNumber)) + suffix;
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

  /**
   * Reveals the given resource in the Project Explorer or Package Explorer view. If neither view is open, it will
   * attempt to open the Project Explorer first, then the Package Explorer.
   *
   * @param resource the resource to reveal
   */
  public static void revealInExplorer(IResource resource) {
    if (resource == null || !resource.exists()) {
      return;
    }
    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (window == null) {
        return;
      }

      IWorkbenchPage page = window.getActivePage();
      if (page == null) {
        return;
      }
      IViewPart view = findOpenResourceView(page);
      if (view != null) {
        selectResourceInView(view, resource);
        page.activate(view);
        return;
      }
      // If no view is open, try to open one
      for (String viewId : REVEAL_IN_EXPLORER_VIEW_IDS) {
        try {
          view = page.showView(viewId);
          selectResourceInView(view, resource);
          page.activate(view);
          return; // Successfully opened and revealed
        } catch (PartInitException e) {
          // Continue to the next view ID if this one fails
        }
      }
    });
  }

  private static IViewPart findOpenResourceView(IWorkbenchPage page) {
    for (String viewId : REVEAL_IN_EXPLORER_VIEW_IDS) {
      IViewPart view = page.findView(viewId);
      if (view != null) {
        return view;
      }
    }
    return null;
  }

  private static void selectResourceInView(IViewPart view, IResource resource) {
    ISelectionProvider selectionProvider = view.getSite().getSelectionProvider();
    if (selectionProvider != null) {
      IStructuredSelection selection = new StructuredSelection(resource);
      selectionProvider.setSelection(selection);
      if (view instanceof IShowInTarget) {
        ShowInContext context = new ShowInContext(null, selection);
        ((IShowInTarget) view).show(context);
      } else if (view instanceof CommonNavigator) {
        CommonNavigator navigator = (CommonNavigator) view;
        CommonViewer viewer = navigator.getCommonViewer();
        viewer.reveal(resource);
        viewer.setSelection(selection, true);
      }
    }
  }

  /**
   * Converts an Instant to a relative date string with time. Examples: "Today", "Yesterday", "2 days ago", "1 week ago"
   *
   * @param instant the instant to format
   * @return formatted relative date string with time, or empty string if instant is null
   */
  public static String formatRelativeDateTime(Instant instant) {
    if (instant == null) {
      return "";
    }

    LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    LocalDate messageDate = dateTime.toLocalDate();
    LocalDate today = LocalDate.now();

    long daysDifference = ChronoUnit.DAYS.between(messageDate, today);
    if (daysDifference == 0) {
      return Messages.relative_dateFormat_today;
    } else if (daysDifference == 1) {
      return Messages.relative_dateFormat_yesterday;
    } else if (daysDifference < 7) {
      return NLS.bind(Messages.relative_dateFormat_daysAgo, Long.toString(daysDifference));
    } else if (daysDifference < 14) {
      return Messages.relative_dateFormat_oneWeekAgo;
    } else if (daysDifference < 30) {
      long weeksDifference = daysDifference / 7;
      return NLS.bind(Messages.relative_dateFormat_weeksAgo, Long.toString(weeksDifference));
    }

    long monthsDifference = ChronoUnit.MONTHS.between(messageDate, today);
    if (monthsDifference == 1) {
      return Messages.relative_dateFormat_oneMonthAgo;
    } else {
      return NLS.bind(Messages.relative_dateFormat_monthsAgo, Long.toString(monthsDifference));
    }
  }

  /**
   * Formats the given local date time as a relative date time string (e.g., "5 minutes ago", "2 hours ago").
   *
   * @param localDateTime the local date time to format
   * @return the formatted relative date time string
   */
  public static String formatRelativeDateTime(LocalDateTime localDateTime) {
    return formatRelativeDateTime(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  /**
   * Formats the given local date as a relative date time string (e.g., "5 minutes ago", "2 hours ago").
   *
   * @param localDate the local date to format
   * @return the formatted relative date time string
   */
  public static String formatRelativeDateTime(LocalDate localDate) {
    return formatRelativeDateTime(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
  }

  /**
   * Applies the given CSS class names to the control using the provided styling engine. If the styling engine is null,
   * the class names are set as data on the control for potential later use.
   *
   * @param widget the widget to style
   * @param classnames the CSS class names to apply
   * @param stylingEngine the styling engine to use, or null if not available
   */
  public static void applyCssClass(Widget widget, String classnames, IStylingEngine stylingEngine) {
    if (stylingEngine != null) {
      stylingEngine.setClassname(widget, classnames);
      stylingEngine.style(widget);
    } else {
      widget.setData(CssConstants.CSS_CLASS_NAME_KEY, classnames);
    }
  }

  /**
   * Set vertical indentation for a specific line.
   *
   * @param text the styled text widget
   * @param widgetLine the widget line number
   * @param height the indent height in pixels (0 to clear)
   */
  public static void setLineVerticalIndent(StyledText text, int widgetLine, int height) {
    if (text == null || widgetLine < 0) {
      return;
    }
    if (widgetLine < text.getLineCount()) {
      text.setLineVerticalIndent(widgetLine, height);
    }
  }

  /**
   * Checks if the given file is an agent file (.agent.md).
   *
   * @param file the file to check
   * @return true if the file is an agent file, false otherwise
   */
  public static boolean isAgentFile(IFile file) {
    if (file == null) {
      return false;
    }
    return isAgentFile(file.getName());
  }

  /**
   * Checks if the given filename represents an agent file (.agent.md).
   *
   * @param fileName the filename to check
   * @return true if the filename ends with .agent.md, false otherwise
   */
  public static boolean isAgentFile(String fileName) {
    return fileName != null && fileName.endsWith(".agent.md");
  }

  /**
   * Find all open .agent.md files across all workbench windows. This method must be called from the UI thread.
   *
   * @return a list of editor parts containing open agent files
   */
  public static List<IEditorPart> findAllOpenAgentFiles() {
    List<IEditorPart> result = new ArrayList<>();

    IWorkbench workbench = PlatformUI.getWorkbench();
    if (workbench == null) {
      return result;
    }

    for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
      if (window == null || window.getActivePage() == null) {
        continue;
      }

      for (IEditorReference editorRef : window.getActivePage().getEditorReferences()) {
        IEditorPart editor = editorRef.getEditor(false);
        if (editor != null) {
          String editorName = editorRef.getName();
          if (isAgentFile(editorName)) {
            result.add(editor);
          }
        }
      }
    }

    return result;
  }

  /**
   * Loads an image from a URL asynchronously and optionally resizes it with high quality. On success, the onSuccess
   * callback is invoked on the UI thread with the loaded image. On failure, the onError callback is invoked on the UI
   * thread.
   *
   * @param urlString the URL of the image to load
   * @param width the desired width of the image, or -1 to keep original width
   * @param height the desired height of the image, or -1 to keep original height
   * @return a CompletableFuture that completes with the loaded image on the UI thread, or fails if loading fails
   *         (caller is responsible for disposing the image)
   */
  public static CompletableFuture<Image> loadImageFromUrl(String urlString, int width, int height) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        URL url = new URL(urlString);
        try (InputStream inputStream = url.openStream()) {
          ImageLoader loader = new ImageLoader();
          ImageData[] imageDataArray = loader.load(inputStream);
          if (imageDataArray.length > 0) {
            ImageData imageData = imageDataArray[0];
            // Resize with high quality if dimensions are specified
            if (width > 0 && height > 0) {
              imageData = scaleImageDataHighQuality(imageData, width, height);
            }
            return imageData;
          } else {
            throw new IllegalStateException("No image data found in image stream");
          }
        }
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    }).thenApplyAsync(imageData -> new Image(Display.getDefault(), imageData), runnable -> {
      Display.getDefault().asyncExec(runnable);
    });
  }

  /**
   * Scales ImageData to the specified dimensions using high-quality interpolation. Uses Java2D's Graphics2D with
   * bicubic interpolation for smooth scaling results.
   *
   * @param imageData the source image data
   * @param width the target width
   * @param height the target height
   * @return the scaled image data
   */
  private static ImageData scaleImageDataHighQuality(ImageData imageData, int width, int height) {
    // Convert SWT ImageData to AWT BufferedImage
    BufferedImage awtImage = convertToAwtImage(imageData);

    // Create scaled image with high quality rendering
    BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = scaledImage.createGraphics();
    g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
        java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
    g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.drawImage(awtImage, 0, 0, width, height, null);
    g2d.dispose();

    // Convert back to SWT ImageData
    return convertToSwtImageData(scaledImage);
  }

  /**
   * Converts SWT ImageData to AWT BufferedImage.
   */
  private static BufferedImage convertToAwtImage(ImageData imageData) {
    int width = imageData.width;
    int height = imageData.height;
    BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    PaletteData palette = imageData.palette;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int pixel = imageData.getPixel(x, y);
        int alpha = imageData.getAlpha(x, y);

        // Use palette.getRGB() which correctly handles all palette types
        RGB rgb = palette.getRGB(pixel);

        int argb = (alpha << 24) | (rgb.red << 16) | (rgb.green << 8) | rgb.blue;
        bufferedImage.setRGB(x, y, argb);
      }
    }
    return bufferedImage;
  }

  /**
   * Converts AWT BufferedImage to SWT ImageData.
   */
  private static ImageData convertToSwtImageData(BufferedImage bufferedImage) {
    int width = bufferedImage.getWidth();
    int height = bufferedImage.getHeight();

    PaletteData palette = new PaletteData(0xFF0000, 0x00FF00, 0x0000FF);
    ImageData imageData = new ImageData(width, height, 24, palette);
    imageData.alphaData = new byte[width * height];

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int argb = bufferedImage.getRGB(x, y);
        int alpha = (argb >> 24) & 0xFF;
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;

        int pixel = (red << 16) | (green << 8) | blue;
        imageData.setPixel(x, y, pixel);
        imageData.alphaData[y * width + x] = (byte) alpha;
      }
    }
    return imageData;
  }
}

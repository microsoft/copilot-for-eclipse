// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.swtbot.test.probe;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * Executes a single {@link ProbeStep} against the running Eclipse workbench.
 *
 * <p>Each action method is intentionally small and throws on failure; the
 * {@link ProbeRunner} wraps execution with try/catch and uniform reporting.</p>
 */
final class StepExecutor {
  /** Matches {@code com.microsoft.copilot.eclipse.ui.swt.CssConstants.CSS_ID_KEY}. */
  private static final String CSS_ID_KEY = "org.eclipse.e4.ui.css.id";
  /** Matches {@code com.microsoft.copilot.eclipse.ui.swt.CssConstants.CSS_CLASS_NAME_KEY}. */
  private static final String CSS_CLASS_NAME_KEY = "org.eclipse.e4.ui.css.CssClassName";
  /** SWTBot's default widget-id key. Mirrors {@code SWTBotPreferences.DEFAULT_KEY}. */
  private static final String SWTBOT_WIDGET_KEY = "org.eclipse.swtbot.widget.key";

  private final SWTWorkbenchBot bot;
  private final Path screenshotsDir;
  private final Path uiDumpsDir;

  StepExecutor(SWTWorkbenchBot bot, Path screenshotsDir, Path uiDumpsDir) {
    this.bot = bot;
    this.screenshotsDir = screenshotsDir;
    this.uiDumpsDir = uiDumpsDir;
  }

  void execute(ProbeStep step, StepResult result) throws Exception {
    String action = step.action == null ? "" : step.action;
    switch (action) {
      case "screenshot":
        screenshot(step, result);
        break;
      case "sleep":
        Thread.sleep(1000L * seconds(step, 1));
        break;
      case "waitForIdle":
        waitForIdle();
        break;
      case "pressKey":
        pressKey(step);
        break;
      case "showView":
        showView(requiredRef(step));
        break;
      case "closeView":
        closeView(requiredRef(step));
        break;
      case "invokeCommand":
        invokeCommand(requiredRef(step));
        break;
      case "assertExists":
        assertExists(step);
        break;
      case "assertContentAssist":
        assertContentAssist(step);
        break;
      case "waitFor":
        waitFor(step);
        break;
      case "waitForMethod":
        waitForMethod(step);
        break;
      case "click":
        click(step);
        break;
      case "typeIn":
        typeIn(step);
        break;
      case "typeKeys":
        typeKeys(step);
        break;
      case "clearElement":
        clearElement(step);
        break;
      case "dumpUi":
        dumpUi(step, result);
        break;
      case "newSession":
        invokeCommand("com.microsoft.copilot.eclipse.commands.newChatSession");
        break;
      case "openPreferencePage":
        openPreferencePage(step);
        break;
      case "createProject":
        createProject(step);
        break;
      case "createWorkspaceFile":
        createWorkspaceFile(step);
        break;
      case "deleteWorkspaceFile":
        deleteWorkspaceFile(step);
        break;
      case "waitForJobFamily":
        waitForJobFamily(step);
        break;
      default:
        throw new IllegalArgumentException("Unknown action: " + action);
    }
  }

  private void screenshot(ProbeStep step, StepResult result) throws IOException {
    Files.createDirectories(screenshotsDir);
    String base = step.id == null ? "step-" + result.index : step.id;
    String name = safeFileSegment(base) + ".png";
    Path out = screenshotsDir.resolve(name);
    boolean ok = captureWorkbenchShell(out);
    if (ok) {
      result.screenshots.add(name);
    }
  }

  void screenshotFailure(int index, String action, String id) {
    try {
      Files.createDirectories(screenshotsDir);
      String tag = id == null ? action : action + "-" + id;
      String name = String.format("FAILED-step%02d-%s.png", index, safeFileSegment(tag));
      Path out = screenshotsDir.resolve(name);
      captureWorkbenchShell(out);
    } catch (Exception ignored) {
      // best effort
    }
  }

  /**
   * Captures a PNG of just the Eclipse workbench window (active shell) rather than
   * the whole display. Falls back to the full display if no shell is reachable.
   */
  private boolean captureWorkbenchShell(Path out) {
    AtomicReference<Rectangle> shellBounds = new AtomicReference<>();
    Display display = Display.getDefault();
    display.syncExec(() -> {
      Shell target = findCaptureShell(display);
      if (target == null || target.isDisposed()) {
        return;
      }
      Rectangle b = target.getBounds();
      if (b.width > 0 && b.height > 0) {
        shellBounds.set(b);
      }
    });
    Rectangle bounds = shellBounds.get();
    if (bounds == null) {
      return false;
    }
    // Use java.awt.Robot to capture the actual on-screen pixels. SWT's own
    // GC-based capture (`gc.copyArea(...)` from a Display GC, or `shell.print(gc)`)
    // returns blank images on macOS Cocoa for workbench shells because SWT can't
    // scrape the compositor and Shell#print is a no-op there. Robot works because
    // the Tycho UI harness leaves the workbench window visible on screen.
    try {
      Robot robot = new Robot();
      // Use the fully-qualified AWT Rectangle to avoid clashing with the SWT one above.
      java.awt.Rectangle awtBounds = new java.awt.Rectangle(
          bounds.x, bounds.y, bounds.width, bounds.height);
      BufferedImage img = robot.createScreenCapture(awtBounds);
      Files.createDirectories(out.getParent());
      if (ImageIO.write(img, "png", out.toFile())) {
        return true;
      }
      // ImageIO.write returns false if no PNG writer is registered (theoretical
      // on a stock JDK). Fall through to SWTBot's full-display fallback below.
    } catch (AWTException | IOException | SecurityException e) {
      // Fall through to SWTBot's best-effort full-display capture.
    }
    return bot.captureScreenshot(out.toString());
  }

  private Shell findCaptureShell(Display display) {
    try {
      org.eclipse.ui.IWorkbench wb = org.eclipse.ui.PlatformUI.getWorkbench();
      org.eclipse.ui.IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
      if (win != null) {
        Shell s = win.getShell();
        if (s != null && !s.isDisposed()) {
          return s;
        }
      }
    } catch (Exception ignored) {
      // workbench not up yet
    }
    Shell active = display.getActiveShell();
    if (active != null && !active.isDisposed()) {
      return active;
    }
    Shell[] shells = display.getShells();
    return shells.length > 0 ? shells[0] : null;
  }

  private void waitForIdle() {
    Display.getDefault().syncExec(() -> { /* flush */ });
  }

  private void pressKey(ProbeStep step) {
    String key = required(step.key, "key");
    int[] keyInfo = resolveKey(key);
    int keyCode = keyInfo[0];
    char character = (char) keyInfo[1];
    Display display = Display.getDefault();

    // Resolve target widget: either the locator target or the display's focus control.
    AtomicReference<Widget> targetRef = new AtomicReference<>();
    if (step.locator != null) {
      Object botWidget = resolve(step.locator);
      if (botWidget instanceof SWTBotStyledText) {
        ((SWTBotStyledText) botWidget).setFocus();
      } else if (botWidget instanceof SWTBotText) {
        ((SWTBotText) botWidget).setFocus();
      }
      targetRef.set(extractWidget(botWidget));
    }
    if (targetRef.get() == null) {
      display.syncExec(() -> targetRef.set(display.getFocusControl()));
    }
    Widget target = targetRef.get();
    if (target == null) {
      throw new RuntimeException("pressKey: no focused widget to receive key event");
    }

    display.syncExec(() -> {
      Event down = new Event();
      down.type = SWT.KeyDown;
      down.keyCode = keyCode;
      down.character = character;
      target.notifyListeners(SWT.KeyDown, down);

      Event up = new Event();
      up.type = SWT.KeyUp;
      up.keyCode = keyCode;
      up.character = character;
      target.notifyListeners(SWT.KeyUp, up);
    });
    try {
      Thread.sleep(100);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Returns {@code [swtKeyCode, swtCharacter]} for a named key.
   */
  private static int[] resolveKey(String key) {
    switch (key.toUpperCase()) {
      case "ENTER":
      case "RETURN":
      case "CR":
        return new int[] { SWT.CR, '\r' };
      case "ESC":
      case "ESCAPE":
        return new int[] { SWT.ESC, '\033' };
      case "TAB":
        return new int[] { SWT.TAB, '\t' };
      case "SPACE":
        return new int[] { ' ', ' ' };
      case "BACKSPACE":
      case "BS":
        return new int[] { SWT.BS, '\b' };
      case "DELETE":
        return new int[] { SWT.DEL, '\177' };
      default:
        throw new IllegalArgumentException("Unsupported key: " + key);
    }
  }

  private void showView(String viewId) {
    IWorkbenchWindow window = waitForWorkbenchWindow();
    AtomicReference<Exception> err = new AtomicReference<>();
    Display.getDefault().syncExec(() -> {
      try {
        window.getActivePage().showView(viewId);
      } catch (Exception e) {
        err.set(e);
      }
    });
    if (err.get() != null) {
      throw new RuntimeException("Failed to show view " + viewId + ": " + err.get().getMessage(), err.get());
    }
  }

  private void closeView(String viewId) {
    IWorkbenchWindow window = waitForWorkbenchWindow();
    Display.getDefault().syncExec(() -> {
      IViewPart part = window.getActivePage().findView(viewId);
      if (part != null) {
        window.getActivePage().hideView(part);
      }
    });
  }

  /**
   * Blocks until {@link PlatformUI#getWorkbench()} exposes an active workbench window.
   * When Tycho launches with {@code useUIThread=false}, the workbench comes up on its
   * own thread while the test thread begins running, so early steps can race ahead of
   * workbench initialization. This polls on the UI thread for up to 30s.
   */
  private IWorkbenchWindow waitForWorkbenchWindow() {
    long deadline = System.currentTimeMillis() + 30_000L;
    AtomicReference<IWorkbenchWindow> found = new AtomicReference<>();
    while (System.currentTimeMillis() < deadline) {
      Display.getDefault().syncExec(() -> {
        try {
          IWorkbenchWindow w = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
          if (w == null) {
            IWorkbenchWindow[] all = PlatformUI.getWorkbench().getWorkbenchWindows();
            if (all.length > 0) {
              w = all[0];
            }
          }
          found.set(w);
        } catch (Exception ignored) {
          // workbench not up yet
        }
      });
      if (found.get() != null) {
        return found.get();
      }
      try {
        Thread.sleep(200);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    throw new AssertionError("Timed out waiting for active workbench window (30s).");
  }

  private void invokeCommand(String commandId) throws Exception {
    IWorkbenchWindow window = waitForWorkbenchWindow();
    ICommandService cmdSvc = window.getService(ICommandService.class);
    IHandlerService handlerSvc = window.getService(IHandlerService.class);
    ParameterizedCommand pc = new ParameterizedCommand(cmdSvc.getCommand(commandId), null);
    AtomicReference<Exception> err = new AtomicReference<>();
    Display.getDefault().syncExec(() -> {
      try {
        handlerSvc.executeCommand(pc, null);
      } catch (Exception e) {
        err.set(e);
      }
    });
    if (err.get() != null) {
      throw err.get();
    }
  }

  private void waitFor(ProbeStep step) {
    long timeoutMs = 1000L * seconds(step, 30);
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      try {
        resolve(step.locator);
        return;
      } catch (Exception ignored) {
        try {
          Thread.sleep(250);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
    throw new AssertionError("waitFor timed out: locator " + describe(step.locator));
  }

  private void assertExists(ProbeStep step) {
    boolean should = step.shouldExist == null ? true : step.shouldExist;
    boolean exists;
    try {
      resolve(step.locator);
      exists = true;
    } catch (Exception e) {
      exists = false;
    }
    if (exists != should) {
      throw new AssertionError(
          "assertExists failed: locator=" + describe(step.locator) + " shouldExist=" + should);
    }
  }

  /**
   * Asserts that the currently visible content-assist popup contains (or does not
   * contain) a proposal whose text includes {@code step.text}.
   *
   * <p>The content-assist popup is a separate {@link Shell} holding a {@link Table}
   * of proposals. This walks all open shells looking for a visible Table whose items
   * match the search text.</p>
   *
   * @param step requires {@code text}; optional {@code shouldExist} (default true)
   */
  private void assertContentAssist(ProbeStep step) {
    String searchText = required(step.text, "text");
    boolean shouldExist = step.shouldExist == null ? true : step.shouldExist;
    AtomicReference<Boolean> found = new AtomicReference<>(false);
    AtomicReference<String> allProposals = new AtomicReference<>("");

    Display.getDefault().syncExec(() -> {
      StringBuilder sb = new StringBuilder();
      for (Shell shell : Display.getDefault().getShells()) {
        if (shell.isDisposed() || !shell.isVisible()) {
          continue;
        }
        Table table = findTable(shell);
        if (table == null || table.isDisposed()) {
          continue;
        }
        for (int i = 0; i < table.getItemCount(); i++) {
          TableItem item = table.getItem(i);
          String itemText = item.getText();
          if (sb.length() > 0) {
            sb.append(", ");
          }
          sb.append(itemText);
          if (itemText.contains(searchText)) {
            found.set(true);
          }
        }
      }
      allProposals.set(sb.toString());
    });

    if (found.get() != shouldExist) {
      throw new AssertionError(
          "assertContentAssist failed: text=\"" + searchText + "\" shouldExist="
              + shouldExist + " proposals=[" + allProposals.get() + "]");
    }
  }

  /**
   * Recursively searches a widget tree for the first visible {@link Table}.
   */
  private Table findTable(Widget w) {
    if (w == null || w.isDisposed()) {
      return null;
    }
    if (w instanceof Table) {
      return (Table) w;
    }
    for (Widget child : childrenOf(w)) {
      Table hit = findTable(child);
      if (hit != null) {
        return hit;
      }
    }
    return null;
  }

  private void click(ProbeStep step) {
    Object widget = resolve(step.locator);
    if (widget instanceof SWTBotStyledText) {
      ((SWTBotStyledText) widget).setFocus();
      return;
    }
    if (widget instanceof SWTBotText) {
      ((SWTBotText) widget).setFocus();
      return;
    }
    if (widget instanceof SWTBotCheckBox) {
      ((SWTBotCheckBox) widget).click();
      return;
    }
    invokeClick(widget);
  }

  private void typeIn(ProbeStep step) {
    Object widget = resolve(step.locator);
    String payload = required(step.text, "text");
    if (widget instanceof SWTBotText) {
      ((SWTBotText) widget).setText(payload);
    } else if (widget instanceof SWTBotStyledText) {
      ((SWTBotStyledText) widget).setText(payload);
    } else {
      throw new IllegalArgumentException(
          "typeIn not supported for widget type: " + widget.getClass().getSimpleName());
    }
  }

  /**
   * Types text character-by-character via SWT event notification on the located
   * widget. Unlike {@code typeIn} (which uses {@code setText}), this fires
   * {@code KeyDown}/{@code KeyUp} events that trigger key listeners such as
   * content-assist auto-activation.
   */
  private void typeKeys(ProbeStep step) {
    Object botWidget = resolve(step.locator);
    String payload = required(step.text, "text");
    if (botWidget instanceof SWTBotStyledText) {
      ((SWTBotStyledText) botWidget).setFocus();
    } else if (botWidget instanceof SWTBotText) {
      ((SWTBotText) botWidget).setFocus();
    }
    Widget target = extractWidget(botWidget);
    for (char c : payload.toCharArray()) {
      Display.getDefault().syncExec(() -> {
        Event down = new Event();
        down.type = SWT.KeyDown;
        down.character = c;
        down.keyCode = c;
        target.notifyListeners(SWT.KeyDown, down);

        Event up = new Event();
        up.type = SWT.KeyUp;
        up.character = c;
        up.keyCode = c;
        target.notifyListeners(SWT.KeyUp, up);
      });
      try {
        Thread.sleep(50);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  /**
   * Extracts the underlying SWT {@link Widget} from an SWTBot wrapper via the
   * public {@code widget} field. Uses reflection to avoid a compile-time
   * dependency on {@code AbstractSWTBot} (which transitively pulls SLF4J).
   */
  private static Widget extractWidget(Object botWrapper) {
    ReflectiveOperationException reflectErr = null;
    try {
      java.lang.reflect.Field f = botWrapper.getClass().getField("widget");
      Object raw = f.get(botWrapper);
      if (raw instanceof Widget) {
        return (Widget) raw;
      }
    } catch (ReflectiveOperationException e) {
      reflectErr = e;
    }
    if (botWrapper instanceof Widget) {
      return (Widget) botWrapper;
    }
    throw new IllegalArgumentException(
        "Cannot extract SWT Widget from " + botWrapper.getClass().getSimpleName(),
        reflectErr);
  }

  private void clearElement(ProbeStep step) {
    Object widget = resolve(step.locator);
    if (widget instanceof SWTBotText) {
      ((SWTBotText) widget).setText("");
    } else if (widget instanceof SWTBotStyledText) {
      ((SWTBotStyledText) widget).setText("");
    } else {
      throw new IllegalArgumentException(
          "clearElement not supported for widget type: " + widget.getClass().getSimpleName());
    }
  }

  private void dumpUi(ProbeStep step, StepResult result) throws IOException {
    Files.createDirectories(uiDumpsDir);
    String base = step.id == null ? "step-" + result.index : step.id;
    String name = safeFileSegment(base) + ".xml";
    Path out = uiDumpsDir.resolve(name);
    try (OutputStream os = Files.newOutputStream(out)) {
      os.write("<ui-dump>\n".getBytes(StandardCharsets.UTF_8));
      Display.getDefault().syncExec(() -> {
        try {
          for (Shell shell : Display.getDefault().getShells()) {
            dumpWidget(os, shell, 1);
          }
        } catch (IOException ignored) {
          // best effort
        }
      });
      os.write("</ui-dump>\n".getBytes(StandardCharsets.UTF_8));
    }
  }

  private static final int MAX_DUMP_DEPTH = 20;

  private void dumpWidget(OutputStream os, Widget w, int depth) throws IOException {
    if (w == null || w.isDisposed() || depth > MAX_DUMP_DEPTH) {
      return;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < depth; i++) {
      sb.append("  ");
    }
    sb.append('<').append(w.getClass().getSimpleName());
    try {
      Object data = w.getData(SWTBotPreferences.DEFAULT_KEY);
      if (data != null) {
        sb.append(" id=\"").append(escape(data.toString())).append('"');
      }
    } catch (Exception ignored) {
      // ignore
    }
    Widget[] children = childrenOf(w);
    if (children.length == 0) {
      sb.append("/>\n");
      os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
      return;
    }
    sb.append(">\n");
    os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    for (Widget child : children) {
      dumpWidget(os, child, depth + 1);
    }
    StringBuilder close = new StringBuilder();
    for (int i = 0; i < depth; i++) {
      close.append("  ");
    }
    close.append("</").append(w.getClass().getSimpleName()).append(">\n");
    os.write(close.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static Widget[] childrenOf(Widget w) {
    if (w instanceof org.eclipse.swt.widgets.Composite) {
      try {
        return ((org.eclipse.swt.widgets.Composite) w).getChildren();
      } catch (Exception ignored) {
        // ignore
      }
    }
    return new Widget[0];
  }

  /**
   * Strips path separators, parent-dir tokens, and control characters from a string
   * so it is safe to use as a single filename segment under {@code target/probe-results/}.
   */
  static String safeFileSegment(String s) {
    if (s == null || s.isEmpty()) {
      return "unnamed";
    }
    String cleaned = s.replace("..", "_")
        .replace('/', '_')
        .replace('\\', '_')
        .replace(':', '_')
        .replaceAll("[\\x00-\\x1F]", "_");
    if (cleaned.isEmpty()) {
      return "unnamed";
    }
    return cleaned;
  }

  private static String escape(String s) {
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }

  private Object resolve(Locator locator) {
    if (locator == null || locator.by == null) {
      throw new IllegalArgumentException("Locator is required");
    }
    switch (locator.by) {
      case "viewId":
        return bot.viewById(required(locator.id, "locator.id"));
      case "label":
        return bot.label(required(locator.text, "locator.text"));
      case "button":
        return bot.button(required(locator.text, "locator.text"));
      case "buttonWithTooltip":
        return bot.buttonWithTooltip(required(locator.tooltip, "locator.tooltip"));
      case "text": {
        int idx = locator.index == null ? 0 : locator.index;
        return bot.text(idx);
      }
      case "tree": {
        SWTBotTree tree = bot.tree();
        List<String> labels = locator.labels;
        if (labels == null || labels.isEmpty()) {
          throw new IllegalArgumentException("tree locator requires labels");
        }
        SWTBotTreeItem item = tree.getTreeItem(labels.get(0)).expand();
        for (int i = 1; i < labels.size(); i++) {
          item = item.getNode(labels.get(i)).expand();
        }
        return item;
      }
      case "styledText":
        return bot.styledText();
      case "cssId":
        return resolveByCssMarker(CSS_ID_KEY, required(locator.value, "locator.value"), true);
      case "cssClass":
        return resolveByCssMarker(CSS_CLASS_NAME_KEY,
            required(locator.value, "locator.value"), false);
      case "widgetClass":
        return resolveByWidgetClass(required(locator.value, "locator.value"));
      case "widgetId":
        return resolveByCssMarker(SWTBOT_WIDGET_KEY, required(locator.value, "locator.value"), true);
      case "checkBox":
        return bot.checkBox(required(locator.text, "locator.text"));
      default:
        throw new IllegalArgumentException("Unknown locator.by: " + locator.by);
    }
  }

  /**
   * Walks all shells and their descendants to find the first widget whose data under {@code key}
   * matches {@code value}. When {@code exact} is true the data must equal value; otherwise the
   * data is treated as a whitespace-separated list of tokens and any match is accepted.
   */
  private Widget resolveByCssMarker(String key, String value, boolean exact) {
    AtomicReference<Widget> found = new AtomicReference<>();
    int[] stats = new int[2]; // [visited, carriesAnyValueAtKey]
    Display.getDefault().syncExec(() -> {
      for (Shell shell : Display.getDefault().getShells()) {
        Widget hit = searchCssMarker(shell, key, value, exact, stats);
        if (hit != null) {
          found.set(hit);
          return;
        }
      }
    });
    Widget w = found.get();
    if (w == null) {
      throw new RuntimeException("No widget found with " + key + "=" + value
          + " (visited=" + stats[0] + ", carriedKey=" + stats[1] + ")");
    }
    return w;
  }

  private Widget searchCssMarker(Widget w, String key, String value, boolean exact, int[] stats) {
    if (w == null || w.isDisposed()) {
      return null;
    }
    stats[0]++;
    try {
      Object data = w.getData(key);
      if (data != null) {
        stats[1]++;
        String s = data.toString();
        if (exact ? s.equals(value) : containsToken(s, value)) {
          return w;
        }
      }
    } catch (Exception ignored) {
      // continue
    }
    for (Widget child : childrenOf(w)) {
      Widget hit = searchCssMarker(child, key, value, exact, stats);
      if (hit != null) {
        return hit;
      }
    }
    return null;
  }

  private static boolean containsToken(String classList, String token) {
    if (classList == null || token == null) {
      return false;
    }
    for (String part : classList.trim().split("\\s+")) {
      if (part.equals(token)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Walks all shells and returns the first widget whose simple class name (e.g.
   * {@code UserTurnWidget}) equals {@code className}.
   */
  private Widget resolveByWidgetClass(String className) {
    AtomicReference<Widget> found = new AtomicReference<>();
    Display.getDefault().syncExec(() -> {
      for (Shell shell : Display.getDefault().getShells()) {
        Widget hit = searchWidgetClass(shell, className);
        if (hit != null) {
          found.set(hit);
          return;
        }
      }
    });
    Widget w = found.get();
    if (w == null) {
      throw new RuntimeException("No widget found with simple class name=" + className);
    }
    return w;
  }

  private Widget searchWidgetClass(Widget w, String className) {
    if (w == null || w.isDisposed()) {
      return null;
    }
    if (className.equals(w.getClass().getSimpleName())) {
      return w;
    }
    for (Widget child : childrenOf(w)) {
      Widget hit = searchWidgetClass(child, className);
      if (hit != null) {
        return hit;
      }
    }
    return null;
  }

  /**
   * Polls a no-arg getter on the widget located by {@code step.locator} until it returns a
   * non-null (and non-empty, for Strings) value or matches {@code step.expectedValue}.
   *
   * <p>Used to wait on UI-level state exposed by widgets (e.g.
   * {@code DropdownButton.getSelectedItemId()}) without adding test-only markers into
   * production code.</p>
   */
  private void waitForMethod(ProbeStep step) {
    String methodName = required(step.method, "method");
    long timeoutMs = 1000L * seconds(step, 30);
    long deadline = System.currentTimeMillis() + timeoutMs;
    Throwable lastErr = null;
    while (System.currentTimeMillis() < deadline) {
      try {
        Object widget = resolve(step.locator);
        Object target = unwrapBotWidget(widget);
        Object result = invokeNoArgMethod(target, methodName);
        if (isMatch(result, step.expectedValue)) {
          return;
        }
      } catch (Exception e) {
        lastErr = e;
      }
      try {
        Thread.sleep(250);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        return;
      }
    }
    throw new AssertionError("waitForMethod timed out: " + methodName + " on "
        + describe(step.locator)
        + (step.expectedValue == null ? " (non-null)" : " (==" + step.expectedValue + ")")
        + (lastErr == null ? "" : " — last error: " + lastErr));
  }

  private static boolean isMatch(Object result, String expected) {
    if (expected == null) {
      if (result == null) {
        return false;
      }
      if (result instanceof String s) {
        return !s.isEmpty();
      }
      return true;
    }
    return result != null && expected.equals(result.toString());
  }

  private static Object invokeNoArgMethod(Object target, String methodName)
      throws ReflectiveOperationException {
    // Walk the class hierarchy so we can reach methods declared on supertypes even if
    // the concrete class has package-private visibility.
    Class<?> c = target.getClass();
    while (c != null) {
      try {
        java.lang.reflect.Method m = c.getDeclaredMethod(methodName);
        m.setAccessible(true);
        return m.invoke(target);
      } catch (NoSuchMethodException ignored) {
        c = c.getSuperclass();
      }
    }
    throw new NoSuchMethodException(methodName + " on " + target.getClass().getName());
  }

  /**
   * If the locator returned an SWTBot wrapper, extract the underlying SWT widget so
   * we can invoke methods declared on the production widget class. Otherwise returns
   * the input as-is (Shell-tree walkers already return the raw Widget).
   */
  private static Object unwrapBotWidget(Object widget) {
    try {
      java.lang.reflect.Method m = widget.getClass().getMethod("widget");
      Object unwrapped = m.invoke(widget);
      if (unwrapped != null) {
        return unwrapped;
      }
    } catch (ReflectiveOperationException ignored) {
      // not a SWTBot wrapper
    }
    return widget;
  }

  @SuppressWarnings("unchecked")
  private static void invokeClick(Object widget) {
    // AbstractSWTBot doesn't expose click() universally; reflectively invoke.
    try {
      widget.getClass().getMethod("click").invoke(widget);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("click not supported on " + widget.getClass().getSimpleName(), e);
    }
  }

  /**
   * Opens the Eclipse Preferences dialog directly to the specified page.
   *
   * @param step must have {@code pageId} set to a preference page id
   */
  private void openPreferencePage(ProbeStep step) {
    String pageId = required(step.pageId, "pageId");
    IWorkbenchWindow window = waitForWorkbenchWindow();
    Display.getDefault().syncExec(() -> {
      Shell shell = window.getShell();
      PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
          shell, pageId, new String[] { pageId }, null);
      dialog.setBlockOnOpen(false);
      dialog.open();
    });
  }

  /**
   * Creates an empty Eclipse project in the workspace. The project name comes from
   * {@code step.text}; if absent the default name {@code "probe-project"} is used.
   * If a project with that name already exists and is open, this is a no-op.
   */
  private void createProject(ProbeStep step) throws Exception {
    String name = step.text == null || step.text.isEmpty() ? "probe-project" : step.text;
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
    if (!project.exists()) {
      project.create(new NullProgressMonitor());
    }
    if (!project.isOpen()) {
      project.open(new NullProgressMonitor());
    }
  }

  /**
   * Creates a file in the first open workspace project. The file path is relative to
   * the project root (e.g. {@code .github/skills/my-skill/SKILL.md}). Parent folders
   * are created automatically.
   */
  private void createWorkspaceFile(ProbeStep step) throws Exception {
    String relativePath = required(step.path, "path");
    String fileContent = step.content == null ? "" : step.content;
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    if (projects.length == 0) {
      throw new IllegalStateException("No projects in workspace; cannot create file.");
    }
    IProject project = projects[0];
    IFile file = project.getFile(relativePath);
    // Create parent folders
    createParentFolders(file);
    try (InputStream is = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8))) {
      if (file.exists()) {
        file.setContents(is, IResource.FORCE, new NullProgressMonitor());
      } else {
        file.create(is, IResource.FORCE, new NullProgressMonitor());
      }
    }
  }

  /**
   * Deletes a file from the first open workspace project.
   */
  private void deleteWorkspaceFile(ProbeStep step) throws Exception {
    String relativePath = required(step.path, "path");
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    if (projects.length == 0) {
      throw new IllegalStateException("No projects in workspace; cannot delete file.");
    }
    IProject project = projects[0];
    IFile file = project.getFile(relativePath);
    if (file.exists()) {
      file.delete(true, new NullProgressMonitor());
    }
  }

  private void createParentFolders(IFile file) throws Exception {
    IContainer parent = file.getParent();
    if (parent instanceof IFolder) {
      createFolderRecursively((IFolder) parent);
    }
  }

  private void createFolderRecursively(IFolder folder) throws Exception {
    if (folder.exists()) {
      return;
    }
    IContainer parent = folder.getParent();
    if (parent instanceof IFolder) {
      createFolderRecursively((IFolder) parent);
    }
    folder.create(true, true, new NullProgressMonitor());
  }

  /**
   * Waits for all Eclipse Jobs belonging to the specified family to finish.
   */
  private void waitForJobFamily(ProbeStep step) throws InterruptedException {
    String family = required(step.jobFamily, "jobFamily");
    long timeoutMs = 1000L * seconds(step, 30);
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      Job[] jobs = Job.getJobManager().find(family);
      if (jobs.length == 0) {
        return;
      }
      for (Job job : jobs) {
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0) {
          throw new AssertionError("waitForJobFamily timed out: " + family);
        }
        job.join(remaining, new NullProgressMonitor());
      }
      // Re-check in case new jobs were scheduled during join.
    }
  }

  private static String required(String value, String name) {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException("Missing required field: " + name);
    }
    return value;
  }

  private static String requiredRef(ProbeStep step) {
    return required(step.idRef, "idRef");
  }

  private static int seconds(ProbeStep step, int defaultSec) {
    return step.timeoutSec == null ? defaultSec : step.timeoutSec;
  }

  private static String describe(Locator l) {
    if (l == null) {
      return "null";
    }
    return "{by=" + l.by + ", id=" + l.id + ", text=" + l.text + ", value=" + l.value
        + ", tooltip=" + l.tooltip + ", labels=" + l.labels + "}";
  }
}

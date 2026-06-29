// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Display;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Encapsulates the Java&#8596;JavaScript boundary for {@link BrowserConversationWidget}: it builds
 * and runs JavaScript against the chat {@link Browser} and registers the Java call-back functions
 * that browser-side JavaScript invokes.
 *
 * <p>The owning widget keeps all SWT lifecycle - it creates and disposes the {@link Browser} and its
 * {@code ProgressListener}, then passes the browser here. The bridge owns the page-load gating state
 * (whether the page has loaded, plus the queues of scripts and result-producing tasks deferred until
 * it has); the widget's load listener calls {@link #notifyPageLoaded()} to flush those queues.
 *
 * <p>Domain reactions to JavaScript callbacks are delegated to {@link JavaCallbacks} so this bridge
 * stays free of clipboard, editor, workbench and logging dependencies.
 */
public class BrowserConversationJavaJsBridge {

  /**
   * Java call-back functions invoked by browser-side JavaScript. Implemented by the owning widget
   * so all domain behavior stays there. Each method corresponds to one registered
   * {@link BrowserFunction}. This does not cover the {@link #executeScriptForResult} result path,
   * which uses a per-call {@link Consumer}.
   */
  public interface JavaCallbacks {

    /** Copies {@code code} to the system clipboard. */
    void copyToClipboard(String code);

    /** Inserts {@code code} at the cursor of the active text editor. */
    void insertAtCursor(String code);

    /** Accepts the pending tool confirmation with the given action index. */
    void acceptToolAction(int actionIndex);

    /** Dismisses the pending tool confirmation. */
    void dismissToolAction();

    /** Handles a generic copilot action (e.g. {@code openLink}, {@code openJobList}). */
    void copilotAction(String action, String param);

    /** Routes a browser-side (JavaScript) error message to the Eclipse error log. */
    void logError(String message);
  }

  private final Browser browser;
  private final JavaCallbacks callbacks;

  private boolean pageLoaded;
  private final Queue<String> pendingScripts = new LinkedList<>();
  /**
   * Result-producing evaluations deferred until the page has loaded and {@link #pendingScripts}
   * have flushed. Used so that operations needing a real return value (e.g. verifying a tool
   * confirmation card was inserted) reflect the actual post-load DOM instead of an optimistic
   * assumption made while the page was still loading.
   */
  private final Queue<Runnable> pendingResultTasks = new LinkedList<>();

  /**
   * Creates the bridge over an already-created {@link Browser} and registers the Java call-back
   * functions. The browser's lifecycle (creation, layout, disposal and load-listener registration)
   * remains the responsibility of the owning widget.
   *
   * @param browser the chat browser to drive (created and owned by the widget)
   * @param callbacks the domain callbacks invoked by browser-side JavaScript
   */
  public BrowserConversationJavaJsBridge(Browser browser, JavaCallbacks callbacks) {
    this.browser = browser;
    this.callbacks = callbacks;
    registerBrowserFunctions();
  }

  /**
   * Marks the page as loaded and flushes any scripts and result-producing tasks queued while it was
   * still loading. Must be called on the UI thread from the widget's page-load listener.
   */
  public void notifyPageLoaded() {
    pageLoaded = true;
    while (!pendingScripts.isEmpty()) {
      browser.execute(pendingScripts.poll());
    }
    // Run deferred result-producing evaluations only after the DOM built by the queued scripts
    // above exists, so their outcome reflects the real post-load DOM.
    while (!pendingResultTasks.isEmpty()) {
      pendingResultTasks.poll().run();
    }
  }

  /** Applies the light/dark theme by invoking the {@code window.setTheme} helper. */
  public void setDarkTheme(boolean dark) {
    browser.execute("window.setTheme('" + (dark ? "dark" : "light") + "')");
  }

  /** Scrolls the chat viewport to the bottom and re-arms auto-scroll (see chat-view.html). */
  public void scrollToBottom() {
    executeScript("window.forceScrollToBottom()");
  }

  /** Appends {@code html} as the last child of the element with {@code parentId}. */
  public void insertBlock(String parentId, String html) {
    executeScript(insertBlockScript(parentId, html));
  }

  /**
   * Inserts {@code html} before the sibling {@code beforeId} within {@code parentId} and reports
   * whether the DOM insertion actually happened to {@code onResult} (always on the UI thread).
   */
  public void insertBlockBefore(String parentId, String html, String beforeId,
      Consumer<Boolean> onResult) {
    executeScriptForResult(insertBlockBeforeScript(parentId, html, beforeId), onResult);
  }

  /** Replaces the element {@code blockId} with {@code html}. */
  public void replaceBlock(String blockId, String html) {
    executeScript(replaceBlockScript(blockId, html));
  }

  /** Removes the element with {@code blockId}. */
  public void removeBlock(String blockId) {
    executeScript(removeBlockScript(blockId));
  }

  /**
   * Collapses a thinking block by removing the {@code open} attribute from its {@code <details>}
   * element and replacing the spinner with the given bulb SVG icon.
   */
  public void collapseThinkingBlock(String blockId, String bulbSvg) {
    executeScript(collapseThinkingBlockScript(blockId, bulbSvg));
  }

  /**
   * Updates the rendered (Markdown -> HTML) body of a thinking block and auto-scrolls to the
   * bottom if the user hasn't scrolled up. Avoids replacing the entire block (which resets scroll).
   * The caller passes pre-rendered HTML so live streaming matches the sealed/restored rendering.
   */
  public void updateThinkingBodyText(String blockId, String bodyHtml) {
    executeScript(updateThinkingBodyTextScript(blockId, bodyHtml));
  }

  /**
   * Updates the summary/title of a thinking block without replacing the entire block. The caller
   * passes pre-rendered inline HTML (Markdown -> HTML) so bold/inline formatting is honored.
   */
  public void updateThinkingBlockTitle(String blockId, String titleHtml) {
    executeScript(updateThinkingBlockTitleScript(blockId, titleHtml));
  }

  private void registerBrowserFunctions() {
    new BrowserFunction(browser, "copyToClipboard") {
      @Override
      public Object function(Object[] arguments) {
        if (arguments.length > 0 && arguments[0] instanceof String code) {
          callbacks.copyToClipboard(code);
        }
        return null;
      }
    };

    new BrowserFunction(browser, "insertAtCursor") {
      @Override
      public Object function(Object[] arguments) {
        if (arguments.length > 0 && arguments[0] instanceof String code) {
          callbacks.insertAtCursor(code);
        }
        return null;
      }
    };

    new BrowserFunction(browser, "acceptToolAction") {
      @Override
      public Object function(Object[] arguments) {
        if (arguments.length > 0 && arguments[0] instanceof Double actionIndex) {
          callbacks.acceptToolAction(actionIndex.intValue());
        }
        return null;
      }
    };

    new BrowserFunction(browser, "dismissToolAction") {
      @Override
      public Object function(Object[] arguments) {
        callbacks.dismissToolAction();
        return null;
      }
    };

    new BrowserFunction(browser, "copilotAction") {
      @Override
      public Object function(Object[] arguments) {
        if (arguments.length < 2) {
          return null;
        }
        callbacks.copilotAction(String.valueOf(arguments[0]), String.valueOf(arguments[1]));
        return null;
      }
    };

    // Routes browser-side (JavaScript) error diagnostics to the Eclipse error log so they are
    // visible to the user; the browser console is not surfaced anywhere in the IDE.
    new BrowserFunction(browser, "copilotLogError") {
      @Override
      public Object function(Object[] arguments) {
        if (arguments.length > 0 && arguments[0] != null) {
          callbacks.logError(String.valueOf(arguments[0]));
        }
        return null;
      }
    };
  }

  private void executeScript(String script) {
    if (browser.isDisposed()) {
      return;
    }
    Display.getDefault().asyncExec(() -> {
      if (browser.isDisposed()) {
        return;
      }
      if (pageLoaded) {
        browser.execute(script);
      } else {
        pendingScripts.add(script);
      }
    });
  }

  /**
   * Evaluates a JavaScript expression and reports whether the call was successful. The
   * {@code expression} must evaluate to a boolean indicating success (e.g. a call to a
   * {@code window.*} helper that returns {@code true}/{@code false}). The {@code onResult} callback
   * is always invoked on the UI thread. Evaluation is deferred behind any previously queued scripts
   * to preserve ordering. While the page is still loading, both the execution and the result
   * capture are deferred until after the initial load completes and {@link #pendingScripts} have
   * flushed, so the reported outcome reflects the real post-load DOM rather than an optimistic
   * assumption. This lets callers such as the tool-confirmation flow reliably detect a genuine
   * insertion failure (and fall back to dismiss) without ever guessing success.
   */
  private void executeScriptForResult(String expression, Consumer<Boolean> onResult) {
    Display.getDefault().asyncExec(() -> {
      if (browser.isDisposed()) {
        onResult.accept(false);
        return;
      }
      if (!pageLoaded) {
        // Defer both the execution and its result capture until the page has loaded and queued
        // scripts have flushed, so success/failure reflects the real DOM instead of a guess.
        pendingResultTasks.add(() -> onResult.accept(evaluateForBoolean(expression)));
        return;
      }
      onResult.accept(evaluateForBoolean(expression));
    });
  }

  /**
   * Executes {@code expression} via {@link Browser#evaluate(String)} and returns whether it
   * evaluated to boolean {@code true}. Must be called on the UI thread with the page loaded. Any
   * evaluation failure is logged and reported as {@code false}.
   */
  private boolean evaluateForBoolean(String expression) {
    try {
      Object result = browser.evaluate("return " + expression + ";");
      return Boolean.TRUE.equals(result);
    } catch (RuntimeException e) {
      CopilotCore.LOGGER.error("Failed to evaluate browser script: " + expression, e);
      return false;
    }
  }

  /** Escapes a string for safe embedding in a single-quoted JavaScript string literal. */
  static String escapeForJs(String text) {
    if (text == null) {
      return "";
    }
    return text.replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  /**
   * Builds the JavaScript call that appends {@code html} as the last child of the element with
   * {@code parentId}. The invoked helper returns whether the insertion was performed.
   */
  static String insertBlockScript(String parentId, String html) {
    return "window.insertBlock('" + escapeForJs(parentId) + "', '" + escapeForJs(html) + "')";
  }

  /**
   * Builds the JavaScript call that inserts {@code html} before the sibling {@code beforeId} within
   * the element with {@code parentId}. The invoked helper returns whether the insertion was done.
   */
  static String insertBlockBeforeScript(String parentId, String html, String beforeId) {
    return "window.insertBlockBefore('" + escapeForJs(parentId) + "', '"
        + escapeForJs(html) + "', '" + escapeForJs(beforeId) + "')";
  }

  /** Builds the JavaScript call that replaces the element {@code blockId} with {@code html}. */
  static String replaceBlockScript(String blockId, String html) {
    return "window.replaceBlock('" + escapeForJs(blockId) + "', '" + escapeForJs(html) + "')";
  }

  /** Builds the JavaScript call that removes the element with {@code blockId}. */
  static String removeBlockScript(String blockId) {
    return "window.removeBlock('" + escapeForJs(blockId) + "')";
  }

  /**
   * Builds the script executed by {@link #collapseThinkingBlock(String, String)}. The SVG is
   * embedded in a double-quoted {@code innerHTML} literal, so its quotes and apostrophes are escaped
   * for that context rather than via {@link #escapeForJs}.
   */
  static String collapseThinkingBlockScript(String blockId, String bulbSvg) {
    String bulbSvgJs = bulbSvg.replace("\"", "\\\"").replace("'", "\\'");
    return """
        var b=document.getElementById('%s');\
        if(b){var d=b.querySelector('details');if(d)d.removeAttribute('open');\
        var s=b.querySelector('.thinking-spinner');\
        if(s){var icon=document.createElement('span');\
        icon.innerHTML='%s';\
        s.parentNode.replaceChild(icon.firstChild,s);}}"""
        .formatted(escapeForJs(blockId), bulbSvgJs);
  }

  /** Builds the script executed by {@link #updateThinkingBodyText(String, String)}. */
  static String updateThinkingBodyTextScript(String blockId, String bodyHtml) {
    return """
        var b=document.getElementById('%s');\
        if(b){var bd=b.querySelector('.thinking-body');if(bd){\
        bd.innerHTML='%s';\
        var thr=60;if(bd.scrollHeight-(bd.scrollTop+bd.clientHeight)<=thr)\
        bd.scrollTop=bd.scrollHeight;}}"""
        .formatted(escapeForJs(blockId), escapeForJs(bodyHtml));
  }

  /** Builds the script executed by {@link #updateThinkingBlockTitle(String, String)}. */
  static String updateThinkingBlockTitleScript(String blockId, String titleHtml) {
    return """
        var b=document.getElementById('%s');\
        if(b){var s=b.querySelector('summary');if(s)s.innerHTML='%s';}"""
        .formatted(escapeForJs(blockId), escapeForJs(titleHtml));
  }
}

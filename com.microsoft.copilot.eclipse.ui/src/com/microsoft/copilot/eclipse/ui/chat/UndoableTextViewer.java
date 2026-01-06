package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.TextViewerUndoManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;

/**
 * A TextViewer with built-in undo/redo support.
 *
 * <p>This base class provides:
 * <ul>
 *   <li>Undo manager configuration with a configurable history size</li>
 *   <li>Key bindings for undo (Cmd+Z/Ctrl+Z) and redo (Cmd+Shift+Z/Ctrl+Y)</li>
 *   <li>Compound change management for word-by-word and line-by-line undo</li>
 *   <li>Proper cleanup on disposal</li>
 * </ul>
 */
public class UndoableTextViewer extends TextViewer {
  private static final int DEFAULT_UNDO_HISTORY_SIZE = 25;

  private IUndoManager undoManager;

  /**
   * Constructs a new UndoableTextViewer.
   *
   * @param parent the parent composite
   * @param styles the SWT style bits
   */
  public UndoableTextViewer(Composite parent, int styles) {
    super(parent, styles);
  }

  /**
   * Configures the undo manager for this text viewer. Call this method after setting the document.
   */
  protected void configureUndoManager() {
    configureUndoManager(DEFAULT_UNDO_HISTORY_SIZE);
  }

  /**
   * Configures the undo manager for this text viewer with a custom history size. Call this method after setting the
   * document.
   *
   * @param historySize the number of undo steps to keep in history
   */
  protected void configureUndoManager(int historySize) {
    this.undoManager = new TextViewerUndoManager(historySize);
    this.setUndoManager(this.undoManager);
    this.undoManager.connect(this);

    // Disconnect undo manager on disposal to prevent memory leaks
    this.getTextWidget().addDisposeListener(e -> {
      if (this.undoManager != null) {
        this.undoManager.disconnect();
      }
    });
  }

  /**
   * Handles undo/redo key events.
   *
   * @param e the key event
   * @return true if the event was handled, false otherwise
   */
  protected boolean handleUndoRedoKeyEvent(KeyEvent e) {
    // Handle undo (Cmd+Z on macOS, Ctrl+Z on other platforms)
    if (e.keyCode == 'z' && (e.stateMask & SWT.MOD1) != 0 && (e.stateMask & SWT.SHIFT) == 0) {
      if (canDoOperation(ITextOperationTarget.UNDO)) {
        doOperation(ITextOperationTarget.UNDO);
      }
      e.doit = false;
      return true;
    }

    // Handle redo (Cmd+Shift+Z on macOS, Ctrl+Y on other platforms)
    if ((e.keyCode == 'z' && (e.stateMask & SWT.MOD1) != 0 && (e.stateMask & SWT.SHIFT) != 0)
        || (e.keyCode == 'y' && (e.stateMask & SWT.MOD1) != 0)) {
      if (canDoOperation(ITextOperationTarget.REDO)) {
        doOperation(ITextOperationTarget.REDO);
      }
      e.doit = false;
      return true;
    }

    return false;
  }

  /**
   * Ends the current compound change in the undo manager. Call this to create undo boundaries (e.g., after whitespace
   * or newlines).
   */
  protected void endCompoundChange() {
    if (undoManager != null) {
      undoManager.endCompoundChange();
    }
  }
}

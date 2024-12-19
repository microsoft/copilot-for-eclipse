package com.microsoft.copilot.eclipse.ui.completion;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Listen to the lifecycle event of an editor parts.
 */
public class EditorLifecycleListener implements IPartListener {

  private EditorsManager manager;

  /**
   * Creates a new EditorLifecycleListener.
   */
  public EditorLifecycleListener(EditorsManager manager) {
    this.manager = manager;
  }

  @Override
  public void partActivated(IWorkbenchPart part) {
    createCompletionHandlerFor(part);

  }

  @Override
  public void partBroughtToTop(IWorkbenchPart part) {
    // do nothing.
  }

  @Override
  public void partClosed(IWorkbenchPart part) {
    disposeCompletionHandlerFor(part);
  }

  @Override
  public void partDeactivated(IWorkbenchPart part) {
    // do nothing.
  }

  @Override
  public void partOpened(IWorkbenchPart part) {
    // do nothing.
  }

  /**
   * Creates the {@link CompletionHandler} for the ITextEditor of the IWorkbenchPart.
   */
  public void createCompletionHandlerFor(IWorkbenchPart part) {
    IEditorPart editorPart = part.getAdapter(IEditorPart.class);
    if (editorPart != null) {
      ITextEditor editor = editorPart.getAdapter(ITextEditor.class);
      if (editor != null) {
        manager.getOrCreateCompletionHandlerFor(editor);
        manager.setActiveEditor(editor);
      }
    }
  }

  void disposeCompletionHandlerFor(IWorkbenchPart part) {
    IEditorPart editorPart = part.getAdapter(IEditorPart.class);
    if (editorPart != null) {
      ITextEditor editor = editorPart.getAdapter(ITextEditor.class);
      if (editor != null) {
        manager.disposeCompletionHandlerFor(editor);
        if (editor.equals(manager.getActiveEditor())) {
          manager.setActiveEditor(null);
        }
      }
    }
  }

}

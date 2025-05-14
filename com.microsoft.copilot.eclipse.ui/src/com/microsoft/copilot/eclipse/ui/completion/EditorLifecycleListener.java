package com.microsoft.copilot.eclipse.ui.completion;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;

/**
 * Listen to the lifecycle event of an editor parts.
 */
public class EditorLifecycleListener implements IPartListener2 {

  private EditorsManager manager;

  /**
   * Creates a new EditorLifecycleListener.
   */
  public EditorLifecycleListener(EditorsManager manager) {
    this.manager = manager;
  }

  @Override
  public void partActivated(IWorkbenchPartReference partRef) {
    createCompletionHandlerFor(partRef.getPart(false));
  }

  @Override
  public void partBroughtToTop(IWorkbenchPartReference partRef) {
    createCompletionHandlerFor(partRef.getPart(false));
  }

  @Override
  public void partInputChanged(IWorkbenchPartReference partRef) {
    // try to re-create the completion handler for the part to fix the completion manager is not created for the ABAP
    // editor in the beginning
    this.partActivated(partRef);
  }

  @Override
  public void partClosed(IWorkbenchPartReference partRef) {
    disposeCompletionHandlerFor(partRef.getPart(false));
  }

  /**
   * Creates the {@link CompletionManager} for the ITextEditor of the IWorkbenchPart.
   */
  public void createCompletionHandlerFor(IWorkbenchPart part) {
    IEditorPart editorPart = part.getAdapter(IEditorPart.class);
    if (editorPart != null) {
      manager.getOrCreateCompletionManagerFor(editorPart);
      manager.setActiveEditor(editorPart);
    }
  }

  void disposeCompletionHandlerFor(IWorkbenchPart part) {
    IEditorPart editorPart = part.getAdapter(IEditorPart.class);
    if (editorPart != null) {
      manager.disposeCompletionManagerFor(editorPart);
      if (editorPart.equals(manager.getActiveEditor())) {
        manager.setActiveEditor(null);
      }
    }
  }

}

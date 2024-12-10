package com.microsoft.copilot.eclipse.ui;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.completion.EditorLifecycleListener;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Activator class for the Copilot UI plugin.
 */
public class CopilotUi implements BundleActivator {

  private static final int RETRY_COUNT = 30;
  private EditorLifecycleListener editorLifecycleListener;
  private EditorsManager editorsManager;

  @Override
  public void start(BundleContext context) throws Exception {
    // wake up Core plugin and wait until copilot LS is ready
    // TODO: check if we can improve logic here, for example, use a listener to wait for LS ready.
    CopilotLanguageServerConnection connection = null;
    for (int i = 0; i < RETRY_COUNT; i++) {
      connection = CopilotCore.getPlugin().getCopilotLanguageServer();
      if (connection != null) {
        break;
      }
      Thread.sleep(1000);
    }
    if (connection == null) {
      // TODO: log & send telemetry
      throw new IllegalStateException("Copilot language server is not ready.");
    }

    this.editorsManager = new EditorsManager(connection);
    this.editorLifecycleListener = new EditorLifecycleListener(editorsManager);

    registerPartListener();

    // Initialize the completion handler for the active editor in case we miss the event
    // to initialize it.
    initComletionHandlerForActiveEditor();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    unregisterPartListener();
    if (this.editorsManager != null) {
      this.editorsManager.dispose();
    }
  }

  private void registerPartListener() {
    IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
    for (IWorkbenchWindow window : windows) {
      window.getPartService().addPartListener(this.editorLifecycleListener);
    }
  }

  private void initComletionHandlerForActiveEditor() {
    IEditorPart editorPart = SwtUtils.getActiveEditorPart();
    if (editorPart != null) {
      ITextEditor textEditor = editorPart.getAdapter(ITextEditor.class);
      if (textEditor != null) {
        this.editorsManager.getOrCreateCompletionHandlerFor(textEditor);
      }
    }
  }

  private void unregisterPartListener() {
    IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
    for (IWorkbenchWindow window : windows) {
      window.getPartService().removePartListener(this.editorLifecycleListener);
    }
  }

}

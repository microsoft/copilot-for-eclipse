package com.microsoft.copilot.eclipse.ui;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.logger.CopilotForEclipseLogger;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.completion.CompletionStatusManager;
import com.microsoft.copilot.eclipse.ui.completion.EditorLifecycleListener;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * The plug-in runtime class for the Copilot plug-in containing the UI support, like dialogs, ghost text rendering, etc.
 */
public class CopilotUi extends Plugin {

  private static final int RETRY_COUNT = 30;
  private static CopilotUi COPILOT_UI_PLUGIN = null;

  private CompletionStatusManager completionStatusManager;
  private EditorLifecycleListener editorLifecycleListener;
  private EditorsManager editorsManager;
  public static final CopilotForEclipseLogger LOGGER = new CopilotForEclipseLogger(CopilotCore.class.getName());

  /**
   * Creates the Copilot ui plugin. The plugin is created automatically by the Eclipse framework. Clients must not call
   * this constructor.
   */
  public CopilotUi() {
    super();
    COPILOT_UI_PLUGIN = this;
  }

  public static CopilotUi getPlugin() {
    return COPILOT_UI_PLUGIN;
  }

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
      var ex = new IllegalStateException("Failed to start copilot language server.");
      LOGGER.log(LogLevel.ERROR, ex);
      throw ex;
    }

    this.editorsManager = new EditorsManager(connection, CopilotCore.getPlugin().getCompletionProvider());
    this.editorLifecycleListener = new EditorLifecycleListener(editorsManager);
    this.completionStatusManager = new CompletionStatusManager();

    registerPartListener();
    addCompletionStatusListener();

    // Initialize the completion handler for the active editor in case we miss the event
    // to initialize it.
    initCompletionHandlerForActiveEditor();
  }
  
  public CompletionStatusManager getCompletionStatusManager() {
    return completionStatusManager;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    unregisterPartListener();
    removeCompletionStatusListener();
    if (this.editorsManager != null) {
      this.editorsManager.dispose();
    }
  }

  public EditorsManager getEditorsManager() {
    return editorsManager;
  }

  private void registerPartListener() {
    IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
    for (IWorkbenchWindow window : windows) {
      window.getPartService().addPartListener(this.editorLifecycleListener);
    }
  }

  private void addCompletionStatusListener() {
    CopilotCore.getPlugin().getCompletionProvider().addCompletionStatusListener(this.completionStatusManager);
  }

  private void initCompletionHandlerForActiveEditor() {
    IEditorPart editorPart = SwtUtils.getActiveEditorPart();
    if (editorPart != null) {
      this.editorLifecycleListener.createCompletionHandlerFor(editorPart);
    }
  }

  private void unregisterPartListener() {
    IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
    for (IWorkbenchWindow window : windows) {
      window.getPartService().removePartListener(this.editorLifecycleListener);
    }
  }

  private void removeCompletionStatusListener() {
    CopilotCore.getPlugin().getCompletionProvider().removeCompletionStatusListener(this.completionStatusManager);
  }

}

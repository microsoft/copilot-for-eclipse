package com.microsoft.copilot.eclipse.ui;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.completion.EditorLifecycleListener;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * The plug-in runtime class for the Copilot plug-in containing the UI support, like dialogs, ghost text rendering, etc.
 */
public class CopilotUi extends AbstractUIPlugin {

  private static CopilotUi COPILOT_UI_PLUGIN = null;

  private CopilotStatusManager copilotStatusManager;
  private EditorLifecycleListener editorLifecycleListener;
  private EditorsManager editorsManager;
  private LanguageServerSettingManager settingMgr;

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
    Job initJob = new Job("Copilot initialization") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          // wait until Core is initialized.
          Job.getJobManager().join(CopilotCore.INIT_JOB_FAMILY, null);

          CopilotLanguageServerConnection connection = CopilotCore.getPlugin().getCopilotLanguageServer();
          if (connection == null) {
            var ex = new IllegalStateException("Failed to start copilot language server.");
            CopilotCore.LOGGER.error(ex);
            throw ex;
          }

          // init the settings manager
          ServiceReference<?> serviceReference = context.getServiceReference(IProxyService.class.getName());
          LanguageServerSettingManager mgr = new LanguageServerSettingManager(
              CopilotCore.getPlugin().getCopilotLanguageServer(), (IProxyService) context.getService(serviceReference),
              getPreferenceStore());
          CopilotUi.this.settingMgr = mgr;
          CopilotUi.this.editorsManager = new EditorsManager(connection,
              CopilotCore.getPlugin().getCompletionProvider(), mgr);
          CopilotUi.this.editorLifecycleListener = new EditorLifecycleListener(editorsManager);
          CopilotUi.this.copilotStatusManager = new CopilotStatusManager();
          // sync to language server on load
          mgr.syncConfiguration();

          registerPartListener();
          addCompletionStatusListener();
          addCopilotAuthStatusListener();

          // Initialize the completion handler for the active editor in case we miss the event
          // to initialize it.
          initCompletionHandlerForActiveEditor();
        } catch (OperationCanceledException | InterruptedException e) {
          CopilotCore.LOGGER.error(e);
          return Status.error("Failed to initialize GitHub Copilot plugin.", e);
        }
        return Status.OK_STATUS;
      }
    };
    initJob.setSystem(true);
    initJob.schedule();
  }

  public CopilotStatusManager getCopilotStatusManager() {
    return copilotStatusManager;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    unregisterPartListener();
    removeCompletionStatusListener();
    removeCopilotAuthStatusListener();

    if (this.editorsManager != null) {
      this.editorsManager.dispose();
    }

    if (this.settingMgr != null) {
      this.settingMgr.dispose();
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

  private void addCopilotAuthStatusListener() {
    CopilotCore.getPlugin().getAuthStatusManager().addCopilotAuthStatusListener(this.copilotStatusManager);
  }

  private void addCompletionStatusListener() {
    CopilotCore.getPlugin().getCompletionProvider().addCompletionStatusListener(this.copilotStatusManager);
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
    CopilotCore.getPlugin().getCompletionProvider().removeCompletionStatusListener(this.copilotStatusManager);
  }

  private void removeCopilotAuthStatusListener() {
    CopilotCore.getPlugin().getAuthStatusManager().removeCopilotAuthStatusListener(this.copilotStatusManager);
  }

}

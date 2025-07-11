package com.microsoft.copilot.eclipse.ui;

import java.io.IOException;
import java.util.Objects;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.completion.EditorLifecycleListener;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * The plug-in runtime class for the Copilot plug-in containing the UI support, like dialogs, ghost text rendering, etc.
 */
public class CopilotUi extends AbstractUIPlugin {

  private static CopilotUi COPILOT_UI_PLUGIN = null;

  private CopilotStatusManager copilotStatusManager;
  private EditorLifecycleListener editorLifecycleListener;
  private EditorsManager editorsManager;
  private ChatServiceManager chatServiceManager;
  private LanguageServerSettingManager settingMgr;

  public static final String INIT_JOB_FAMILY = "com.microsoft.copilot.eclipse.ui.initJob";

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
        showHintIfNecessary(context);

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
          CopilotUi.this.editorLifecycleListener = new EditorLifecycleListener(connection, editorsManager);
          CopilotUi.this.chatServiceManager = new ChatServiceManager();
          // inject the chat service manager into the core plugin, so that it can be used to handle
          // some server to client request that needs to be handled with UI logics.
          CopilotCore.getPlugin().setChatServiceManager(chatServiceManager);
          CopilotUi.this.copilotStatusManager = new CopilotStatusManager();
          // sync to language server on load
          mgr.syncConfiguration();

          registerPartListener();
          // Initialize the completion handler for the active editor in case we miss the event
          // to initialize it.
          initCompletionHandlerForActiveEditor();

          addCopilotAuthStatusListener();
          // refresh the menu icon in case we miss the event to refresh it.
          UiUtils.refreshCopilotMenu();

        } catch (OperationCanceledException | InterruptedException e) {
          CopilotCore.LOGGER.error(e);
          return Status.error("Failed to initialize GitHub Copilot plugin.", e);
        }
        return Status.OK_STATUS;
      }

      @Override
      public boolean belongsTo(Object family) {
        return INIT_JOB_FAMILY.equals(family);
      }
    };
    initJob.setSystem(true);
    initJob.schedule();
  }

  private void showHintIfNecessary(BundleContext context) {
    IPreferenceStore preferenceStore = CopilotUi.getPlugin().getPreferenceStore();
    if (!(preferenceStore instanceof IPersistentPreferenceStore)) {
      // to make sure the updated preference store is saved, we will only show the quick start
      // if the preference store is IPersistentPreferenceStore.
      return;
    }

    int storedVersion = preferenceStore.getInt(Constants.QUICK_START_VERSION);
    if (storedVersion < Constants.CURRENT_QUICK_START_VERSION) {
      SwtUtils.invokeOnDisplayThreadAsync(
          () -> UiUtils.executeCommandWithParameters("com.microsoft.copilot.eclipse.commands.openQuickStart", null));
      preferenceStore.setValue(Constants.QUICK_START_VERSION, Constants.CURRENT_QUICK_START_VERSION);
    }

    String lastUsedVersion = preferenceStore.getString(Constants.LAST_USED_PLUGIN_VERSION);
    Version bundleVersion = context.getBundle().getVersion();
    String currentVersion = bundleVersion.getMajor() + "." + bundleVersion.getMinor();
    if (!Objects.equals(lastUsedVersion, currentVersion)) {
      SwtUtils.invokeOnDisplayThreadAsync(
          () -> UiUtils.executeCommandWithParameters("com.microsoft.copilot.eclipse.commands.showWhatIsNew", null));
      preferenceStore.setValue(Constants.LAST_USED_PLUGIN_VERSION, currentVersion);
    }

    IPersistentPreferenceStore ps = (IPersistentPreferenceStore) preferenceStore;
    if (ps.needsSaving()) {
      try {
        ps.save();
      } catch (IOException e) {
        CopilotCore.LOGGER.error("Failed to save preference store during preference update.", e);
      }
    }
  }

  public CopilotStatusManager getCopilotStatusManager() {
    return copilotStatusManager;
  }

  public LanguageServerSettingManager getLanguageServerSettingManager() {
    return settingMgr;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    unregisterPartListener();
    removeCopilotAuthStatusListener();

    if (this.editorsManager != null) {
      this.editorsManager.dispose();
    }

    if (this.settingMgr != null) {
      this.settingMgr.dispose();
    }

    if (this.chatServiceManager != null) {
      this.chatServiceManager.dispose();
    }
  }

  public EditorsManager getEditorsManager() {
    return editorsManager;
  }

  public ChatServiceManager getChatServiceManager() {
    return chatServiceManager;
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

  private void initCompletionHandlerForActiveEditor() {
    IEditorPart editorPart = SwtUtils.getActiveEditorPart();
    if (editorPart != null) {
      this.editorLifecycleListener.partActivated(editorPart);
    }
  }

  private void unregisterPartListener() {
    IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
    for (IWorkbenchWindow window : windows) {
      window.getPartService().removePartListener(this.editorLifecycleListener);
    }
  }

  private void removeCopilotAuthStatusListener() {
    CopilotCore.getPlugin().getAuthStatusManager().removeCopilotAuthStatusListener(this.copilotStatusManager);
  }

}
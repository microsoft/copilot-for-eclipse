package com.microsoft.copilot.eclipse.ui;

import java.util.Objects;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;

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
        try {
          // wait until Core is initialized.
          Job.getJobManager().join(CopilotCore.INIT_JOB_FAMILY, null);
        } catch (OperationCanceledException | InterruptedException e) {
          CopilotCore.LOGGER.error(e);
          return Status.error("Failed to initialize GitHub Copilot plugin.", e);
        }

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
        CopilotUi.this.editorsManager = new EditorsManager(connection, CopilotCore.getPlugin().getCompletionProvider(),
            mgr);
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

        showHintIfNecessary(context);
        return Status.OK_STATUS;
      }

      @Override
      public boolean belongsTo(Object family) {
        return INIT_JOB_FAMILY.equals(family);
      }
    };
    initJob.setSystem(true);
    initJob.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        // refresh the menu icon in case we miss the event to refresh it.
        UiUtils.refreshCopilotMenu();
      }
    });
    initJob.schedule();
  }

  private void showHintIfNecessary(BundleContext context) {
    IPreferenceStore preferenceStore = CopilotUi.getPlugin().getPreferenceStore();
    IEclipsePreferences configPrefs = ConfigurationScope.INSTANCE.getNode(Constants.PLUGIN_ID);
    boolean needToFlush = false;
    int storedQuickStartVersion = Math.max(configPrefs.getInt(Constants.COPILOT_QUICK_START_VERSION, 0),
        preferenceStore.getInt(Constants.QUICK_START_VERSION));

    if (storedQuickStartVersion < Constants.CURRENT_COPILOT_QUICK_START_VERSION) {
      SwtUtils.invokeOnDisplayThreadAsync(
          () -> UiUtils.executeCommandWithParameters(UiConstants.OPEN_QUICK_START_COMMAND_ID, null));
      configPrefs.putInt(Constants.COPILOT_QUICK_START_VERSION, Constants.CURRENT_COPILOT_QUICK_START_VERSION);
      needToFlush = true;
    }

    if (configPrefs.getBoolean(Constants.AUTO_SHOW_WHAT_IS_NEW, true)) {
      String lastUsedVersion = configPrefs.get(Constants.LAST_USED_COPILOT_PLUGIN_VERSION, "0.0");
      Version bundleVersion = context.getBundle().getVersion();
      String currentVersion = bundleVersion.getMajor() + "." + bundleVersion.getMinor();
      if (!Objects.equals(lastUsedVersion, currentVersion)) {
        SwtUtils.invokeOnDisplayThreadAsync(
            () -> UiUtils.executeCommandWithParameters(UiConstants.OPEN_WHATS_NEW_COMMAND_ID, null));
        configPrefs.put(Constants.LAST_USED_COPILOT_PLUGIN_VERSION, currentVersion);
        needToFlush = true;
      }
    }

    if (needToFlush) {
      try {
        configPrefs.flush();
      } catch (BackingStoreException e) {
        CopilotCore.LOGGER.error("Failed to persist hint in ConfigurationScope", e);
      }
    }
  }

  public LanguageServerSettingManager getLanguageServerSettingManager() {
    return settingMgr;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    unregisterPartListener();

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

}
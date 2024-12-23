package com.microsoft.copilot.eclipse.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.lsp4e.LanguageServersRegistry;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.osgi.framework.BundleContext;

import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.logger.CopilotForEclipseLogger;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;

/**
 * The plug-in runtime class for the Copilot plug-in containing the core (UI-free) support, like the completion,
 * authentication, language server connection, etc.
 */
public class CopilotCore extends Plugin {

  private CopilotLanguageServerConnection copilotLanguageServer;
  private CopilotStatusManager copilotStatusManager;
  private CompletionProvider completionProvider;

  private static CopilotCore COPILOT_CORE_PLUGIN = null;
  public static final CopilotForEclipseLogger LOGGER = new CopilotForEclipseLogger(CopilotCore.class.getName());

  /**
   * Creates the Copilot core plugin. The plugin is created automatically by the Eclipse framework. Clients must not
   * call this constructor.
   */
  public CopilotCore() {
    super();
    COPILOT_CORE_PLUGIN = this;
  }

  public static CopilotCore getPlugin() {
    return COPILOT_CORE_PLUGIN;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    init();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    if (copilotLanguageServer != null) {
      copilotLanguageServer.stop();
    }
  }

  @SuppressWarnings("restriction")
  void init() {
    final Runnable initRunnable = () -> {
      LanguageServersRegistry.LanguageServerDefinition serverDef = LanguageServersRegistry.getInstance()
          .getDefinition(CopilotLanguageServerConnection.SERVER_ID);
      if (serverDef == null) {
        var ex = new IllegalStateException(
            "Language server definition not found for " + CopilotLanguageServerConnection.SERVER_ID);
        CopilotCore.LOGGER.log(LogLevel.ERROR, ex);
        throw ex;
      }

      LanguageServerWrapper wrapper = LanguageServiceAccessor.startLanguageServer(serverDef);
      this.copilotLanguageServer = new CopilotLanguageServerConnection(wrapper);
      this.copilotStatusManager = new CopilotStatusManager(this.copilotLanguageServer);
      this.completionProvider = new CompletionProvider(this.copilotLanguageServer, copilotStatusManager);

      this.copilotStatusManager.checkStatus();
    };

    Job initJob = new Job("GitHub Copilot Initialization...") {
      protected IStatus run(IProgressMonitor monitor) {
        initRunnable.run();
        return Status.OK_STATUS;
      }
    };
    initJob.setUser(true);
    initJob.schedule();
  }

  public CopilotLanguageServerConnection getCopilotLanguageServer() {
    return copilotLanguageServer;
  }

  public CopilotStatusManager getCopilotStatusManager() {
    return copilotStatusManager;
  }

  public CompletionProvider getCompletionProvider() {
    return completionProvider;
  }

}

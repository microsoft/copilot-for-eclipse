// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.lsp4e.LanguageServersRegistry;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.osgi.framework.BundleContext;

import com.microsoft.copilot.eclipse.core.chat.ChatEventsManager;
import com.microsoft.copilot.eclipse.core.chat.service.IChatServiceManager;
import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.format.FormatOptionProvider;
import com.microsoft.copilot.eclipse.core.logger.CopilotForEclipseLogger;
import com.microsoft.copilot.eclipse.core.logger.GithubPanicErrorReport;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.nes.NextEditSuggestionProvider;

/**
 * The plug-in runtime class for the Copilot plug-in containing the core (UI-free) support, like the completion,
 * authentication, language server connection, etc.
 */
public class CopilotCore extends Plugin {

  private CopilotLanguageServerConnection copilotLanguageServer;
  private AuthStatusManager authStatusManager;
  private CompletionProvider completionProvider;
  private NextEditSuggestionProvider nextEditSuggestionProvider;
  private FormatOptionProvider formatOptionProvider;
  private GithubPanicErrorReport githubPanicErrorReport;
  private ChatEventsManager chatEventsManager;
  private IChatServiceManager chatServiceManager;
  private FeatureFlags featureFlags;

  /**
   * Reports exceptions away from the thread that logged them. A platform log listener runs on
   * whatever thread happened to log, so talking to the language server there would let an unrelated
   * caller's locks meet the language server's own locks. See {@link #reportException(Throwable)}.
   */
  private final ExecutorService exceptionReporter = Executors.newSingleThreadExecutor(runnable -> {
    Thread thread = new Thread(runnable, "GitHub Copilot exception reporter");
    thread.setDaemon(true);
    return thread;
  });

  /**
   * Set as soon as the bundle starts stopping, so errors logged during shutdown do not ask a
   * language server that is going away - or worse, start a fresh one that nothing will ever stop.
   */
  private volatile boolean stopping;

  private static CopilotCore COPILOT_CORE_PLUGIN = null;
  public static final CopilotForEclipseLogger LOGGER = new CopilotForEclipseLogger(CopilotCore.class.getName());

  /**
   * The job family for the initialization job.
   */
  public static final String INIT_JOB_FAMILY = "com.microsoft.copilot.eclipse.core.initJob";

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
    init(context);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    stopping = true;
    exceptionReporter.shutdownNow();
    if (copilotLanguageServer != null) {
      copilotLanguageServer.stop();
    }
  }

  @SuppressWarnings("restriction")
  void init(BundleContext context) {
    final Runnable initRunnable = () -> {
      addPlatformLogListener();
      LanguageServersRegistry.LanguageServerDefinition serverDef = LanguageServersRegistry.getInstance()
          .getDefinition(CopilotLanguageServerConnection.SERVER_ID);
      if (serverDef == null) {
        var ex = new IllegalStateException(
            "Language server definition not found for " + CopilotLanguageServerConnection.SERVER_ID);
        CopilotCore.LOGGER.error(ex);
        throw ex;
      }

      LanguageServerWrapper wrapper = LanguageServiceAccessor.startLanguageServer(serverDef);
      this.copilotLanguageServer = new CopilotLanguageServerConnection(wrapper);
      this.authStatusManager = new AuthStatusManager(this.copilotLanguageServer);
      this.completionProvider = new CompletionProvider(this.copilotLanguageServer, authStatusManager);
      this.githubPanicErrorReport = new GithubPanicErrorReport();
      this.featureFlags = new FeatureFlags();
    };

    Job initJob = new Job("GitHub Copilot Initialization...") {
      protected IStatus run(IProgressMonitor monitor) {
        initRunnable.run();
        return Status.OK_STATUS;
      }

      @Override
      public boolean belongsTo(Object family) {
        return Objects.equals(INIT_JOB_FAMILY, family);
      }
    };
    initJob.setUser(false);
    initJob.schedule();
  }

  /**
   * Add platform level log listener to catch the uncaught exceptions.
   */
  private void addPlatformLogListener() {
    Platform.addLogListener((status, plugin) -> {
      if (status.getSeverity() != IStatus.ERROR || plugin.equals(Constants.PLUGIN_ID)) {
        // only send telemetry for those errors that are not from the plugin itself
        return;
      }
      Throwable rawException = status.getException();
      if (rawException == null) {
        return;
      }
      Throwable currentException = rawException;
      do {
        StackTraceElement[] traces = currentException.getStackTrace();
        for (StackTraceElement trace : traces) {
          if (!trace.getClassName().startsWith(Constants.PLUGIN_ID)) {
            continue;
          }
          reportException(rawException);
          return;
        }
      } while ((currentException = currentException.getCause()) != null);
    });
  }

  public CopilotLanguageServerConnection getCopilotLanguageServer() {
    return copilotLanguageServer;
  }

  public AuthStatusManager getAuthStatusManager() {
    return authStatusManager;
  }

  public CompletionProvider getCompletionProvider() {
    return completionProvider;
  }

  /**
   * Get the next edit suggestion provider in lazy-load manner.
   */
  public NextEditSuggestionProvider getNextEditSuggestionProvider() {
    if (this.nextEditSuggestionProvider == null) {
      this.nextEditSuggestionProvider = new NextEditSuggestionProvider(this.copilotLanguageServer);
    }
    return nextEditSuggestionProvider;
  }

  public GithubPanicErrorReport getGithubPanicErrorReport() {
    return githubPanicErrorReport;
  }

  public FeatureFlags getFeatureFlags() {
    return featureFlags;
  }

  /**
   * Get the format option provider in lazy-load manner.
   */
  public FormatOptionProvider getFormatOptionProvider() {
    if (this.formatOptionProvider == null) {
      this.formatOptionProvider = new FormatOptionProvider();
    }
    return formatOptionProvider;
  }

  /**
   * Report the exception to the telemetry.
   *
   * <p>This is driven by a platform log listener, so it can run on any thread that happens to log an
   * error - including the language server's own start-up, which holds
   * {@code LanguageServerWrapper}'s inner context lock. Reaching back into the language server here
   * would take that class' locks in the opposite order to shutdown, and the two deadlock: start-up
   * waits for the wrapper that shutdown holds, while shutdown waits for the context that start-up
   * holds. Nothing then releases, the JVM never exits, and a Tycho test fork hangs until CI gives
   * up hours later. Report on our own thread instead, so the logging thread is free to move on.</p>
   *
   * @param ex the exception to report
   */
  public void reportException(Throwable ex) {
    if (stopping) {
      return;
    }
    CopilotLanguageServerConnection server = this.copilotLanguageServer;
    if (server == null) {
      GithubPanicErrorReport report = this.githubPanicErrorReport;
      if (report != null) {
        report.report(ex);
      }
      return;
    }
    try {
      exceptionReporter.execute(() -> {
        if (!stopping) {
          server.sendExceptionTelemetry(ex);
        }
      });
    } catch (RejectedExecutionException e) {
      // Shutting down; the exception is already in the platform log, which is all we can offer now.
    }
  }

  /**
   * Get the chat provider.
   *
   * @return the chat provider.
   */
  public ChatEventsManager getChatEventsManager() {
    if (chatEventsManager == null) {
      chatEventsManager = new ChatEventsManager();
    }
    return chatEventsManager;
  }

  @Nullable
  public IChatServiceManager getChatServiceManager() {
    return chatServiceManager;
  }

  public void setChatServiceManager(IChatServiceManager chatServiceManager) {
    this.chatServiceManager = chatServiceManager;
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import com.microsoft.copilot.eclipse.core.logger.ExceptionReporter;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.nes.NextEditSuggestionProvider;

/**
 * The plug-in runtime class for the Copilot plug-in containing the core (UI-free) support, like the completion,
 * authentication, language server connection, etc.
 */
public class CopilotCore extends Plugin {

  private volatile CopilotLanguageServerConnection copilotLanguageServer;
  private AuthStatusManager authStatusManager;
  private CompletionProvider completionProvider;
  private NextEditSuggestionProvider nextEditSuggestionProvider;
  private FormatOptionProvider formatOptionProvider;
  private ChatEventsManager chatEventsManager;
  private IChatServiceManager chatServiceManager;
  private FeatureFlags featureFlags;
  private final ExceptionReporter exceptionReporter;
  private final AtomicBoolean stopping = new AtomicBoolean();
  private volatile Job initJob;

  private static CopilotCore COPILOT_CORE_PLUGIN = null;
  public static final CopilotForEclipseLogger LOGGER = new CopilotForEclipseLogger(CopilotCore.class.getName());

  /**
   * The job family for the initialization job.
   */
  public static final String INIT_JOB_FAMILY = "com.microsoft.copilot.eclipse.core.initJob";

  /**
   * How long {@link #stop(BundleContext)} waits for the language server wind-down before it gives
   * up and lets the framework shutdown continue.
   */
  private static final long SHUTDOWN_TIMEOUT_MS = 30_000L;

  /**
   * Creates the Copilot core plugin. The plugin is created automatically by the Eclipse framework. Clients must not
   * call this constructor.
   */
  public CopilotCore() {
    super();
    COPILOT_CORE_PLUGIN = this;
    exceptionReporter = new ExceptionReporter(this::currentExceptionSink);
  }

  /**
   * Resolves where exception reports go right now. The language server connection is the only owner
   * of that endpoint, and it already drops reports once it has stopped, so there is nothing to
   * unregister here.
   */
  private Consumer<Throwable> currentExceptionSink() {
    CopilotLanguageServerConnection connection = copilotLanguageServer;
    return connection == null ? null : connection::sendExceptionTelemetry;
  }

  public static CopilotCore getPlugin() {
    return COPILOT_CORE_PLUGIN;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    exceptionReporter.start();
    init(context);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    stopping.set(true);
    exceptionReporter.close();

    Job job = initJob;
    if (job != null) {
      // cancel() only raises a flag the job has to poll, so it cannot unblock a job that is already
      // inside language server startup; the wait below is what keeps that bounded.
      job.cancel();
    }

    // The wind-down can block for an unbounded time: LanguageServerWrapper guards start() and
    // stop() with the same monitor, so stop() waits for an in-flight start() that may itself be
    // waiting on a server process that never finishes initializing. Run it on a daemon thread with
    // a budget so a wedged server delays the framework shutdown instead of blocking it forever.
    Thread shutdown = new Thread(() -> {
      try {
        if (job != null) {
          job.join();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        stopLanguageServer();
      }
    }, "Copilot shutdown");
    shutdown.setDaemon(true);
    shutdown.start();

    try {
      shutdown.join(SHUTDOWN_TIMEOUT_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw e;
    }
    if (shutdown.isAlive()) {
      LOGGER.error(new IllegalStateException("Gave up waiting for the GitHub Copilot language server"
          + " to shut down after " + SHUTDOWN_TIMEOUT_MS + " ms."));
    }
  }

  private void stopLanguageServer() {
    CopilotLanguageServerConnection connection = copilotLanguageServer;
    if (connection == null) {
      return;
    }
    try {
      connection.stop();
    } catch (RuntimeException e) {
      LOGGER.error(e);
    }
  }

  @SuppressWarnings("restriction")
  void init(BundleContext context) {
    initJob = new Job("GitHub Copilot Initialization...") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        if (monitor.isCanceled() || stopping.get()) {
          return Status.CANCEL_STATUS;
        }

        LanguageServersRegistry.LanguageServerDefinition serverDef = LanguageServersRegistry.getInstance()
            .getDefinition(CopilotLanguageServerConnection.SERVER_ID);
        if (serverDef == null) {
          var ex = new IllegalStateException(
              "Language server definition not found for " + CopilotLanguageServerConnection.SERVER_ID);
          CopilotCore.LOGGER.error(ex);
          throw ex;
        }

        LanguageServerWrapper wrapper = LanguageServiceAccessor.startLanguageServer(serverDef);
        CopilotLanguageServerConnection connection = new CopilotLanguageServerConnection(wrapper);
        copilotLanguageServer = connection;
        if (monitor.isCanceled() || stopping.get()) {
          connection.stop();
          return Status.CANCEL_STATUS;
        }

        authStatusManager = new AuthStatusManager(connection);
        completionProvider = new CompletionProvider(connection, authStatusManager);
        featureFlags = new FeatureFlags();
        if (monitor.isCanceled() || stopping.get()) {
          connection.stop();
          return Status.CANCEL_STATUS;
        }
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
   * Report an exception without blocking the caller.
   *
   * @param ex the exception to report
   */
  public void reportException(Throwable ex) {
    exceptionReporter.report(ex);
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

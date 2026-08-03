// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
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
   * How long {@link #stop(BundleContext)} waits for the initialization job to observe the
   * cancellation before it gives up and lets the framework shutdown continue.
   */
  private static final long INIT_JOB_SHUTDOWN_TIMEOUT_MS = 30_000L;

  /**
   * Creates the Copilot core plugin. The plugin is created automatically by the Eclipse framework. Clients must not
   * call this constructor.
   */
  public CopilotCore() {
    super();
    COPILOT_CORE_PLUGIN = this;
    exceptionReporter = new ExceptionReporter();
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
    try {
      Job job = initJob;
      if (job != null) {
        // Blocking is intentional: the bundle must not be unloaded while the initialization job is
        // still touching the language server, so wait for it to observe the cancellation. The wait
        // is bounded because cancel() only raises a flag the job has to poll, and the job may be
        // blocked inside language server startup where there is no such checkpoint; waiting forever
        // there would stall the whole framework shutdown. Giving up is safe: `stopping` is already
        // set, so the job stops the connection itself at its next checkpoint.
        job.cancel();
        if (!job.join(INIT_JOB_SHUTDOWN_TIMEOUT_MS, new NullProgressMonitor())) {
          LOGGER.error(new IllegalStateException("Gave up waiting for the GitHub Copilot initialization"
              + " job to stop after " + INIT_JOB_SHUTDOWN_TIMEOUT_MS + " ms."));
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw e;
    } finally {
      CopilotLanguageServerConnection connection = copilotLanguageServer;
      if (connection != null) {
        connection.stop();
      }
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

        exceptionReporter.setSink(connection::sendExceptionTelemetry);
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

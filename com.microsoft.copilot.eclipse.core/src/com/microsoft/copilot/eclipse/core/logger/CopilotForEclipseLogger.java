package com.microsoft.copilot.eclipse.core.logger;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.microsoft.copilot.eclipse.core.logger.handlers.EclipseConsoleHandler;
import com.microsoft.copilot.eclipse.core.logger.handlers.GithubExceptionTelemetryHandler;

/**
 * The logger for Copilot for Eclipse.
 */
public class CopilotForEclipseLogger {
  // TODO: migrate to xml configuration
  private Logger logger;

  /**
   * Constructor.
   *
   * @param name the name of the logger
   *
   */
  public CopilotForEclipseLogger(String name) {
    logger = Logger.getLogger(name);
    setupLoggers(logger);
  }

  /**
   * Log level.
   */
  public void info(String message) {
    LogRecord logRecord = new LogRecord(Level.INFO, message);
    logRecord.setParameters(new Object[] { LogLevel.INFO, message });
    logger.log(logRecord);
  }

  /**
   * Log level.
   */
  public void error(String message, Throwable ex) {
    LogRecord logRecord = new LogRecord(Level.SEVERE, message);
    logRecord.setParameters(new Object[] { LogLevel.ERROR, ex });
    logger.log(logRecord);
  }

  /**
   * Log level.
   */
  public void error(Throwable ex) {
    LogRecord logRecord = new LogRecord(Level.SEVERE, ex.getMessage());
    logRecord.setParameters(new Object[] { LogLevel.ERROR, ex });
    logger.log(logRecord);
  }


  /**
   * Set up the loggers.
   *
   * @param LOGGER the logger
   */
  private static void setupLoggers(Logger logger) {
    logger.setUseParentHandlers(false);
    Bundle bundle = FrameworkUtil.getBundle(CopilotForEclipseLogger.class);
    if (bundle == null) {
      return;
    }
    EclipseConsoleHandler consoleHandler = new EclipseConsoleHandler(Platform.getLog(bundle));
    consoleHandler.setLevel(Level.ALL);
    logger.addHandler(consoleHandler);
    GithubExceptionTelemetryHandler ghHandler = new GithubExceptionTelemetryHandler();
    ghHandler.setLevel(Level.ALL);
    logger.addHandler(ghHandler);
  }
}
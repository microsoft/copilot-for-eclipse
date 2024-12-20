package com.microsoft.copilot.eclipse.core.logger;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.microsoft.copilot.eclipse.core.enums.LogLevel;
import com.microsoft.copilot.eclipse.core.logger.handlers.EclipseConsoleHandler;

/**
 * The logger for Copilot for Eclipse.
 */
public class CopilotForEclipseLogger {
  //TODO: migrate to xml configuration
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
   * Log the message.
   *
   * @param lvl the log level
   * @param parameters the parameters
   */
  public void log(LogLevel lvl, Object... parameters) {
    Level level = map2Level(lvl);
    LogRecord logRecord = new LogRecord(level, "");
    logRecord.setParameters(new Object[] { lvl, parameters });
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
  }

  /**
   * Map the LogLevel to the Level.
   *
   * @param level the LogLevel
   * @return the Level
   */
  private Level map2Level(LogLevel level) {
    switch (level) {
      case INFO:
        return Level.INFO;
      case WARNING:
        return Level.WARNING;
      case ERROR:
        return Level.SEVERE;
      default:
        return Level.INFO;
    }
  }
}
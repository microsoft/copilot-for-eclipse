package com.microsoft.copilot.eclipse.core.logger.handlers;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;

/**
 * The log appender to send info and error messages to the Eclipse console.
 */
public class EclipseConsoleHandler extends Handler {
  private ILog logger;

  /**
   * Constructor.
   */
  public EclipseConsoleHandler(ILog logger) {
    this.logger = logger;
  }

  @Override
  public void publish(LogRecord logRecord) {
    if (logger == null) {
      return;
    }
    Object[] property = logRecord.getParameters();
    if (property == null || property.length < 2) {
      return;
    }
    if (!(property[0] instanceof LogLevel)) {
      return;
    }
    LogLevel lvl = (LogLevel) property[0];
    switch (lvl) {
      case INFO:
        logger.log(new Status(IStatus.INFO, Constants.PLUGIN_ID, "[Info] " + logRecord.getMessage(), null));
        break;
      case ERROR:
        Throwable ex = (Throwable) property[1];
        logger.log(new Status(IStatus.ERROR, Constants.PLUGIN_ID, "[Error] " + logRecord.getMessage(), ex));
        break;
      default:
        break;
    }
  }

  @Override
  public void flush() {
    // do nothing
  }

  @Override
  public void close() throws SecurityException {
    // do nothing
  }
}
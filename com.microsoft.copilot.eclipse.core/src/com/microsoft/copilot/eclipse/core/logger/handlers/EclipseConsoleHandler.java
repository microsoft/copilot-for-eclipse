package com.microsoft.copilot.eclipse.core.logger.handlers;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.google.gson.Gson;
import org.eclipse.core.runtime.ILog;
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
    Object[] params = (Object[]) property[1];
    int level = map2StatusLevel(lvl);
    logger.log(new Status(level, Constants.PLUGIN_ID, getFormatedMessage(params), getThrowable(params)));
  }

  private String getFormatedMessage(Object[] properties) {
    String str = "";
    for (int i = 0; i < properties.length; i++) {
      str += "argv" + i + " = ";
      try {
        str += new Gson().toJson(properties[i]);
      } catch (Exception e) {
        str += "exceptionInToJson";
      }
      str += " ;";
    }
    return str;
  }

  private Throwable getThrowable(Object[] properties) {
    for (int i = 1; i < properties.length; i++) {
      if (properties[i] instanceof Throwable) {
        return (Throwable) properties[i];
      }
    }
    return null;
  }

  @Override
  public void flush() {
    // do nothing
  }

  @Override
  public void close() throws SecurityException {
    // do nothing
  }

  private int map2StatusLevel(LogLevel level) {
    switch (level) {
      case INFO:
        return Status.INFO;
      case WARNING:
        return Status.WARNING;
      case ERROR:
        return Status.ERROR;
      default:
        return Status.INFO;
    }
  }
}
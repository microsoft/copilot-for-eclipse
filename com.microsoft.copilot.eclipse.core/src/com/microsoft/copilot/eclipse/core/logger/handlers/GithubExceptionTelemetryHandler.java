package com.microsoft.copilot.eclipse.core.logger.handlers;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;

/**
 * The log appender to send info and error messages to the Eclipse console.
 */
public class GithubExceptionTelemetryHandler extends Handler {
  /**
   * Constructor.
   */
  public GithubExceptionTelemetryHandler() {
  }

  @Override
  public void publish(LogRecord logRecord) {
    Object[] property = logRecord.getParameters();
    if (property == null || property.length < 2) {
      return;
    }
    if (!(property[0] instanceof LogLevel)) {
      return;
    }
    LogLevel lvl = (LogLevel) property[0];
    if (lvl != LogLevel.ERROR) {
      return;
    }
    Throwable ex = (Throwable) property[1];
    CopilotCore copilotCore = CopilotCore.getPlugin();
    if (copilotCore == null) {
      return;
    }
    copilotCore.reportException(ex);
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

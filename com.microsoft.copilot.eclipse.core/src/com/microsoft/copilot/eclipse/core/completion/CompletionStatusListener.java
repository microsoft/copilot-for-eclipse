package com.microsoft.copilot.eclipse.core.completion;

/**
 * Listener for completion status to track the completion progress.
 */
public interface CompletionStatusListener {

  /**
   * Notifies to the listeners when the completion is about to run.
   */
  void onCompletionAboutToRun();

  /**
   * Notifies to the listeners when the completion is done.
   */
  void onCompletionDone();

}

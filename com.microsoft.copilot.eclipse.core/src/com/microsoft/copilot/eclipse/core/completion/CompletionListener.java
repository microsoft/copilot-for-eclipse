package com.microsoft.copilot.eclipse.core.completion;

/**
 * Listener for completion resolution.
 */
public interface CompletionListener {

  /**
   * Notifies to the listeners when the completion is resolved.
   */
  void onCompletionResolved(CompletionCollection completions);

}

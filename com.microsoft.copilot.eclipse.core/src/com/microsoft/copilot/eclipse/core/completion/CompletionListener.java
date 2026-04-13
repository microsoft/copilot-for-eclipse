// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.completion;

import java.util.List;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;

/**
 * Listener for completion resolution.
 */
public interface CompletionListener {

  /**
   * Notifies to the listeners when the completion is resolved.
   */
  void onCompletionResolved(String uriString, List<CompletionItem> completions);

}

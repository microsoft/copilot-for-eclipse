// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;

/**
 * Result of the {@code workspace/findTextInFiles} request, containing the list of matches.
 *
 * @param matches the list of text search matches
 */
public record FindTextInFilesResult(List<TextSearchMatch> matches) {

  /**
   * A single text search match. Field names mirror the CLS protocol.
   *
   * @param uri the URI of the file containing the match
   * @param lineNumber the 1-based line number of the match within the file
   * @param lineText the full text of the line containing the match
   */
  public record TextSearchMatch(String uri, int lineNumber, String lineText) {
  }

}

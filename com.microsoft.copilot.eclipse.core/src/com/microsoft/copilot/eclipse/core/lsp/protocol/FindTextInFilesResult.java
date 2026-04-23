// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Result of the {@code workspace/findTextInFiles} request, containing the list of matches.
 */
public class FindTextInFilesResult {

  private List<TextSearchMatch> matches;

  /**
   * Constructs a new FindTextInFilesResult object.
   *
   * @param matches the list of text search matches
   */
  public FindTextInFilesResult(List<TextSearchMatch> matches) {
    this.matches = matches;
  }

  public List<TextSearchMatch> getMatches() {
    return matches;
  }

  public void setMatches(List<TextSearchMatch> matches) {
    this.matches = matches;
  }

  @Override
  public int hashCode() {
    return Objects.hash(matches);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    FindTextInFilesResult other = (FindTextInFilesResult) obj;
    return Objects.equals(matches, other.matches);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("matches", matches);
    return builder.toString();
  }

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

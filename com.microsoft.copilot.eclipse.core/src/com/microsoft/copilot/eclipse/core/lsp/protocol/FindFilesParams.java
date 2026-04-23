// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Parameters for the {@code workspace/findFiles} request. Used by the language server to ask the client to search for
 * files matching a glob pattern under a given base URI (e.g. a semanticfs workspace folder).
 */
public class FindFilesParams {

  private String baseUri;
  private String pattern;
  private Integer maxResults;

  /**
   * Constructs a new FindFilesParams object.
   *
   * @param baseUri the base URI to search under (e.g. a semanticfs workspace folder)
   * @param pattern the glob pattern to match file paths against
   * @param maxResults the maximum number of results to return (optional)
   */
  public FindFilesParams(String baseUri, String pattern, Integer maxResults) {
    this.baseUri = baseUri;
    this.pattern = pattern;
    this.maxResults = maxResults;
  }

  public String getBaseUri() {
    return baseUri;
  }

  public void setBaseUri(String baseUri) {
    this.baseUri = baseUri;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public Integer getMaxResults() {
    return maxResults;
  }

  public void setMaxResults(Integer maxResults) {
    this.maxResults = maxResults;
  }

  @Override
  public int hashCode() {
    return Objects.hash(baseUri, pattern, maxResults);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    FindFilesParams other = (FindFilesParams) obj;
    return Objects.equals(baseUri, other.baseUri) && Objects.equals(pattern, other.pattern)
        && Objects.equals(maxResults, other.maxResults);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("baseUri", baseUri);
    builder.append("pattern", pattern);
    builder.append("maxResults", maxResults);
    return builder.toString();
  }

}

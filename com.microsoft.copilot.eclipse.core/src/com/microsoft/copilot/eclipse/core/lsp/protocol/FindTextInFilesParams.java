// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Parameters for the {@code workspace/findTextInFiles} request. Used by the language server to ask the client to search
 * for text (or a regex) in files under a given base URI.
 */
public class FindTextInFilesParams {

  private String baseUri;
  private String query;
  private Boolean isRegexp;
  private String includePattern;
  private Integer maxResults;

  /**
   * Constructs a new FindTextInFilesParams object.
   *
   * @param baseUri the base URI to search under (e.g. a semanticfs workspace folder)
   * @param query the text or regex pattern to search for in files
   * @param isRegexp whether the query is a regular expression
   * @param includePattern an optional glob pattern to filter which files to search
   * @param maxResults the maximum number of results to return (optional)
   */
  public FindTextInFilesParams(String baseUri, String query, Boolean isRegexp, String includePattern,
      Integer maxResults) {
    this.baseUri = baseUri;
    this.query = query;
    this.isRegexp = isRegexp;
    this.includePattern = includePattern;
    this.maxResults = maxResults;
  }

  public String getBaseUri() {
    return baseUri;
  }

  public void setBaseUri(String baseUri) {
    this.baseUri = baseUri;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public Boolean getIsRegexp() {
    return isRegexp;
  }

  public void setIsRegexp(Boolean isRegexp) {
    this.isRegexp = isRegexp;
  }

  public String getIncludePattern() {
    return includePattern;
  }

  public void setIncludePattern(String includePattern) {
    this.includePattern = includePattern;
  }

  public Integer getMaxResults() {
    return maxResults;
  }

  public void setMaxResults(Integer maxResults) {
    this.maxResults = maxResults;
  }

  @Override
  public int hashCode() {
    return Objects.hash(baseUri, query, isRegexp, includePattern, maxResults);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    FindTextInFilesParams other = (FindTextInFilesParams) obj;
    return Objects.equals(baseUri, other.baseUri) && Objects.equals(query, other.query)
        && Objects.equals(isRegexp, other.isRegexp) && Objects.equals(includePattern, other.includePattern)
        && Objects.equals(maxResults, other.maxResults);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("baseUri", baseUri);
    builder.append("query", query);
    builder.append("isRegexp", isRegexp);
    builder.append("includePattern", includePattern);
    builder.append("maxResults", maxResults);
    return builder.toString();
  }

}

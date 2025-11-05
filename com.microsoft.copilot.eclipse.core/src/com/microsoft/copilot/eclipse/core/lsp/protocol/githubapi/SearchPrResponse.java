package com.microsoft.copilot.eclipse.core.lsp.protocol.githubapi;

import java.util.List;
import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Response for searching pull requests.
 */
public class SearchPrResponse {
  private List<GitHubPullRequestItem> pullRequests;

  public List<GitHubPullRequestItem> getPullRequests() {
    return pullRequests;
  }

  public void setPullRequests(List<GitHubPullRequestItem> pullRequests) {
    this.pullRequests = pullRequests;
  }

  @Override
  public int hashCode() {
    return Objects.hash(pullRequests);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SearchPrResponse other = (SearchPrResponse) obj;
    return Objects.equals(pullRequests, other.pullRequests);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("pullRequests", pullRequests);
    return builder.toString();
  }
}

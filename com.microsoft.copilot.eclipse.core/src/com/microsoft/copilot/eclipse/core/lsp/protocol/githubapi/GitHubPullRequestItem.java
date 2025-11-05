package com.microsoft.copilot.eclipse.core.lsp.protocol.githubapi;

import com.google.gson.annotations.SerializedName;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Represents a GitHub Pull Request item with relevant details.
 */
//@formatter:off
public record GitHubPullRequestItem(
    String id,
    int number,
    String title,
    GitHubUser user,
    @SerializedName("html_url") String htmlUrl,
    Boolean draft,
    String body,
    GitHubRepository repository,
    CopilotWorkingStatus copilotWorkStatus
//@formatter:on
) {

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("id", id);
    builder.add("number", number);
    builder.add("title", title);
    builder.add("user", user);
    builder.add("htmlUrl", htmlUrl);
    builder.add("draft", draft);
    builder.add("body", body);
    builder.add("repository", repository);
    builder.add("copilotWorkStatus", copilotWorkStatus);
    return builder.toString();
  }
}

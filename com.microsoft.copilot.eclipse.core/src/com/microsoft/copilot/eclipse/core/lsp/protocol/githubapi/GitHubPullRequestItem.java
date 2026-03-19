package com.microsoft.copilot.eclipse.core.lsp.protocol.githubapi;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

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
    builder.append("id", id);
    builder.append("number", number);
    builder.append("title", title);
    builder.append("user", user);
    builder.append("htmlUrl", htmlUrl);
    builder.append("draft", draft);
    builder.append("body", body);
    builder.append("repository", repository);
    builder.append("copilotWorkStatus", copilotWorkStatus);
    return builder.toString();
  }
}

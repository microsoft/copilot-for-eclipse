package com.microsoft.copilot.eclipse.core.lsp.protocol.githubapi;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Represents a GitHub repository with its owner and name.
 */
public class GitHubRepository {
  /**
   * Repository owner/organization.
   */
  private GitHubUser owner;

  /**
   * Repository name.
   */
  private String name;

  public GitHubUser getOwner() {
    return owner;
  }

  public void setOwner(GitHubUser owner) {
    this.owner = owner;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, owner);
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
    GitHubRepository other = (GitHubRepository) obj;
    return Objects.equals(name, other.name) && Objects.equals(owner, other.owner);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("owner", owner);
    builder.add("name", name);
    return builder.toString();
  }

}

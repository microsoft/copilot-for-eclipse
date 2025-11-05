package com.microsoft.copilot.eclipse.core.lsp.protocol.githubapi;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Represents a GitHub user with basic information.
 */
public class GitHubUser {
  /**
   * User login/username.
   */
  private String login;

  public String getLogin() {
    return login;
  }

  public void setLogin(String login) {
    this.login = login;
  }

  @Override
  public int hashCode() {
    return Objects.hash(login);
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
    GitHubUser other = (GitHubUser) obj;
    return Objects.equals(login, other.login);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("login", login);
    return builder.toString();
  }

}

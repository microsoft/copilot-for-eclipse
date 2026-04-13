package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Agents are used to provide additional functionality to the conversation, e.g. resolving the whole project context.
 * Agents can be used by sending a message starting with @name
 */
public class ConversationAgent {

  private String slug;

  private String name;

  private String description;

  private String avatarUrl;

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  @Override
  public int hashCode() {
    return Objects.hash(avatarUrl, description, name, slug);
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
    ConversationAgent other = (ConversationAgent) obj;
    return Objects.equals(avatarUrl, other.avatarUrl) && Objects.equals(description, other.description)
        && Objects.equals(name, other.name) && Objects.equals(slug, other.slug);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("slug", slug);
    builder.append("name", name);
    builder.append("description", description);
    builder.append("avatarUrl", avatarUrl);
    return builder.toString();
  }
}

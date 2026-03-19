package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Get the templates.
 */
public class ConversationTemplate {


  private String id;
  private String description;
  private String shortDescription;
  private List<String> scopes;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  public List<String> getScopes() {
    return scopes;
  }

  public void setScopes(List<String> scopes) {
    this.scopes = scopes;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, description, shortDescription, scopes);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ConversationTemplate that = (ConversationTemplate) obj;
    return Objects.equals(id, that.id) && Objects.equals(description, that.description)
        && Objects.equals(shortDescription, that.shortDescription) && Objects.equals(scopes, that.scopes);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("id", id);
    builder.append("description", description);
    builder.append("shortDescription", shortDescription);
    builder.append("scopes", scopes);
    return builder.toString();
  }
}

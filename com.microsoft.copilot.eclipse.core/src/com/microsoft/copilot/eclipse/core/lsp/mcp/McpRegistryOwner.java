// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents an owner in the MCP Registry system. Contains information about the owner of an MCP registry entry,
 * including organization details and hierarchical parent relationships for enterprise structures.
 */
public class McpRegistryOwner {
  public String login;
  public int id;

  /**
   * The type of owner organization. Typically "Business" for Enterprise accounts or "Organization" for regular
   * organizations.
   */
  public String type;

  /**
   * The login of the parent organization, if this is a sub-organization. This field is null for top-level organizations
   * and contains the parent organization's login for nested organizational structures.
   */
  @SerializedName("parent_login")
  public String parentLogin;

  /**
   * The ID of the parent organization, if this is a sub-organization.
   * This field is null for top-level organizations and contains the parent organization's ID for nested organizational
   * structures.
   */
  @SerializedName("parent_id")
  public int parentId;

  public String getLogin() {
    return login;
  }

  public void setLogin(String login) {
    this.login = login;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getParentLogin() {
    return parentLogin;
  }

  public void setParentLogin(String parentLogin) {
    this.parentLogin = parentLogin;
  }

  public int getParentId() {
    return parentId;
  }

  public void setParentId(int parentId) {
    this.parentId = parentId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, login, parentId, parentLogin, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof McpRegistryOwner)) {
      return false;
    }
    McpRegistryOwner other = (McpRegistryOwner) obj;
    return Objects.equals(id, other.id) && Objects.equals(login, other.login)
        && Objects.equals(parentId, other.parentId) && Objects.equals(parentLogin, other.parentLogin)
        && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("login", login);
    builder.append("id", id);
    builder.append("type", type);
    builder.append("parentLogin", parentLogin);
    builder.append("parentId", parentId);
    return builder.toString();
  }

}

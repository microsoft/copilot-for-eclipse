// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents a conversation mode from the LSP.
 */
public class ConversationMode {
  private String id;
  private String name;
  private String kind;
  private boolean isBuiltIn;
  private String uri;
  private String description;
  private List<String> customTools;
  private String model;
  private List<HandOff> handOffs;

  /**
   * Creates a new ConversationMode.
   */
  public ConversationMode() {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public boolean isBuiltIn() {
    return isBuiltIn;
  }

  public void setBuiltIn(boolean isBuiltIn) {
    this.isBuiltIn = isBuiltIn;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<String> getCustomTools() {
    return customTools;
  }

  public void setCustomTools(List<String> customTools) {
    this.customTools = customTools;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public List<HandOff> getHandOffs() {
    return handOffs;
  }

  public void setHandOffs(List<HandOff> handOffs) {
    this.handOffs = handOffs;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, kind, isBuiltIn, uri, description, customTools, model, handOffs);
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
    ConversationMode other = (ConversationMode) obj;
    return Objects.equals(id, other.id) && Objects.equals(name, other.name) && Objects.equals(kind, other.kind)
        && isBuiltIn == other.isBuiltIn && Objects.equals(uri, other.uri)
        && Objects.equals(description, other.description) && Objects.equals(customTools, other.customTools)
        && Objects.equals(model, other.model) && Objects.equals(handOffs, other.handOffs);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("id", id);
    builder.append("name", name);
    builder.append("kind", kind);
    builder.append("isBuiltIn", isBuiltIn);
    builder.append("uri", uri);
    builder.append("description", description);
    builder.append("customTools", customTools);
    builder.append("model", model);
    builder.append("handOffs", handOffs);
    return builder.toString();
  }

  /**
   * Represents a hand-off configuration.
   */
  public static class HandOff {
    private String agent;
    private String label;
    private String prompt;
    private Boolean send;

    /**
     * Creates a new HandOff.
     */
    public HandOff() {
    }

    public String getAgent() {
      return agent;
    }

    public void setAgent(String agent) {
      this.agent = agent;
    }

    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }

    public String getPrompt() {
      return prompt;
    }

    public void setPrompt(String prompt) {
      this.prompt = prompt;
    }

    public Boolean getSend() {
      return send;
    }

    public void setSend(Boolean send) {
      this.send = send;
    }

    @Override
    public int hashCode() {
      return Objects.hash(agent, label, prompt, send);
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
      HandOff other = (HandOff) obj;
      return Objects.equals(agent, other.agent) && Objects.equals(label, other.label)
          && Objects.equals(prompt, other.prompt) && Objects.equals(send, other.send);
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("agent", agent);
      builder.append("label", label);
      builder.append("prompt", prompt);
      builder.append("send", send);
      return builder.toString();
    }
  }
}

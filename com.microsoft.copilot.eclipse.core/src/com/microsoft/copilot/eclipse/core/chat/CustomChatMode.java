package com.microsoft.copilot.eclipse.core.chat;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a custom chat mode created by the user.
 */
public class CustomChatMode {
  private String id;
  private String displayName;
  private String description;
  private List<String> tools;
  private String model;

  /**
   * Constructor for CustomChatMode.
   *
   * @param id the unique identifier
   * @param displayName the display name
   * @param description the description
   */
  public CustomChatMode(String id, String displayName, String description) {
    this.id = id;
    this.displayName = displayName;
    this.description = description;
    this.tools = new ArrayList<>();
  }

  /**
   * Constructor for CustomChatMode with tools.
   *
   * @param id the unique identifier
   * @param displayName the display name
   * @param description the description
   * @param tools the list of tools
   */
  public CustomChatMode(String id, String displayName, String description, List<String> tools) {
    this.id = id;
    this.displayName = displayName;
    this.description = description;
    this.tools = tools != null ? new ArrayList<>(tools) : new ArrayList<>();
    this.model = null;
  }

  /**
   * Constructor for CustomChatMode with tools and model.
   *
   * @param id the unique identifier
   * @param displayName the display name
   * @param description the description
   * @param tools the list of tools
   * @param model the model to use
   */
  public CustomChatMode(String id, String displayName, String description, List<String> tools, String model) {
    this.id = id;
    this.displayName = displayName;
    this.description = description;
    this.tools = tools != null ? new ArrayList<>(tools) : new ArrayList<>();
    this.model = model;
  }

  /**
   * Get the unique identifier.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Set the unique identifier.
   *
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Get the display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Set the display name.
   *
   * @param displayName the display name to set
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Get the description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Set the description.
   *
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Get the tools list.
   *
   * @return the tools list
   */
  public List<String> getTools() {
    return tools != null ? new ArrayList<>(tools) : new ArrayList<>();
  }

  /**
   * Set the tools list.
   *
   * @param tools the tools list to set
   */
  public void setTools(List<String> tools) {
    this.tools = tools != null ? new ArrayList<>(tools) : new ArrayList<>();
  }

  /**
   * Get the model.
   *
   * @return the model
   */
  public String getModel() {
    return model;
  }

  /**
   * Set the model.
   *
   * @param model the model to set
   */
  public void setModel(String model) {
    this.model = model;
  }
}

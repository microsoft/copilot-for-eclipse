package com.microsoft.copilot.eclipse.core.chat;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationMode;

/**
 * Base class for all chat modes (built-in and custom).
 * Provides common functionality and properties shared by both mode types.
 */
public abstract class BaseChatMode {
  protected String id;
  protected String displayName;
  protected String description;
  protected List<String> tools;
  protected String model;
  protected List<ConversationMode.HandOff> handOffs;

  /**
   * Simple constructor with basic properties.
   *
   * @param id the unique identifier
   * @param displayName the display name
   * @param description the description
   */
  protected BaseChatMode(String id, String displayName, String description) {
    this.id = id;
    this.displayName = displayName;
    this.description = description;
    this.tools = new ArrayList<>();
    this.handOffs = new ArrayList<>();
  }

  /**
   * Full constructor with all properties.
   *
   * @param id the unique identifier
   * @param displayName the display name
   * @param description the description
   * @param tools the list of tools
   * @param model the model to use
   * @param handOffs the list of hand-off configurations
   */
  protected BaseChatMode(String id, String displayName, String description,
      List<String> tools, String model, List<ConversationMode.HandOff> handOffs) {
    this.id = id;
    this.displayName = displayName;
    this.description = description;
    this.tools = tools != null ? new ArrayList<>(tools) : new ArrayList<>();
    this.model = model;
    this.handOffs = handOffs != null ? new ArrayList<>(handOffs) : new ArrayList<>();
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
   * Get the tools list (defensive copy).
   *
   * @return a copy of the tools list
   */
  public List<String> getTools() {
    return tools != null ? new ArrayList<>(tools) : new ArrayList<>();
  }

  /**
   * Set the tools list (defensive copy).
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

  /**
   * Get the hand-off configurations (defensive copy).
   *
   * @return a copy of the hand-offs list
   */
  public List<ConversationMode.HandOff> getHandOffs() {
    return handOffs != null ? new ArrayList<>(handOffs) : new ArrayList<>();
  }

  /**
   * Set the hand-off configurations (defensive copy).
   *
   * @param handOffs the hand-offs list to set
   */
  public void setHandOffs(List<ConversationMode.HandOff> handOffs) {
    this.handOffs = handOffs != null ? new ArrayList<>(handOffs) : new ArrayList<>();
  }

  /**
   * Determines whether this mode allows tool configuration.
   * Subclasses must implement this to define mode-specific behavior.
   *
   * @return true if tool configuration is allowed, false otherwise
   */
  public abstract boolean allowsToolConfiguration();

  /**
   * Determines whether this is a built-in mode.
   * Subclasses must implement this to identify their type.
   *
   * @return true if this is a built-in mode, false if custom
   */
  public abstract boolean isBuiltIn();
}
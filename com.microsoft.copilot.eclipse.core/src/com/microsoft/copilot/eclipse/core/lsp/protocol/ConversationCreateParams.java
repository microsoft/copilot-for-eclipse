package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Parameters for creating a conversation.
 */
public class ConversationCreateParams {
  String workDoneToken;
  Turn[] turns;
  Capabilities capabilities;
  boolean computeSuggestions;
  ArrayList<FileReferenceParams> references;
  String source = "panel";
  String workspaceFolder;
  String[] ignoredSkills;
  String userLanguage;
  String model;
  String chatMode;

  /**
   * Capabilities for the conversation.
   */
  public class Capabilities {
    boolean allSkills;
    String[] skills;

    /**
     * Creates a new Capabilities.
     */
    public Capabilities() {
      this.allSkills = true;
      this.skills = new String[0];
    }

    public boolean isAllSkills() {
      return allSkills;
    }

    public String[] getSkills() {
      return skills;
    }

    public void setAllSkills(boolean allSkills) {
      this.allSkills = allSkills;
    }

    public void setSkills(String[] skills) {
      this.skills = skills;
    }

    @Override
    public int hashCode() {
      return Objects.hash(allSkills, Arrays.hashCode(skills));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Capabilities that = (Capabilities) o;
      return allSkills == that.allSkills && Arrays.equals(skills, that.skills);
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("allSkills", allSkills);
      builder.add("skills", Arrays.toString(skills));
      return builder.toString();
    }
  }

  /**
   * Creates a new ConversationCreateParams.
   */
  public ConversationCreateParams(String prompt, String workDoneToken) {
    this.workDoneToken = workDoneToken;
    this.turns = new Turn[] { new Turn(prompt, null, null) };
    this.capabilities = new Capabilities();
    this.computeSuggestions = true;
    this.references = new ArrayList<>();
    this.ignoredSkills = new String[0];
    this.userLanguage = "en";
  }

  public String getWorkDoneToken() {
    return workDoneToken;
  }

  public Turn[] getTurns() {
    return turns;
  }

  public Capabilities getCapabilities() {
    return capabilities;
  }

  public boolean isComputeSuggestions() {
    return computeSuggestions;
  }

  public String getSource() {
    return source;
  }

  public String getWorkspaceFolder() {
    return workspaceFolder;
  }

  public String[] getIgnoredSkills() {
    return ignoredSkills;
  }

  public String getUserLanguage() {
    return userLanguage;
  }

  public String getModel() {
    return model;
  }

  public void setWorkDoneToken(String workDoneToken) {
    this.workDoneToken = workDoneToken;
  }

  public void setTurns(Turn[] turns) {
    this.turns = turns;
  }

  public void setCapabilities(Capabilities capabilities) {
    this.capabilities = capabilities;
  }

  public void setComputeSuggestions(boolean computeSuggestions) {
    this.computeSuggestions = computeSuggestions;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public void setWorkspaceFolder(String workspaceFolder) {
    this.workspaceFolder = workspaceFolder;
  }

  public void setIgnoredSkills(String[] ignoredSkills) {
    this.ignoredSkills = ignoredSkills;
  }

  public void setUserLanguage(String userLanguage) {
    this.userLanguage = userLanguage;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getChatMode() {
    return chatMode;
  }

  public void setChatMode(String chatMode) {
    this.chatMode = chatMode;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(ignoredSkills);
    result = prime * result + Arrays.hashCode(turns);
    result = prime * result + Objects.hash(capabilities, chatMode, computeSuggestions, model, references, source,
        userLanguage, workDoneToken, workspaceFolder);
    return result;
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
    ConversationCreateParams other = (ConversationCreateParams) obj;
    return Objects.equals(capabilities, other.capabilities) && chatMode == other.chatMode
        && computeSuggestions == other.computeSuggestions && Arrays.equals(ignoredSkills, other.ignoredSkills)
        && Objects.equals(model, other.model) && Objects.equals(references, other.references)
        && Objects.equals(source, other.source) && Arrays.equals(turns, other.turns)
        && Objects.equals(userLanguage, other.userLanguage) && Objects.equals(workDoneToken, other.workDoneToken)
        && Objects.equals(workspaceFolder, other.workspaceFolder);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("workDoneToken", workDoneToken);
    builder.add("turns", Arrays.toString(turns));
    builder.add("capabilities", capabilities);
    builder.add("computeSuggestions", computeSuggestions);
    builder.add("references", references);
    builder.add("source", source);
    builder.add("workspaceFolder", workspaceFolder);
    builder.add("ignoredSkills", Arrays.toString(ignoredSkills));
    builder.add("userLanguage", userLanguage);
    builder.add("model", model);
    builder.add("chatMode", chatMode);
    return builder.toString();
  }

  /**
   * Adds file references to the conversation.
   */
  public void addFileRefs(List<IFile> files) {
    for (IFile file : files) {
      this.references.add(new FileReferenceParams(file));
    }
  }
}

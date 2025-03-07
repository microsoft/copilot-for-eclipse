package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.ArrayList;
import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Model for the Copilot model. ref:
 * https://github.com/microsoft/copilot-client/blob/main/agent/src/methods/copilotModels.ts#L29
 */
public class CopilotModel {
  private String modelFamily;
  private String modelName;
  private String id;
  private CopilotModelPolicy modelPolicy;
  private ArrayList<String> scopes;
  private boolean preview;

  /**
   * Policy for the model.
   */
  public class CopilotModelPolicy {
    private String state;
    private String terms;

    public String getState() {
      return state;
    }

    public void setState(String state) {
      this.state = state;
    }

    public String getTerms() {
      return terms;
    }

    public void setTerms(String terms) {
      this.terms = terms;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CopilotModelPolicy that = (CopilotModelPolicy) o;
      return Objects.equals(state, that.state) && Objects.equals(terms, that.terms);
    }

    @Override
    public int hashCode() {
      return Objects.hash(state, terms);
    }

    @Override
    public String toString() {
      return "CopilotModelPolicy{" + "state='" + state + '\'' + ", terms='" + terms + '\'' + '}';
    }
  }

  public String getModelFamily() {
    return modelFamily;
  }

  public void setModelFamily(String modelFamily) {
    this.modelFamily = modelFamily;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public CopilotModelPolicy getModelPolicy() {
    return modelPolicy;
  }

  public void setModelPolicy(CopilotModelPolicy modelPolicy) {
    this.modelPolicy = modelPolicy;
  }

  public ArrayList<String> getScopes() {
    return scopes;
  }

  public void setScopes(ArrayList<String> scopes) {
    this.scopes = scopes;
  }

  public boolean isPreview() {
    return preview;
  }

  public void setPreview(boolean preview) {
    this.preview = preview;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CopilotModel that = (CopilotModel) o;
    return preview == that.preview && Objects.equals(modelFamily, that.modelFamily)
        && Objects.equals(modelName, that.modelName) && Objects.equals(id, that.id)
        && Objects.equals(modelPolicy, that.modelPolicy) && Objects.equals(scopes, that.scopes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(modelFamily, modelName, id, modelPolicy, scopes, preview);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("modelFamily", modelFamily);
    builder.add("modelName", modelName);
    builder.add("id", id);
    builder.add("modelPolicy", modelPolicy);
    builder.add("scopes", scopes);
    builder.add("preview", preview);
    return builder.toString();
  }
}
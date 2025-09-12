package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
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
  private List<String> scopes;
  private boolean preview;
  private boolean isChatDefault;
  private boolean isChatFallback;
  private CopilotModelCapabilities capabilities;
  private CopilotModelBilling billing;
  private String providerName;

  /**
   * Policy for the model.
   */
  public record CopilotModelPolicy(String state, String terms) {
    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("state", state);
      builder.add("terms", terms);
      return builder.toString();
    }
  }

  /**
   * Capabilities supports for the model.
   */
  public record CopilotModelCapabilitiesSupports(boolean vision) {
    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("vision", vision);
      return builder.toString();
    }
  }

  /**
   * Capabilities for the model.
   */
  public record CopilotModelCapabilities(CopilotModelCapabilitiesSupports supports) {
    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("supports", supports);
      return builder.toString();
    }
  }

  /**
   * Billing for the model.
   */
  public record CopilotModelBilling(boolean isPremium, double multiplier) {
    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("isPremium", isPremium);
      builder.add("multiplier", multiplier);
      return builder.toString();
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

  public List<String> getScopes() {
    return scopes;
  }

  public void setScopes(List<String> scopes) {
    this.scopes = scopes;
  }

  public boolean isPreview() {
    return preview;
  }

  public void setPreview(boolean preview) {
    this.preview = preview;
  }

  public boolean isChatDefault() {
    return isChatDefault;
  }

  public void setChatDefault(boolean isChatDefault) {
    this.isChatDefault = isChatDefault;
  }

  public boolean isChatFallback() {
    return isChatFallback;
  }

  public void setChatFallback(boolean isChatFallback) {
    this.isChatFallback = isChatFallback;
  }

  public CopilotModelCapabilities getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(CopilotModelCapabilities capabilities) {
    this.capabilities = capabilities;
  }

  public CopilotModelBilling getBilling() {
    return billing;
  }

  public void setBilling(CopilotModelBilling billing) {
    this.billing = billing;
  }

  public String getProviderName() {
    return providerName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
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
    CopilotModel other = (CopilotModel) obj;
    return Objects.equals(billing, other.billing) && Objects.equals(capabilities, other.capabilities)
        && Objects.equals(id, other.id) && isChatDefault == other.isChatDefault
        && isChatFallback == other.isChatFallback && Objects.equals(modelFamily, other.modelFamily)
        && Objects.equals(modelName, other.modelName) && Objects.equals(modelPolicy, other.modelPolicy)
        && preview == other.preview && Objects.equals(scopes, other.scopes)
        && Objects.equals(providerName, other.providerName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(billing, capabilities, id, isChatDefault, isChatFallback, modelFamily, modelName, modelPolicy,
        preview, scopes, providerName);
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
    builder.add("isChatDefault", isChatDefault);
    builder.add("isChatFallback", isChatFallback);
    builder.add("capabilities", capabilities);
    builder.add("billing", billing);
    builder.add("providerName", providerName);
    return builder.toString();
  }
}

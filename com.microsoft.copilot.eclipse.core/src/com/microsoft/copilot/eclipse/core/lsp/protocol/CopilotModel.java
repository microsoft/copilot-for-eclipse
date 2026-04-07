package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Model for the Copilot model. ref:
 * https://github.com/microsoft/copilot-client/blob/main/agent/src/methods/copilotModels.ts#L29
 */
public class CopilotModel {
  private String modelFamily;
  private String modelName;
  private String id;
  private String vendor;
  private CopilotModelPolicy modelPolicy;
  private List<String> scopes;
  private boolean preview;
  private boolean isChatDefault;
  private boolean isChatFallback;
  private CopilotModelCapabilities capabilities;
  private CopilotModelBilling billing;
  private String degradationReason;
  private String providerName;

  /**
   * Policy for the model.
   */
  public record CopilotModelPolicy(String state, String terms) {
    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("state", state);
      builder.append("terms", terms);
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
      builder.append("vision", vision);
      return builder.toString();
    }
  }

  /**
   * Token limits for the model capabilities.
   */
  public record CopilotModelCapabilitiesLimits(int maxInputTokens, int maxOutputTokens,
      int maxContextWindowTokens) {
    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("maxInputTokens", maxInputTokens);
      builder.append("maxOutputTokens", maxOutputTokens);
      builder.append("maxContextWindowTokens", maxContextWindowTokens);
      return builder.toString();
    }
  }

  /**
   * Capabilities for the model.
   */
  public record CopilotModelCapabilities(CopilotModelCapabilitiesSupports supports,
      CopilotModelCapabilitiesLimits limits) {
    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("supports", supports);
      builder.append("limits", limits);
      return builder.toString();
    }
  }

  /**
   * Token prices for billing.
   */
  public record CopilotModelBillingTokenPrices(double cachePrice, double inputPrice, double outputPrice,
      int tokenUnit) {
    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("cachePrice", cachePrice);
      builder.append("inputPrice", inputPrice);
      builder.append("outputPrice", outputPrice);
      builder.append("tokenUnit", tokenUnit);
      return builder.toString();
    }
  }

  /**
   * Billing for the model.
   */
  public record CopilotModelBilling(boolean isPremium, double multiplier,
      CopilotModelBillingTokenPrices tokenPrices) {
    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("isPremium", isPremium);
      builder.append("multiplier", multiplier);
      builder.append("tokenPrices", tokenPrices);
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

  public String getVendor() {
    return vendor;
  }

  public void setVendor(String vendor) {
    this.vendor = vendor;
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

  public String getDegradationReason() {
    return degradationReason;
  }

  public void setDegradationReason(String degradationReason) {
    this.degradationReason = degradationReason;
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
        && Objects.equals(degradationReason, other.degradationReason) && Objects.equals(id, other.id)
        && isChatDefault == other.isChatDefault && isChatFallback == other.isChatFallback
        && Objects.equals(modelFamily, other.modelFamily) && Objects.equals(modelName, other.modelName)
        && Objects.equals(modelPolicy, other.modelPolicy) && preview == other.preview
        && Objects.equals(providerName, other.providerName) && Objects.equals(scopes, other.scopes)
        && Objects.equals(vendor, other.vendor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(billing, capabilities, degradationReason, id, isChatDefault, isChatFallback, modelFamily,
        modelName, modelPolicy, preview, providerName, scopes, vendor);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("modelFamily", modelFamily);
    builder.append("modelName", modelName);
    builder.append("id", id);
    builder.append("vendor", vendor);
    builder.append("modelPolicy", modelPolicy);
    builder.append("scopes", scopes);
    builder.append("preview", preview);
    builder.append("isChatDefault", isChatDefault);
    builder.append("isChatFallback", isChatFallback);
    builder.append("capabilities", capabilities);
    builder.append("billing", billing);
    builder.append("degradationReason", degradationReason);
    builder.append("providerName", providerName);
    return builder.toString();
  }

}

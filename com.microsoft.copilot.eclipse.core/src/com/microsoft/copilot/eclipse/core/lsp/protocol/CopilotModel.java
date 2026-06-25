// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
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
  private String modelPickerCategory;
  private String modelPickerPriceCategory;

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
   *
   * @param vision whether the model supports vision input
   * @param reasoningEfforts the list of reasoning effort levels advertised by the model (e.g. {@code low}, {@code
   *     medium}, {@code high}); may be {@code null} or empty when the model does not expose this list
   * @param supportsReasoningEffortLevel whether the model surfaces selectable reasoning effort levels to the user
   *     (the language server only reports {@code true} when the model has more than one effort level and is hosted on
   *     a compatible endpoint)
   */
  public record CopilotModelCapabilitiesSupports(boolean vision, List<String> reasoningEfforts,
      boolean supportsReasoningEffortLevel) {

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("vision", vision);
      builder.append("reasoningEfforts", reasoningEfforts);
      builder.append("supportsReasoningEffortLevel", supportsReasoningEffortLevel);
      return builder.toString();
    }
  }

  /**
   * Capabilities limits for the model. All components are optional ({@code null} when the server does not provide a
   * value), mirroring the {@code number | undefined} fields in the language-server schema.
   */
  public record CopilotModelCapabilitiesLimits(Integer maxContextWindowTokens, Integer maxOutputTokens,
      Integer maxInputTokens, Integer maxNonStreamingOutputTokens) {
    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("maxContextWindowTokens", maxContextWindowTokens);
      builder.append("maxOutputTokens", maxOutputTokens);
      builder.append("maxInputTokens", maxInputTokens);
      builder.append("maxNonStreamingOutputTokens", maxNonStreamingOutputTokens);
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
   * Per-tier token prices, quoted in USD per {@link CopilotModelBillingTokenPrices#batchSize} tokens and applying up
   * to {@code maxContext} context tokens. Requests larger than the {@code default} tier's {@code maxContext} are
   * billed at the {@code longContext} tier. All components are optional ({@code null} when the server does not provide
   * a value).
   *
   * @param cachePrice the price for cached input tokens
   * @param inputPrice the price for input tokens
   * @param outputPrice the price for output tokens
   * @param maxContext the maximum number of context (input) tokens this tier applies to
   */
  public record CopilotModelTokenPriceTier(Double cachePrice, Double inputPrice, Double outputPrice,
      Integer maxContext) {
    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("cachePrice", cachePrice);
      builder.append("inputPrice", inputPrice);
      builder.append("outputPrice", outputPrice);
      builder.append("maxContext", maxContext);
      return builder.toString();
    }
  }

  /**
   * Per-tier token prices for the model. When token-based billing is enabled the server returns a {@code default}
   * tier and, for models that support long context, a {@code longContext} tier.
   *
   * @param batchSize the number of tokens each tier price is quoted per
   * @param defaultTier the {@code default} price tier (deserialized from the {@code default} JSON field)
   * @param longContext the {@code longContext} price tier, or {@code null} when the model does not support long
   *     context
   */
  public record CopilotModelBillingTokenPrices(Double batchSize,
      @SerializedName("default") CopilotModelTokenPriceTier defaultTier, CopilotModelTokenPriceTier longContext) {
    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("batchSize", batchSize);
      builder.append("defaultTier", defaultTier);
      builder.append("longContext", longContext);
      return builder.toString();
    }
  }

  /**
   * Billing for the model.
   */
  public record CopilotModelBilling(boolean isPremium, double multiplier, boolean tokenBasedBillingEnabled,
      CopilotModelBillingTokenPrices tokenPrices) {
    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("isPremium", isPremium);
      builder.append("multiplier", multiplier);
      builder.append("tokenBasedBillingEnabled", tokenBasedBillingEnabled);
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

  public String getModelPickerCategory() {
    return modelPickerCategory;
  }

  public void setModelPickerCategory(String modelPickerCategory) {
    this.modelPickerCategory = modelPickerCategory;
  }

  public String getModelPickerPriceCategory() {
    return modelPickerPriceCategory;
  }

  public void setModelPickerPriceCategory(String modelPickerPriceCategory) {
    this.modelPickerPriceCategory = modelPickerPriceCategory;
  }

  /**
   * Builds the composite key used to identify this model in maps and user preferences.
   *
   * @return the composite key
   */
  public String getModelKey() {
    return providerName != null ? providerName + "_" + id : id;
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
        && Objects.equals(modelPickerCategory, other.modelPickerCategory)
        && Objects.equals(modelPickerPriceCategory, other.modelPickerPriceCategory)
        && Objects.equals(modelPolicy, other.modelPolicy) && preview == other.preview
        && Objects.equals(providerName, other.providerName) && Objects.equals(scopes, other.scopes)
        && Objects.equals(vendor, other.vendor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(billing, capabilities, degradationReason, id, isChatDefault, isChatFallback, modelFamily,
        modelName, modelPickerCategory, modelPickerPriceCategory, modelPolicy, preview, providerName, scopes, vendor);
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
    builder.append("modelPickerCategory", modelPickerCategory);
    builder.append("modelPickerPriceCategory", modelPickerPriceCategory);
    return builder.toString();
  }

}

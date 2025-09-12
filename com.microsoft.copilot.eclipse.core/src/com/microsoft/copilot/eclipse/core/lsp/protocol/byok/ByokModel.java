package com.microsoft.copilot.eclipse.core.lsp.protocol.byok;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Model representing a BYOK (Bring Your Own Key) model configuration.
 */
public class ByokModel {

  private String modelId;
  private String providerName;
  private boolean isRegistered;
  private boolean isCustomModel;
  private String deploymentUrl;
  private String apiKey;
  private ByokModelCapabilities modelCapabilities;

  public String getModelId() {
    return modelId;
  }

  public void setModelId(String modelId) {
    this.modelId = modelId;
  }

  public String getProviderName() {
    return providerName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public boolean isRegistered() {
    return isRegistered;
  }

  public void setRegistered(boolean isRegistered) {
    this.isRegistered = isRegistered;
  }

  public boolean isCustomModel() {
    return isCustomModel;
  }

  public void setCustomModel(boolean isCustomModel) {
    this.isCustomModel = isCustomModel;
  }

  public String getDeploymentUrl() {
    return deploymentUrl;
  }

  public void setDeploymentUrl(String deploymentUrl) {
    this.deploymentUrl = deploymentUrl;
  }

  public ByokModelCapabilities getModelCapabilities() {
    return modelCapabilities;
  }

  public void setModelCapabilities(ByokModelCapabilities modelCapabilities) {
    this.modelCapabilities = modelCapabilities;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getModelKey() {
    return providerName + "_" + modelId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(modelId, providerName, isRegistered, isCustomModel, deploymentUrl, modelCapabilities, apiKey);
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
    ByokModel other = (ByokModel) obj;
    return Objects.equals(modelId, other.modelId) && Objects.equals(providerName, other.providerName)
        && isRegistered == other.isRegistered && isCustomModel == other.isCustomModel
        && Objects.equals(deploymentUrl, other.deploymentUrl)
        && Objects.equals(modelCapabilities, other.modelCapabilities) && Objects.equals(apiKey, other.apiKey);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("modelId", modelId);
    builder.add("providerName", providerName);
    builder.add("isRegistered", isRegistered);
    builder.add("isCustomModel", isCustomModel);
    builder.add("deploymentUrl", deploymentUrl);
    builder.add("modelCapabilities", modelCapabilities);
    builder.add("apiKey", apiKey);
    return builder.toString();
  }
}

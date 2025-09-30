package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * An MCP package definition.
 */
public class Package {
  @SerializedName("registry_type")
  public String registryType;

  @SerializedName("registry_base_url")
  public String registryBaseUrl;

  public String identifier;
  public String version;

  @SerializedName("file_sha256")
  public String fileSha256;

  @SerializedName("runtime_hint")
  public String runtimeHint;

  @SerializedName("runtime_arguments")
  public List<Argument> runtimeArguments;

  @SerializedName("package_arguments")
  public List<Argument> packageArguments;

  @SerializedName("environment_variables")
  public List<KeyValueInput> environmentVariables;

  public String getRegistryType() {
    return registryType;
  }

  public void setRegistryType(String registryType) {
    this.registryType = registryType;
  }

  public String getRegistryBaseUrl() {
    return registryBaseUrl;
  }

  public void setRegistryBaseUrl(String registryBaseUrl) {
    this.registryBaseUrl = registryBaseUrl;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getFileSha256() {
    return fileSha256;
  }

  public void setFileSha256(String fileSha256) {
    this.fileSha256 = fileSha256;
  }

  public String getRuntimeHint() {
    return runtimeHint;
  }

  public void setRuntimeHint(String runtimeHint) {
    this.runtimeHint = runtimeHint;
  }

  public List<Argument> getRuntimeArguments() {
    return runtimeArguments;
  }

  public void setRuntimeArguments(List<Argument> runtimeArguments) {
    this.runtimeArguments = runtimeArguments;
  }

  public List<Argument> getPackageArguments() {
    return packageArguments;
  }

  public void setPackageArguments(List<Argument> packageArguments) {
    this.packageArguments = packageArguments;
  }

  public List<KeyValueInput> getEnvironmentVariables() {
    return environmentVariables;
  }

  public void setEnvironmentVariables(List<KeyValueInput> environmentVariables) {
    this.environmentVariables = environmentVariables;
  }

  @Override
  public int hashCode() {
    return Objects.hash(environmentVariables, fileSha256, identifier, packageArguments, registryBaseUrl, registryType,
        runtimeArguments, runtimeHint, version);
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
    Package other = (Package) obj;
    return Objects.equals(environmentVariables, other.environmentVariables)
        && Objects.equals(fileSha256, other.fileSha256) && Objects.equals(identifier, other.identifier)
        && Objects.equals(packageArguments, other.packageArguments)
        && Objects.equals(registryBaseUrl, other.registryBaseUrl) && Objects.equals(registryType, other.registryType)
        && Objects.equals(runtimeArguments, other.runtimeArguments) && Objects.equals(runtimeHint, other.runtimeHint)
        && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("registryType", registryType);
    builder.append("registryBaseUrl", registryBaseUrl);
    builder.append("identifier", identifier);
    builder.append("version", version);
    builder.append("fileSha256", fileSha256);
    builder.append("runtimeHint", runtimeHint);
    builder.append("runtimeArguments", runtimeArguments);
    builder.append("packageArguments", packageArguments);
    builder.append("environmentVariables", environmentVariables);
    return builder.toString();
  }

}

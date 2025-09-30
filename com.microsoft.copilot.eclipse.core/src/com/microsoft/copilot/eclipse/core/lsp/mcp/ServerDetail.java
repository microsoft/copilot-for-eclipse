package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Detailed information about a server from the MCP Registry.
 */
public class ServerDetail {

  private String name;
  private String description;
  private ServerStatus status;
  private Repository repository;
  private String version;

  @SerializedName("website_url")
  private String websiteUrl;

  @SerializedName("created_at")
  private String createdAt;

  @SerializedName("updated_at")
  private String updatedAt;

  @SerializedName("$schema")
  private String schema;
  private List<Package> packages;
  private List<Remote> remotes;

  @SerializedName("_meta")
  private ServerMeta meta;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ServerStatus getStatus() {
    return status;
  }

  public void setStatus(ServerStatus status) {
    this.status = status;
  }

  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getWebsiteUrl() {
    return websiteUrl;
  }

  public void setWebsiteUrl(String websiteUrl) {
    this.websiteUrl = websiteUrl;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public List<Package> getPackages() {
    return packages;
  }

  public void setPackages(List<Package> packages) {
    this.packages = packages;
  }

  public List<Remote> getRemotes() {
    return remotes;
  }

  public void setRemotes(List<Remote> remotes) {
    this.remotes = remotes;
  }

  public ServerMeta getMeta() {
    return meta;
  }

  public void setMeta(ServerMeta meta) {
    this.meta = meta;
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdAt, description, meta, name, packages, remotes, repository, schema, status, updatedAt,
        version, websiteUrl);
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
    ServerDetail other = (ServerDetail) obj;
    return Objects.equals(createdAt, other.createdAt) && Objects.equals(description, other.description)
        && Objects.equals(meta, other.meta) && Objects.equals(name, other.name)
        && Objects.equals(packages, other.packages) && Objects.equals(remotes, other.remotes)
        && Objects.equals(repository, other.repository) && Objects.equals(schema, other.schema)
        && status == other.status && Objects.equals(updatedAt, other.updatedAt)
        && Objects.equals(version, other.version) && Objects.equals(websiteUrl, other.websiteUrl);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("name", name);
    builder.append("description", description);
    builder.append("status", status);
    builder.append("repository", repository);
    builder.append("version", version);
    builder.append("websiteUrl", websiteUrl);
    builder.append("createdAt", createdAt);
    builder.append("updatedAt", updatedAt);
    builder.append("schema", schema);
    builder.append("packages", packages);
    builder.append("remotes", remotes);
    builder.append("meta", meta);
    return builder.toString();
  }

  /**
   * Enum for server status.
   */
  public static enum ServerStatus {
    active, deprecated
  }

  /**
   * Metadata about the server.
   */
  public static class ServerMeta {

    @SerializedName("io.modelcontextprotocol.registry/publisher-provided")
    private PublisherProvidedMeta publisherProvided;

    @SerializedName("io.modelcontextprotocol.registry/official")
    private OfficialMeta official;

    /**
     * Default constructor for JSON deserialization.
     */
    public ServerMeta() {
    }

    public PublisherProvidedMeta getPublisherProvided() {
      return publisherProvided;
    }

    public void setPublisherProvided(PublisherProvidedMeta publisherProvided) {
      this.publisherProvided = publisherProvided;
    }

    public OfficialMeta getOfficial() {
      return official;
    }

    public void setOfficial(OfficialMeta official) {
      this.official = official;
    }

    @Override
    public int hashCode() {
      return Objects.hash(official, publisherProvided);
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
      ServerMeta other = (ServerMeta) obj;
      return Objects.equals(official, other.official) && Objects.equals(publisherProvided, other.publisherProvided);
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("publisherProvided", publisherProvided);
      builder.append("official", official);
      return builder.toString();
    }

    /**
     * Metadata provided by the server publisher.
     */
    public static class PublisherProvidedMeta {
      private String tool;
      private String version;
      private BuildInfo buildInfo;

      /**
       * Default constructor for JSON deserialization.
       */
      public PublisherProvidedMeta() {
      }

      public String getTool() {
        return tool;
      }

      public void setTool(String tool) {
        this.tool = tool;
      }

      public String getVersion() {
        return version;
      }

      public void setVersion(String version) {
        this.version = version;
      }

      public BuildInfo getBuildInfo() {
        return buildInfo;
      }

      public void setBuildInfo(BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
      }

      @Override
      public int hashCode() {
        return Objects.hash(buildInfo, tool, version);
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
        PublisherProvidedMeta other = (PublisherProvidedMeta) obj;
        return Objects.equals(buildInfo, other.buildInfo) && Objects.equals(tool, other.tool)
            && Objects.equals(version, other.version);
      }

      @Override
      public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("tool", tool);
        builder.append("version", version);
        builder.append("buildInfo", buildInfo);
        return builder.toString();
      }

      /**
       * Build information about the server.
       */
      public static class BuildInfo {
        private String commit;
        private String timestamp;
        private String pipelineId;

        /**
         * Default constructor for JSON deserialization.
         */
        public BuildInfo() {
        }

        public String getCommit() {
          return commit;
        }

        public void setCommit(String commit) {
          this.commit = commit;
        }

        public String getTimestamp() {
          return timestamp;
        }

        public void setTimestamp(String timestamp) {
          this.timestamp = timestamp;
        }

        public String getPipelineId() {
          return pipelineId;
        }

        public void setPipelineId(String pipelineId) {
          this.pipelineId = pipelineId;
        }

        @Override
        public int hashCode() {
          return Objects.hash(commit, pipelineId, timestamp);
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
          BuildInfo other = (BuildInfo) obj;
          return Objects.equals(commit, other.commit) && Objects.equals(pipelineId, other.pipelineId)
              && Objects.equals(timestamp, other.timestamp);
        }

        @Override
        public String toString() {
          ToStringBuilder builder = new ToStringBuilder(this);
          builder.append("commit", commit);
          builder.append("timestamp", timestamp);
          builder.append("pipelineId", pipelineId);
          return builder.toString();
        }

      }
    }

    /**
     * Official metadata about the server.
     */
    public static class OfficialMeta {
      private String id;
      @SerializedName("published_at")
      private String publishedAt;
      @SerializedName("updated_at")
      private String updatedAt;
      @SerializedName("is_latest")
      private boolean isLatest;

      public String getId() {
        return id;
      }

      public void setId(String id) {
        this.id = id;
      }

      public String getPublishedAt() {
        return publishedAt;
      }

      public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
      }

      public String getUpdatedAt() {
        return updatedAt;
      }

      public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
      }

      public boolean isLatest() {
        return isLatest;
      }

      public void setLatest(boolean isLatest) {
        this.isLatest = isLatest;
      }

      @Override
      public int hashCode() {
        return Objects.hash(id, isLatest, publishedAt, updatedAt);
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
        OfficialMeta other = (OfficialMeta) obj;
        return Objects.equals(id, other.id) && isLatest == other.isLatest
            && Objects.equals(publishedAt, other.publishedAt) && Objects.equals(updatedAt, other.updatedAt);
      }

      @Override
      public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("id", id);
        builder.append("publishedAt", publishedAt);
        builder.append("updatedAt", updatedAt);
        builder.append("isLatest", isLatest);
        return builder.toString();
      }

    }
  }
}

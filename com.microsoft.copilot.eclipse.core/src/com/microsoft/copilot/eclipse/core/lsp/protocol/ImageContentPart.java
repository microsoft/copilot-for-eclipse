package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Represents an image content part in a chat completion message.
 * This class encapsulates the image URL and its detail level.
 */

public class ImageContentPart implements ChatCompletionContentPart {
  private final String type = ContentType.IMAGE_URL.getValue();
  private final ImageUrl imageUrl;
  
  /**
   * Constructs an ImageContentPart with the specified image URL and detail.
   *
   * @param url    The URL of the image.
   * @param detail The detail level of the image (e.g., LOW, HIGH Or Null).
   */
  public ImageContentPart(String url, ImageDetail detail) {
    this.imageUrl = new ImageUrl(url, detail);
  }

  @Override
  public String getType() {
    return type;
  }

  public ImageUrl getImageUrl() {
    return imageUrl;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, imageUrl);
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
    ImageContentPart other = (ImageContentPart) obj;
    return Objects.equals(type, other.type) && Objects.equals(imageUrl, other.imageUrl);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("type", type);
    builder.add("image_url", imageUrl);
    return builder.toString();
  }
  
  /**
   * Enum representing the detail level of an image.
   * LOW for low-resolution images, HIGH for high-resolution images.
   */
  public enum ImageDetail {
    LOW, HIGH;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }
  
  /**
   * Represents the URL and detail level of an image.
   * This class is used to encapsulate the image URL and its detail level.
   */
  public class ImageUrl {
    private final String url;
    private final String detail;
    
    /**
     * Constructs an ImageUrl with the specified URL and detail.
     *
     * @param url    The URL of the image.
     * @param detail The detail level of the image (e.g., LOW, HIGH Or Null).
     */
    public ImageUrl(String url, ImageDetail detail) {
      this.url = url;
      this.detail = detail.toString();
    }

    public String getUrl() {
      return url;
    }

    public String getDetail() {
      return detail;
    }

    @Override
    public int hashCode() {
      return Objects.hash(url, detail);
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
      ImageUrl other = (ImageUrl) obj;
      return Objects.equals(url, other.url) && Objects.equals(detail, other.detail);
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("url", url);
      builder.add("detail", detail);
      return builder.toString();
    }
  }
}

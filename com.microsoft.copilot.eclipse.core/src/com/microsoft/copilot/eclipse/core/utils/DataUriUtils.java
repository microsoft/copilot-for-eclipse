// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import java.util.Base64;

/**
 * Utility methods for building {@code data:} URIs from binary content, for embedding resources
 * (e.g. PNG icons) directly into HTML instead of referencing them by URL.
 */
public final class DataUriUtils {

  private DataUriUtils() {
  }

  /**
   * Encodes the given bytes as a base64 {@code data:} URI with the given MIME type.
   *
   * @param data the binary content to encode
   * @param mimeType the MIME type to use in the data URI (e.g. {@code "image/png"})
   * @return the {@code data:} URI string, or an empty string if {@code data} is {@code null}
   */
  public static String toDataUri(byte[] data, String mimeType) {
    if (data == null) {
      return "";
    }
    return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(data);
  }
}

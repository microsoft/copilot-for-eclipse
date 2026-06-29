// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.osgi.framework.Bundle;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Utility methods for loading resource files (text, binary, images such as PNG or SVG)
 * bundled inside an OSGi {@link Bundle}.
 *
 * <p>All methods degrade gracefully: if the resource cannot be located or read they log the
 * failure and return {@code null} (or an empty string for the data-URI helper) instead of
 * throwing, so that a missing icon never breaks the surrounding feature.
 */
public final class BundleUtils {

  private BundleUtils() {
  }

  /**
   * Reads a bundle resource as a UTF-8 string. Intended for text resources such as SVG icons.
   *
   * @param bundle the bundle containing the resource
   * @param path the bundle-relative resource path (e.g. {@code "resources/html/icons/x.svg"})
   * @return the file content as a UTF-8 string, or {@code null} if it cannot be read
   */
  public static String readResourceAsString(Bundle bundle, String path) {
    byte[] bytes = readResourceAsBytes(bundle, path);
    return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
  }

  /**
   * Reads a bundle resource as a byte array. Intended for binary resources such as PNG icons.
   *
   * @param bundle the bundle containing the resource
   * @param path the bundle-relative resource path
   * @return the file content as bytes, or {@code null} if it cannot be read
   */
  public static byte[] readResourceAsBytes(Bundle bundle, String path) {
    if (bundle == null || path == null) {
      return null;
    }
    URL entry = bundle.getEntry(path);
    if (entry == null) {
      CopilotCore.LOGGER.error("Bundle resource not found: " + path,
          new FileNotFoundException(path));
      return null;
    }
    try (InputStream is = entry.openStream()) {
      return is.readAllBytes();
    } catch (IOException e) {
      CopilotCore.LOGGER.error("Failed to read bundle resource: " + path, e);
      return null;
    }
  }

  /**
   * Reads a bundle resource and encodes it as a {@code data:} URI with the given MIME type,
   * for embedding a binary resource (e.g. a PNG icon) directly into HTML.
   *
   * @param bundle the bundle containing the resource
   * @param path the bundle-relative resource path
   * @param mimeType the MIME type to use in the data URI (e.g. {@code "image/png"})
   * @return the {@code data:} URI string, or an empty string if the resource cannot be read
   */
  public static String readResourceAsDataUri(Bundle bundle, String path, String mimeType) {
    byte[] bytes = readResourceAsBytes(bundle, path);
    if (bytes == null) {
      return "";
    }
    return DataUriUtils.toDataUri(bytes, mimeType);
  }

  /**
   * Reads a bundle PNG resource and encodes it as an {@code image/png} {@code data:} URI, for
   * embedding the icon directly into HTML.
   *
   * @param bundle the bundle containing the PNG resource
   * @param path the bundle-relative resource path
   * @return the {@code data:} URI string, or an empty string if the resource cannot be read
   */
  public static String loadPngAsDataUri(Bundle bundle, String path) {
    return readResourceAsDataUri(bundle, path, "image/png");
  }
}

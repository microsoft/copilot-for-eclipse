package com.microsoft.copilot.eclipse.core.utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.osgi.framework.Bundle;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Utility class for platform related operations.
 */
public class PlatformUtils {

  public static final String EC_PLATFORM_BUNDLE_NAME = "org.eclipse.platform";

  private PlatformUtils() {
  }

  /**
   * Get the version of the Eclipse platform.
   */
  public static String getEclipseVersion() {
    Bundle bundle = Platform.getBundle(EC_PLATFORM_BUNDLE_NAME);
    if (bundle == null) {
      return "unknown";
    }
    return bundle.getVersion().toString();
  }

  /**
   * Get the version of the Copilot plugin.
   */
  public static String getBundleVersion() {
    Bundle bundle = CopilotCore.getPlugin().getBundle();
    return bundle == null ? "unknown" : bundle.getVersion().toString();
  }

  /**
   * Check if the Copilot plugin is a nightly build.
   */
  public static boolean isNightly() {
    return getBundleVersion().toString().endsWith("_nightly");
  }

  /**
   * Read the content of a file.
   *
   * @param path the file path
   * @return the file content
   */
  public static String readFileContent(Path path) {
    String content = "";
    try {
      if (Files.exists(path) && Files.isReadable(path)) {
        content = Files.readString(path);
      }
      return content;
    } catch (IOException e) {
      CopilotCore.LOGGER.error("File not found: " + path, e);
    }
    return "";
  }

  /**
   * Write the content to a file.
   *
   * @param path the file path
   * @param content the content to write
   */
  public static void writeFileContent(@NonNull Path path, String content) {
    try {
      if (Files.notExists(path)) {
        Files.createDirectories(path.getParent());
      }
      Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      CopilotCore.LOGGER.error("Failed to write to file: " + path, e);
    }
  }

  /**
   * Escapes spaces in a URL string.
   *
   * @param urlString the URL string to escape
   * @return the escaped URL string
   */
  public static String escapeSpaceInUrl(String urlString) {
    char[] chars = urlString.toCharArray();
    StringBuffer sb = new StringBuffer(chars.length);
    for (int i = 0; i < chars.length; i++) {
      if (chars[i] == ' ') {
        sb.append("%20");
      } else {
        sb.append(chars[i]);
      }
    }
    return sb.toString();
  }

  public static boolean isMac() {
    return Platform.getOS().equals(Platform.OS_MACOSX);
  }

  public static boolean isLinux() {
    return Platform.getOS().equals(Platform.OS_LINUX);
  }

  public static boolean isWindows() {
    return Platform.getOS().equals(Platform.OS_WIN32);
  }

  public static boolean isIntel64() {
    return Platform.getOSArch().equals(Platform.ARCH_X86_64);
  }

  public static boolean isArm64() {
    return Platform.getOSArch().equals(Platform.ARCH_AARCH64);
  }

  /**
   * get the property value of the object with reflection.
   *
   * @param object the object
   * @param propertyName the property name
   * @return the property value
   */
  public static Object getPropertyWithReflection(Object object, String propertyName) {
    if (object == null) {
      return null;
    }
    Field[] fields = object.getClass().getDeclaredFields();
    for (Field field : fields) {
      if (field.getName().equals(propertyName)) {
        field.setAccessible(true);
        try {
          return field.get(object);
        } catch (IllegalArgumentException | IllegalAccessException e) {
          return null;
        }
      }
    }
    return null;
  }

  /**
   * Return the workspace root URI string.
   */
  public static String getWorkspaceRootUri() {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    URI uri = LSPEclipseUtils.toUri((IResource) workspaceRoot);
    return uri != null ? uri.toASCIIString() : "";
  }

  /**
   * Get the charset name from an IFile, defaulting to UTF-8 if empty or on error.
   *
   * @param file the IFile to get charset from
   * @return the charset name, never null
   */
  public static String getFileCharset(IFile file) {
    try {
      String charsetName = file.getCharset(true);
      if (StringUtils.isNotEmpty(charsetName)) {
        return charsetName;
      }
    } catch (CoreException e) {
      CopilotCore.LOGGER.error("Failed to get charset for file: " + file.getFullPath(), e);
    }
    return ResourcesPlugin.getEncoding();
  }

  /**
   * Convert Eclipse/Java charset names to Node.js BufferEncoding names.
   * CLS expects Node.js-compatible encoding names.
   *
   * @param javaCharset Eclipse/Java charset name
   * @return Node.js BufferEncoding name, defaults to "utf8" for unknown charsets
   */
  public static String convertToNodeEncoding(String javaCharset) {
    if (javaCharset == null || javaCharset.isEmpty()) {
      return "utf8";
    }

    // Normalize to lowercase for case-insensitive matching
    String normalized = javaCharset.toLowerCase().replace("_", "-");

    // Map Java/Eclipse charset names to Node.js BufferEncoding names
    // Note: Node.js only supports: utf8, utf16le, latin1 (ISO-8859-1), ascii, base64, hex
    return switch (normalized) {
      // UTF-8 (most common, full Unicode support)
      case "utf-8", "utf8" -> "utf8";
      // UTF-16 Little Endian (Node.js only supports LE variant)
      case "utf-16", "utf-16le", "utf16le" -> "utf16le";
      // ASCII (7-bit, subset of latin1)
      case "us-ascii", "ascii" -> "ascii";
      // ISO-8859-1 / Latin-1 (Western European, 8-bit single-byte)
      case "iso-8859-1", "iso8859-1" -> "latin1";
      // Windows-1252 (superset of ISO-8859-1, but Node.js only has latin1)
      case "cp1252", "windows-1252" -> "latin1";
      // Fallback to UTF-8 for incompatible/unsupported charsets
      // - UTF-16 BE: Node.js doesn't support big-endian UTF-16
      // - ISO-8859-15: Has Euro symbol, but Node.js latin1 is ISO-8859-1 only
      // - CP1250 and other codepages: Not compatible with latin1
      default -> {
        CopilotCore.LOGGER.info("Charset '" + javaCharset
            + "' not directly compatible with Node.js BufferEncoding, falling back to utf8");
        yield "utf8";
      }
    };
  }

  /**
   * Get the Node.js-compatible encoding for a file URI.
   * Uses Eclipse's built-in fallback chain via {@link #getFileCharset(IFile)}.
   *
   * @param fileUri LSP file URI (e.g., "file:///path/to/file.txt")
   * @return Node.js BufferEncoding name (e.g., "latin1", "utf8")
   */
  public static String getEncodingForFileUri(String fileUri) {
    IFile file = FileUtils.getFileFromUri(fileUri);
    String charset;
    
    if (file != null && file.exists()) {
      charset = getFileCharset(file);
    } else {
      charset = ResourcesPlugin.getEncoding();
    }
    
    return convertToNodeEncoding(charset);
  }

}

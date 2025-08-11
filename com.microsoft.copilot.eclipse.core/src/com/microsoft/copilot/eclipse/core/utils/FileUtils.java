package com.microsoft.copilot.eclipse.core.utils;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.LSPEclipseUtils;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatReference;
import com.microsoft.copilot.eclipse.core.lsp.protocol.DirectoryChatReference;
import com.microsoft.copilot.eclipse.core.lsp.protocol.FileChatReference;

/**
 * Utilities for the core module.
 */
public class FileUtils {
  private FileUtils() {
  }

  /**
   * Gets a URI string for a resource using multiple approaches.
   *
   * @param resource The resource
   * @return A URI string or null if no URI could be determined
   */
  public static String getResourceUri(IResource resource) {
    if (resource == null) {
      return null;
    }

    // Try standard file location first
    if (resource.getLocation() != null) {
      URI url = LSPEclipseUtils.toUri(resource);
      if (url != null) {
        return url.toASCIIString();
      }
    }

    // Try getting direct URI (works for remote resources)
    if (resource.getLocationURI() != null) {
      URI url = resource.getLocationURI();
      if (url != null) {
        return url.toASCIIString();
      }
    }

    // Fall back to platform resource URI
    // See:
    // https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/e4/ui/css/swt/helpers/URI.html#createPlatformResourceURI(java.lang.String,boolean)
    return "platform:/resource" + resource.getFullPath().toString();
  }

  /**
   * Get all the IFile instance in a List of resources.
   *
   * @param resources The list of resources to filter.
   * @return A list of IFile instances or an empty list if no files are found.
   */
  public static List<IFile> filterFilesFrom(List<IResource> resources) {
    return resources.stream().filter(IFile.class::isInstance).map(IFile.class::cast).collect(Collectors.toList());
  }

  /**
   * Converts a list of resources to a list of ChatReference objects.
   *
   * @param resources The list of resources to convert
   * @return A list of ChatReference objects
   */
  public static List<ChatReference> convertToChatReferences(List<IResource> resources) {
    return resources.stream().map(resource -> {
      if (resource instanceof IFile file && !ChatMessageUtils.isImageFile(file)) {
        return new FileChatReference(file);
      } else if (resource instanceof IFolder folder) {
        return new DirectoryChatReference(folder);
      }
      return null;
    }).filter(Objects::nonNull).collect(Collectors.toList());
  }

  /**
   * Returns true if the file needs to be excluded from the referenced files.
   */
  public static boolean isExcludedFromReferencedFiles(@Nullable IFile file) {
    if (file == null) {
      return true;
    }

    if (file.getFileExtension() == null) {
      return false; // If the file has no extension, we do not exclude it.
    }
    return Constants.EXCLUDED_REFERENCE_FILE_TYPE.contains(file.getFileExtension());
  }

  /**
   * Returns true if the file needs to be excluded from 'Current file' reference in chat.
   */
  public static boolean isExcludedFromCurrentFile(@Nullable IFile file) {
    if (file == null) {
      return true;
    }

    if (file.getFileExtension() == null) {
      return false; // If the file has no extension, we do not exclude it.
    }
    return Constants.EXCLUDED_CURRENT_FILE_TYPE.contains(file.getFileExtension());
  }
}

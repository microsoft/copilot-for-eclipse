// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.LSPEclipseUtils;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatReference;
import com.microsoft.copilot.eclipse.core.lsp.protocol.DirectoryChatReference;
import com.microsoft.copilot.eclipse.core.lsp.protocol.FileChatReference;
import com.microsoft.copilot.eclipse.core.lsp.protocol.FileStat;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ReadDirectoryResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ReadDirectoryResult.DirectoryEntry;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ReadFileResult;

/**
 * Utilities for the core module.
 */
public class FileUtils {
  private static final Pattern URI_SCHEME_PATTERN = Pattern.compile("^\\w[\\w\\d+.-]*:/");

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

  /**
   * Convert an LSP file URI to an Eclipse IFile.
   *
   * @param fileUri LSP file URI (e.g., "file:///path/to/file.txt")
   * @return IFile instance, or null if not found
   */
  @Nullable
  public static IFile getFileFromUri(String fileUri) {
    if (fileUri == null || fileUri.isEmpty()) {
      return null;
    }

    try {
      URI uri = new URI(fileUri);
      IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(uri);
      if (files != null && files.length > 0) {
        return files[0];
      }
    } catch (URISyntaxException e) {
      CopilotCore.LOGGER.error("Invalid file URI: " + fileUri, e);
    }
    return null;
  }

  /**
   * Normalizes a file path or URI string to a proper file URI string. Handles Windows absolute paths, POSIX absolute
   * paths, and existing URI strings. Line number fragments (e.g., #L123) are preserved.
   *
   * <p>Examples:
   * <ul>
   * <li>{@code C:\Users\file.java} → {@code file:///C:/Users/file.java}
   * <li>{@code /home/user/file.java} → {@code file:///home/user/file.java}
   * <li>{@code file:///path/file.java} → {@code file:///path/file.java} (unchanged)
   * <li>{@code C:\file.java#L100} → {@code file:///C:/file.java#L100}
   * </ul>
   *
   * @param pathOrUri the file path or URI string to normalize
   * @return the normalized URI string, or null if the path is invalid or cannot be converted
   */
  public static String normalizeToUri(String pathOrUri) {
    if (pathOrUri == null || pathOrUri.isEmpty()) {
      return null;
    }

    // Split off line number fragment if present (e.g., #L123, #L1-L10)
    String fragment = null;
    String pathPart = pathOrUri;
    int fragmentIndex = pathOrUri.indexOf('#');
    if (fragmentIndex > 0) {
      fragment = pathOrUri.substring(fragmentIndex);
      pathPart = pathOrUri.substring(0, fragmentIndex);
    }

    URI uri = resolvePathToUri(pathPart);
    if (uri == null) {
      return null;
    }

    // Append fragment if present
    if (fragment != null) {
      return uri.toString() + fragment;
    }
    return uri.toString();
  }

  /**
   * Resolves a file path to a URI. Handles Windows absolute paths, POSIX absolute paths, and existing URI strings.
   *
   * @param filepath the file path to resolve
   * @return the resolved URI, or null if the path is invalid
   */
  private static URI resolvePathToUri(String filepath) {
    // Check for POSIX-like absolute paths or Windows-like absolute paths
    if (filepath.startsWith("/")
        || hasDriveLetter(filepath)
        || (PlatformUtils.isWindows() && filepath.startsWith("\\"))) {
      try {
        return Paths.get(filepath).toUri();
      } catch (Exception e) {
        CopilotCore.LOGGER.error("Failed to convert path to URI: " + filepath, e);
        return null;
      }
    }

    // Check if the filepath starts with a URI scheme (e.g., file:, http:)
    // Verify the character after colon is "/" to distinguish from Windows drive letters
    if (URI_SCHEME_PATTERN.matcher(filepath).find()) {
      try {
        return new URI(filepath);
      } catch (URISyntaxException e) {
        CopilotCore.LOGGER.error("Failed to parse URI: " + filepath, e);
        return null;
      }
    }

    return null;
  }

  /**
   * Get an IFile from a file path string. This method tries multiple approaches to locate the file in the workspace: 1.
   * First tries getFileForLocation for absolute filesystem paths 2. Falls back to getFile for workspace-relative paths
   * (e.g., ADT files)
   *
   * @param filePath The file path, either absolute (e.g., "C:/project/file.java") or workspace-relative (e.g.,
   *     "/ProjectName/src/file.java")
   * @param checkExistence If true, only returns the file if it exists; if false, returns a file handle even for
   *     non-existent files (useful for creating new files)
   * @return IFile instance, or null if not found/resolved in the workspace
   */
  @Nullable
  public static IFile getFileFromPath(String filePath, boolean checkExistence) {
    if (filePath == null || filePath.isEmpty()) {
      return null;
    }

    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IPath eclipsePath = org.eclipse.core.runtime.Path.fromOSString(filePath);

    // Try getFileForLocation first - works for absolute filesystem paths
    IFile file = root.getFileForLocation(eclipsePath);
    if (file != null && (file.exists() || !checkExistence)) {
      return file;
    }

    // Fall back to getFile - works for workspace-relative paths (e.g., ADT files)
    // Workspace-relative paths must have at least 2 segments (project name + resource name)
    if (eclipsePath.segmentCount() >= 2) {
      file = root.getFile(eclipsePath);
      if (file != null && (file.exists() || !checkExistence)) {
        return file;
      }
    }

    return null;
  }

  /**
   * Checks if the filepath starts with a Windows drive letter (e.g., C:).
   *
   * @param filepath the file path to check
   * @return true if the path starts with a drive letter, false otherwise
   */
  private static boolean hasDriveLetter(String filepath) {
    return filepath.length() > 1 && Character.isLetter(filepath.charAt(0)) && filepath.charAt(1) == ':';
  }

  /**
   * Reads the contents and stats of a file given its URI. Used by workspace/readFile API to read file content along
   * with file stats using uri.
   *
   * @param uri the file URI (e.g., "file:///path/to/file.txt")
   * @return ReadFileResult containing text content and file stats
   */
  public static ReadFileResult readFileWithStats(String uri) {
    IFile file = getFileFromUri(uri);
    if (file == null) {
      return new ReadFileResult("file not found: " + uri, null);
    }

    try {
      String text = readFileContent(file);
      FileStat stat = getFileStatFromFile(file);
      return new ReadFileResult(text, stat);
    } catch (CoreException | IOException e) {
      CopilotCore.LOGGER.error("Failed to read file: " + uri, e);
      return new ReadFileResult("Failed to read file: " + e.getMessage(), null);
    }

  }

  /**
   * Reads the contents of a directory given its URI. Used by workspace/readDirectory API to list directory entries.
   * Works with both local filesystem URIs and virtual URIs (e.g., semanticfs://) through Eclipse's IResource API.
   *
   * @param uri the directory URI
   * @return ReadDirectoryResult containing the directory entries
   */
  public static ReadDirectoryResult readDirectoryEntries(String uri) {
    if (StringUtils.isBlank(uri)) {
      return new ReadDirectoryResult(Collections.emptyList());
    }

    try {
      URI parsedUri = new URI(uri);
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

      // Try to find containers (folders/projects) for the given URI
      IContainer[] containers = root.findContainersForLocationURI(parsedUri);
      if (containers == null || containers.length == 0) {
        return new ReadDirectoryResult(Collections.emptyList());
      }

      // findContainersForLocationURI may return multiple matches;
      // pick the first accessible one to avoid reading a closed/phantom project
      IContainer container = null;
      for (IContainer c : containers) {
        if (c.isAccessible()) {
          container = c;
          break;
        }
      }
      if (container == null) {
        return new ReadDirectoryResult(Collections.emptyList());
      }

      IResource[] members = container.members();
      List<DirectoryEntry> entries = new ArrayList<>();
      for (IResource member : members) {
        int type;
        switch (member.getType()) {
          case IResource.FILE:
            type = DirectoryEntry.FILE_TYPE_FILE;
            break;
          case IResource.FOLDER:
          case IResource.PROJECT:
            type = DirectoryEntry.FILE_TYPE_DIRECTORY;
            break;
          default:
            type = DirectoryEntry.FILE_TYPE_UNKNOWN;
            break;
        }
        entries.add(new DirectoryEntry(member.getName(), type));
      }
      return new ReadDirectoryResult(entries);
    } catch (URISyntaxException e) {
      CopilotCore.LOGGER.error("Invalid directory URI: " + uri, e);
      return new ReadDirectoryResult(Collections.emptyList());
    } catch (CoreException e) {
      CopilotCore.LOGGER.error("Failed to read directory: " + uri, e);
      return new ReadDirectoryResult(Collections.emptyList());
    }
  }

  private static String readFileContent(IFile file) throws CoreException, IOException {
    try (InputStream is = file.getContents()) {
      return new String(is.readAllBytes(), file.getCharset());
    }
  }

  private static FileStat getFileStatFromFile(IFile file) {
    // Prefer local filesystem metadata when available.
    if (file.getLocation() != null) {
      return getFileStatFromPath(file.getLocation().toFile().toPath());
    }

    // Fall back to Eclipse resource metadata.
    return getFileStatFromEclipseResource(file);
  }

  private static FileStat getFileStatFromPath(Path path) {
    FileStat stat = new FileStat();
    try {
      BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      stat.setSize(attrs.size());
    } catch (IOException e) {
      CopilotCore.LOGGER.error("Failed to read file size attribute", e);
    }
    return stat;
  }

  private static FileStat getFileStatFromEclipseResource(IFile file) {
    FileStat stat = new FileStat();

    if (file.getLocationURI() != null) {
      try (InputStream is = file.getContents(true)) {
        stat.setSize(is.readAllBytes().length);
      } catch (IOException | CoreException e) {
        // Ignore; size stays 0.
      }
    }
    return stat;
  }
}

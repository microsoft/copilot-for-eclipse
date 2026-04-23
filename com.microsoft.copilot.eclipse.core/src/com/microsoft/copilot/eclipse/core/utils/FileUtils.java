// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
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
import com.microsoft.copilot.eclipse.core.lsp.protocol.FindFilesParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.FindFilesResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.FindTextInFilesParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.FindTextInFilesResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.FindTextInFilesResult.TextSearchMatch;
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

    // Try URI-based resolution first for non-filesystem URI schemes (e.g., semanticfs://)
    if (URI_SCHEME_PATTERN.matcher(filePath).find() && !filePath.startsWith("file:")) {
      IResource resource = getResourceFromUri(filePath);
      if (resource instanceof IFile file) {
        return file;
      }
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
   * Works with local filesystem URIs, platform:/resource URIs (produced by {@link #getResourceUri(IResource)}), and
   * virtual URIs (e.g., semanticfs://) through Eclipse's IResource API.
   *
   * @param uri the directory URI
   * @return ReadDirectoryResult containing the directory entries
   */
  public static ReadDirectoryResult readDirectoryEntries(String uri) {
    if (StringUtils.isBlank(uri)) {
      return new ReadDirectoryResult(Collections.emptyList());
    }

    try {
      IContainer container = findContainerForUri(uri);
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

  /**
   * Finds files under the given base URI whose path (relative to the base container) matches the provided glob pattern.
   * Used by the {@code workspace/findFiles} request so the language server can perform file search over custom URI
   * schemes such as {@code semanticfs}.
   *
   * @param params the search parameters
   * @return a {@link FindFilesResult} containing the matching file URIs
   */
  public static FindFilesResult findFiles(FindFilesParams params) {
    if (params == null || StringUtils.isBlank(params.getBaseUri()) || StringUtils.isBlank(params.getPattern())) {
      return new FindFilesResult(List.of());
    }

    int maxResults = params.getMaxResults() != null && params.getMaxResults() > 0 ? params.getMaxResults()
        : Integer.MAX_VALUE;

    try {
      IContainer container = findContainerForUri(params.getBaseUri());
      if (container == null) {
        CopilotCore.LOGGER.info("findFiles: base URI not found in workspace: " + params.getBaseUri());
        return new FindFilesResult(List.of());
      }

      PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + params.getPattern());
      List<String> uris = new ArrayList<>();
      IPath basePath = container.getFullPath();

      collectMatchingFiles(container, basePath, matcher, uris, maxResults);
      return new FindFilesResult(uris);
    } catch (CoreException e) {
      CopilotCore.LOGGER.error("Failed to find files under: " + params.getBaseUri(), e);
      return new FindFilesResult(List.of());
    } catch (IllegalArgumentException e) {
      CopilotCore.LOGGER.error("Invalid glob pattern for findFiles: " + params.getPattern(), e);
      return new FindFilesResult(List.of());
    }
  }

  private static void collectMatchingFiles(IContainer container, IPath basePath, PathMatcher matcher,
      List<String> results, int maxResults) throws CoreException {
    if (results.size() >= maxResults) {
      return;
    }
    for (IResource member : container.members()) {
      if (results.size() >= maxResults) {
        return;
      }
      if (member.getType() == IResource.FILE) {
        IPath relative = member.getFullPath().makeRelativeTo(basePath);
        // PathMatcher uses the platform default file system; convert to a java.nio.file.Path via
        // the portable string so glob patterns like ** and *.ext work consistently.
        Path nioPath = Paths.get(relative.toPortableString().replace('/', java.io.File.separatorChar));
        if (matcher.matches(nioPath) || matcher.matches(Paths.get(relative.toPortableString()))) {
          String uri = getResourceUri(member);
          if (uri != null) {
            results.add(uri);
          }
        }
      } else if (member instanceof IContainer) {
        collectMatchingFiles((IContainer) member, basePath, matcher, results, maxResults);
      }
    }
  }

  /**
   * Searches for text (or a regex) in files under the given base URI. Used by the {@code workspace/findTextInFiles}
   * request.
   *
   * @param params the search parameters
   * @return a {@link FindTextInFilesResult} containing the matches
   */
  public static FindTextInFilesResult findTextInFiles(FindTextInFilesParams params) {
    if (params == null || StringUtils.isBlank(params.getBaseUri()) || StringUtils.isBlank(params.getQuery())) {
      return new FindTextInFilesResult(List.of());
    }

    int maxResults = params.getMaxResults() != null && params.getMaxResults() > 0 ? params.getMaxResults()
        : Integer.MAX_VALUE;
    boolean isRegexp = Boolean.TRUE.equals(params.getIsRegexp());

    Pattern pattern;
    try {
      pattern = isRegexp ? Pattern.compile(params.getQuery()) : Pattern.compile(Pattern.quote(params.getQuery()));
    } catch (PatternSyntaxException e) {
      CopilotCore.LOGGER.error("Invalid regex for findTextInFiles: " + params.getQuery(), e);
      return new FindTextInFilesResult(List.of());
    }

    // Compile the optional include glob pattern to filter which files are searched
    PathMatcher includeMatcher = null;
    if (params.getIncludePattern() != null && !params.getIncludePattern().isEmpty()) {
      try {
        includeMatcher = FileSystems.getDefault().getPathMatcher("glob:" + params.getIncludePattern());
      } catch (IllegalArgumentException e) {
        CopilotCore.LOGGER.error("Invalid glob for findTextInFiles includePattern: " + params.getIncludePattern(), e);
        return new FindTextInFilesResult(List.of());
      }
    }

    // Resolve the base URI to a workspace container and recursively search for text matches
    try {
      IContainer container = findContainerForUri(params.getBaseUri());
      if (container == null) {
        CopilotCore.LOGGER.info("findTextInFiles: base URI not found in workspace: " + params.getBaseUri());
        return new FindTextInFilesResult(List.of());
      }

      List<TextSearchMatch> matches = new ArrayList<>();
      searchTextInContainer(container, container.getFullPath(), pattern, includeMatcher, matches, maxResults);
      return new FindTextInFilesResult(matches);
    } catch (CoreException e) {
      CopilotCore.LOGGER.error("Failed to search text under: " + params.getBaseUri(), e);
      return new FindTextInFilesResult(List.of());
    }
  }

  private static void searchTextInContainer(IContainer container, IPath basePath, Pattern pattern,
      @Nullable PathMatcher includeMatcher, List<TextSearchMatch> results, int maxResults) throws CoreException {
    if (results.size() >= maxResults) {
      return;
    }
    for (IResource member : container.members()) {
      if (results.size() >= maxResults) {
        return;
      }
      if (member.getType() == IResource.FILE) {
        if (includeMatcher != null) {
          IPath relative = member.getFullPath().makeRelativeTo(basePath);
          Path nioPath = Paths.get(relative.toPortableString());
          if (!includeMatcher.matches(nioPath)) {
            continue;
          }
        }
        searchTextInFile((IFile) member, pattern, results, maxResults);
      } else if (member instanceof IContainer) {
        searchTextInContainer((IContainer) member, basePath, pattern, includeMatcher, results, maxResults);
      }
    }
  }

  /**
   * Default maximum number of characters for text search. Files exceeding this are skipped to avoid loading very large
   * blobs into memory.
   */
  private static final long TEXT_SEARCH_MAX_CHARS = 5L * 1024 * 1024;

  private static void searchTextInFile(IFile file, Pattern pattern, List<TextSearchMatch> results, int maxResults) {
    String uri = getResourceUri(file);
    if (uri == null) {
      return;
    }
    try (InputStream is = file.getContents(true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, file.getCharset()))) {
      long totalChars = 0;
      String line;
      int lineNumber = 0;
      while ((line = reader.readLine()) != null) {
        if (results.size() >= maxResults) {
          return;
        }
        lineNumber++;
        totalChars += line.length();
        if (totalChars > TEXT_SEARCH_MAX_CHARS) {
          return;
        }
        Matcher m = pattern.matcher(line);
        if (m.find()) {
          results.add(new TextSearchMatch(uri, lineNumber, line));
        }
      }
    } catch (CoreException | IOException e) {
      // Skip files we cannot read; other files may still yield matches.
      CopilotCore.LOGGER.info("findTextInFiles: skipping unreadable file " + uri + ": " + e.getMessage());
    }
  }

  /**
   * Resolves a workspace container (folder/project/root) for the given URI, or {@code null} if none exists. Used by
   * findFiles / findTextInFiles.
   */
  @Nullable
  private static IContainer findContainerForUri(String uri) {
    if (StringUtils.isBlank(uri)) {
      return null;
    }
    try {
      URI parsedUri = new URI(uri);
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

      if ("platform".equals(parsedUri.getScheme())) {
        String path = parsedUri.getPath();
        String prefix = "/resource";
        if (path != null && path.startsWith(prefix)) {
          IResource resource = root.findMember(path.substring(prefix.length()));
          if (resource instanceof IContainer c && c.isAccessible()) {
            return c;
          }
        }
      }

      IContainer[] containers = root.findContainersForLocationURI(parsedUri);
      if (containers != null) {
        for (IContainer c : containers) {
          if (c != null && c.isAccessible()) {
            return c;
          }
        }
      }
    } catch (URISyntaxException e) {
      CopilotCore.LOGGER.error("Invalid container URI: " + uri, e);
    }
    return null;
  }

  /**
   * Resolves a workspace resource from a URI. Supports file URIs, platform resource URIs, and Eclipse-managed virtual
   * URIs such as semanticfs.
   *
   * @param resourceUri the resource URI
   * @return the matching workspace resource, or null if not found
   */
  @Nullable
  private static IResource getResourceFromUri(String resourceUri) {
    if (StringUtils.isBlank(resourceUri)) {
      return null;
    }

    try {
      URI uri = new URI(resourceUri);
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

      IFile[] files = root.findFilesForLocationURI(uri);
      if (files != null) {
        for (IFile file : files) {
          if (file != null && file.exists()) {
            return file;
          }
        }
      }

      // Handle platform:/resource/... URIs by resolving via workspace path
      if ("platform".equals(uri.getScheme())) {
        String path = uri.getPath();
        String prefix = "/resource";
        if (path != null && path.startsWith(prefix)) {
          IResource resource = root.findMember(path.substring(prefix.length()));
          if (resource != null && resource.exists()) {
            return resource;
          }
        }
      }

      // For file://, semanticfs://, and other URIs, use location URI lookup
      IContainer[] containers = root.findContainersForLocationURI(uri);
      if (containers != null) {
        for (IContainer container : containers) {
          if (container != null && container.exists()) {
            return container;
          }
        }
      }
    } catch (URISyntaxException e) {
      CopilotCore.LOGGER.error("Invalid resource URI: " + resourceUri, e);
    }
    return null;
  }
}

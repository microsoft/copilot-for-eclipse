package com.microsoft.copilot.eclipse.ui.style;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test class to validate that Composite instances are not created with
 * SWT.CENTER style. This test dynamically scans all Java files in the UI
 * package and verifies composite styles.
 */
class CompositeStyleValidationTest {
  private static final Pattern COMPOSITE_PATTERN = Pattern
      .compile("new\\s+Composite\\s*\\(\\s*[^,]+\\s*,\\s*([^)]+)\\s*\\)");
  private static final Pattern SWT_CENTER_PATTERN = Pattern.compile("SWT\\.CENTER");

  private static List<Path> javaFiles;
  private static Path uiSourcePath;

  @BeforeAll
  static void setUp() throws Exception {
    // Find the UI source directory
    uiSourcePath = findUiSourcePath();
    assertNotNull(uiSourcePath, "UI source path should be found");
    assertTrue(Files.exists(uiSourcePath), "UI source path should exist");

    // Collect all Java files in the UI package
    javaFiles = collectJavaFiles(uiSourcePath);
    assertFalse(javaFiles.isEmpty(), "Should find Java files in UI package");
  }

  /**
   * Main test that scans all Composite constructor calls and validates their
   * styles.
   */
  @Test
  void testCompositeStylesAreNotSwtCenter() throws IOException {
    List<CompositeUsage> violations = new ArrayList<>();

    for (Path javaFile : javaFiles) {
      List<CompositeUsage> fileViolations = scanFileForCompositeViolations(javaFile);
      violations.addAll(fileViolations);
    }

    // Report any violations found
    if (!violations.isEmpty()) {
      StringBuilder errorMessage = new StringBuilder();
      errorMessage.append("Found ").append(violations.size())
          .append(" Composite instances with SWT.CENTER style:\n");

      for (CompositeUsage violation : violations) {
        errorMessage.append("  - File: ").append(violation.getFilePath()).append(", Line: ")
            .append(violation.getLineNumber()).append(", Code: ").append(violation.getCodeSnippet())
            .append("\n");
      }

      assertFalse(true, errorMessage.toString());
    }
  }

  /**
   * Scan a single file for Composite constructor calls that violate the
   * SWT.CENTER rule.
   */
  private static List<CompositeUsage> scanFileForCompositeViolations(Path javaFile) throws IOException {
    List<CompositeUsage> violations = new ArrayList<>();
    List<CompositeUsage> allUsages = scanFileForCompositeUsages(javaFile);

    for (CompositeUsage usage : allUsages) {
      if (containsSwtCenter(usage.getStyleExpression())) {
        violations.add(usage);
      }
    }

    return violations;
  }

  /**
   * Scan a single file for all Composite constructor calls.
   */
  private static List<CompositeUsage> scanFileForCompositeUsages(Path javaFile) throws IOException {
    List<CompositeUsage> usages = new ArrayList<>();

    try (BufferedReader reader = Files.newBufferedReader(javaFile)) {
      String line;
      int lineNumber = 0;

      while ((line = reader.readLine()) != null) {
        lineNumber++;
        Matcher matcher = COMPOSITE_PATTERN.matcher(line);

        while (matcher.find()) {
          String styleExpression = matcher.group(1).trim();
          usages.add(new CompositeUsage(javaFile.toString(), lineNumber, line.trim(), styleExpression));
        }
      }
    }

    return usages;
  }

  /**
   * Check if a style expression contains SWT.CENTER.
   */
  private static boolean containsSwtCenter(String styleExpression) {
    return SWT_CENTER_PATTERN.matcher(styleExpression).find();
  }

  /**
   * Find the UI source path by looking for the UI module structure.
   */
  private static Path findUiSourcePath() {
    try {
      // Start from the current working directory and look for the UI module
      Path currentPath = Paths.get(System.getProperty("user.dir"));

      // Look for the UI module in various possible locations
      String[] possiblePaths = { "com.microsoft.copilot.eclipse.ui/src/com/microsoft/copilot/eclipse/ui",
          "../com.microsoft.copilot.eclipse.ui/src/com/microsoft/copilot/eclipse/ui",
          "../../com.microsoft.copilot.eclipse.ui/src/com/microsoft/copilot/eclipse/ui" };

      for (String pathStr : possiblePaths) {
        Path candidate = currentPath.resolve(pathStr);
        if (Files.exists(candidate) && Files.isDirectory(candidate)) {
          return candidate;
        }
      }

      // If not found, try to find it by walking up the directory tree
      Path searchPath = currentPath;
      for (int i = 0; i < 5; i++) { // Limit search depth
        Path uiModulePath = searchPath.resolve("com.microsoft.copilot.eclipse.ui");
        if (Files.exists(uiModulePath)) {
          Path srcPath = uiModulePath.resolve("src/com/microsoft/copilot/eclipse/ui");
          if (Files.exists(srcPath)) {
            return srcPath;
          }
        }
        searchPath = searchPath.getParent();
        if (searchPath == null)
          break;
      }

    } catch (Exception e) {
      System.err.println("Error finding UI source path: " + e.getMessage());
    }

    return null;
  }

  /**
   * Collect all Java files in the UI package recursively.
   */
  private static List<Path> collectJavaFiles(Path rootPath) throws IOException {
    List<Path> javaFiles = new ArrayList<>();

    try (Stream<Path> paths = Files.walk(rootPath)) {
      paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java"))
          .forEach(javaFiles::add);
    }

    return javaFiles;
  }

  /**
   * Data class to represent a Composite usage instance.
   */
  private static class CompositeUsage {
    private final String filePath;
    private final int lineNumber;
    private final String codeSnippet;
    private final String styleExpression;

    public CompositeUsage(String filePath, int lineNumber, String codeSnippet, String styleExpression) {
      this.filePath = filePath;
      this.lineNumber = lineNumber;
      this.codeSnippet = codeSnippet;
      this.styleExpression = styleExpression;
    }

    public String getFilePath() {
      return filePath;
    }

    public int getLineNumber() {
      return lineNumber;
    }

    public String getCodeSnippet() {
      return codeSnippet;
    }

    public String getStyleExpression() {
      return styleExpression;
    }
  }
}

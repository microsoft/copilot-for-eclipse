package com.microsoft.copilot.eclipse.ui.editors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.ITextContentDescriber;

// @formatter:off
/**
 * Content describer for .agent.md files. This describer validates that markdown files match the agent file format:
 * <ul>
 * <li>Must start with YAML frontmatter (--- ... ---)</li>
 * <li>Must contain 'description:' and 'tools:' fields in the frontmatter</li>
 * </ul>
 *
 * <p>The plugin.xml registers this for file-extensions="md" with priority="high", so this describer validates content
 * to distinguish agent files from regular markdown.
 */
// @formatter:on
public class AgentFileContentDescriber implements ITextContentDescriber {

  private static final String FRONTMATTER_DELIMITER = "---";
  private static final int MAX_LINES_TO_CHECK = 50; // Reasonable limit for frontmatter

  @Override
  public int describe(InputStream contents, IContentDescription description) throws IOException {
    if (contents == null) {
      return INVALID;
    }

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(contents, StandardCharsets.UTF_8))) {
      return checkContent(reader);
    }
  }

  @Override
  public int describe(Reader contents, IContentDescription description) throws IOException {
    if (contents == null) {
      return INVALID;
    }

    BufferedReader reader = contents instanceof BufferedReader bufferReader ? bufferReader
        : new BufferedReader(contents);

    return checkContent(reader);
  }

  /**
   * Check if the content matches the agent file format. Agent files must have YAML frontmatter with 'description:' and
   * 'tools:' fields.
   *
   * @param reader the content reader
   * @return VALID if content matches agent format, INVALID otherwise
   */
  private int checkContent(BufferedReader reader) throws IOException {
    String firstLine = reader.readLine();

    // Must start with ---
    if (firstLine == null || !firstLine.trim().equals(FRONTMATTER_DELIMITER)) {
      return INVALID;
    }

    boolean hasDescription = false;
    boolean hasTools = false;
    boolean foundClosingDelimiter = false;
    int lineCount = 0;

    String line;
    while ((line = reader.readLine()) != null && lineCount < MAX_LINES_TO_CHECK) {
      lineCount++;
      String trimmedLine = line.trim();

      // Check for closing delimiter
      if (trimmedLine.equals(FRONTMATTER_DELIMITER)) {
        foundClosingDelimiter = true;
        break;
      }

      // Check for required fields (looking for "description:" or "tools:")
      if (trimmedLine.startsWith("description:")) {
        hasDescription = true;
      } else if (trimmedLine.startsWith("tools:")) {
        hasTools = true;
      }
    }

    // Valid agent file must have both fields and proper frontmatter structure
    if (foundClosingDelimiter && hasDescription && hasTools) {
      return VALID;
    }

    return INVALID;
  }

  @Override
  public QualifiedName[] getSupportedOptions() {
    return new QualifiedName[0];
  }
}

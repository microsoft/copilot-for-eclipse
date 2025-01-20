package com.microsoft.copilot.eclipse.core.format;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Java format.
 */
public class JavaFormatReader extends LanguageFormatReader {
  private String tabChar;
  private int tabSize;
  private int indentationSize;

  private static final String JavaCore_PLUGIN_ID = "org.eclipse.jdt.core";
  private static final String DefaultCodeFormatterConstants_FORMATTER_TAB_CHAR = JavaCore_PLUGIN_ID
      + ".formatter.tabulation.char";
  private static final String DefaultCodeFormatterConstants_FORMATTER_TAB_SIZE = JavaCore_PLUGIN_ID
      + ".formatter.tabulation.size";
  private static final String DefaultCodeFormatterConstants_FORMATTER_INDENTATION_SIZE = JavaCore_PLUGIN_ID
      + ".formatter.indentation.size";

  /**
   * Creates a new JavaFormat.
   */
  public JavaFormatReader(IProject project) {
    super(project);
    this.tabChar = PREFERENCE_DEFAULT_TAB_CHAR;
    this.tabSize = PREFERENCE_DEFAULT_TAB_SIZE;
    this.indentationSize = PREFERENCE_DEFAULT_INDENTATION_SIZE;
    fetchJavaFormatPreferences();
  }

  @Override
  public boolean getUseSpaces() {
    return this.tabChar.equalsIgnoreCase(PREFERENCE_DEFAULT_TAB_CHAR);
  }

  @Override
  public int getIndentSize() {
    return getUseSpaces() ? getIndentationSize() : getTabSize();
  }

  private int getTabSize() {
    return this.tabSize;
  }

  private int getIndentationSize() {
    return this.indentationSize;
  }

  @Override
  protected IScopeContext[] getScopeContexts(IProject project) {
    if (project == null) {
      return DEFAULT_SCOPE_CONTEXTS;
    } else {
      return new IScopeContext[] { new ProjectScope(project), InstanceScope.INSTANCE, ConfigurationScope.INSTANCE,
          DefaultScope.INSTANCE };
    }
  }

  private void fetchJavaFormatPreferences() {
    IScopeContext[] scopeContexts = getScopeContexts(this.project);
    String tabCharString = getFormatValue(scopeContexts, JavaCore_PLUGIN_ID,
        DefaultCodeFormatterConstants_FORMATTER_TAB_CHAR);
    String tabSizeString = getFormatValue(scopeContexts, JavaCore_PLUGIN_ID,
        DefaultCodeFormatterConstants_FORMATTER_TAB_SIZE);
    String indentationSizeString = getFormatValue(scopeContexts, JavaCore_PLUGIN_ID,
        DefaultCodeFormatterConstants_FORMATTER_INDENTATION_SIZE);

    this.tabChar = tabCharString != null ? tabCharString : PREFERENCE_DEFAULT_TAB_CHAR;
    this.tabSize = tabSizeString != null ? Integer.parseInt(tabSizeString) : PREFERENCE_DEFAULT_TAB_SIZE;
    this.indentationSize = indentationSizeString != null ? Integer.parseInt(indentationSizeString)
        : PREFERENCE_DEFAULT_INDENTATION_SIZE;
  }
}

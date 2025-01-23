package com.microsoft.copilot.eclipse.core.format;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.lsp4j.FormattingOptions;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Java format.
 */
public class JavaFormatReader extends LanguageFormatReader {
  private IProject project;
  private FormattingOptions formattingOptions;

  private static final String JavaCore_PLUGIN_ID = "org.eclipse.jdt.core";
  private static final String DefaultCodeFormatterConstants_FORMATTER_TAB_CHAR = JavaCore_PLUGIN_ID
      + ".formatter.tabulation.char";
  private static final String DefaultCodeFormatterConstants_FORMATTER_TAB_SIZE = JavaCore_PLUGIN_ID
      + ".formatter.tabulation.size";
  private static final Set<String> monitoredPreferences = Set.of(DefaultCodeFormatterConstants_FORMATTER_TAB_CHAR,
      DefaultCodeFormatterConstants_FORMATTER_TAB_SIZE);

  /**
   * Creates a new JavaFormatReader for the given project.
   */
  public JavaFormatReader(IProject project) {
    this.project = project;
  }

  /**
   * Get the language format option for the project.
   */
  @Override
  public FormattingOptions getFormattingOptions() {
    if (this.formattingOptions == null) {
      this.formattingOptions = new FormattingOptions(PREFERENCE_DEFAULT_TAB_SIZE,
          PREFERENCE_DEFAULT_TAB_CHAR.equalsIgnoreCase("space"));

      fetchTabCharPreference();
      fetchTabSizePreference();
      registerPreferencesChangeListener(project, JavaCore_PLUGIN_ID);
    }
    return this.formattingOptions;
  }

  @Override
  public void preferenceChange(PreferenceChangeEvent event) {
    if (event == null) {
      return;
    }

    String key = event.getKey();
    if (monitoredPreferences.contains(key)) {
      switch (key) {
        case JavaFormatReader.DefaultCodeFormatterConstants_FORMATTER_TAB_CHAR:
          fetchTabCharPreference();
          break;
        case JavaFormatReader.DefaultCodeFormatterConstants_FORMATTER_TAB_SIZE:
          fetchTabSizePreference();
          break;
        default:
          break;
      }
    }
  }

  private void fetchTabCharPreference() {
    IScopeContext[] scopeContexts = getScopeContexts(this.project);
    String tabCharString = getFormatValue(scopeContexts, JavaCore_PLUGIN_ID,
        DefaultCodeFormatterConstants_FORMATTER_TAB_CHAR);
    this.formattingOptions.setInsertSpaces((tabCharString != null ? tabCharString.equalsIgnoreCase("space")
        : PREFERENCE_DEFAULT_TAB_CHAR.equalsIgnoreCase("space")));

    logPreferenceUpdate(DefaultCodeFormatterConstants_FORMATTER_TAB_CHAR, tabCharString);
  }

  private void fetchTabSizePreference() {
    IScopeContext[] scopeContexts = getScopeContexts(this.project);
    String tabSizeString = getFormatValue(scopeContexts, JavaCore_PLUGIN_ID,
        DefaultCodeFormatterConstants_FORMATTER_TAB_SIZE);
    this.formattingOptions
        .setTabSize(tabSizeString != null ? Integer.parseInt(tabSizeString) : PREFERENCE_DEFAULT_TAB_SIZE);

    logPreferenceUpdate(DefaultCodeFormatterConstants_FORMATTER_TAB_SIZE, tabSizeString);
  }

  private void logPreferenceUpdate(String key, String newValue) {
    CopilotCore.LOGGER.info(String.format("Java format preference cache for %s is updated. key: %s, new value: %s",
        project.getName(), key, newValue));
  }
}

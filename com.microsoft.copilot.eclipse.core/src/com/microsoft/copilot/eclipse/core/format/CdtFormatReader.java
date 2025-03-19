package com.microsoft.copilot.eclipse.core.format;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.lsp4j.FormattingOptions;

/**
 * C/C++ format.
 */
public class CdtFormatReader extends LanguageFormatReader {
  private IProject project;
  private FormattingOptions formattingOptions;

  // The following constants are copied from org.eclipse.cdt.core.formatter.DefaultCodeFormatterConstants
  // https://github.com/eclipse-cdt/cdt/blob/ca5dabc3a3b2652f6fe0fbdfaaa838b31fa42aa8/core/org.eclipse.cdt.core/src/org/eclipse/cdt/core/formatter/DefaultCodeFormatterConstants.java#L2662
  private static final String CCore_PLUGIN_ID = "org.eclipse.cdt.core";
  private static final String DefaultCodeFormatterConstants_FORMATTER_TAB_CHAR = CCore_PLUGIN_ID
      + ".formatter.tabulation.char";
  private static final String DefaultCodeFormatterConstants_FORMATTER_TAB_SIZE = CCore_PLUGIN_ID
      + ".formatter.tabulation.size";
  private static final Set<String> monitoredPreferences = Set.of(DefaultCodeFormatterConstants_FORMATTER_TAB_CHAR,
      DefaultCodeFormatterConstants_FORMATTER_TAB_SIZE);

  /**
   * Creates a new CdtFormatReader for the given project.
   */
  public CdtFormatReader(IProject project) {
    this.project = project;
  }

  /**
   * Get the language format option for C/C++.
   */
  @Override
  public FormattingOptions getFormattingOptions() {
    if (this.formattingOptions == null) {
      this.formattingOptions = new FormattingOptions(PREFERENCE_DEFAULT_TAB_SIZE,
          PREFERENCE_DEFAULT_TAB_CHAR.equalsIgnoreCase("space"));

      fetchTabCharPreference();
      fetchTabSizePreference();
      registerPreferencesChangeListener(project, CCore_PLUGIN_ID);
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
        case CdtFormatReader.DefaultCodeFormatterConstants_FORMATTER_TAB_CHAR:
          fetchTabCharPreference();
          break;
        case CdtFormatReader.DefaultCodeFormatterConstants_FORMATTER_TAB_SIZE:
          fetchTabSizePreference();
          break;
        default:
          break;
      }
    }
  }

  private void fetchTabCharPreference() {
    IScopeContext[] scopeContexts = getScopeContexts(this.project);
    String tabCharString = getFormatValue(scopeContexts, CCore_PLUGIN_ID,
        DefaultCodeFormatterConstants_FORMATTER_TAB_CHAR);
    this.formattingOptions.setInsertSpaces((tabCharString != null ? tabCharString.equalsIgnoreCase("space")
        : PREFERENCE_DEFAULT_TAB_CHAR.equalsIgnoreCase("space")));
  }

  private void fetchTabSizePreference() {
    IScopeContext[] scopeContexts = getScopeContexts(this.project);
    String tabSizeString = getFormatValue(scopeContexts, CCore_PLUGIN_ID,
        DefaultCodeFormatterConstants_FORMATTER_TAB_SIZE);
    this.formattingOptions
        .setTabSize(tabSizeString != null ? Integer.parseInt(tabSizeString) : PREFERENCE_DEFAULT_TAB_SIZE);
  }
}

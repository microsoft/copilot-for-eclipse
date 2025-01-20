package com.microsoft.copilot.eclipse.core.format;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.lsp4j.FormattingOptions;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;

/**
 * A class to provide the format options for the completion job.
 */
public class FormatOptionProvider {
  // A hashMap of project -> (languageExtension -> format)
  private Map<IProject, Map<String, FormattingOptions>> projectTolanguageIdToFormatMap;
  private Map<String, String> languageExtensionToIdMap;

  private static final String JAVA_LANGUAGE_ID = "java";
  private static final boolean DEFAULT_USE_SPACE = LanguageFormatReader.PREFERENCE_DEFAULT_TAB_CHAR.equals("space");
  private static final int DEFAULT_TAB_SIZE = LanguageFormatReader.PREFERENCE_DEFAULT_INDENTATION_SIZE;

  /**
   * Creates a new FormatOptionProvider.
   */
  public FormatOptionProvider() {
    initializeLanguageExtensionToIdMap();
    projectTolanguageIdToFormatMap = new HashMap<>();
  }

  private void initializeLanguageExtensionToIdMap() {
    languageExtensionToIdMap = new HashMap<>();
    languageExtensionToIdMap.put("java", JAVA_LANGUAGE_ID);
  }

  /**
   * Determines if indentation should use spaces. Copilot will attempt to retrieve the format options from the project
   * preferences. If the project preferences are not set, Copilot will use the workspace preferences. If the workspace
   * preferences are also not set, Copilot will default to using spaces.
   */
  public boolean useSpace(IFile file) {
    FormattingOptions languageFormat = getLanguageFormat(file);
    return languageFormat != null ? languageFormat.isInsertSpaces() : DEFAULT_USE_SPACE;
  }

  /**
   * Retrieves the tab size for indentation. Copilot first attempts to get the format options from the project
   * preferences. If the project preferences are not set, it will use the workspace preferences. If the workspace
   * preferences are also not set, it defaults to a tab size of 4.
   */
  public int getTabSize(IFile file) {
    FormattingOptions languageFormat = getLanguageFormat(file);
    return languageFormat != null ? languageFormat.getTabSize() : DEFAULT_TAB_SIZE;
  }

  /**
   * Helper method to get the LanguageFormat for a given file.
   */
  private FormattingOptions getLanguageFormat(IFile file) {
    if (file == null) {
      CopilotCore.LOGGER.log(LogLevel.INFO, "File is null");
      return null;
    }

    if (!file.exists() || !file.isAccessible()) {
      CopilotCore.LOGGER.log(LogLevel.INFO, "File is not valid: ", file.getName());
      return null;
    }

    IProject project = file.getProject();
    if (project == null) {
      CopilotCore.LOGGER.log(LogLevel.INFO, "Project is null for file: ", file.getName(),
          "default format will be applied.");
      return null;
    }

    String fileExtension = file.getFileExtension();
    if (StringUtils.isEmpty(fileExtension)) {
      CopilotCore.LOGGER.log(LogLevel.INFO, "File extension is null or empty for file: ", file.getName());
      return null;
    } else {
      fileExtension = fileExtension.toLowerCase();
    }

    String languageId = languageExtensionToIdMap.get(fileExtension);
    if (languageId == null) {
      languageId = "unknown";
      languageExtensionToIdMap.put(fileExtension, languageId);
      CopilotCore.LOGGER.log(LogLevel.INFO, "Language ID not found for extension: ", fileExtension);
    }

    Map<String, FormattingOptions> languageIdToFormatMap = projectTolanguageIdToFormatMap.computeIfAbsent(project,
        k -> new HashMap<>());
    if (!languageIdToFormatMap.containsKey(languageId)) {
      loadLanguageFormatPreferencesForProject(project, languageId);
    }

    FormattingOptions format = languageIdToFormatMap.get(languageId);
    if (format == null) {
      CopilotCore.LOGGER.log(LogLevel.INFO, "Format not found for extension: ", file.getName());
    }

    return format;
  }

  private void loadLanguageFormatPreferencesForProject(IProject project, String languageId) {
    switch (languageId) {
      case JAVA_LANGUAGE_ID:
        loadAndCacheJavaFormatPreferences(project, languageId);
        break;
      default:
        loadAndCacheDefaultFormatPreferences(project, languageId);
        CopilotCore.LOGGER.log(LogLevel.INFO, "Auto format unsupported for language ID: ", languageId);
        break;
    }
  }

  private void loadAndCacheJavaFormatPreferences(IProject project, String languageId) {
    JavaFormatReader reader = new JavaFormatReader(project);
    boolean insertSpaces = reader.getUseSpaces();
    int indentationSize = reader.getIndentSize();

    FormattingOptions format = new FormattingOptions(indentationSize, insertSpaces);
    cacheFormatPreferences(project, languageId, format);
  }

  private void loadAndCacheDefaultFormatPreferences(IProject project, String languageId) {
    FormattingOptions format = new FormattingOptions(DEFAULT_TAB_SIZE, DEFAULT_USE_SPACE);
    cacheFormatPreferences(project, languageId, format);
  }

  private void cacheFormatPreferences(IProject project, String languageId, FormattingOptions format) {
    Map<String, FormattingOptions> languageIdToFormatMap = projectTolanguageIdToFormatMap.get(project);
    languageIdToFormatMap.put(languageId, format);
    CopilotCore.LOGGER.log(LogLevel.INFO,
        String.format("A new language format for %s is added to the cache. tabSize: %s, insertSpaces: %s", languageId,
            format.getTabSize(), format.isInsertSpaces()));
  }
}
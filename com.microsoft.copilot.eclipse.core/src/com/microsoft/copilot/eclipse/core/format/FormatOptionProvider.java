package com.microsoft.copilot.eclipse.core.format;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.lsp4j.FormattingOptions;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * A class to provide the format options for the completion job.
 */
public class FormatOptionProvider {
  private Map<String, String> languageExtensionToIdMap;
  private Map<String, Map<IProject, LanguageFormatReader>> languageIdToProjectToLanguageFormatReaderMap;

  private static final String JAVA_LANGUAGE_ID = "java";
  private static final boolean DEFAULT_USE_SPACE = LanguageFormatReader.PREFERENCE_DEFAULT_TAB_CHAR.equals("space");
  private static final int DEFAULT_TAB_SIZE = LanguageFormatReader.PREFERENCE_DEFAULT_TAB_SIZE;

  /**
   * Creates a new FormatOptionProvider.
   */
  public FormatOptionProvider() {
    initializeLanguageExtensionToIdMap();
    languageIdToProjectToLanguageFormatReaderMap = new HashMap<>();
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
      CopilotCore.LOGGER.info("File is null");
      return null;
    }

    if (!file.exists() || !file.isAccessible()) {
      CopilotCore.LOGGER.info("File is not valid: " + file.getName());
      return null;
    }

    IProject project = file.getProject();
    if (project == null) {
      CopilotCore.LOGGER.info("Project is null for file: " + file.getName() + "default format will be applied.");
      return null;
    }

    String fileExtension = file.getFileExtension();
    if (StringUtils.isEmpty(fileExtension)) {
      CopilotCore.LOGGER.info("File extension is null or empty for file: " + file.getName());
      return null;
    } else {
      fileExtension = fileExtension.toLowerCase();
    }

    String languageId = languageExtensionToIdMap.get(fileExtension);
    if (languageId == null) {
      languageId = "unknown";
      languageExtensionToIdMap.put(fileExtension, languageId);
      CopilotCore.LOGGER.info("Language ID not found for extension: " + fileExtension);
    }

    Map<IProject, LanguageFormatReader> projectToLanguageFormatReaderMap = languageIdToProjectToLanguageFormatReaderMap
        .computeIfAbsent(languageId, k -> new HashMap<>());

    LanguageFormatReader languageFormatReaderfirProject = projectToLanguageFormatReaderMap.get(project);
    if (languageFormatReaderfirProject == null) {
      languageFormatReaderfirProject = loadFormatReaderForTheProject(languageId, project);
      if (languageFormatReaderfirProject == null) {
        return new FormattingOptions(DEFAULT_TAB_SIZE, DEFAULT_USE_SPACE);
      }
      projectToLanguageFormatReaderMap.put(project, languageFormatReaderfirProject);
    }

    return languageFormatReaderfirProject.getFormattingOptions();
  }

  private LanguageFormatReader loadFormatReaderForTheProject(String languageId, IProject project) {
    switch (languageId) {
      case JAVA_LANGUAGE_ID:
        return new JavaFormatReader(project);
      default:
        return null;
    }
  }
}
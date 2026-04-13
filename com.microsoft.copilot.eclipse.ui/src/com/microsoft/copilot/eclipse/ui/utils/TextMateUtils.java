package com.microsoft.copilot.eclipse.ui.utils;

import java.util.Map;

import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.registry.IGrammarRegistryManager;
import org.eclipse.tm4e.registry.TMEclipseRegistryPlugin;
import org.eclipse.tm4e.ui.TMUIPlugin;
import org.eclipse.tm4e.ui.text.TMPresentationReconciler;

/**
 * Utility class for TextMate related operations.
 */
public class TextMateUtils {
  private static final Map<String, String> LANGUAGE_ID_TO_EXTENSION = Map.ofEntries(
      Map.entry("javascript", "js"),
      Map.entry("typescript", "ts"),
      Map.entry("python", "py"),
      Map.entry("java", "java"),
      Map.entry("cpp", "cpp"),
      Map.entry("csharp", "cs"),
      Map.entry("html", "html"),
      Map.entry("css", "css"),
      Map.entry("json", "json"),
      Map.entry("markdown", "md"),
      Map.entry("sql", "sql"),
      Map.entry("go", "go"),
      Map.entry("rust", "rs"),
      Map.entry("php", "php"),
      Map.entry("ruby", "rb"),
      Map.entry("shellscript", "sh"),
      Map.entry("yaml", "yaml"),
      Map.entry("xml", "xml"));

  /**
   * Get or create a SourceViewerConfiguration for the given language.
   */
  public static SourceViewerConfiguration getConfiguration(String lang) {
    TMPresentationReconciler reconciler = new TMPresentationReconciler();
    IGrammarRegistryManager mgr = TMEclipseRegistryPlugin.getGrammarRegistryManager();
    IGrammar grammar = null;
    try {
      grammar = mgr.getGrammarForFileExtension(resolveFileExtension(lang));
    } catch (Throwable e) {
      // getGrammarForFileExtension not exist in org.eclipse.tm4e.registry versions 0.6.5 or earlier, skip the grammar
      // setting for eclipse 2023-12 or earlier.
    }
    reconciler.setGrammar(grammar);
    try {
      // getDefaultTheme with isDark parameter not exist in org.eclipse.tm4e.ui versions 0.6.5 or earlier, skip the
      // theme setting for eclipse 2023-12 or earlier.
      reconciler.setTheme(TMUIPlugin.getThemeManager().getDefaultTheme(UiUtils.isDarkTheme()));
    } catch (Throwable e) {
      reconciler.setTheme(TMUIPlugin.getThemeManager().getDefaultTheme());
    }
    return new SourceViewerConfiguration() {
      @Override
      public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
        return reconciler;
      }
    };
  }

  private static String resolveFileExtension(String languageOrExtension) {
    if (languageOrExtension == null) {
      return "";
    }

    String trimmed = languageOrExtension.trim();
    if (trimmed.isEmpty()) {
      return trimmed;
    }

    String normalized = trimmed.startsWith(".") && trimmed.length() > 1 ? trimmed.substring(1) : trimmed;
    String mapped = LANGUAGE_ID_TO_EXTENSION.get(normalized.toLowerCase());
    return mapped != null ? mapped : normalized;
  }
}

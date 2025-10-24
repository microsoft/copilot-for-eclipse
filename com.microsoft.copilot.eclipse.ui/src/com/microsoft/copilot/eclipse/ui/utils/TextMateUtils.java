package com.microsoft.copilot.eclipse.ui.utils;

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
  /**
   * Get or create a SourceViewerConfiguration for the given language.
   */
  public static SourceViewerConfiguration getConfiguration(String lang) {
    TMPresentationReconciler reconciler = new TMPresentationReconciler();
    IGrammarRegistryManager mgr = TMEclipseRegistryPlugin.getGrammarRegistryManager();
    IGrammar grammar = mgr.getGrammarForFileExtension(lang);
    reconciler.setGrammar(grammar);
    reconciler.setTheme(TMUIPlugin.getThemeManager().getDefaultTheme(UiUtils.isDarkTheme()));
    return new SourceViewerConfiguration() {
      @Override
      public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
        return reconciler;
      }
    };
  }
}

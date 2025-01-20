package com.microsoft.copilot.eclipse.core.format;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Interface for Copilot Language Format.
 */
abstract class LanguageFormatReader {

  protected IProject project;
  public static final int PREFERENCE_DEFAULT_TAB_SIZE = 4;
  public static final int PREFERENCE_DEFAULT_INDENTATION_SIZE = 2;
  public static final String PREFERENCE_DEFAULT_TAB_CHAR = "space";

  protected static final IScopeContext[] DEFAULT_SCOPE_CONTEXTS = new IScopeContext[] { InstanceScope.INSTANCE,
      ConfigurationScope.INSTANCE, DefaultScope.INSTANCE };

  protected LanguageFormatReader(IProject project) {
    this.project = project;
  }

  /**
   * Get if the language format is using spaces for indentation.
   */
  public abstract boolean getUseSpaces();

  /**
   * Get the indentation size for the language format. Be careful, for some languages, Eclipse uses indentation size for
   * space indentation and tab size for tab indentation.
   */
  public abstract int getIndentSize();

  /**
   * Fetch the language specific scope contexts based on if the project specific settings is enabled.
   */
  protected abstract IScopeContext[] getScopeContexts(IProject project);

  /**
   * Get the value of a preference from the scope contexts.
   *
   * @param contexts the scope contexts to search for the preference value.
   * @param qualifier a qualifier for the preference name.
   * @param key key whose associated value is to be returned.
   */
  protected String getFormatValue(IScopeContext[] contexts, String qualifier, String key) {
    String value = null;
    for (int i = 0; i < contexts.length; ++i) {
      value = contexts[i].getNode(qualifier).get(key, null);
      if (value != null) {
        value = value.trim();
        if (!value.isEmpty()) {
          return value;
        }
      }
    }

    return null;
  }

}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.format;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.lsp4j.FormattingOptions;

/**
 * Interface for Copilot Language Format.
 */
abstract class LanguageFormatReader implements IPreferenceChangeListener {
  public static final int PREFERENCE_DEFAULT_TAB_SIZE = 4;
  public static final String PREFERENCE_DEFAULT_TAB_CHAR = "space";

  protected final IPreferenceChangeListener preferencesChangeListener = this;
  protected static final IScopeContext[] DEFAULT_SCOPE_CONTEXTS = new IScopeContext[] { InstanceScope.INSTANCE,
      ConfigurationScope.INSTANCE, DefaultScope.INSTANCE };

  /**
   * Get the language format options.
   */
  public abstract FormattingOptions getFormattingOptions();

  /**
   * Fetch the language specific scope contexts based on if the project specific settings is enabled.
   */
  protected IScopeContext[] getScopeContexts(IProject project) {
    if (project == null) {
      return DEFAULT_SCOPE_CONTEXTS;
    } else {
      return new IScopeContext[] { new ProjectScope(project), InstanceScope.INSTANCE, ConfigurationScope.INSTANCE,
          DefaultScope.INSTANCE };
    }
  }

  /**
   * Register a preference change listener for the given project.
   *
   * @param project the project to register the preference change listener.
   * @param qualifier a qualifier for the preference name.
   */
  protected void registerPreferencesChangeListener(IProject project, String qualifier) {
    IScopeContext[] scopeContexts = getScopeContexts(project);
    for (IScopeContext context : scopeContexts) {
      IEclipsePreferences node = context.getNode(qualifier);
      node.addPreferenceChangeListener(this.preferencesChangeListener);
    }
  }

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

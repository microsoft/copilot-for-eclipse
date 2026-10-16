// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

class GeneralPreferencesPageTest {

  private Shell shell;
  private TestPreferencesPage page;

  @AfterEach
  void tearDown() {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (page != null) {
        page.dispose();
      }
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "https://company.ghe.com", "https://company.ghe.com/",
      "https://github.company.example:8443", "HTTPS://company.ghe.com", "HtTpS://company.ghe.com"})
  void testEnterpriseEndpoint_validStoredValue_pageIsValid(String endpoint) {
    SwtUtils.invokeOnDisplayThread(() -> {
      createPage(endpoint);
      assertTrue(page.isValid());
      assertNull(page.getErrorMessage());
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"http://company.ghe.com/", "company.ghe.com", "ftp://company.ghe.com",
      "https://", "https:///company.ghe.com", "https://company ghe.com", "https://company.ghe.com/%",
      " ", " https://company.ghe.com", "https://company.ghe.com "})
  void testEnterpriseEndpoint_invalidStoredValue_pageShowsError(String endpoint) {
    SwtUtils.invokeOnDisplayThread(() -> {
      createPage(endpoint);
      assertFalse(page.isValid());
      assertEquals(Messages.preferences_page_github_enterprise_validation_error, page.getErrorMessage());
    });
  }

  @Test
  void testEnterpriseEndpoint_editValue_validationUpdatesImmediately() {
    SwtUtils.invokeOnDisplayThread(() -> {
      createPage("");
      page.endpointField.setStringValue("http://company.ghe.com/");
      assertFalse(page.isValid());
      assertEquals(Messages.preferences_page_github_enterprise_validation_error, page.getErrorMessage());

      page.endpointField.setStringValue("https://company.ghe.com/");
      assertTrue(page.isValid());
      assertNull(page.getErrorMessage());

      page.endpointField.setStringValue("");
      assertTrue(page.isValid());
      assertNull(page.getErrorMessage());
    });
  }

  private void createPage(String endpoint) {
    shell = new Shell(Display.getDefault());
    PreferenceStore store = new PreferenceStore();
    store.setValue(Constants.GITHUB_ENTERPRISE, endpoint);
    page = new TestPreferencesPage();
    page.setPreferenceStore(store);
    page.createControl(shell);
  }

  private static class TestPreferencesPage extends GeneralPreferencesPage {
    private StringFieldEditor endpointField;

    @Override
    protected void addField(FieldEditor editor) {
      super.addField(editor);
      if (Constants.GITHUB_ENTERPRISE.equals(editor.getPreferenceName())) {
        endpointField = (StringFieldEditor) editor;
      }
    }
  }
}

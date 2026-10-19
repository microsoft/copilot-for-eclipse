// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
  @CsvSource({
      "'', ''",
      "'   ', ''",
      "company.ghe.com, https://company.ghe.com",
      "http://company.ghe.com/, https://company.ghe.com/",
      "https://company.ghe.com, https://company.ghe.com",
      "HTTPS://company.ghe.com, https://company.ghe.com",
      "' https://company.ghe.com ', https://company.ghe.com",
      "github.company.example:8443/copilot%2Flogin, https://github.company.example:8443/copilot%2Flogin",
      "http://company.ghe.com/login?a=1&b=2, https://company.ghe.com/login?a=1&b=2"
  })
  void testEnterpriseEndpoint_validStoredValue_previewsAndStoresHttps(String endpoint, String expected) {
    SwtUtils.invokeOnDisplayThread(() -> {
      createPage(endpoint);
      assertTrue(page.isValid());
      assertNull(page.getErrorMessage());
      assertPreview(expected);

      page.endpointField.store();
      assertEquals(expected, page.getPreferenceStore().getString(Constants.GITHUB_ENTERPRISE));
      assertEquals(expected, page.endpointField.getStringValue());
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"ftp://company.ghe.com", "http://", "company ghe.com",
      "https://", "https:///company.ghe.com", "https://company ghe.com", "https://company.ghe.com/%",
      "company.ghe.com:invalid"})
  void testEnterpriseEndpoint_invalidStoredValue_pageShowsError(String endpoint) {
    SwtUtils.invokeOnDisplayThread(() -> {
      createPage(endpoint);
      assertFalse(page.isValid());
      assertEquals(Messages.preferences_page_github_enterprise_validation_error, page.getErrorMessage());
      assertNotNull(findLabel((Composite) page.getControl(),
          Messages.preferences_page_github_enterprise_validation_error));
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "https://company.ghe.com", "http://company.ghe.com", "company.ghe.com"})
  void testEnterpriseEndpoint_nativeHints_preserveValueUntilSave(String endpoint) {
    SwtUtils.invokeOnDisplayThread(() -> {
      createPage(endpoint);
      Text endpointText = findTextControl((Composite) page.getControl());
      assertNotNull(endpointText);
      assertEquals(Messages.preferences_page_github_enterprise_placeholder, endpointText.getMessage());
      assertEquals(Messages.preferences_page_github_enterprise_tooltip, endpointText.getToolTipText());
      assertEquals(endpoint, endpointText.getText());

      assertEquals(endpoint, page.getPreferenceStore().getString(Constants.GITHUB_ENTERPRISE));
    });
  }

  @Test
  void testEnterpriseEndpoint_editValue_validationUpdatesImmediately() {
    SwtUtils.invokeOnDisplayThread(() -> {
      createPage("");
      page.endpointField.setStringValue("http://company.ghe.com/");
      assertTrue(page.isValid());
      assertNull(page.getErrorMessage());
      assertPreview("https://company.ghe.com/");

      page.endpointField.setStringValue("ftp://company.ghe.com/");
      assertFalse(page.isValid());
      assertEquals(Messages.preferences_page_github_enterprise_validation_error, page.getErrorMessage());

      page.endpointField.setStringValue("company.ghe.com");
      assertTrue(page.isValid());
      assertNull(page.getErrorMessage());
      assertPreview("https://company.ghe.com");

      page.endpointField.setStringValue("");
      assertTrue(page.isValid());
      assertNull(page.getErrorMessage());
      assertPreview("");
    });
  }

  @Test
  void testEnterpriseEndpoint_restoreDefaults_resetsValidationAndPreview() {
    SwtUtils.invokeOnDisplayThread(() -> {
      createPage("ftp://company.ghe.com");
      assertFalse(page.isValid());

      page.restoreDefaults();
      assertTrue(page.isValid());
      assertNull(page.getErrorMessage());
      assertEquals("", page.endpointField.getStringValue());
      assertPreview("");
    });
  }

  private void assertPreview(String endpoint) {
    String expected = endpoint.isEmpty() ? Messages.preferences_page_github_enterprise_default
        : NLS.bind(Messages.preferences_page_github_enterprise_preview, endpoint.replace("&", "&&"));
    assertNotNull(findLabel((Composite) page.getControl(), expected));
  }

  private void createPage(String endpoint) {
    shell = new Shell(Display.getDefault());
    PreferenceStore store = new PreferenceStore();
    store.setValue(Constants.GITHUB_ENTERPRISE, endpoint);
    page = new TestPreferencesPage();
    page.setPreferenceStore(store);
    page.createControl(shell);
  }

  private Text findTextControl(Composite parent) {
    for (Control child : parent.getChildren()) {
      if (child instanceof Text text) {
        return text;
      }
      if (child instanceof Composite composite) {
        Text text = findTextControl(composite);
        if (text != null) {
          return text;
        }
      }
    }
    return null;
  }

  private Label findLabel(Composite parent, String text) {
    for (Control child : parent.getChildren()) {
      if (child instanceof Label label && text.equals(label.getText())) {
        return label;
      }
      if (child instanceof Composite composite) {
        Label label = findLabel(composite, text);
        if (label != null) {
          return label;
        }
      }
    }
    return null;
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

    private void restoreDefaults() {
      performDefaults();
    }
  }
}

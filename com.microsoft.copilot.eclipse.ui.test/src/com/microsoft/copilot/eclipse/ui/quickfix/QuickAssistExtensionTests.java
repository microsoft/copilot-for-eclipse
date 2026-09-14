// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.quickfix;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.junit.jupiter.api.Test;

class QuickAssistExtensionTests {

  @Test
  void registersJavaQuickAssistProcessor() {
    IConfigurationElement element = findProcessor("org.eclipse.jdt.ui.quickAssistProcessors",
        "com.microsoft.copilot.eclipse.ui.quickfix.JavaCopilotQuickAssistProcessor");

    assertNotNull(element);
  }

  private IConfigurationElement findProcessor(String extensionPointId, String className) {
    IConfigurationElement element = Arrays.stream(
        Platform.getExtensionRegistry().getConfigurationElementsFor(extensionPointId))
        .filter(candidate -> className.equals(candidate.getAttribute("class")))
        .findFirst()
        .orElse(null);
    assertNotNull(element, () -> "Missing quick assist processor registration for " + className);
    return element;
  }
}

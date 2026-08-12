// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.quickfix;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.junit.jupiter.api.Test;

class QuickAssistExtensionTests {

  @Test
  void registersJavaQuickAssistProcessor() throws Exception {
    IConfigurationElement element = findProcessor("org.eclipse.jdt.ui.quickAssistProcessors",
        JavaCopilotQuickAssistProcessor.class.getName());

    assertInstanceOf(JavaCopilotQuickAssistProcessor.class, element.createExecutableExtension("class"));
  }

  @Test
  void hidesProposalWhenCopilotIsUnavailable() throws Exception {
    JavaCopilotQuickAssistProcessor processor = new JavaCopilotQuickAssistProcessor(() -> false);

    assertFalse(processor.hasAssists(null));
    assertNull(processor.getAssists(null, null));
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

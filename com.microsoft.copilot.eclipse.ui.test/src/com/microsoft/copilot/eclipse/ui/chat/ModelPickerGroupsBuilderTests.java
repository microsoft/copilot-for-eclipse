// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.ui.swt.DropdownItemGroup;

class ModelPickerGroupsBuilderTests {

  @Test
  void testBuild_selectedLabelIncludesContextWindowAndReasoningEffort() {
    CopilotModel model = new CopilotModel();
    model.setModelName("gpt-5");

    List<DropdownItemGroup> groups = ModelPickerGroupsBuilder.build(Map.of("gpt-5", model), false, false,
        ignored -> "high", ignored -> "1M");

    assertEquals("gpt-5 - 1M - High", groups.get(0).getItems().get(0).getSelectedLabel());
  }
}

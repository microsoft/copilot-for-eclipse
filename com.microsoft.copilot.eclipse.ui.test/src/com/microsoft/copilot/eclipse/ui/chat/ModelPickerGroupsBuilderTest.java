// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.ui.swt.DropdownItem;
import com.microsoft.copilot.eclipse.ui.swt.DropdownItemGroup;

class ModelPickerGroupsBuilderTest {

  private static CopilotModel buildNativeModel(String id, String name) {
    CopilotModel model = new CopilotModel();
    model.setId(id);
    model.setModelName(name);
    model.setScopes(List.of(CopilotScope.CHAT_PANEL, CopilotScope.AGENT_PANEL));
    return model;
  }

  private static CopilotModel buildByokModel(String id, String name, String provider) {
    CopilotModel model = new CopilotModel();
    model.setId(id);
    model.setModelName(name);
    model.setProviderName(provider);
    model.setScopes(List.of(CopilotScope.CHAT_PANEL, CopilotScope.AGENT_PANEL));
    return model;
  }

  @Test
  void twoModelsWithSameNameProduceDistinctDropdownItemIds() {
    CopilotModel nativeModel = buildNativeModel("gpt-4", "GPT-4");
    CopilotModel byokModel = buildByokModel("gpt-4", "GPT-4", "Azure");

    Map<String, CopilotModel> modelMap = new HashMap<>();
    modelMap.put(nativeModel.getModelKey(), nativeModel);
    modelMap.put(byokModel.getModelKey(), byokModel);

    List<DropdownItemGroup> groups = ModelPickerGroupsBuilder.build(modelMap, false, false, null);

    List<String> ids = groups.stream()
        .flatMap(g -> g.getItems().stream())
        .map(DropdownItem::getId)
        .collect(Collectors.toList());

    assertEquals(2, ids.size(), "Expected two dropdown items for two models");
    assertNotEquals(ids.get(0), ids.get(1), "Items sharing a model name must have distinct IDs");
    assertEquals(nativeModel.getModelKey(), ids.stream()
        .filter(id -> id.equals(nativeModel.getModelKey())).findFirst().orElse(null));
    assertEquals(byokModel.getModelKey(), ids.stream()
        .filter(id -> id.equals(byokModel.getModelKey())).findFirst().orElse(null));
  }

  @Test
  void dropdownItemIdIsModelKeyNotModelName() {
    CopilotModel model = buildNativeModel("claude-3-5-sonnet", "Claude 3.5 Sonnet");

    Map<String, CopilotModel> modelMap = new HashMap<>();
    modelMap.put(model.getModelKey(), model);

    List<DropdownItemGroup> groups = ModelPickerGroupsBuilder.build(modelMap, false, false, null);

    List<DropdownItem> items = groups.stream()
        .flatMap(g -> g.getItems().stream())
        .collect(Collectors.toList());

    assertEquals(1, items.size());
    assertEquals(model.getModelKey(), items.get(0).getId());
    assertEquals(model.getModelName(), items.get(0).getLabel());
  }
}

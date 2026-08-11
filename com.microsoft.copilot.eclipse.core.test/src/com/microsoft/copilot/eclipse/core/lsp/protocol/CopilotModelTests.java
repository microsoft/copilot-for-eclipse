// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelCustomModel;

/**
 * Tests for {@link CopilotModel}, focusing on the {@code customModel} metadata that carries organization- and
 * enterprise-contributed custom (BYOK) models exposed through {@code copilot/models}.
 */
class CopilotModelTests {

  private final Gson gson = new Gson();

  @Test
  void testDeserialize_populatesCustomModelMetadata() {
    String json = """
        {
          "modelFamily": "custom",
          "modelName": "Sonnet (Org)",
          "id": "claude-sonnet-org",
          "scopes": ["chat-panel", "agent-panel"],
          "customModel": {
            "keyName": "Contoso Azure Key",
            "ownerName": "Contoso",
            "ownerType": "organization",
            "provider": "azure"
          }
        }
        """;

    CopilotModel model = gson.fromJson(json, CopilotModel.class);

    assertNotNull(model.getCustomModel());
    assertEquals("Contoso Azure Key", model.getCustomModel().keyName());
    assertEquals("Contoso", model.getCustomModel().ownerName());
    assertEquals("organization", model.getCustomModel().ownerType());
    assertEquals("azure", model.getCustomModel().provider());
  }

  @Test
  void testDeserialize_customModelAbsentIsNull() {
    String json = """
        {
          "modelFamily": "gpt-4o",
          "modelName": "GPT-4o",
          "id": "gpt-4o",
          "scopes": ["chat-panel"]
        }
        """;

    CopilotModel model = gson.fromJson(json, CopilotModel.class);

    assertNull(model.getCustomModel());
  }

  @Test
  void testEqualsAndHashCode_accountForCustomModel() {
    CopilotModel base = new CopilotModel();
    base.setId("claude-sonnet-org");
    base.setModelName("Sonnet (Org)");
    base.setCustomModel(new CopilotModelCustomModel("Contoso Azure Key", "Contoso", "organization", "azure"));

    CopilotModel same = new CopilotModel();
    same.setId("claude-sonnet-org");
    same.setModelName("Sonnet (Org)");
    same.setCustomModel(new CopilotModelCustomModel("Contoso Azure Key", "Contoso", "organization", "azure"));

    CopilotModel differentOwner = new CopilotModel();
    differentOwner.setId("claude-sonnet-org");
    differentOwner.setModelName("Sonnet (Org)");
    differentOwner.setCustomModel(new CopilotModelCustomModel("Contoso Azure Key", "Fabrikam", "organization",
        "azure"));

    assertEquals(base, same);
    assertEquals(base.hashCode(), same.hashCode());
    assertNotEquals(base, differentOwner);
  }
}

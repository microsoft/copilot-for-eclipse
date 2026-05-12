// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import com.google.gson.annotations.SerializedName;

/**
 * Source of a conversation template.
 */
public enum TemplateSource {
  @SerializedName("builtin")
  BUILTIN,

  @SerializedName("prompt")
  PROMPT,

  @SerializedName("skill")
  SKILL
}

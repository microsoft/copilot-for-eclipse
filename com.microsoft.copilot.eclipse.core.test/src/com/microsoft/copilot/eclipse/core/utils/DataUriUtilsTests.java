// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class DataUriUtilsTests {

  @Test
  void toDataUri_encodesBase64WithMimeType() {
    byte[] content = {10, 20, 30, 40};

    String dataUri = DataUriUtils.toDataUri(content, "image/png");

    String prefix = "data:image/png;base64,";
    assertTrue(dataUri.startsWith(prefix), "unexpected data URI: " + dataUri);
    byte[] decoded = Base64.getDecoder().decode(dataUri.substring(prefix.length()));
    assertArrayEquals(content, decoded);
  }

  @Test
  void toDataUri_nullData_returnsEmptyString() {
    assertEquals("", DataUriUtils.toDataUri(null, "image/png"));
  }
}

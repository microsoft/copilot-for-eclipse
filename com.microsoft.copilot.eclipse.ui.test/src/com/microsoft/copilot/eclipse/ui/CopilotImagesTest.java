// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifies that every {@code IMG_*} constant in {@link CopilotImages} refers to a bundle resource
 * that actually exists on the classpath.
 *
 * <p>This test is independent of a running Display or plugin registry — it only needs the bundle's
 * classpath. It guards against typos in icon paths and missing files caused by renames or deletions.
 * New constants are discovered automatically via reflection; no separate list needs to be maintained.
 */
class CopilotImagesTest {

  static Stream<Arguments> iconPaths() {
    return Stream.of(CopilotImages.class.getDeclaredFields())
        .filter(f -> f.getName().startsWith("IMG_"))
        .map(f -> {
          try {
            f.setAccessible(true);
            return Arguments.of(f.getName(), f.get(null));
          } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read constant: " + f.getName(), e);
          }
        });
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("iconPaths")
  void testIconFileExists(String fieldName, String path) {
    assertNotNull(CopilotImages.class.getResource("/" + path),
        "Bundle resource not found for constant " + fieldName + ": " + path);
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.jobs;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifies that every public constant in {@link CopilotJobsImages} refers to a bundle resource
 * that actually exists on the classpath.
 *
 * <p>New constants are discovered automatically via reflection; no separate list needs to be
 * maintained.
 */
class CopilotJobsImagesTest {

  static Stream<Arguments> iconPaths() {
    return Stream.of(CopilotJobsImages.class.getDeclaredFields())
        .filter(f -> f.getName().startsWith("IMG_"))
        .map(f -> {
          try {
            return Arguments.of(f.getName(), f.get(null));
          } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read constant: " + f.getName(), e);
          }
        });
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("iconPaths")
  void testIconFileExists(String fieldName, String path) {
    assertNotNull(CopilotJobsImages.class.getResource("/" + path),
        "Bundle resource not found for constant " + fieldName + ": " + path);
  }
}

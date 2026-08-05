// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.jobs;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.stream.Stream;

import org.eclipse.core.runtime.Platform;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.osgi.framework.Bundle;

/**
 * Verifies that every public image path in the Jobs bundle refers to an existing resource without
 * activating the bundle.
 */
class CopilotJobsImagesTest {

  private static final String JOBS_BUNDLE_ID = "com.microsoft.copilot.eclipse.ui.jobs";

  static Stream<Arguments> iconPaths() {
    return Stream.of(
        Arguments.of("IMG_REPO", "icons/repo.png"),
        Arguments.of("IMG_INFORMATION", "icons/information.png"),
        Arguments.of("IMG_STATUS_LOADING", "icons/status/loading.png"),
        Arguments.of("IMG_STATUS_COMPLETE", "icons/status/complete.png"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("iconPaths")
  void testIconFileExists(String fieldName, String path) {
    Bundle jobsBundle = Platform.getBundle(JOBS_BUNDLE_ID);
    assertNotNull(jobsBundle, "Jobs bundle not found");
    assertNotNull(jobsBundle.getEntry(path),
        "Bundle resource not found for constant " + fieldName + ": " + path);
    assertNotEquals(Bundle.ACTIVE, jobsBundle.getState(),
        "Checking image resources must not activate the Jobs bundle");
  }
}

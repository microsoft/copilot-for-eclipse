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
 * Verifies that the curated public image paths from {@code CopilotJobsImages} refer to existing
 * resources without activating the Jobs bundle.
 *
 * <p>The paths are duplicated here because loading {@code CopilotJobsImages} for reflection would
 * activate its bundle and start the Copilot language server. Keep this list in sync with its public
 * {@code IMG_*} constants.
 */
class CopilotJobsImagesTest {

  private static final String CORE_BUNDLE_ID = "com.microsoft.copilot.eclipse.core";
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
    Bundle coreBundle = Platform.getBundle(CORE_BUNDLE_ID);
    assertNotNull(jobsBundle, "Jobs bundle not found");
    assertNotNull(coreBundle, "Core bundle not found");
    assertBundleNotActive(jobsBundle, "Jobs");
    assertBundleNotActive(coreBundle, "Core");
    assertNotNull(jobsBundle.getEntry(path),
        "Bundle resource not found for constant " + fieldName + ": " + path);
    assertBundleNotActive(jobsBundle, "Jobs");
    assertBundleNotActive(coreBundle, "Core");
  }

  private static void assertBundleNotActive(Bundle bundle, String name) {
    // Tycho lazy-starts dependencies, leaving them in STARTING without invoking their activators.
    assertNotEquals(Bundle.ACTIVE, bundle.getState(),
        "Checking image resources must not activate the " + name + " bundle");
  }
}

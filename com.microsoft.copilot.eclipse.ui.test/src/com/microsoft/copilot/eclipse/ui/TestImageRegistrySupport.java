// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;

/**
 * Test helper that builds a real {@link ImageRegistry} populated with the Copilot UI bundle icons.
 *
 * <p>Lives in the {@code com.microsoft.copilot.eclipse.ui} package so it can call the package-private
 * {@link CopilotImages#initialize(ImageRegistry)}. The test bundle is a fragment of the UI bundle, so
 * this class shares the host package at runtime.
 */
public final class TestImageRegistrySupport {

  private TestImageRegistrySupport() {
    // prevent instantiation
  }

  /**
   * Creates an {@link ImageRegistry} pre-populated with all Copilot UI icon descriptors. Must be
   * called on the UI thread.
   *
   * @return a populated image registry; callers own it and must {@link ImageRegistry#dispose()} it
   */
  public static ImageRegistry createCopilotImageRegistry() {
    ImageRegistry registry = new ImageRegistry(Display.getDefault());
    CopilotImages.initialize(registry);
    return registry;
  }
}

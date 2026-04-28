// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.nes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.texteditor.AnnotationTypeLookup;
import org.junit.jupiter.api.Test;

class NesAnnotationTypeMappingTest {

  private static final String ANNOTATION_TYPES_EXTENSION_POINT = "org.eclipse.ui.editors.annotationTypes";
  private static final String FOREIGN_TEXT_MARKER =
      "com.microsoft.copilot.eclipse.ui.test.foreignTextMarker";
  private static final String NES_ANNOTATION_PREFIX = "com.microsoft.copilot.eclipse.ui.nes.";
  private static final String NES_CHANGE_ANNOTATION = NES_ANNOTATION_PREFIX + "change";
  private static final String NES_DELETE_ANNOTATION = NES_ANNOTATION_PREFIX + "delete";
  private static final String ROOT_TEXT_MARKER = "org.eclipse.core.resources.textmarker";

  @Test
  void testForeignTextMarkerSubtypeDoesNotResolveToNesAnnotations() {
    AnnotationTypeLookup lookup = new AnnotationTypeLookup();

    String annotationType = lookup.getAnnotationType(FOREIGN_TEXT_MARKER, IMarker.SEVERITY_INFO);

    assertNotEquals(NES_CHANGE_ANNOTATION, annotationType);
    assertNotEquals(NES_DELETE_ANNOTATION, annotationType);
  }

  @Test
  void testNesAnnotationTypesDoNotMapToRootTextMarker() {
    IExtensionPoint extensionPoint = Platform.getExtensionRegistry()
        .getExtensionPoint(ANNOTATION_TYPES_EXTENSION_POINT);
    assertNotNull(extensionPoint);
    assertTrue(hasNesAnnotationType(extensionPoint, NES_CHANGE_ANNOTATION));
    assertTrue(hasNesAnnotationType(extensionPoint, NES_DELETE_ANNOTATION));
    assertFalse(hasNesRootTextMarkerMapping(extensionPoint));
  }

  private boolean hasNesAnnotationType(IExtensionPoint extensionPoint, String annotationType) {
    for (IConfigurationElement element : extensionPoint.getConfigurationElements()) {
      if ("type".equals(element.getName()) && annotationType.equals(element.getAttribute("name"))) {
        return true;
      }
    }
    return false;
  }

  private boolean hasNesRootTextMarkerMapping(IExtensionPoint extensionPoint) {
    for (IConfigurationElement element : extensionPoint.getConfigurationElements()) {
      String annotationType = element.getAttribute("name");
      if ("type".equals(element.getName()) && annotationType != null
          && annotationType.startsWith(NES_ANNOTATION_PREFIX)
          && ROOT_TEXT_MARKER.equals(element.getAttribute("markerType"))) {
        return true;
      }
    }
    return false;
  }
}

package com.microsoft.copilot.eclipse.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.Platform;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

class IdeCapabilitiesTests {

  @Test
  void testCanUseCodeMiningWithNewerVersion() {
    try (MockedStatic<Platform> platformMock = mockStatic(Platform.class)) {
      // Mock bundle with version 4.35.0 (newer than required 4.34.0)
      Bundle bundleMock = org.mockito.Mockito.mock(Bundle.class);
      when(bundleMock.getVersion()).thenReturn(new Version(4, 35, 0));
      when(Platform.getBundle("org.eclipse.platform")).thenReturn(bundleMock);

      // Assert that code mining should be available
      assertTrue(IdeCapabilities.canUseCodeMining());
    }
  }

  @Test
  void testCanUseCodeMiningWithExactRequiredVersion() {
    try (MockedStatic<Platform> platformMock = mockStatic(Platform.class)) {
      // Mock bundle with version 4.34.0 (exactly the required version)
      Bundle bundleMock = org.mockito.Mockito.mock(Bundle.class);
      when(bundleMock.getVersion()).thenReturn(new Version(4, 34, 0));
      when(Platform.getBundle("org.eclipse.platform")).thenReturn(bundleMock);

      // Assert that code mining should be available
      assertTrue(IdeCapabilities.canUseCodeMining());
    }
  }

  @Test
  void testCanUseCodeMiningWithOlderVersion() {
    try (MockedStatic<Platform> platformMock = mockStatic(Platform.class)) {
      // Mock bundle with version 4.33.0 (older than required 4.34.0)
      Bundle bundleMock = org.mockito.Mockito.mock(Bundle.class);
      when(bundleMock.getVersion()).thenReturn(new Version(4, 33, 0));
      when(Platform.getBundle("org.eclipse.platform")).thenReturn(bundleMock);

      // Assert that code mining should not be available
      assertFalse(IdeCapabilities.canUseCodeMining());
    }
  }

  @Test
  void testCanUseCodeMiningWithMuchOlderVersion() {
    try (MockedStatic<Platform> platformMock = mockStatic(Platform.class)) {
      // Mock bundle with version 3.8.0 (much older than required 4.34.0)
      Bundle bundleMock = org.mockito.Mockito.mock(Bundle.class);
      when(bundleMock.getVersion()).thenReturn(new Version(3, 8, 0));
      when(Platform.getBundle("org.eclipse.platform")).thenReturn(bundleMock);

      // Assert that code mining should not be available
      assertFalse(IdeCapabilities.canUseCodeMining());
    }
  }
}
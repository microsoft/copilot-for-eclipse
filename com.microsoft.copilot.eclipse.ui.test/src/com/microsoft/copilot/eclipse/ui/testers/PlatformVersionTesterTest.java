package com.microsoft.copilot.eclipse.ui.testers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.eclipse.core.runtime.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Test class for PlatformVersionTester
 */
@ExtendWith(MockitoExtension.class)
class PlatformVersionTesterTest {

  private static final String PLATFORM_VERSION = "platformVersion";
  private PlatformVersionTester tester;
  private Bundle mockBundle;

  @BeforeEach
  void setUp() {
    tester = new PlatformVersionTester();
    mockBundle = mock(Bundle.class);
  }

  /**
   * Test case for "le:4.36" (less than or equal to 4.36) Tests the scenario where current platform version is 4.35,
   * which should return true for le:4.36
   */
  @Test
  void testPlatformVersionLessOrEqualTo() {
    // Given: Platform version is 4.35.0 (less than 4.36)
    Version currentVersion = new Version(4, 35, 0);
    when(mockBundle.getVersion()).thenReturn(currentVersion);

    try (MockedStatic<Platform> platformMock = mockStatic(Platform.class)) {
      platformMock.when(() -> Platform.getBundle("org.eclipse.platform")).thenReturn(mockBundle);

      // When: Testing le:4.36
      String[] args = { "le:4.36" };
      boolean result = tester.test(null, PLATFORM_VERSION, args, null);

      // Then: Should return true (4.35 <= 4.36)
      assertTrue(result, "Platform version 4.35 should be less than or equal to 4.36");
    }
  }

  /**
   * Test case for "gt:4.36" (greater than 4.36) Tests the scenario where current platform version is 4.37, which should
   * return true for gt:4.36
   */
  @Test
  void testPlatformVersionGreaterThan() {
    // Given: Platform version is 4.37.0 (greater than 4.36)
    Version currentVersion = new Version(4, 37, 0);
    when(mockBundle.getVersion()).thenReturn(currentVersion);

    try (MockedStatic<Platform> platformMock = mockStatic(Platform.class)) {
      platformMock.when(() -> Platform.getBundle("org.eclipse.platform")).thenReturn(mockBundle);

      // When: Testing gt:4.36
      String[] args = { "gt:4.36" };
      boolean result = tester.test(null, PLATFORM_VERSION, args, null);

      // Then: Should return true (4.37 > 4.36)
      assertTrue(result, "Platform version 4.37 should be greater than 4.36");
    }
  }

  /**
   * Test case for "eq:4.36" (equal to 4.36) Tests the scenario where current platform version is exactly 4.36, which
   * should return true for eq:4.36
   */
  @Test
  void testPlatformVersionEqualTo() {
    // Given: Platform version is exactly 4.36.0
    Version currentVersion = new Version(4, 36, 0);
    when(mockBundle.getVersion()).thenReturn(currentVersion);

    try (MockedStatic<Platform> platformMock = mockStatic(Platform.class)) {
      platformMock.when(() -> Platform.getBundle("org.eclipse.platform")).thenReturn(mockBundle);

      // When: Testing eq:4.36
      String[] args = { "eq:4.36" };
      boolean result = tester.test(null, PLATFORM_VERSION, args, null);

      // Then: Should return true (4.36 == 4.36)
      assertTrue(result, "Platform version 4.36 should be equal to 4.36");
    }
  }

  /**
   * Additional test to verify that micro and qualifier versions are ignored
   */
  @Test
  void testMicroAndQualifierIgnored() {
    // Given: Platform version is 4.36.1.qualifier (should be treated as 4.36.0)
    Version currentVersion = new Version(4, 36, 1, "qualifier");
    when(mockBundle.getVersion()).thenReturn(currentVersion);

    try (MockedStatic<Platform> platformMock = mockStatic(Platform.class)) {
      platformMock.when(() -> Platform.getBundle("org.eclipse.platform")).thenReturn(mockBundle);

      // When: Testing eq:4.36
      String[] args = { "eq:4.36" };
      boolean result = tester.test(null, PLATFORM_VERSION, args, null);

      // Then: Should return true (4.36.1.qualifier treated as 4.36.0 == 4.36.0)
      assertTrue(result, "Platform version 4.36.1.qualifier should be treated as 4.36 and equal to 4.36");
    }
  }

  /**
   * Test invalid property name
   */
  @Test
  void testInvalidProperty() {
    String[] args = { "eq:4.36" };
    boolean result = tester.test(null, "invalidProperty", args, null);
    assertFalse(result, "Should return false for invalid property name");
  }

  /**
   * Test invalid argument format
   */
  @Test
  void testInvalidArgFormat() {
    String[] args = { "invalid_format" };
    boolean result = tester.test(null, PLATFORM_VERSION, args, null);
    assertFalse(result, "Should return false for invalid argument format");
  }
}
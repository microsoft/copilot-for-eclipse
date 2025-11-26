package com.microsoft.copilot.eclipse.ui.testers;

import org.eclipse.core.expressions.PropertyTester;

import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;

/**
 * Property tester for Copilot-specific conditions.
 */
public class IsCopilotNightlyTester extends PropertyTester {

  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    if ("isNightlyCopilotPlugin".equals(property)) {
      return PlatformUtils.isNightly();
    }
    return false;
  }
}

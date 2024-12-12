package com.microsoft.copilot.eclipse.ui.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * Message class for the i18n.
 */
public final class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.microsoft.copilot.eclipse.ui.i18n.messages"; //$NON-NLS-1$
  public static String INFO_signToGitHub;
  public static String INFO_signOutFromGitHub;
  
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
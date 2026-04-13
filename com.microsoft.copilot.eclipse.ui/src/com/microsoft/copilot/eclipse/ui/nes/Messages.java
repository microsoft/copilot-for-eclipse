package com.microsoft.copilot.eclipse.ui.nes;

import org.eclipse.osgi.util.NLS;

/**
 * Messages for NES (Next Edit Suggestion) feature.
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.microsoft.copilot.eclipse.ui.nes.messages"; //$NON-NLS-1$

  public static String bottomBar_jumpMessage;
  public static String bottomBar_press;
  public static String actionMenu_accept;
  public static String actionMenu_reject;


  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}

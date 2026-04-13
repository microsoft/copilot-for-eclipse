package com.microsoft.copilot.eclipse.ui.jobs.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * Message class for the i18n.
 */
public final class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.microsoft.copilot.eclipse.ui.jobs.i18n.messages"; //$NON-NLS-1$

  public static String jobsView_toolTip_refreshAgentJobs;
  public static String jobsView_toolTip_collapseAll;
  public static String jobsView_toolTip_expandAll;
  public static String jobsView_toolTip_pullRequest;
  public static String jobsView_label_loadingAgentJobs;
  public static String jobsView_label_noOpenProjects;
  public static String jobsView_label_copilotNotInitialized;
  public static String jobsView_label_noAgentJobsFound;
  public static String jobsView_label_draftPrefix;
  public static String jobsView_job_loadingPullRequests;
  public static String jobsView_job_loadingPullRequestsForProjects;
  public static String jobsView_job_loadingPRsForProject;
  public static String jobsView_error_loadingPullRequests;
  public static String jobsView_error_searchingPRsForProject;
  public static String jobsView_error_loadingPRsForProject;
  public static String jobsView_error_languageServerNotAvailable;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
    // prevent instantiation
  }
}

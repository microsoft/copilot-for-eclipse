package com.microsoft.copilot.eclipse.ui.jobs.views;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.githubapi.GitHubPullRequestItem;
import com.microsoft.copilot.eclipse.core.lsp.protocol.githubapi.SearchPrParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.githubapi.SearchPrResponse;
import com.microsoft.copilot.eclipse.core.utils.WorkspaceUtils;
import com.microsoft.copilot.eclipse.ui.jobs.events.JobsViewEvents;
import com.microsoft.copilot.eclipse.ui.jobs.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.jobs.utils.UiUtils;

/**
 * The view to display coding agent jobs with PRs from GitHub. Shows a 2-level
 * hierarchy: Repository -> Pull Requests
 */
public class JobsView {
  private static final String SEARCH_PR_QUERY = 
      "repo:${owner}/${repository} is:open author:@copilot involves:${user} is:pull-request";
  private static final Set<String> SPECIAL_STATUS_LABELS = Set.of(Messages.jobsView_label_noOpenProjects,
      Messages.jobsView_label_copilotNotInitialized, Messages.jobsView_label_noAgentJobsFound,
      Messages.jobsView_label_loadingAgentJobs);

  @Inject
  private UISynchronize sync;

  private TreeViewer treeViewer;
  private Map<String, Either<List<GitHubPullRequestItem>, String>> repoToPullRequestsMap = Collections
      .synchronizedMap(new LinkedHashMap<>());
  private Map<String, IProject> projectNameToProjectMap = Collections.synchronizedMap(new LinkedHashMap<>());

  private Image directoryIcon;
  private Image informationIcon;

  /**
   * Create the view part control.
   */
  @PostConstruct
  public void createPartControl(Composite parent) {
    parent.setLayout(new GridLayout(1, false));

    this.directoryIcon = UiUtils.buildImageFromPngPath("/icons/repo.png");
    this.informationIcon = UiUtils.buildImageFromPngPath("/icons/information.png");

    treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
    treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
    treeViewer.setContentProvider(new PullRequestContentProvider());
    treeViewer.setLabelProvider(new PullRequestLabelProvider());

    // Enable native tooltip support using mouse tracking (doesn't block scrolling)
    treeViewer.getTree().addListener(SWT.MouseHover, event -> {
      TreeItem item = treeViewer.getTree().getItem(new Point(event.x, event.y));
      if (item != null) {
        Object data = item.getData();
        if (data instanceof GitHubPullRequestItem pr) {
          treeViewer.getTree().setToolTipText(NLS.bind(Messages.jobsView_toolTip_pullRequest, pr.title()));
        } else if (data instanceof String nodeText) {
          treeViewer.getTree().setToolTipText(nodeText);
        } else {
          treeViewer.getTree().setToolTipText(null);
        }
      } else {
        treeViewer.getTree().setToolTipText(null);
      }
    });

    // Add dispose listener to clean up images
    treeViewer.getTree().addDisposeListener(e -> {
      if (directoryIcon != null && !directoryIcon.isDisposed()) {
        directoryIcon.dispose();
      }
      if (informationIcon != null && !informationIcon.isDisposed()) {
        informationIcon.dispose();
      }
      if (treeViewer.getLabelProvider() instanceof PullRequestLabelProvider labelProvider) {
        labelProvider.disposeImages();
      }
    });

    treeViewer.getTree().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {
        TreeItem[] selection = treeViewer.getTree().getSelection();
        if (selection.length > 0) {
          Object data = selection[0].getData();
          if (data instanceof GitHubPullRequestItem pr) {
            String url = pr.htmlUrl();
            if (StringUtils.isNotBlank(url)) {
              Program.launch(url);
            }
          } else if (data instanceof String projectName) {
            // Only handle project click if it's not already expanded
            // Allow normal tree collapse behavior
            if (!treeViewer.getExpandedState(projectName)) {
              handleProjectClick(projectName);
            }
          }
        }
      }
    });

    // Add mouse move listener to change cursor to hand when hovering over PR items
    // or repo items
    treeViewer.getTree().addMouseMoveListener(e -> {
      TreeItem item = treeViewer.getTree().getItem(new Point(e.x, e.y));
      if (item != null && (item.getData() instanceof GitHubPullRequestItem)) {
        treeViewer.getTree().setCursor(treeViewer.getTree().getDisplay().getSystemCursor(SWT.CURSOR_HAND));
      } else {
        treeViewer.getTree().setCursor(null);
      }
    });

    // Add tree expansion listener to load PRs when expand icon is clicked
    treeViewer.addTreeListener(new ITreeViewerListener() {
      @Override
      public void treeExpanded(TreeExpansionEvent event) {
        Object element = event.getElement();
        if (element instanceof String projectName) {
          handleProjectClick(projectName);
        }
      }

      @Override
      public void treeCollapsed(TreeExpansionEvent event) {
        // No action needed on collapse
      }
    });

    treeViewer.setInput(repoToPullRequestsMap);

    // Load and display opened projects initially
    loadOpenedProjects();
  }

  /**
   * Shared initialization logic for loading projects and setting initial map
   * values. Returns the list of projects whose PRs should be (re)loaded (expanded
   * ones).
   */
  private List<IProject> initializeProjects(List<String> expandedProjectNames) throws Exception {
    List<IProject> projects = WorkspaceUtils.listTopLevelProjectsWithGitRepository();

    repoToPullRequestsMap.clear();
    projectNameToProjectMap.clear();

    if (projects.isEmpty()) {
      repoToPullRequestsMap.put(Messages.jobsView_label_noOpenProjects,
          Either.forRight(Messages.jobsView_label_noOpenProjects));
      return Collections.emptyList();
    }

    List<IProject> projectsToReload = new ArrayList<>();
    for (IProject project : projects) {
      String projectName = project.getName();
      projectNameToProjectMap.put(projectName, project);
      if (expandedProjectNames != null && expandedProjectNames.contains(projectName)) {
        // Mark for reload with loading placeholder (no expansion here - preserve state)
        setProjectLoadingPlaceholder(projectName, false);
        projectsToReload.add(project);
      } else {
        // Null indicates PRs not loaded yet
        repoToPullRequestsMap.put(projectName, null);
      }
    }
    return projectsToReload;
  }

  /**
   * Helper to set loading placeholder and optionally expand the node.
   */
  private void setProjectLoadingPlaceholder(String projectName, boolean expand) {
    repoToPullRequestsMap.put(projectName, Either.forRight(Messages.jobsView_label_loadingAgentJobs));
    sync.asyncExec(() -> {
      if (!treeViewer.getControl().isDisposed()) {
        treeViewer.refresh(projectName);
        if (expand) {
          treeViewer.expandToLevel(projectName, 1);
        }
      }
    });
  }

  /**
   * Load and display all opened projects in the workspace without loading PRs.
   */
  private void loadOpenedProjects() {
    Job job = new Job(Messages.jobsView_job_loadingPullRequests) {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          // No pre-expanded projects on first load
          List<IProject> projects = WorkspaceUtils.listTopLevelProjectsWithGitRepository();
          monitor.beginTask(Messages.jobsView_job_loadingPullRequestsForProjects, projects.size());

          // Use shared initializer (no expanded projects => no reload list)
          initializeProjects(Collections.emptyList());

          sync.asyncExec(() -> {
            if (!treeViewer.getControl().isDisposed()) {
              treeViewer.refresh();
            }
          });

          return Status.OK_STATUS;
        } catch (Exception e) {
          CopilotCore.LOGGER.error(Messages.jobsView_error_loadingPullRequests, e);
          return new Status(IStatus.ERROR, "com.microsoft.copilot.eclipse.ui.jobs",
              Messages.jobsView_error_loadingPullRequests, e);
        } finally {
          monitor.done();
        }
      }
    };
    job.schedule();
  }

  /**
   * Handle user clicking on a project tree node - load PRs lazily.
   */
  private void handleProjectClick(String projectName) {
    // Skip if not a valid project or if PRs are already loading/loaded
    if (!projectNameToProjectMap.containsKey(projectName)) {
      return;
    }

    Either<List<GitHubPullRequestItem>, String> prListOrStatusMsg = repoToPullRequestsMap.get(projectName);
    if (prListOrStatusMsg != null) {
      // Already loaded or loading
      return;
    }

    // Set loading placeholder and expand
    setProjectLoadingPlaceholder(projectName, true);

    // Load PRs for this project
    IProject project = projectNameToProjectMap.get(projectName);
    if (project != null) {
      loadPullRequestsForProject(project);
    }
  }

  /**
   * Refresh the pull requests by reloading opened projects and refreshing
   * expanded repos.
   */
  private void refreshPullRequests() {
    Object[] expandedElements = treeViewer.getExpandedElements();
    List<String> expandedProjectNames = new ArrayList<>();
    for (Object element : expandedElements) {
      if (element instanceof String projectName && projectNameToProjectMap.containsKey(projectName)) {
        expandedProjectNames.add(projectName);
      }
    }

    Job job = new Job(Messages.jobsView_job_loadingPullRequests) {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          List<IProject> projects = WorkspaceUtils.listTopLevelProjectsWithGitRepository();
          monitor.beginTask(Messages.jobsView_job_loadingPullRequestsForProjects, projects.size());

          List<IProject> projectsToReload = initializeProjects(expandedProjectNames);

          sync.asyncExec(() -> {
            if (!treeViewer.getControl().isDisposed()) {
              treeViewer.refresh();
            }
          });

          // Reload PRs for previously expanded projects
          for (IProject project : projectsToReload) {
            loadPullRequestsForProject(project);
          }

          return Status.OK_STATUS;
        } catch (Exception e) {
          CopilotCore.LOGGER.error(Messages.jobsView_error_loadingPullRequests, e);
          return new Status(IStatus.ERROR, "com.microsoft.copilot.eclipse.ui.jobs",
              Messages.jobsView_error_loadingPullRequests, e);
        } finally {
          monitor.done();
        }
      }
    };
    job.schedule();
  }

  /**
   * Load pull requests for a specific project using GitHub API. Uses the query:
   * "repo:${owner}/${repository} is:open author:@copilot involves:${user}
   * is:pull-request"
   *
   * @param project The project to load PRs for
   */
  private void loadPullRequestsForProject(IProject project) {
    String projectName = project.getName();
    URI projectUri = project.getLocationURI();
    if (projectUri == null) {
      return;
    }

    try {
      CopilotLanguageServerConnection lsConnection = CopilotCore.getPlugin().getCopilotLanguageServer();
      if (lsConnection == null) {
        CopilotCore.LOGGER.error(
            new IllegalStateException(NLS.bind(Messages.jobsView_error_languageServerNotAvailable, projectName)));
        return;
      }

      WorkspaceFolder workspaceFolder = new WorkspaceFolder();
      workspaceFolder.setUri(projectUri.toString());
      workspaceFolder.setName(projectName);

      SearchPrParams params = new SearchPrParams();
      params.setQuery(SEARCH_PR_QUERY);
      params.setWorkspaceFolders(List.of(workspaceFolder));

      CompletableFuture<SearchPrResponse> prListFuture = lsConnection.searchPr(params);

      prListFuture.thenAccept(response -> {
        if (response != null && response.getPullRequests() != null && !response.getPullRequests().isEmpty()) {
          repoToPullRequestsMap.put(projectName, Either.forLeft(response.getPullRequests()));
        } else {
          repoToPullRequestsMap.put(projectName, Either.forRight(Messages.jobsView_label_noAgentJobsFound));
        }

        sync.asyncExec(() -> {
          if (!treeViewer.getControl().isDisposed()) {
            treeViewer.refresh(projectName);
            treeViewer.expandToLevel(projectName, 1);
          }
        });
      }).exceptionally(ex -> {
        handlePullRequestLoadError(projectName, ex);
        return null;
      });

    } catch (Exception e) {
      handlePullRequestLoadError(projectName, e);
    }
  }

  /**
   * Handle errors that occur when loading pull requests for a project.
   *
   * @param projectName The name of the project
   * @param error       The error that occurred
   */
  private void handlePullRequestLoadError(String projectName, Throwable error) {
    CopilotCore.LOGGER.error(NLS.bind(Messages.jobsView_error_loadingPRsForProject, projectName), error);
    String errorMessage = error.getMessage();
    if (StringUtils.isBlank(errorMessage)) {
      errorMessage = error.getClass().getSimpleName();
    }
    repoToPullRequestsMap.put(projectName, Either.forRight(errorMessage));
    sync.asyncExec(() -> {
      if (!treeViewer.getControl().isDisposed()) {
        treeViewer.refresh(projectName);
        treeViewer.expandToLevel(projectName, 1);
      }
    });
  }

  /**
   * Expand all projects in the tree view. For projects that are already loaded,
   * just expand them. For projects not yet loaded, load PRs first then expand.
   */
  private void expandAllProjects() {
    // Iterate through all projects in the tree
    for (String projectName : repoToPullRequestsMap.keySet()) {
      // Skip special nodes like "No open projects"
      if (!projectNameToProjectMap.containsKey(projectName)) {
        continue;
      }

      Either<List<GitHubPullRequestItem>, String> prListOrStatusMsg = repoToPullRequestsMap.get(projectName);

      if (prListOrStatusMsg == null) {
        // PRs not loaded yet - load them first, then expand
        setProjectLoadingPlaceholder(projectName, true);
        IProject project = projectNameToProjectMap.get(projectName);
        if (project != null) {
          loadPullRequestsForProject(project);
        }
      } else if (prListOrStatusMsg.isLeft()) {
        // PRs already loaded - just expand
        sync.asyncExec(() -> {
          if (!treeViewer.getControl().isDisposed()) {
            treeViewer.expandToLevel(projectName, 1);
          }
        });
      } else {
        // Loading or error state - just expand to show the status
        sync.asyncExec(() -> {
          if (!treeViewer.getControl().isDisposed()) {
            treeViewer.expandToLevel(projectName, 1);
          }
        });
      }
    }
  }

  /**
   * Collapse all projects in the tree view.
   */
  private void collapseAll() {
    sync.asyncExec(() -> {
      if (!treeViewer.getControl().isDisposed()) {
        treeViewer.collapseAll();
      }
    });
  }

  /**
   * Event handler for refresh requests.
   *
   * @param event the event data (not used)
   */
  @Inject
  @Optional
  private void onRefreshEvent(@UIEventTopic(JobsViewEvents.TOPIC_REFRESH) Object event) {
    refreshPullRequests();
  }

  /**
   * Event handler for collapse all requests.
   *
   * @param event the event data (not used)
   */
  @Inject
  @Optional
  private void onCollapseAllEvent(@UIEventTopic(JobsViewEvents.TOPIC_COLLAPSE_ALL) Object event) {
    collapseAll();
  }

  /**
   * Event handler for expand all requests.
   *
   * @param event the event data (not used)
   */
  @Inject
  @Optional
  private void onExpandAllEvent(@UIEventTopic(JobsViewEvents.TOPIC_EXPAND_ALL) Object event) {
    expandAllProjects();
  }

  /**
   * Content provider for the jobs tree view with 2-level hierarchy. Level 1:
   * Repository (Project name) Level 2: Pull Request
   */
  private class PullRequestContentProvider implements ITreeContentProvider {
    @Override
    public Object[] getElements(Object inputElement) {
      if (inputElement instanceof Map<?, ?>) {
        return repoToPullRequestsMap.keySet().toArray();
      }
      return new Object[0];
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof String nodeName) {
        Either<List<GitHubPullRequestItem>, String> prListOrStatusMsg = repoToPullRequestsMap.get(nodeName);
        if (prListOrStatusMsg == null) {
          return new Object[0];
        } else if (prListOrStatusMsg.isRight()) {
          return new Object[] { prListOrStatusMsg.getRight() };
        } else {
          return prListOrStatusMsg.getLeft().toArray();
        }
      }
      return new Object[0];
    }

    @Override
    public Object getParent(Object element) {
      return null;
    }

    @Override
    public boolean hasChildren(Object element) {
      if (element instanceof String nodeName) {
        if (SPECIAL_STATUS_LABELS.contains(nodeName)) {
          return false;
        }

        Either<List<GitHubPullRequestItem>, String> prListOrStatusMsg = repoToPullRequestsMap.get(nodeName);
        if (prListOrStatusMsg == null) {
          return true;
        } else if (prListOrStatusMsg.isRight()) {
          return true;
        } else {
          return !prListOrStatusMsg.getLeft().isEmpty();
        }
      }
      return false;
    }
  }

  /**
   * Label provider for the pull requests tree view.
   */
  private class PullRequestLabelProvider extends ColumnLabelProvider {
    private Image loadingIcon;
    private Image completeIcon;

    @Override
    public String getText(Object element) {
      if (element instanceof String nodeName) {
        return nodeName;
      } else if (element instanceof GitHubPullRequestItem pr) {
        String draftLabel = pr.draft() ? Messages.jobsView_label_draftPrefix : "";
        return draftLabel + pr.title();
      }
      return element.toString();
    }

    @Override
    public Image getImage(Object element) {
      if (element instanceof String nodeText) {
        return getStatusIconForLabel(nodeText);
      } else if (element instanceof GitHubPullRequestItem pr) {
        return getStatusIconForPr(pr);
      }
      return null;
    }

    private Image getStatusIconForLabel(String nodeText) {
      if (Messages.jobsView_label_loadingAgentJobs.equals(nodeText)) {
        if (this.loadingIcon == null) {
          loadingIcon = UiUtils.buildImageFromPngPath("/icons/status/loading.png");
        }
        return this.loadingIcon;
      } else if (SPECIAL_STATUS_LABELS.contains(nodeText)) {
        return informationIcon;
      } else if (projectNameToProjectMap.containsKey(nodeText)) {
        return directoryIcon;
      } else {
        // This is an error message or unknown status, use warning icon
        return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
      }
    }

    private Image getStatusIconForPr(GitHubPullRequestItem pr) {
      if (pr == null || pr.copilotWorkStatus() == null) {
        return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
      }

      switch (pr.copilotWorkStatus()) {
        case done:
          if (this.completeIcon == null) {
            completeIcon = UiUtils.buildImageFromPngPath("/icons/status/complete.png");
          }
          return this.completeIcon;
        case error:
          return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
        case in_progress:
        default:
          if (this.loadingIcon == null) {
            loadingIcon = UiUtils.buildImageFromPngPath("/icons/status/loading.png");
          }
          return this.loadingIcon;
      }
    }

    public void disposeImages() {
      if (loadingIcon != null && !loadingIcon.isDisposed()) {
        loadingIcon.dispose();
      }
      if (completeIcon != null && !completeIcon.isDisposed()) {
        completeIcon.dispose();
      }
    }
  }

}

package com.microsoft.copilot.eclipse.ui.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModelProvider;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ByokService;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * BYOK (Bring Your Own Key) preference page for configuring AI model providers. This page allows users to add, remove,
 * and change AI model providers and their models.
 */
public class ByokPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
  public static final String ID = "com.microsoft.copilot.eclipse.ui.preferences.ByokPreferencePage";

  private static final int CONTAINER_HEIGHT_HINT = 400;

  private ByokService byokService = null;

  private Map<String, List<ByokModel>> byProviderModels = new HashMap<>();

  private Map<String, String> byProviderApiKeys = new HashMap<>();

  // used to determine whether remote models are fetched
  private Set<String> remotelyLoadedProviders = new HashSet<>();

  // Track loading providers for provider-specific UI feedback
  private Set<String> loadingProviders = new HashSet<>();

  // Track expanded providers to maintain state across refreshes
  private Set<String> expandedProviders = new HashSet<>();

  // Page-level stack to manage empty/full content views
  private Composite pageStateStack;
  private StackLayout pageStateStackLayout;
  private Composite emptyStateComposite;
  private Composite contentComposite;
  private Label pageDescription;
  // UI Components in contentComposite
  private Composite viewerStack;
  private Composite treeComposite;
  private Composite loadingOverlay;
  private Image enabledIcon;
  private Image disabledIcon;
  private TreeViewer viewer;
  private Button addModelButton;
  private Button removeModelButton;
  private Button toggleStatusButton;
  private Button reloadButton;
  private Button changeApiButton;
  private Button deleteApiButton;

  // ========================= Lifecycle =========================
  @Override
  public void init(IWorkbench workbench) {
    noDefaultAndApplyButton();
    setTitle(Messages.preferences_page_byok_title);
    Job job = new Job("Binding to model service...") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          Job.getJobManager().join(CopilotUi.INIT_JOB_FAMILY, null);
        } catch (OperationCanceledException | InterruptedException e) {
          CopilotCore.LOGGER.error(e);
        }
        byokService = CopilotUi.getPlugin().getChatServiceManager().getByokService();
        byokService.bindByokPreferencePage(ByokPreferencePage.this);
        refreshPageData();
        return Status.OK_STATUS;
      }
    };
    job.setUser(true);
    job.schedule();
    initializeProviderStates();
  }

  /**
   * Refresh both API keys and local models when page opens.
   */
  private void refreshPageData() {
    if (byokService != null) {
      byokService.refreshData().whenComplete((result, throwable) -> {
        SwtUtils.invokeOnDisplayThreadAsync(() -> {
          setPageLoading(false);
          if (throwable != null) {
            CopilotCore.LOGGER.error("BYOK data refresh failed", throwable);
            handleError("Failed to refresh data: " + throwable.getMessage());
          }
        });
      });
    }
  }

  private void initializeProviderStates() {
    for (ByokModelProvider provider : ByokModelProvider.values()) {
      String providerName = provider.getDisplayName();
      if (provider == ByokModelProvider.AZURE) {
        remotelyLoadedProviders.add(providerName);
      }
    }
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite root = new Composite(parent, SWT.NONE);
    GridLayout rootLayout = new GridLayout(1, false);
    rootLayout.marginWidth = 0;
    rootLayout.marginHeight = 0;
    root.setLayout(rootLayout);
    root.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    pageDescription = new Label(root, SWT.WRAP);
    pageDescription.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

    // Build a page-level stack with two states: empty and full content
    pageStateStack = new Composite(root, SWT.NONE);
    pageStateStackLayout = new StackLayout();
    pageStateStack.setLayout(pageStateStackLayout);
    pageStateStack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    emptyStateComposite = new Composite(pageStateStack, SWT.NONE);
    contentComposite = createByokView(pageStateStack);
    updatePageState();
    
    root.addDisposeListener(e -> {
      if (byokService != null) {
        byokService.unbindByokPreferencePage();
      }
    });
    
    return root;
  }

  private void updatePageState() {
    if (pageStateStack == null || pageStateStack.isDisposed()) {
      return;
    }
    AuthStatusManager auth = CopilotCore.getPlugin().getAuthStatusManager();
    if (auth == null) {
      pageStateStackLayout.topControl = emptyStateComposite;
    } else if (!auth.isSignedIn()) {
      pageDescription.setText(Messages.preferences_page_byok_signin_description);
      pageStateStackLayout.topControl = emptyStateComposite;
    } else {
      pageDescription.setText(Messages.preferences_page_byok_description);
      pageStateStackLayout.topControl = contentComposite;
    }
    pageStateStack.requestLayout();
  }

  // ========================= UI Construction =========================

  /**
   * Create BYOK configuration view.
   */
  private Composite createByokView(Composite parent) {
    Composite root = new Composite(parent, SWT.NONE);
    root.setLayout(new GridLayout(1, false));

    // Provider group
    Group providerGroup = new Group(root, SWT.NONE);
    providerGroup.setText(Messages.preferences_page_byok_provider_title);
    GridLayout groupLayout = new GridLayout(1, false);
    groupLayout.marginWidth = 5;
    groupLayout.marginHeight = 5;
    providerGroup.setLayout(groupLayout);
    GridData groupData = new GridData(SWT.FILL, SWT.FILL, true, false);
    groupData.heightHint = CONTAINER_HEIGHT_HINT;
    providerGroup.setLayoutData(groupData);

    // Description inside the group
    Label description = new Label(providerGroup, SWT.WRAP);
    description.setText(Messages.preferences_page_byok_provider_description);
    description.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Container for the main content
    Composite container = new Composite(providerGroup, SWT.NONE);
    GridLayout containerLayout = new GridLayout(1, false);
    containerLayout.marginWidth = 0;
    containerLayout.marginHeight = 0;
    container.setLayout(containerLayout);
    GridData containerData = new GridData(SWT.FILL, SWT.FILL, true, true);
    container.setLayoutData(containerData);

    createContentArea(container);
    return root;
  }

  private Composite createContentArea(Composite parent) {
    Composite contentArea = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    contentArea.setLayout(layout);
    contentArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    // Stack composite for switching between tree viewer and loading (left side)
    viewerStack = new Composite(contentArea, SWT.NONE);
    StackLayout stackLayout = new StackLayout();
    stackLayout.marginHeight = 0;
    stackLayout.marginWidth = 0;
    viewerStack.setLayout(stackLayout);
    GridData stackData = new GridData(SWT.FILL, SWT.FILL, true, true);
    viewerStack.setLayoutData(stackData);
    enabledIcon = UiUtils.buildImageFromPngPath("/icons/chat/keep.png");
    disabledIcon = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_DELETE);

    viewerStack.addDisposeListener(e -> {
      if (enabledIcon != null && !enabledIcon.isDisposed()) {
        enabledIcon.dispose();
        enabledIcon = null;
      }
    });

    // Create tree viewer composite
    treeComposite = createTreeViewer(viewerStack);
    // Create loading overlay
    loadingOverlay = createLoadingOverlay(viewerStack);
    // Set initial top control to loading
    stackLayout.topControl = loadingOverlay;
    // Button Group
    createButtonGroup(contentArea);
    // Initialize tree viewer with empty provider list
    initializeTreeViewer();

    return contentArea;
  }

  private Composite createTreeViewer(Composite parent) {
    Composite tree = new Composite(parent, SWT.NONE);
    GridLayout treeLayout = new GridLayout(1, false);
    treeLayout.marginHeight = 0;
    treeLayout.marginWidth = 0;
    tree.setLayout(treeLayout);

    // TreeViewer with columns
    viewer = new TreeViewer(tree, SWT.SINGLE | SWT.FULL_SELECTION);
    Tree treeControl = viewer.getTree();
    GridData treeData = new GridData(SWT.FILL, SWT.FILL, true, true);
    treeControl.setLayoutData(treeData);
    treeControl.setHeaderVisible(true);
    treeControl.setLinesVisible(true);

    // Create name column
    TreeViewerColumn nameColumn = new TreeViewerColumn(viewer, SWT.NONE);
    TreeColumn nameTreeColumn = nameColumn.getColumn();
    nameTreeColumn.setText(Messages.preferences_page_byok_customModels);
    nameTreeColumn.setWidth(300);
    nameColumn.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        return getNameText(element);
      }
    });

    // Create status column
    TreeViewerColumn statusColumn = new TreeViewerColumn(viewer, SWT.NONE);
    TreeColumn statusTreeColumn = statusColumn.getColumn();
    statusTreeColumn.setText(Messages.preferences_page_byok_table_status_column);
    statusTreeColumn.setWidth(100);
    statusColumn.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        return getStatusText(element);
      }

      @Override
      public Image getImage(Object element) {
        return getStatusImage(element);
      }
    });

    viewer.setContentProvider(new ByokContentProvider(this));
    viewer.addSelectionChangedListener(e -> {
      refreshButtonsEnabled();
      updateToggleButtonText();
    });

    // Add tree expansion listener for lazy loading
    viewer.addTreeListener(new ITreeViewerListener() {
      @Override
      public void treeExpanded(TreeExpansionEvent event) {
        Object element = event.getElement();
        if (element instanceof String providerName && byokService != null) {
          expandedProviders.add(providerName);
          onProviderExpanded(providerName);
        }
      }

      @Override
      public void treeCollapsed(TreeExpansionEvent event) {
        Object element = event.getElement();
        if (element instanceof String providerName) {
          expandedProviders.remove(providerName);
        }
      }
    });
    return tree;
  }

  private Composite createLoadingOverlay(Composite parent) {
    Composite overlay = new Composite(parent, SWT.NONE);
    overlay.setLayout(new GridLayout(1, false));
    overlay.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

    // Vertical center container
    Composite centerContainer = new Composite(overlay, SWT.NONE);
    centerContainer.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
    centerContainer.setLayout(new GridLayout(1, false));
    centerContainer.setBackground(overlay.getBackground());

    Label loadingLabel = new Label(centerContainer, SWT.NONE);
    loadingLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
    loadingLabel.setText(Messages.preferences_page_loading);
    return overlay;
  }

  private Composite createButtonGroup(Composite parent) {
    Composite btnGroup = new Composite(parent, SWT.NONE);
    btnGroup.setLayout(new GridLayout(1, false));
    GridData btnData = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
    btnGroup.setLayoutData(btnData);

    addModelButton = new Button(btnGroup, SWT.PUSH);
    addModelButton.setText(Messages.preferences_page_byok_addModel_button);
    addModelButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    addModelButton.addListener(SWT.Selection, e -> onAddModel());

    removeModelButton = new Button(btnGroup, SWT.PUSH);
    removeModelButton.setText(Messages.preferences_page_byok_removeModel);
    removeModelButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    removeModelButton.addListener(SWT.Selection, e -> onRemoveModel());

    toggleStatusButton = new Button(btnGroup, SWT.PUSH);
    toggleStatusButton.setText(Messages.preferences_page_byok_enableModel_button);
    toggleStatusButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    toggleStatusButton.addListener(SWT.Selection, e -> onToggleModelStatus());

    reloadButton = new Button(btnGroup, SWT.PUSH);
    reloadButton.setText(Messages.preferences_page_byok_reload_button);
    reloadButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    reloadButton.addListener(SWT.Selection, e -> onReload());

    changeApiButton = new Button(btnGroup, SWT.PUSH);
    changeApiButton.setText(Messages.preferences_page_byok_changeApi_button);
    changeApiButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    changeApiButton.addListener(SWT.Selection, e -> onChangeProviderApi());

    deleteApiButton = new Button(btnGroup, SWT.PUSH);
    deleteApiButton.setText(Messages.preferences_page_byok_deleteApi_button);
    deleteApiButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    deleteApiButton.addListener(SWT.Selection, e -> onDeleteProviderApi());
    return btnGroup;
  }

  /**
   * Get the name text for tree elements (Name column).
   */
  private String getNameText(Object element) {
    if (element instanceof String providerName) {
      List<ByokModel> models = byProviderModels.get(providerName);
      int registeredCount = 0;
      if (models != null) {
        registeredCount = (int) models.stream().filter(ByokModel::isRegistered).count();
      }

      String baseText = providerName;

      // Add loading indicator if provider is loading
      if (loadingProviders.contains(providerName)) {
        return baseText + " (" + Messages.preferences_page_loading + ")";
      }

      if (models != null && !models.isEmpty()) {
        baseText += " ( " + String.format(Messages.preferences_page_byok_enabledCount, registeredCount, models.size())
            + " )";
      }
      return baseText;
    }

    if (element instanceof ByokModel model) {
      String modelText = model.getModelCapabilities() != null && model.getModelCapabilities().getName() != null
          ? model.getModelCapabilities().getName()
          : model.getModelId();

      // Add (Default) suffix for non-custom models
      if (!model.isCustomModel()) {
        modelText += " (" + Messages.preferences_page_byok_default + ")";
      }
      return modelText;
    }

    return "";
  }

  /**
   * Get the status text for tree elements (Status column).
   */
  private String getStatusText(Object element) {
    if (element instanceof ByokModel model) {
      return model.isRegistered() ? Messages.preferences_page_byok_enableModel_button
          : Messages.preferences_page_byok_disableModel_button;
    }

    return "";
  }

  /**
   * Get the status image for tree elements (Status column).
   */
  private Image getStatusImage(Object element) {
    if (element instanceof ByokModel model) {
      return model.isRegistered() ? enabledIcon : disabledIcon;
    }
    return null;
  }

  /**
   * Update the toggle button text based on selected model status.
   */
  private void updateToggleButtonText() {
    if (toggleStatusButton == null || toggleStatusButton.isDisposed()) {
      return;
    }
    ByokModel model = getSelectedModel();
    if (model != null) {
      String buttonText = model.isRegistered() ? Messages.preferences_page_byok_disableModel_button
          : Messages.preferences_page_byok_enableModel_button;
      toggleStatusButton.setText(buttonText);
    } else {
      toggleStatusButton.setText(Messages.preferences_page_byok_enableModel_button);
    }
  }

  /**
   * Refresh the enabled state of buttons based on current selection.
   */
  private void refreshButtonsEnabled() {
    Object sel = getFirstSelection();
    ByokModel selectedModel = getSelectedModel();
    addModelButton.setEnabled(sel != null);
    removeModelButton.setEnabled(selectedModel != null && selectedModel.isCustomModel());
    toggleStatusButton.setEnabled(selectedModel != null);
    reloadButton.setEnabled(true);
    // Check if provider is not Azure and has API key
    boolean canManageApiKey = false;
    String providerName = getSelectedProviderName();
    if (providerName != null) {
      boolean isAzureProvider = ByokModelProvider.isAzure(providerName);
      boolean hasApiKeyForProvider = byProviderApiKeys.containsKey(providerName);
      canManageApiKey = !isAzureProvider && hasApiKeyForProvider;
    }
    // Change API: enabled when provider is not Azure and has API key
    changeApiButton.setEnabled(canManageApiKey);
    // Delete API: enabled when provider is not Azure and has API key
    deleteApiButton.setEnabled(canManageApiKey);
  }

  private void initializeTreeViewer() {
    // Initialize with all providers, even if they have no models yet
    byProviderModels.clear();
    for (ByokModelProvider provider : ByokModelProvider.values()) {
      byProviderModels.put(provider.getDisplayName(), new ArrayList<>());
    }

    if (viewer != null && !viewer.getControl().isDisposed()) {
      viewer.setInput(byProviderModels);
      refreshButtonsEnabled();
    }
  }

  // ========================= Data Binding Update Entry Points =========================
  /**
   * Called by service to update models display.
   */
  public void updateModelsDisplay(Map<String, List<ByokModel>> modelsByProvider) {
    if (viewer != null && !viewer.getControl().isDisposed()) {
      byProviderModels.clear();
      // Prevent UI flicker during bulk update
      viewer.getControl().setRedraw(false);
      for (ByokModelProvider provider : ByokModelProvider.values()) {
        byProviderModels.put(provider.getDisplayName(), new ArrayList<>());
      }
      if (modelsByProvider != null) {
        for (Map.Entry<String, List<ByokModel>> entry : modelsByProvider.entrySet()) {
          String providerName = entry.getKey();
          List<ByokModel> models = entry.getValue();
          if (models != null) {
            byProviderModels.computeIfAbsent(providerName, k -> new ArrayList<>()).addAll(models);
          }
        }
      }
      viewer.setInput(byProviderModels);
      restoreExpansionState();
      // Re-enable drawing after update
      viewer.getControl().setRedraw(true);
      refreshButtonsEnabled();
    }
  }

  /**
   * Called by service to update API keys display.
   */
  public void updateApiKeysDisplay(Map<String, String> apiKeys) {
    if (viewer != null && !viewer.getControl().isDisposed()) {
      byProviderApiKeys.clear();
      if (apiKeys != null) {
        byProviderApiKeys.putAll(apiKeys);
      }
      refreshButtonsEnabled();
    }
  }

  /**
   * Restore the expansion state of the tree viewer.
   */
  private void restoreExpansionState() {
    if (viewer != null && !viewer.getControl().isDisposed() && !expandedProviders.isEmpty()) {
      viewer.setExpandedElements(expandedProviders.toArray());
    }
  }

  // ========================= State Management =========================
  /**
   * Update page loading state.
   */
  public void setPageLoading(boolean isLoading) {
    if (viewerStack == null || viewerStack.isDisposed()) {
      return;
    }
    StackLayout stackLayout = (StackLayout) viewerStack.getLayout();
    if (isLoading) {
      stackLayout.topControl = loadingOverlay;
      setButtonsEnabled(false);
    } else {
      stackLayout.topControl = treeComposite;
      refreshButtonsEnabled();
    }
    viewerStack.requestLayout();
  }

  /**
   * Set loading state for a specific provider.
   */
  public void setProviderLoading(String providerName, boolean isLoading) {
    if (isLoading) {
      loadingProviders.add(providerName);
      setButtonsEnabled(false);
    } else {
      loadingProviders.remove(providerName);
      refreshButtonsEnabled();
    }
    if (viewer != null && !viewer.getControl().isDisposed()) {
      viewer.refresh(providerName, true);
      restoreExpansionState();
    }
  }

  /**
   * Handle all types of errors with unified logic based on message prefix.
   */
  public void handleError(String errorMessage) {
    if (errorMessage == null) {
      return;
    }

    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      boolean pageAlive = getControl() != null && !getControl().isDisposed();
      if (pageAlive) {
        MessageDialog.openError(getShell(), Messages.preferences_page_byok_title, errorMessage);
      } else {
        IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (win != null) {
          MessageDialog.openError(win.getShell(), Messages.preferences_page_byok_title, errorMessage);
        }
      }
    });
  }

  private void setButtonsEnabled(boolean enabled) {
    if (addModelButton != null && !addModelButton.isDisposed()) {
      addModelButton.setEnabled(enabled);
    }
    if (removeModelButton != null && !removeModelButton.isDisposed()) {
      removeModelButton.setEnabled(enabled);
    }
    if (toggleStatusButton != null && !toggleStatusButton.isDisposed()) {
      toggleStatusButton.setEnabled(enabled);
    }
    if (reloadButton != null && !reloadButton.isDisposed()) {
      reloadButton.setEnabled(enabled);
    }
    if (changeApiButton != null && !changeApiButton.isDisposed()) {
      changeApiButton.setEnabled(enabled);
    }
    if (deleteApiButton != null && !deleteApiButton.isDisposed()) {
      deleteApiButton.setEnabled(enabled);
    }
  }

  // ========================= Selection Helpers =========================
  private Object getFirstSelection() {
    if (viewer == null || viewer.getSelection() == null) {
      return null;
    }
    Object selection = viewer.getSelection();
    if (selection instanceof IStructuredSelection structured) {
      return structured.getFirstElement();
    }
    return null;
  }

  private ByokModel getSelectedModel() {
    Object sel = getFirstSelection();
    if (sel instanceof ByokModel model) {
      return model;
    }
    return null;
  }

  private String getSelectedProviderName() {
    Object sel = getFirstSelection();
    if (sel instanceof String) {
      return (String) sel;
    }
    if (sel instanceof ByokModel model) {
      return model.getProviderName();
    }
    return null;
  }

  // ========================= User Action Handlers =========================
  private void onAddModel() {
    String providerName = getSelectedProviderName();

    if (providerName != null) {
      final String finalProviderName = providerName;
      boolean hasApiKey = byProviderApiKeys.containsKey(providerName);
      if (!hasApiKey && !ByokModelProvider.isAzure(providerName)) {
        AddApiKeyDialog apiKeyDialog = new AddApiKeyDialog(getShell(), providerName, apiKey -> {
          if (apiKey != null && StringUtils.isNotBlank(apiKey) && byokService != null) {
            executeAsyncProviderOperation(finalProviderName, 
                byokService.addApiKey(finalProviderName, apiKey), 
                "Failed to save API key");
          }
        });
        apiKeyDialog.open();
      } else {
        AddByokModelDialog dialog = new AddByokModelDialog(getShell(), providerName, model -> {
          if (model != null && byokService != null) {
            byokService.saveModel(model).whenComplete((result, throwable) -> {
              if (throwable != null) {
                CopilotCore.LOGGER.error("Failed to add model: ", throwable);
                handleError("Failed to add model: " + throwable.getMessage());
              }
            });
          }
        });
        dialog.open();
      }
    }
  }

  private void onRemoveModel() {
    ByokModel model = getSelectedModel();
    if (model != null) {
      if (showRemoveModelConfirmationDialog()) {
        if (byokService != null) {
          byokService.deleteModel(model).whenComplete((result, throwable) -> {
            if (throwable != null) {
              CopilotCore.LOGGER.error("Failed to remove model: ", throwable);
              handleError("Failed to remove model: " + throwable.getMessage());
            }
          });
        }
      }
    }
  }

  private boolean showRemoveModelConfirmationDialog() {
    MessageDialog dialog = new MessageDialog(getShell(), Messages.preferences_page_byok_removeModel, null,
        Messages.preferences_page_byok_removeModel_confirmDialog_message, MessageDialog.QUESTION,
        new String[] { Messages.preferences_page_byok_dialog_remove, Messages.preferences_page_byok_dialog_cancel }, 0);
    return dialog.open() == 0;
  }

  private void onToggleModelStatus() {
    ByokModel model = getSelectedModel();
    if (model != null) {
      model.setRegistered(!model.isRegistered());
      updateToggleButtonText();
      if (byokService != null) {
        byokService.saveModel(model).whenComplete((result, throwable) -> {
          if (throwable != null) {
            CopilotCore.LOGGER.error("Failed to save model enable status: ", throwable);
            handleError("Failed to save model enable status: " + throwable.getMessage());
          }
        });
      }
    }
  }

  private void onReload() {
    Object sel = getFirstSelection();
    String providerName = getSelectedProviderName();
    if (sel instanceof String && providerName != null) {
      reloadProvider(providerName);
    } else {
      reloadAllProviders();
    }
  }

  /**
   * Reload data for a specific provider.
   */
  private void reloadProvider(String providerName) {
    if (byokService == null) {
      return;
    }
    setProviderLoading(providerName, true);
    byokService.reloadProvider(providerName).thenRun(() -> {
      SwtUtils.invokeOnDisplayThreadAsync(() -> {
        setProviderLoading(providerName, false);
      });
    }).exceptionally(throwable -> {
      SwtUtils.invokeOnDisplayThreadAsync(() -> {
        setProviderLoading(providerName, false);
        CopilotCore.LOGGER.error("Failed to reload " + providerName, throwable);
        handleError("Failed to reload " + providerName + ": " + throwable.getMessage());
      });
      return null;
    });
  }

  /**
   * Reload all providers that have API keys.
   */
  private void reloadAllProviders() {
    if (byokService == null) {
      return;
    }
    setPageLoading(true);
    byokService.reloadAllProviders().thenRun(() -> {
      SwtUtils.invokeOnDisplayThreadAsync(() -> {
        setPageLoading(false);
      });
    }).exceptionally(throwable -> {
      SwtUtils.invokeOnDisplayThreadAsync(() -> {
        setPageLoading(false);
        CopilotCore.LOGGER.error("Failed to reload providers", throwable);
        handleError("Failed to reload providers: " + throwable.getMessage());
      });
      return null;
    });
  }

  private void onChangeProviderApi() {
    String providerName = getSelectedProviderName();
    String apiKey = byProviderApiKeys.get(providerName);
    if (!ByokModelProvider.isAzure(providerName)) {
      final String finalProviderName = providerName;
      if (!showChangeApiConfirmationDialog(finalProviderName)) {
        return;
      }
      AddApiKeyDialog apiKeyDialog = new AddApiKeyDialog(getShell(), providerName, apiKey, newApiKey -> {
        if (newApiKey != null && !newApiKey.trim().isEmpty() && byokService != null) {
          executeAsyncProviderOperation(finalProviderName, 
              byokService.changeApiKey(finalProviderName, newApiKey), 
              "Failed to update API key");
        }
      });
      apiKeyDialog.open();
    }
  }

  private boolean showChangeApiConfirmationDialog(String providerName) {
    MessageDialog dialog = new MessageDialog(getShell(),
        String.format(Messages.preferences_page_byok_changeApi_dialog_title, providerName), null,
        Messages.preferences_page_byok_changeApi_dialog_description, MessageDialog.QUESTION,
        new String[] { Messages.preferences_page_byok_dialog_yes, Messages.preferences_page_byok_dialog_cancel }, 0);
    return dialog.open() == 0;
  }

  private void onDeleteProviderApi() {
    String providerName = getSelectedProviderName();

    final String finalProviderName = providerName;

    if (!ByokModelProvider.isAzure(providerName)) {
      if (showDeleteApiKeyConfirmationDialog(providerName)) {
        executeAsyncProviderOperation(finalProviderName, 
            byokService.deleteApiKey(providerName), 
            "Failed to delete API key");
      }
    }
  }

  /**
   * Show confirmation dialog for deleting API key.
   */
  private boolean showDeleteApiKeyConfirmationDialog(String providerName) {
    MessageDialog dialog = new MessageDialog(getShell(),
        String.format(Messages.preferences_page_byok_deleteApi_dialog_title, providerName), null,
        Messages.preferences_page_byok_deleteApi_dialog_description, MessageDialog.QUESTION,
        new String[] { Messages.preferences_page_byok_dialog_delete, Messages.preferences_page_byok_dialog_cancel }, 0);
    return dialog.open() == 0;
  }

  private void onProviderExpanded(String providerName) {
    // If provider is first expanded, need to fetch models for this provider from remote site
    if (!remotelyLoadedProviders.contains(providerName)) {
      if (byProviderApiKeys == null || !byProviderApiKeys.containsKey(providerName)) {
        // No API key for provider, skip loading
        remotelyLoadedProviders.add(providerName);
        return;
      }
      byokService.reloadProvider(providerName).whenComplete((result, throwable) -> {
        if (throwable != null) {
          handleError(throwable.getMessage());
        }
      });
      remotelyLoadedProviders.add(providerName);
    }
  }

  /**
   * Execute an async operation with provider loading state management.
   */
  private void executeAsyncProviderOperation(String providerName, 
      CompletableFuture<Void> operation, String errorMessagePrefix) {
    setProviderLoading(providerName, true);
    operation.whenComplete((result, throwable) -> {
      SwtUtils.invokeOnDisplayThreadAsync(() -> {
        setProviderLoading(providerName, false);
        if (throwable != null) {
          CopilotCore.LOGGER.error(errorMessagePrefix + " for " + providerName, throwable);
          handleError(errorMessagePrefix + ": " + throwable.getMessage());
        }
      });
    });
  }

  // ========================= Content Provider =========================
  private static class ByokContentProvider implements ITreeContentProvider {
    private ByokPreferencePage page;

    public ByokContentProvider(ByokPreferencePage page) {
      this.page = page;
    }

    @Override
    public Object[] getElements(Object input) {
      @SuppressWarnings("unchecked")
      Map<String, List<ByokModel>> byProviderModelsInput = (Map<String, List<ByokModel>>) input;
      return byProviderModelsInput.keySet().toArray();
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof String providerName) {
        if (page.loadingProviders.contains(providerName)) {
          return new Object[0];
        }

        List<ByokModel> models = page.byProviderModels.get(providerName);
        if (models != null && !models.isEmpty()) {
          return models.toArray();
        }
        return new Object[0];
      }
      return new Object[0];
    }

    @Override
    public Object getParent(Object element) {
      if (element instanceof ByokModel model) {
        return model.getProviderName();
      }
      return null;
    }

    @Override
    public boolean hasChildren(Object element) {
      if (element instanceof String providerName) {
        if (page.loadingProviders.contains(providerName)) {
          return true;
        }
        if (ByokModelProvider.isAzure(providerName)) {
          List<ByokModel> models = page.byProviderModels.get(providerName);
          return models != null && !models.isEmpty();
        }
        if (!page.remotelyLoadedProviders.contains(providerName)) {
          return true;
        }
        List<ByokModel> models = page.byProviderModels.get(providerName);
        return models != null && !models.isEmpty();
      }
      return false;
    }
  }
}

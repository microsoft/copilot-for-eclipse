package com.microsoft.copilot.eclipse.ui.chat.services;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotAuthStatusListener;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.UserPreference;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatPersistence;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;

/**
 * Manager for chat services.
 */
public class CopilotModelService extends ChatBaseService implements CopilotAuthStatusListener {
  private static final String PREF_FILE_NAME = "pref.json";
  private static final Gson gson = new Gson();
  private CopilotLanguageServerConnection lsConnection;
  private AuthStatusManager authStatusManager;

  // data
  private IObservableValue<HashMap<String, CopilotModel>> modelObservable;
  private IObservableValue<CopilotModel> activeModelObservable;
  private CopilotModel defaultModel;
  private String path;

  // Track side effects for each combo
  private final Map<Combo, ISideEffect[]> comboSideEffects = new HashMap<>();

  /**
   * Constructor for the CopilotModelService.
   */
  public CopilotModelService(CopilotLanguageServerConnection lsConnection, AuthStatusManager authStatusManager) {
    this.authStatusManager = authStatusManager;
    this.lsConnection = lsConnection;
    this.authStatusManager.addCopilotAuthStatusListener(this);
    ensureRealm(() -> {
      modelObservable = new WritableValue<>(new HashMap<>(), HashMap.class);
      activeModelObservable = new WritableValue<>(null, CopilotModel.class);
    });
    this.syncModels(this.authStatusManager.getCopilotStatus());
  }

  private void init() {
    Job job = new Job("Fetching model list...") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          // fetch the models
          CopilotModel[] models = lsConnection.listModels().get();
          HashMap<String, CopilotModel> modelMap = new HashMap<>();
          for (CopilotModel model : models) {
            if (model.getScopes().contains(CopilotScope.CHAT_PANEL)) {
              if (defaultModel == null) {
                defaultModel = model;
              }
              modelMap.put(model.getModelName(), model);
            }
          }
          ensureRealm(() -> {
            modelObservable.setValue(modelMap);
          });

          final String modelName = restoreModelName();
          ensureRealm(() -> {
            activeModelObservable.setValue(modelMap.get(modelName));
          });
        } catch (InterruptedException | ExecutionException e) {
          CopilotCore.LOGGER.error("Failed to list models", e);
        }
        return Status.OK_STATUS;
      }
    };
    job.setSystem(true);
    job.schedule();
  }

  /**
   * Set the active model.
   *
   * @param modeleName the name of the model
   */
  public void setActiveModel(String modeleName) {
    CopilotModel model = modelObservable.getValue().get(modeleName);
    if (model == null) {
      return;
    }
    // Try to remember the model name
    Path modelPath = getModelPersistentFilePath();
    if (modelPath != null) {
      // TODO: should have a service/manager to handle this
      UserPreference preference = new UserPreference();
      preference.setModelName(modeleName);
      String jsonContent = gson.toJson(preference);
      PlatformUtils.writeFileContent(modelPath, jsonContent);
    }
    ensureRealm(() -> {
      activeModelObservable.setValue(model);
    });
  }

  /**
   * Get the active model.
   *
   * @return the active model
   */
  public CopilotModel getActiveModel() {
    return activeModelObservable.getValue();
  }

  /**
   * Get the default model.
   *
   * @return the default model
   */
  public CopilotModel getDefaultModel() {
    return defaultModel;
  }

  private void syncModels(String status) {
    switch (status) {
      case CopilotStatusResult.OK:
        init();
        break;
      default:
        defaultModel = null;
        ensureRealm(() -> {
          modelObservable.setValue(new HashMap<>());
          activeModelObservable.setValue(null);
        });
        break;
    }
  }

  @Override
  public void onDidCopilotStatusChange(CopilotStatusResult copilotStatusResult) {
    String status = copilotStatusResult.getStatus();
    syncModels(status);
  }

  /**
   * Register a side effect for the given Combo when the model names.
   *
   * @param combo the combo to set the items
   */
  public void bindModelPicker(final Combo combo) {
    // First unbind if previously bound to prevent leaks
    unbindModelPicker(combo);

    ensureRealm(() -> {
      ISideEffect modelNamesSideEffect = ISideEffect.create(() -> {
        HashMap<String, CopilotModel> modelMap = this.modelObservable.getValue();
        String[] names = modelMap.keySet().toArray(new String[0]);
        Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
      }, (String[] modelNames) -> {
        if (!combo.isDisposed()) {
          combo.setItems(modelNames);
        }
      });

      ISideEffect activeModelSideEffect = ISideEffect.create(() -> {
        CopilotModel activeModel = this.activeModelObservable.getValue();
        return activeModel == null ? "" : activeModel.getModelName();
      }, (String modelName) -> {
        if (combo.isDisposed()) {
          return;
        }
        int index = Arrays.asList(combo.getItems()).indexOf(modelName);
        if (index >= 0) {
          combo.select(index);
          // adjust the width according to the item
          GC gc = new GC(combo);
          Point textExtent = gc.textExtent(modelName);
          gc.dispose();

          GridData gridData = (GridData) combo.getLayoutData();
          // Add some padding (dropdown button width + horizontal margins)
          gridData.widthHint = textExtent.x + 40;

          // TODO: how to refresh the layout in a more systematic way?
          combo.getParent().getParent().layout();
        }
      });

      // Store the side effects for later disposal
      comboSideEffects.put(combo, new ISideEffect[] { modelNamesSideEffect, activeModelSideEffect });

      // Add a dispose listener to auto-unbind when the combo is disposed
      combo.addDisposeListener(e -> unbindModelPicker(combo));
    });
  }

  /**
   * Unbind and dispose side effects for a specific combo.
   *
   * @param combo the combo to unbind
   */
  public void unbindModelPicker(Combo combo) {
    ISideEffect[] effects = comboSideEffects.remove(combo);
    if (effects != null) {
      for (ISideEffect effect : effects) {
        if (effect != null) {
          effect.dispose();
        }
      }
    }
  }

  /**
   * Dispose the service.
   */
  public void dispose() {
    // Dispose all combo side effects
    for (ISideEffect[] effects : comboSideEffects.values()) {
      for (ISideEffect effect : effects) {
        if (effect != null) {
          effect.dispose();
        }
      }
    }
    comboSideEffects.clear();

    // Dispose observables
    if (modelObservable != null) {
      modelObservable.dispose();
      modelObservable = null;
    }

    if (activeModelObservable != null) {
      activeModelObservable.dispose();
      activeModelObservable = null;
    }
    this.authStatusManager.removeCopilotAuthStatusListener(this);
  }

  private @Nullable Path getModelPersistentFilePath() {
    final String user = this.authStatusManager.getUserName();
    if (user == null || user.isEmpty()) {
      CopilotCore.LOGGER.error(new IllegalStateException("User name is empty"));
      return null;
    }
    return Paths.get(this.path, user, PREF_FILE_NAME);
  }

  private String restoreModelName() {
    // TODO: check if the model name is in modelMap
    // get the path for the chat persistence
    try {
      if (this.path == null) {
        ChatPersistence chatPersistence = this.lsConnection.persistence().get();
        this.path = chatPersistence.getPath();
      }
      Path modelFilePath = this.getModelPersistentFilePath();
      if (modelFilePath == null) {
        return defaultModel.getModelName();
      }
      // read from model file
      String jsonContent = PlatformUtils.readFileContent(modelFilePath);
      if (!jsonContent.isEmpty()) {
        UserPreference preference = gson.fromJson(jsonContent, UserPreference.class);
        if (preference != null && preference.getModelName() != null) {
          return preference.getModelName();
        }
      }
    } catch (InterruptedException | ExecutionException | JsonSyntaxException e) {
      CopilotCore.LOGGER.error("Failed to get chat persistence", e);
    }
    return defaultModel.getModelName();
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.contextwindow;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ContextSizeInfo;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.ui.chat.services.ModelService;
import com.microsoft.copilot.eclipse.ui.utils.ModelUtils;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * State holder for context window information. Exposes {@link ContextSizeInfo} as an observable so the UI can react
 * through databinding side effects instead of direct event subscriptions.
 */
public class ContextWindowService {

  private IObservableValue<ContextSizeInfo> contextSizeObservable;
  private final Map<ContextWindowPopup, ISideEffect> popupSideEffects = new HashMap<>();
  private final ModelService modelService;

  /**
   * Creates the service and initializes the observable state on the UI realm.
   *
   * @param modelService the model service used to resolve the active model's context window
   */
  public ContextWindowService(ModelService modelService) {
    this.modelService = modelService;
    AtomicReference<IObservableValue<ContextSizeInfo>> observableRef = new AtomicReference<>();
    SwtUtils.invokeOnDisplayThread(() -> {
      Realm realm = Realm.getDefault();
      if (realm == null) {
        realm = DisplayRealm.getRealm(Display.getCurrent());
      }
      Realm.runWithDefault(realm, () -> observableRef.set(new WritableValue<>(null, ContextSizeInfo.class)));
    });
    contextSizeObservable = observableRef.get();
  }

  /**
   * Returns the current context size state.
   *
   * @return the current state, or {@code null} if no state has been recorded
   */
  public ContextSizeInfo getState() {
    if (contextSizeObservable == null) {
      return null;
    }

    AtomicReference<ContextSizeInfo> result = new AtomicReference<>();
    contextSizeObservable.getRealm().exec(() -> result.set(contextSizeObservable.getValue()));
    return result.get();
  }

  /**
   * Returns the context-window limit shown in the donut popup. Prefer the active model's resolved full context window,
   * falling back to the language-server usage snapshot when the model metadata is unavailable.
   *
   * @param info the context usage snapshot
   * @return the display context-window limit
   */
  public int getDisplayTokenLimit(ContextSizeInfo info) {
    if (info == null) {
      return 0;
    }

    Integer outputLimit = getActiveModelOutputLimit();
    if (info.reservedOutputTokens() > 0) {
      outputLimit = info.reservedOutputTokens();
    }
    if (outputLimit != null && outputLimit > 0) {
      return info.totalTokenLimit() + outputLimit;
    }

    Integer modelContextWindow = getActiveModelContextWindow();
    return modelContextWindow != null && modelContextWindow > 0 ? modelContextWindow : info.totalTokenLimit();
  }

  /**
   * Returns the utilization percentage against the displayed context window.
   *
   * @param info the context usage snapshot
   * @return the utilization percentage
   */
  public double getDisplayUtilizationPercentage(ContextSizeInfo info) {
    if (info == null) {
      return 0;
    }
    int displayLimit = getDisplayTokenLimit(info);
    if (displayLimit <= 0) {
      return info.utilizationPercentage();
    }
    return (double) info.totalUsedTokens() / displayLimit * 100;
  }

  private Integer getActiveModelContextWindow() {
    CopilotModel activeModel = getActiveModel();
    if (activeModel == null) {
      return null;
    }
    return ModelUtils.resolveContextWindowSize(activeModel);
  }

  private Integer getActiveModelOutputLimit() {
    CopilotModel activeModel = getActiveModel();
    if (activeModel == null || activeModel.getCapabilities() == null
        || activeModel.getCapabilities().limits() == null) {
      return null;
    }
    return activeModel.getCapabilities().limits().maxOutputTokens();
  }

  private CopilotModel getActiveModel() {
    return modelService == null ? null : modelService.getActiveModel();
  }

  /**
   * Updates the current context size state and notifies bound UI.
   *
   * @param contextSizeInfo the new context size state, or {@code null} to clear it
   */
  public void updateContextSize(ContextSizeInfo contextSizeInfo) {
    if (contextSizeObservable != null) {
      contextSizeObservable.getRealm().asyncExec(() -> contextSizeObservable.setValue(contextSizeInfo));
    }
  }

  /**
   * Clears the current context size state.
   */
  public void clearContextSize() {
    updateContextSize(null);
  }

  /**
   * Binds the context size donut canvas to the current observable state.
   *
   * @param canvas the donut canvas
   */
  public void bindContextSizeDonut(Canvas canvas) {
    if (canvas == null) {
      return;
    }

    contextSizeObservable.getRealm().exec(() -> {
      ISideEffect sideEffect = ISideEffect.create(contextSizeObservable::getValue, (ContextSizeInfo info) -> {
        boolean hasData = info != null;
        canvas.setVisible(hasData);
        if (canvas.getLayoutData() instanceof GridData gridData) {
          gridData.exclude = !hasData;
        }
        canvas.redraw();
        canvas.requestLayout();
      });
      canvas.addDisposeListener(e -> sideEffect.dispose());
    });
  }

  /**
   * Binds the popup shell contents to the current observable state.
   *
   * @param popup the popup to update
   */
  void bindContextWindowPopup(ContextWindowPopup popup) {
    if (popup == null || contextSizeObservable == null) {
      return;
    }

    contextSizeObservable.getRealm().exec(() -> {
      unbindContextWindowPopup(popup);
      popupSideEffects.put(popup, ISideEffect.create(contextSizeObservable::getValue, popup::onContextSizeInfoChanged));
    });
  }

  /**
   * Unbinds a previously bound popup.
   *
   * @param popup the popup to unbind
   */
  void unbindContextWindowPopup(ContextWindowPopup popup) {
    if (popup == null || contextSizeObservable == null) {
      return;
    }

    contextSizeObservable.getRealm().exec(() -> {
      ISideEffect sideEffect = popupSideEffects.remove(popup);
      if (sideEffect != null) {
        sideEffect.dispose();
      }
    });
  }

  /**
   * Disposes all bindings owned by this service.
   */
  public void dispose() {
    if (contextSizeObservable == null) {
      return;
    }

    contextSizeObservable.getRealm().exec(() -> {
      popupSideEffects.values().forEach(ISideEffect::dispose);
      popupSideEffects.clear();
      contextSizeObservable.dispose();
      contextSizeObservable = null;
    });
  }
}

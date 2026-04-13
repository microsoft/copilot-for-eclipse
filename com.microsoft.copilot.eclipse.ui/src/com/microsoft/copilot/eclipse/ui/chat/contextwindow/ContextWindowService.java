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
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * State holder for context window information. Exposes {@link ContextSizeInfo} as an observable so the UI can react
 * through databinding side effects instead of direct event subscriptions.
 */
public class ContextWindowService {

  private IObservableValue<ContextSizeInfo> contextSizeObservable;
  private final Map<ContextWindowPopup, ISideEffect> popupSideEffects = new HashMap<>();

  /**
   * Creates the service and initializes the observable state on the UI realm.
   */
  public ContextWindowService() {
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

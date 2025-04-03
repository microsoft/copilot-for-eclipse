package com.microsoft.copilot.eclipse.ui.chat.services;

import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotAuthStatusListener;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * A service that manages the authentication status of the user and update the widget in UI.
 */
public class AuthStatusService extends ChatBaseService implements CopilotAuthStatusListener {

  private IObservableValue<String> statusObservable;

  private ISideEffect chatViewSideEffect;

  private ChatView boundChatView;

  /**
   * Constructor for the AuthStatusService.
   *
   * @param authStatusManager the authentication status manager.
   */
  public AuthStatusService(AuthStatusManager authStatusManager) {
    super(null, authStatusManager);

    this.authStatusManager.addCopilotAuthStatusListener(this);
    ensureRealm(() -> {
      statusObservable = new WritableValue<>(authStatusManager.getCopilotStatus(), CopilotStatusResult.class);
    });
  }

  @Override
  public void onDidCopilotStatusChange(CopilotStatusResult copilotStatusResult) {
    ensureRealm(() -> {
      statusObservable.setValue(copilotStatusResult.getStatus());
    });
  }

  /**
   * Bind the chat view to the auth status.
   */
  public void bindChatView(ChatView chatView) {
    if (chatView == null) {
      return;
    }

    // Unbind any previously bound chat view
    unbindChatView();
    this.boundChatView = chatView;

    ensureRealm(() -> {
      chatViewSideEffect = ISideEffect.create(() -> {
        return this.statusObservable.getValue();
      }, chatView::buildViewFor);
    });

    // Update the view immediately after binding to show the initial state.
    SwtUtils.invokeOnDisplayThread(() -> {
      chatView.buildViewFor(this.statusObservable.getValue());
    });
  }

  /**
   * Unbind the currently bound chat view if any.
   */
  public void unbindChatView() {
    if (chatViewSideEffect != null) {
      chatViewSideEffect.dispose();
      chatViewSideEffect = null;
    }
    boundChatView = null;
  }

  /**
   * Unbind the specified chat view if it is currently bound.
   *
   * @param chatView the chat view to unbind
   */
  public void unbindChatView(ChatView chatView) {
    if (chatView != null && chatView.equals(boundChatView)) {
      unbindChatView();
    }
  }

  /**
   * Dispose the service.
   */
  public void dispose() {
    this.authStatusManager.removeCopilotAuthStatusListener(this);
    unbindChatView();
    if (statusObservable != null) {
      statusObservable.dispose();
      statusObservable = null;
    }
  }

}

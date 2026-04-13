// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.service.IMcpConfigService;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpOauthRequest;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpServerToolsCollection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.DidChangeFeatureFlagsParams;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.dialogs.DynamicOauthDialog;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.preferences.McpPreferencePage;

/**
 * This class is responsible for handling the MCP config service.
 */
public class McpConfigService extends ChatBaseService implements IMcpConfigService {
  // MCP tools
  private IObservableValue<List<McpServerToolsCollection>> mcpToolsObservableValue;
  private ISideEffect mcpToolsSideEffect;
  private EventHandler mcpToolNotifiedEventHandler;
  private boolean mcpToolsInitialized = false;

  // MCP feature flag
  private EventHandler featureFlagNotifiedEventHandler;
  private IObservableValue<Boolean> mcpEnabledObservableValue;
  private IObservableValue<Boolean> newExtMcpRegFoundObservableValue;

  private ISideEffect mcpPreferenceSideEffect;
  private ISideEffect mcpToolButtonEnableSideEffect;
  private ISideEffect mcpToolsButtonRedNoticeSideEffect;
  private ISideEffect mcpPrefencePageExtMcpTitleRedNoticeSideEffect;

  private IEventBroker eventBroker;

  /**
   * Constructor for the McpConfigService.
   */
  public McpConfigService() {
    super(null, null);

    eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    if (eventBroker == null) {
      CopilotCore.LOGGER.error(new IllegalStateException("Event broker is null"));
      return;
    }

    initializeMcpToolUpdateEvent();
    initializeMcpFeatureFlagUpdateEvent();
    ensureRealm(() -> newExtMcpRegFoundObservableValue = new WritableValue<>(false, Boolean.class));
  }

  private void initializeMcpToolUpdateEvent() {
    // User may open the mcp preference page before the event comes.
    // Initialization of the ObservableValue is needed, or "bind" will fail by null pointer.
    ensureRealm(() -> mcpToolsObservableValue = new WritableValue<>(new ArrayList<>(), List.class));

    mcpToolNotifiedEventHandler = event -> {
      // On IDE startup: Initialize MCP tools status after MCP servers start.
      // This event is always received because the event broker is set up before the setting manager syncs MCP servers
      // to language server.
      if (!mcpToolsInitialized) {
        CopilotUi.getPlugin().getLanguageServerSettingManager().initializeMcpToolsStatus();
        mcpToolsInitialized = true;
      }

      Object params = event.getProperty(IEventBroker.DATA);
      if (params instanceof List mcpServerTools) {
        ensureRealm(() -> mcpToolsObservableValue.setValue(mcpServerTools));
      }
    };

    eventBroker.subscribe(CopilotEventConstants.ON_DID_CHANGE_MCP_TOOLS, mcpToolNotifiedEventHandler);
  }

  private void initializeMcpFeatureFlagUpdateEvent() {
    ensureRealm(
        () -> mcpEnabledObservableValue = new WritableValue<>(CopilotCore.getPlugin().getFeatureFlags().isMcpEnabled(),
            Boolean.class));

    featureFlagNotifiedEventHandler = event -> {
      Object params = event.getProperty(IEventBroker.DATA);
      if (params instanceof DidChangeFeatureFlagsParams featureFlagsParams) {
        ensureRealm(() -> mcpEnabledObservableValue.setValue(featureFlagsParams.isMcpEnabled()));
      }
    };

    eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_DID_CHANGE_FEATURE_FLAGS, featureFlagNotifiedEventHandler);
  }

  /**
   * Bind the observable with UI in McpPreferencePage.
   */
  public void bindWithMcpPreferencePage(McpPreferencePage page) {
    ensureRealm(() -> {
      unbindWithMcpPreferencePage();
      mcpToolsSideEffect = ISideEffect.create(mcpToolsObservableValue::getValue, page::displayServerToolsInfo);
      mcpPreferenceSideEffect = ISideEffect.create(mcpEnabledObservableValue::getValue, page::updateMcpPreferencePage);
      mcpPrefencePageExtMcpTitleRedNoticeSideEffect = ISideEffect.create(newExtMcpRegFoundObservableValue::getValue,
          hasNew -> {
            if (Boolean.FALSE.equals(hasNew)) {
              page.disposeNoticeIcon();
            }
          });
    });
  }

  private void unbindWithMcpPreferencePage() {
    if (mcpToolsSideEffect != null) {
      mcpToolsSideEffect.dispose();
      mcpToolsSideEffect = null;
    }
    if (mcpPreferenceSideEffect != null) {
      mcpPreferenceSideEffect.dispose();
      mcpPreferenceSideEffect = null;
    }
    if (mcpPrefencePageExtMcpTitleRedNoticeSideEffect != null) {
      mcpPrefencePageExtMcpTitleRedNoticeSideEffect.dispose();
      mcpPrefencePageExtMcpTitleRedNoticeSideEffect = null;
    }
  }

  /**
   * Bind the observable with mcpToolButton in ActionBar.
   */
  public void bindWithMcpToolButton(Button mcpToolButton, Image mcpToolImage, Image mcpToolDisabledImage,
      Image mcpToolDetectedImage) {
    unbindWithMcpToolButton();
    ensureRealm(() -> {
      mcpToolButtonEnableSideEffect = ISideEffect.create(mcpEnabledObservableValue::getValue, (Boolean isEnabled) -> {
        if (mcpToolButton != null && !mcpToolButton.isDisposed()) {
          if (Boolean.TRUE.equals(isEnabled)) {
            if (isNewExtMcpRegFound()) {
              mcpToolButton.setImage(mcpToolDetectedImage);
              mcpToolButton.setToolTipText(Messages.chat_actionBar_toolButton_detected_toolTip);
            } else {
              mcpToolButton.setImage(mcpToolImage);
              mcpToolButton.setToolTipText(Messages.chat_actionBar_toolButton_toolTip);
            }
          } else {
            mcpToolButton.setImage(mcpToolDisabledImage);
            mcpToolButton.setToolTipText(Messages.chat_actionBar_toolButton_disabled_toolTip);
          }
        }
      });

      mcpToolsButtonRedNoticeSideEffect = ISideEffect.create(newExtMcpRegFoundObservableValue::getValue,
          (Boolean newFound) -> {
            if (mcpToolButton != null && !mcpToolButton.isDisposed()) {
              if (Boolean.TRUE.equals(mcpEnabledObservableValue.getValue())) {
                if (Boolean.TRUE.equals(newFound)) {
                  mcpToolButton.setImage(mcpToolDetectedImage);
                  mcpToolButton.setToolTipText(Messages.chat_actionBar_toolButton_detected_toolTip);
                } else {
                  mcpToolButton.setImage(mcpToolImage);
                  mcpToolButton.setToolTipText(Messages.chat_actionBar_toolButton_toolTip);
                }
              }
            }
          });
    });
  }

  /**
   * Unbind the observable with mcpToolButton in ActionBar.
   */
  public void unbindWithMcpToolButton() {
    if (mcpToolButtonEnableSideEffect != null) {
      mcpToolButtonEnableSideEffect.dispose();
      mcpToolButtonEnableSideEffect = null;
    }
    if (mcpToolsButtonRedNoticeSideEffect != null) {
      mcpToolsButtonRedNoticeSideEffect.dispose();
      mcpToolsButtonRedNoticeSideEffect = null;
    }
  }

  /**
   * Handles the Dynamic OAuth request from MCP servers.
   *
   * @return a map of input field names to values, or null if the user cancelled
   */
  public Map<String, String> mcpOauth(McpOauthRequest request) {
    CompletableFuture<Map<String, String>> result = new CompletableFuture<>();
    ensureRealm(() -> {
      var shell = new Shell(PlatformUI.getWorkbench().getDisplay());
      var dialog = new DynamicOauthDialog(shell, request);
      if (dialog.open() == Window.OK) {
        result.complete(dialog.getInputValues());
      } else {
        result.complete(null);
      }
    });

    try {
      return result.get();
    } catch (ExecutionException | InterruptedException e) {
      CopilotCore.LOGGER.error("Error during MCP Dynamic OAuth", e);
      return null;
    }
  }

  /**
   * Check if there is any new MCP registration from extension point.
   */
  public boolean isNewExtMcpRegFound() {
    Boolean[] result = new Boolean[1];
    newExtMcpRegFoundObservableValue.getRealm().exec(() -> result[0] = newExtMcpRegFoundObservableValue.getValue());
    return Boolean.TRUE.equals(result[0]);
  }

  /**
   * Set the newExtMcpRegFound flag.
   */
  public void setNewExtMcpRegFound(boolean value) {
    ensureRealm(() -> newExtMcpRegFoundObservableValue.setValue(value));
  }

  /**
   * Dispose the service.
   */
  public void dispose() {
    if (eventBroker != null) {
      if (mcpToolNotifiedEventHandler != null) {
        eventBroker.unsubscribe(mcpToolNotifiedEventHandler);
        mcpToolNotifiedEventHandler = null;
      }

      if (featureFlagNotifiedEventHandler != null) {
        eventBroker.unsubscribe(featureFlagNotifiedEventHandler);
        featureFlagNotifiedEventHandler = null;
      }
    }
  }
}

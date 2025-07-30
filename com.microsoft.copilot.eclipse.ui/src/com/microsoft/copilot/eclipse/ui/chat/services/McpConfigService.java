package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.DidChangeFeatureFlagsParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.McpServerToolsCollection;
import com.microsoft.copilot.eclipse.ui.preferences.McpPreferencePage;

/**
 * This class is responsible for handling the MCP config service.
 */
public class McpConfigService extends ChatBaseService {
  // MCP tools
  private IObservableValue<List<McpServerToolsCollection>> mcpToolsObservableValue;
  private ISideEffect mcpToolsSideEffect;
  private EventHandler mcpToolNotifiedEventHandler;  

  // MCP feature flag
  private EventHandler featureFlagNotifiedEventHandler;
  private IObservableValue<Boolean> mcpEnabledObservableValue;
  private ISideEffect mcpPreferenceSideEffect;
  private ISideEffect mcpToolButtonSideEffect;

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
  }

  private void initializeMcpToolUpdateEvent() {
    // User may open the mcp preference page before the event comes.
    // Initialization of the ObservableValue is needed, or "bind" will fail by null pointer.
    ensureRealm(() -> mcpToolsObservableValue = new WritableValue<>(new ArrayList<>(), List.class));
    
    mcpToolNotifiedEventHandler = event -> {
      Object params = event.getProperty(IEventBroker.DATA);
      if (params instanceof List mcpServerTools) {
        ensureRealm(() -> mcpToolsObservableValue.setValue(mcpServerTools));
      }
    };

    eventBroker.subscribe(CopilotEventConstants.ON_DID_CHANGE_MCP_TOOLS, mcpToolNotifiedEventHandler);
  }

  private void initializeMcpFeatureFlagUpdateEvent() {
    ensureRealm(() -> mcpEnabledObservableValue = new WritableValue<>(true, Boolean.class));
    
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
  }
  
  /**
   * Bind the observable with mcpToolButton in ActionBar.
   */
  public void bindWithMcpToolButton(Button mcpToolButton) {
    unbindWithMcpToolButton();
    ensureRealm(() -> 
        mcpToolButtonSideEffect = ISideEffect
          .create(mcpEnabledObservableValue::getValue, mcpToolButton::setEnabled));
  }
  
  /**
   * Unbind the observable with mcpToolButton in ActionBar.
   */
  public void unbindWithMcpToolButton() {
    if (mcpToolButtonSideEffect != null) {
      mcpToolButtonSideEffect.dispose();
      mcpToolButtonSideEffect = null;
    }
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

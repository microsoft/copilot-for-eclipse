package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.McpServerToolsCollection;
import com.microsoft.copilot.eclipse.ui.preferences.McpPreferencePage;

/**
 * This class is responsible for handling the MCP tool service.
 */
public class McpToolService extends ChatBaseService {

  private IObservableValue<List<McpServerToolsCollection>> mcpToolsObservableValue;

  private ISideEffect mcpToolsSideEffect;

  private EventHandler mcpToolNotifiedEventHandler;

  private IEventBroker eventBroker;

  /**
   * Constructor for the McpToolService.
   */
  public McpToolService() {
    super(null, null);
    
    ensureRealm(() -> {
      // User may open the mcp preference page before the event comes.
      // Initialization is needed, or "bind" will fail by null pointer.
      mcpToolsObservableValue = new WritableValue<>(new ArrayList<>(), List.class);
    });
    
    mcpToolNotifiedEventHandler = event -> {
      Object params = event.getProperty(IEventBroker.DATA);
      if (params instanceof List mcpServerTools) {
        ensureRealm(() -> {
          mcpToolsObservableValue.setValue(mcpServerTools);
        });
      }
    };

    eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    if (eventBroker != null) {
      eventBroker.subscribe(CopilotEventConstants.ON_DID_CHANGE_MCP_TOOLS, mcpToolNotifiedEventHandler);
    } else {
      CopilotCore.LOGGER.error(new IllegalStateException("Event broker is null"));
    }
  }

  /**
   * Bind the observable with UI.
   */
  public void bindWithMcpPreferencePage(McpPreferencePage page) {
    ensureRealm(() -> {
      unbindWithMcpPreferencePage();
      mcpToolsSideEffect = ISideEffect.create(mcpToolsObservableValue::getValue, page::displayServerToolsInfo);
    });
  }

  private void unbindWithMcpPreferencePage() {
    if (mcpToolsSideEffect != null) {
      mcpToolsSideEffect.dispose();
      mcpToolsSideEffect = null;
    }
  }

  /**
   * Dispose the service.
   */
  public void dispose() {
    if (eventBroker != null && mcpToolNotifiedEventHandler != null) {
      eventBroker.unsubscribe(mcpToolNotifiedEventHandler);
      mcpToolNotifiedEventHandler = null;
    }
  }
}

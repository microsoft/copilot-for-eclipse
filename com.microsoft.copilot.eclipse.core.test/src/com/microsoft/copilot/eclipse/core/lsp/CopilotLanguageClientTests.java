// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.lsp4e.LanguageServerPlugin;
import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.lsp4e.LanguageServersRegistry.LanguageServerDefinition;
import org.eclipse.lsp4e.client.DefaultLanguageClient;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventHandler;

import com.google.gson.Gson;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.core.chat.service.IChatServiceManager;
import com.microsoft.copilot.eclipse.core.chat.service.IReferencedFileService;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationCapabilities;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationContextParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CurrentEditorContext;
import com.microsoft.copilot.eclipse.core.lsp.protocol.DidChangeFeatureFlagsParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.policy.DidChangePolicyParams;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;

@ExtendWith(MockitoExtension.class)
class CopilotLanguageClientTests {

  private CopilotLanguageClient client;

  @Mock
  private CopilotCore plugin;

  @Mock
  private IChatServiceManager chatServiceManager;

  @Mock
  private IReferencedFileService fileService;

  /** Captures log entries written to LanguageServerPlugin's ILog during each test. */
  private final List<IStatus> loggedStatuses = new ArrayList<>();
  private ILogListener logListener;
  private ILog lsp4eLog;

  @BeforeEach
  void setUp() throws Exception {
    client = new CopilotLanguageClient();
    setupWrapperOnClient(client);
    loggedStatuses.clear();
    logListener = (status, pluginId) -> {
      if (LanguageServerPlugin.PLUGIN_ID.equals(status.getPlugin())) {
        loggedStatuses.add(status);
      }
    };
    lsp4eLog = LanguageServerPlugin.getDefault().getLog();
    lsp4eLog.addLogListener(logListener);
  }

  @AfterEach
  void tearDown() {
    lsp4eLog.removeLogListener(logListener);
    loggedStatuses.clear();
  }

  /**
   * Uses reflection to inject a LanguageServerWrapper with a valid serverDefinition into the client, so that
   * DefaultLanguageClient.logMessage() can run end-to-end without NPE on wrapper.serverDefinition.label.
   */
  private static void setupWrapperOnClient(CopilotLanguageClient client) throws Exception {
    LanguageServerDefinition serverDef = mock(LanguageServerDefinition.class);

    LanguageServerWrapper mockWrapper = mock(LanguageServerWrapper.class);
    Field serverDefField = LanguageServerWrapper.class.getDeclaredField("serverDefinition");
    serverDefField.setAccessible(true);
    serverDefField.set(mockWrapper, serverDef);

    Field wrapperField = DefaultLanguageClient.class.getDeclaredField("wrapper");
    wrapperField.setAccessible(true);
    wrapperField.set(client, mockWrapper);
  }

  @Test
  void testResolveCurrentEditorSkill() throws InterruptedException, ExecutionException {
    // Arrange
    ConversationContextParams params = new ConversationContextParams("", "",
        ConversationCapabilities.CURRENT_EDITOR_SKILL);
    IFile file = mock(IFile.class);
    String expectedUri = "file:///path/to/file.txt";

    try (MockedStatic<CopilotCore> copilotCoreMock = Mockito.mockStatic(CopilotCore.class);
        MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {

      copilotCoreMock.when(CopilotCore::getPlugin).thenReturn(plugin);
      when(plugin.getChatServiceManager()).thenReturn(chatServiceManager);
      when(chatServiceManager.getReferencedFileService()).thenReturn(fileService);
      when(fileService.getCurrentFile()).thenReturn(file);
      fileUtilsMock.when(() -> FileUtils.getResourceUri(file)).thenReturn(expectedUri);

      // Act
      CompletableFuture<Object[]> future = client.getConversationContext(params);
      Object[] result = future.get();

      // Assert
      assertNotNull(result);
      assertEquals(2, result.length);
      assertEquals(CurrentEditorContext.class, result[0].getClass());
      assertEquals(expectedUri, ((CurrentEditorContext) result[0]).getUri());
      assertNull(result[1]);
    }
  }

  @Test
  void testOnDidChangeFeatureFlags() {
    // Arrange
    DidChangeFeatureFlagsParams params = new DidChangeFeatureFlagsParams();
    Map<String, String> featureFlags = new HashMap<>();
    featureFlags.put("agent_mode", "1");
    featureFlags.put("mcp", "0");
    params.setFeatureFlags(featureFlags);
    params.setByokEnabled(false);

    FeatureFlags mockFeatureFlags = mock(FeatureFlags.class);

    try (MockedStatic<CopilotCore> copilotCoreMock = Mockito.mockStatic(CopilotCore.class)) {
      copilotCoreMock.when(CopilotCore::getPlugin).thenReturn(plugin);
      when(plugin.getFeatureFlags()).thenReturn(mockFeatureFlags);

      // Act
      client.onDidChangeFeatureFlags(params);

      // Assert
      verify(mockFeatureFlags).setAgentModeEnabled(true);
      verify(mockFeatureFlags).setMcpEnabled(false);
      verify(mockFeatureFlags).setByokEnabled(false);
    }
  }

  @Test
  void testOnDidChangeFeatureFlagsWithEmptyFeatureFlags() {
    // Arrange
    DidChangeFeatureFlagsParams params = new DidChangeFeatureFlagsParams();
    Map<String, String> featureFlags = new HashMap<>();
    params.setFeatureFlags(featureFlags);

    FeatureFlags mockFeatureFlags = mock(FeatureFlags.class);

    try (MockedStatic<CopilotCore> copilotCoreMock = Mockito.mockStatic(CopilotCore.class)) {
      copilotCoreMock.when(CopilotCore::getPlugin).thenReturn(plugin);
      when(plugin.getFeatureFlags()).thenReturn(mockFeatureFlags);

      // Act
      client.onDidChangeFeatureFlags(params);

      // Assert - should by default enable agent mode, MCP and editor preview
      verify(mockFeatureFlags).setAgentModeEnabled(true);
      verify(mockFeatureFlags).setMcpEnabled(true);
      verify(mockFeatureFlags).setByokEnabled(true);
    }
  }

  @Test
  void testOnDidChangePolicy_publishesAutoModelPolicyEventOnlyWhenValueChanges() throws InterruptedException {
    IEventBroker eventBroker = EclipseContextFactory
        .getServiceContext(FrameworkUtil.getBundle(getClass()).getBundleContext()).get(IEventBroker.class);
    assertNotNull(eventBroker);

    CountDownLatch eventReceived = new CountDownLatch(1);
    CountDownLatch duplicateEventReceived = new CountDownLatch(1);
    AtomicInteger eventCount = new AtomicInteger();
    AtomicReference<Object> eventData = new AtomicReference<>();
    EventHandler eventHandler = event -> {
      eventData.set(event.getProperty(IEventBroker.DATA));
      if (eventCount.incrementAndGet() == 1) {
        eventReceived.countDown();
      } else {
        duplicateEventReceived.countDown();
      }
    };
    eventBroker.subscribe(CopilotEventConstants.TOPIC_DID_CHANGE_AUTO_MODEL_POLICY, eventHandler);

    DidChangePolicyParams params = new Gson().fromJson("""
        {
          "mcp.contributionPoint.enabled": false,
          "customAgent.enabled": true,
          "agentMode.autoApproval.enabled": true,
          "autoModel.enabled": false
        }
        """, DidChangePolicyParams.class);
    FeatureFlags featureFlags = new FeatureFlags();

    try (MockedStatic<CopilotCore> copilotCoreMock = Mockito.mockStatic(CopilotCore.class)) {
      copilotCoreMock.when(CopilotCore::getPlugin).thenReturn(plugin);
      when(plugin.getFeatureFlags()).thenReturn(featureFlags);

      client.onDidChangePolicy(params);

      assertTrue(eventReceived.await(5, TimeUnit.SECONDS));
      assertEquals(Boolean.FALSE, eventData.get());

      client.onDidChangePolicy(params);

      assertFalse(duplicateEventReceived.await(500, TimeUnit.MILLISECONDS));
      assertEquals(1, eventCount.get());
    } finally {
      eventBroker.unsubscribe(eventHandler);
    }
  }
  // -------------------------------------------------------------------------
  // logMessage tests
  // -------------------------------------------------------------------------

  @Test
  void testLogMessage_nullMessage_doesNotLog() throws Exception {
    loggedStatuses.clear();
    client.logMessage(null);

    TimeUnit.MILLISECONDS.sleep(200);
    assertTrue(loggedStatuses.isEmpty(), "No log entry expected for null message");
  }

  @Test
  void testLogMessage_nullType_doesNotLog() throws Exception {
    MessageParams params = mock(MessageParams.class);
    when(params.getType()).thenReturn(null);

    loggedStatuses.clear();
    client.logMessage(params);

    TimeUnit.MILLISECONDS.sleep(200);
    assertTrue(loggedStatuses.isEmpty(), "No log entry expected when MessageType is null");
  }

  @Test
  void testLogMessage_errorType_alwaysLogs() throws Exception {
    MessageParams params = new MessageParams(MessageType.Error, "something went wrong");

    loggedStatuses.clear();
    client.logMessage(params);

    TimeUnit.MILLISECONDS.sleep(200);
    assertEquals(1, loggedStatuses.size(), "Expected exactly one log entry for Error");
    assertEquals(IStatus.ERROR, loggedStatuses.get(0).getSeverity());
    assertTrue(loggedStatuses.get(0).getMessage().contains("something went wrong"));
  }

  @Test
  void testLogMessage_warningType_alwaysLogs() throws Exception {
    MessageParams params = new MessageParams(MessageType.Warning, "watch out");

    loggedStatuses.clear();
    client.logMessage(params);

    TimeUnit.MILLISECONDS.sleep(200);
    assertEquals(1, loggedStatuses.size(), "Expected exactly one log entry for Warning");
    assertEquals(IStatus.WARNING, loggedStatuses.get(0).getSeverity());
    assertTrue(loggedStatuses.get(0).getMessage().contains("watch out"));
  }

  @Test
  void testLogMessage_infoType_logsOnlyWhenTraceEnabled() throws Exception {
    MessageParams params = new MessageParams(MessageType.Info, "info message");

    BundleContext ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
    ServiceReference<DebugOptions> ref = ctx.getServiceReference(DebugOptions.class);
    DebugOptions opts = ctx.getService(ref);
    boolean wasDebugEnabled = opts.isDebugEnabled();
    String prevTraceOption = opts.getOption("org.eclipse.lsp4e/trace");
    try {
      opts.setDebugEnabled(true);
      opts.setOption("org.eclipse.lsp4e/trace", "true");

      loggedStatuses.clear();

      client.logMessage(params);

      TimeUnit.MILLISECONDS.sleep(200);
      assertEquals(1, loggedStatuses.size(), "Expected one log entry for Info when trace is enabled");
    } finally {
      if (prevTraceOption != null) {
        opts.setOption("org.eclipse.lsp4e/trace", prevTraceOption);
      } else {
        opts.removeOption("org.eclipse.lsp4e/trace");
      }
      opts.setDebugEnabled(wasDebugEnabled);
      ctx.ungetService(ref);
    }
  }

  @Test
  void testLogMessage_infoType_doesNotLogWhenTraceDisabled() throws Exception {
    MessageParams params = new MessageParams(MessageType.Info, "info message");

    BundleContext ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
    ServiceReference<DebugOptions> ref = ctx.getServiceReference(DebugOptions.class);
    DebugOptions opts = ctx.getService(ref);
    boolean wasDebugEnabled = opts.isDebugEnabled();
    String prevTraceOption = opts.getOption("org.eclipse.lsp4e/trace");
    try {
      opts.setDebugEnabled(true);
      opts.setOption("org.eclipse.lsp4e/trace", "false");

      loggedStatuses.clear();
      client.logMessage(params);

      TimeUnit.MILLISECONDS.sleep(200);
      assertTrue(loggedStatuses.isEmpty(), "No log entry expected for Info when trace is disabled");
    } finally {
      if (prevTraceOption != null) {
        opts.setOption("org.eclipse.lsp4e/trace", prevTraceOption);
      } else {
        opts.removeOption("org.eclipse.lsp4e/trace");
      }
      opts.setDebugEnabled(wasDebugEnabled);
      ctx.ungetService(ref);
    }
  }

  @Test
  void testLogMessage_logType_doesNotLogWhenTraceDisabled() throws Exception {
    MessageParams params = new MessageParams(MessageType.Log, "log message");

    BundleContext ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
    ServiceReference<DebugOptions> ref = ctx.getServiceReference(DebugOptions.class);
    DebugOptions opts = ctx.getService(ref);
    boolean wasDebugEnabled = opts.isDebugEnabled();
    String prevTraceOption = opts.getOption("org.eclipse.lsp4e/trace");
    try {
      opts.setDebugEnabled(true);
      opts.setOption("org.eclipse.lsp4e/trace", "false");

      loggedStatuses.clear();
      client.logMessage(params);

      TimeUnit.MILLISECONDS.sleep(200);
      assertTrue(loggedStatuses.isEmpty(), "No log entry expected for Log when trace is disabled");
    } finally {
      if (prevTraceOption != null) {
        opts.setOption("org.eclipse.lsp4e/trace", prevTraceOption);
      } else {
        opts.removeOption("org.eclipse.lsp4e/trace");
      }
      opts.setDebugEnabled(wasDebugEnabled);
      ctx.ungetService(ref);
    }
  }
}

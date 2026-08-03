// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokApiKey;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListApiKeyResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListModelResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListProviderConfigParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListProviderConfigResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModelProvider;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokProviderConfig;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokStatusResponse;
import com.microsoft.copilot.eclipse.ui.preferences.ByokPreferencePage;

@ExtendWith(MockitoExtension.class)
class ByokServiceTests {

  private static final String OLLAMA_ENDPOINT = "http://localhost:11434";
  private static final String OLLAMA_PROVIDER = ByokModelProvider.OLLAMA.getDisplayName();

  @Mock
  private CopilotLanguageServerConnection lsConnection;

  @Mock
  private ByokPreferencePage preferencePage;

  private ByokService byokService;

  @BeforeEach
  void setUp() {
    byokService = new ByokService(lsConnection);
    byokService.bindByokPreferencePage(preferencePage);
    clearInvocations(preferencePage);
  }

  @AfterEach
  void tearDown() {
    byokService.ensureRealm(byokService::dispose);
  }

  @Test
  void testConfigureOllama_discoveryFailureKeepsSavedEndpointVisible() {
    when(lsConnection.saveByokProviderConfig(any())).thenReturn(completedStatus());
    when(lsConnection.listByokModels(any()))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Ollama is unavailable")));

    assertThrows(CompletionException.class, () -> byokService.configureOllama(OLLAMA_ENDPOINT).join());

    verify(preferencePage).updateProviderUrlsDisplay(argThat(
        providerUrls -> OLLAMA_ENDPOINT.equals(providerUrls.get(OLLAMA_PROVIDER))));
  }

  @Test
  void testLoadProviderUrls_ignoresBlankUrlsAndKeepsFirstDuplicate() {
    String duplicateEndpoint = "http://localhost:11435";
    when(lsConnection.listByokProviderConfigs(any(ByokListProviderConfigParams.class)))
        .thenReturn(CompletableFuture.completedFuture(new ByokListProviderConfigResponse(List.of(
            new ByokProviderConfig(OLLAMA_PROVIDER, " "),
            new ByokProviderConfig(OLLAMA_PROVIDER, OLLAMA_ENDPOINT),
            new ByokProviderConfig(OLLAMA_PROVIDER, duplicateEndpoint)))));

    byokService.loadProviderUrls().join();

    verify(preferencePage).updateProviderUrlsDisplay(Map.of(OLLAMA_PROVIDER, OLLAMA_ENDPOINT));
  }

  @Test
  void testConfigureOllama_emptyDiscoveryRefreshesLocalModels() {
    when(lsConnection.saveByokProviderConfig(any())).thenReturn(completedStatus());
    configureRefreshResponses(List.of());

    byokService.configureOllama(OLLAMA_ENDPOINT).join();

    verify(lsConnection, times(2)).listByokModels(any());
    verify(lsConnection).listByokModels(argThat(params -> Boolean.FALSE.equals(params.getEnableFetchUrl())));
  }

  @Test
  void testConfigureOllama_discoveredModelsAreRegistered() {
    ByokModel discoveredModel = new ByokModel();
    discoveredModel.setProviderName(OLLAMA_PROVIDER);
    discoveredModel.setModelId("qwen3.5:0.8b");
    discoveredModel.setRegistered(false);
    when(lsConnection.saveByokProviderConfig(any())).thenReturn(completedStatus());
    configureRefreshResponses(List.of(discoveredModel));
    when(lsConnection.saveByokModel(any())).thenReturn(completedStatus());

    byokService.configureOllama(OLLAMA_ENDPOINT).join();

    ArgumentCaptor<ByokModel> modelCaptor = ArgumentCaptor.forClass(ByokModel.class);
    verify(lsConnection).saveByokModel(modelCaptor.capture());
    assertTrue(modelCaptor.getValue().isRegistered());
  }

  @Test
  void testDeleteOllamaConfig_removesEndpointBeforeRefresh() {
    when(lsConnection.deleteByokProviderConfig(any())).thenReturn(completedStatus());
    configureRefreshResponses(List.of());
    byokService.loadProviderUrls().join();
    clearInvocations(preferencePage);

    byokService.deleteOllamaConfig().join();

    verify(preferencePage).updateProviderUrlsDisplay(argThat(Map::isEmpty));
  }

  private void configureRefreshResponses(List<ByokModel> discoveredModels) {
    when(lsConnection.listByokModels(any())).thenAnswer(invocation -> {
      ByokListModelResponse response = new ByokListModelResponse();
      response.setModels(discoveredModels);
      return CompletableFuture.completedFuture(response);
    });
    when(lsConnection.listByokApiKeys(any(ByokApiKey.class)))
        .thenReturn(CompletableFuture.completedFuture(new ByokListApiKeyResponse(List.of())));
    when(lsConnection.listByokProviderConfigs(any(ByokListProviderConfigParams.class)))
        .thenReturn(CompletableFuture.completedFuture(new ByokListProviderConfigResponse(
            List.of(new ByokProviderConfig(OLLAMA_PROVIDER, OLLAMA_ENDPOINT)))));
  }

  private CompletableFuture<ByokStatusResponse> completedStatus() {
    ByokStatusResponse response = new ByokStatusResponse();
    response.setSuccess(true);
    return CompletableFuture.completedFuture(response);
  }
}

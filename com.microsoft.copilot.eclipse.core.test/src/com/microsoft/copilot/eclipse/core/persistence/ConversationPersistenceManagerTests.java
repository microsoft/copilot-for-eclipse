package com.microsoft.copilot.eclipse.core.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.lsp4j.WorkDoneProgressKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.logger.CopilotForEclipseLogger;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.Turn;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ReplyData;
import com.microsoft.copilot.eclipse.core.persistence.UserTurnData.MessageData;

@ExtendWith(MockitoExtension.class)
class ConversationPersistenceManagerTests {

  @Mock
  private AuthStatusManager mockAuthStatusManager;

  @Mock
  private ConversationPersistenceService mockPersistenceService;

  @Mock
  private ConversationDataFactory mockDataFactory;

  @Mock
  private CopilotForEclipseLogger mockLogger;

  private ConversationPersistenceManager persistenceManager;

  @BeforeEach
  void setUp() {
    persistenceManager = new ConversationPersistenceManager(mockAuthStatusManager);

    // Use reflection to inject mocks for testing
    try {
      var persistenceServiceField = ConversationPersistenceManager.class.getDeclaredField("persistenceService");
      persistenceServiceField.setAccessible(true);
      persistenceServiceField.set(persistenceManager, mockPersistenceService);

      var dataFactoryField = ConversationPersistenceManager.class.getDeclaredField("dataFactory");
      dataFactoryField.setAccessible(true);
      dataFactoryField.set(persistenceManager, mockDataFactory);
    } catch (Exception e) {
      throw new RuntimeException("Failed to inject mocks", e);
    }
  }

  @Test
  void testLoadConversation_Success() throws Exception {
    String conversationId = "00000000-0000-0000-0000-000000000000";
    ConversationData expectedData = createTestConversationData(conversationId);

    when(mockPersistenceService.loadConversationFromPersistedJsonFile(conversationId)).thenReturn(expectedData);

    CompletableFuture<ConversationData> result = persistenceManager.loadConversation(conversationId);
    ConversationData actualData = result.get();

    assertNotNull(actualData);
    assertEquals(conversationId, actualData.getConversationId());
    verify(mockPersistenceService).loadConversationFromPersistedJsonFile(conversationId);
  }

  @Test
  void testLoadConversationTurns_Success() throws Exception {
    String conversationId = "00000000-0000-0000-0000-000000000000";
    ConversationData conversationData = createTestConversationData(conversationId);
    List<AbstractTurnData> turnDataList = createTestTurnDataList();
    conversationData.setTurns(turnDataList);
    List<Turn> expectedTurns = new ArrayList<>();

    when(mockPersistenceService.loadConversationFromPersistedJsonFile(conversationId)).thenReturn(conversationData);
    when(mockDataFactory.convertToTurns(turnDataList)).thenReturn(expectedTurns);

    List<Turn> actualTurns = persistenceManager.loadConversationTurns(conversationId);

    assertEquals(expectedTurns.size(), actualTurns.size());
    verify(mockDataFactory).convertToTurns(turnDataList);
  }

  @Test
  void testUpdateConversationIdToHistoryRecord_Success() throws Exception {
    String newConversationId = "00000000-0000-0000-0000-000000000001";
    String historyConversationId = "00000000-0000-0000-0000-000000000002";
    ConversationData conversationData = createTestConversationData(historyConversationId);

    // Add conversation to cache manually
    var cacheField = ConversationPersistenceManager.class.getDeclaredField("conversationCache");
    cacheField.setAccessible(true);
    var cache = (Map<String, ConversationData>) cacheField.get(persistenceManager);
    cache.put(historyConversationId, conversationData);

    persistenceManager.updateConversationIdToHistoryRecord(newConversationId, historyConversationId).get();

    assertNull(cache.get(historyConversationId));
    assertNotNull(cache.get(newConversationId));
    assertEquals(newConversationId, cache.get(newConversationId).getConversationId());
    verify(mockPersistenceService).updatePersistedConversationId(historyConversationId, newConversationId);
  }

  @Test
  void testUpdateConversationIdToHistoryRecord_ConversationNotInCache() throws Exception {
    String newConversationId = "00000000-0000-0000-0000-000000000001";
    String historyConversationId = "00000000-0000-0000-0000-000000000002";

    persistenceManager.updateConversationIdToHistoryRecord(newConversationId, historyConversationId);

    verify(mockPersistenceService, never()).updatePersistedConversationId(anyString(), anyString());
  }

  @Test
  void testListConversations() {
    List<ConversationXmlData> expectedConversations = createTestConversationXmlDataList();
    when(mockPersistenceService.listConversations()).thenReturn(expectedConversations);

    List<ConversationXmlData> actualConversations = persistenceManager.listConversations();

    assertEquals(expectedConversations, actualConversations);
    verify(mockPersistenceService).listConversations();
  }

  @Test
  void testPersistUserTurnInfo_Success() throws Exception {
    String conversationId = "00000000-0000-0000-0000-000000000000";
    String turnId = "00000000-0000-0000-0000-000000000001";
    String message = "Test user message";
    CopilotModel model = new CopilotModel();
    model.setModelName("gpt-4");
    String chatMode = "chat";
    IFile mockFile = mock(IFile.class);
    IPath mockPath = new Path("/workspace/test/TestFile.java");
    when(mockFile.getFullPath()).thenReturn(mockPath);
    List<IResource> references = new ArrayList<>();

    ConversationData conversationData = createTestConversationData(conversationId);
    UserTurnData userTurnData = createTestUserTurnData(turnId);

    when(mockPersistenceService.loadConversationFromPersistedJsonFile(conversationId)).thenReturn(conversationData);

    CompletableFuture<ConversationData> result = persistenceManager.persistUserTurnInfo(conversationId, turnId, message,
        model, chatMode, null, mockFile, references);

    ConversationData actualData = result.get();

    assertNotNull(actualData);
    assertEquals(message, userTurnData.getMessage().getText());
    assertEquals("gpt-4", userTurnData.getModel());
    assertEquals(chatMode, userTurnData.getChatMode());
    verify(mockPersistenceService).saveConversation(any(ConversationData.class));
  }

  @Test
  void testCacheConversationProgress_Success() throws Exception {
    String conversationId = "00000000-0000-0000-0000-000000000000";
    ChatProgressValue progress = createTestChatProgressValue();
    ConversationData conversationData = createTestConversationData(conversationId);

    when(mockPersistenceService.loadConversationFromPersistedJsonFile(conversationId)).thenReturn(conversationData);

    CompletableFuture<Void> result = persistenceManager.cacheConversationProgress(conversationId, progress);

    result.get(); // Wait for completion

    // Verify the conversation is in cache
    var cacheField = ConversationPersistenceManager.class.getDeclaredField("conversationCache");
    cacheField.setAccessible(true);
    var cache = (Map<String, ConversationData>) cacheField.get(persistenceManager);
    assertTrue(cache.containsKey(conversationId));
  }

  @Test
  void testPersistConversationProgress_Success() throws Exception {
    String conversationId = "00000000-0000-0000-0000-000000000000";
    ChatProgressValue progress = createTestChatProgressValue();
    ConversationData conversationData = createTestConversationData(conversationId);

    when(mockPersistenceService.loadConversationFromPersistedJsonFile(conversationId)).thenReturn(conversationData);

    CompletableFuture<Void> result = persistenceManager.persistConversationProgress(conversationId, progress);

    result.get(); // Wait for completion

    verify(mockPersistenceService).saveConversation(any(ConversationData.class));
  }

  @Test
  void testUpdateConversationProgress_NewConversation() throws Exception {
    String conversationId = "00000000-0000-0000-0000-000000000001";
    ChatProgressValue progress = createTestChatProgressValue();
    ConversationData newConversationData = createTestConversationData(conversationId);
    createTestCopilotTurnData(progress.getTurnId());

    when(mockPersistenceService.loadConversationFromPersistedJsonFile(conversationId))
        .thenThrow(new IOException("File not found"));
    when(mockDataFactory.createConversationData(conversationId)).thenReturn(newConversationData);

    ConversationData result = persistenceManager.updateConversationProgress(conversationId, progress).get();

    assertNotNull(result);
    assertEquals(conversationId, result.getConversationId());
    verify(mockDataFactory).updateConversationMetadata(newConversationData, progress);
    verify(mockDataFactory).updateReplyFromProgress(any(), eq(progress));
  }

  // Helper methods to create test data
  private ConversationData createTestConversationData(String conversationId) {
    ConversationData data = new ConversationData();
    data.setConversationId(conversationId);
    data.setTitle("Test Conversation");
    data.setRequesterUsername("testuser");
    data.setCreationDate(Instant.now());
    data.setLastMessageDate(Instant.now());
    data.setTurns(createTestTurnDataList());
    return data;
  }

  private List<AbstractTurnData> createTestTurnDataList() {
    List<AbstractTurnData> turns = new ArrayList<>();
    UserTurnData userTurn = createTestUserTurnData("00000000-0000-0000-0000-000000000001");
    CopilotTurnData copilotTurn = createTestCopilotTurnData("00000000-0000-0000-0000-000000000002");
    turns.add(userTurn);
    turns.add(copilotTurn);
    return turns;
  }

  private UserTurnData createTestUserTurnData(String turnId) {
    UserTurnData userData = new UserTurnData();
    userData.setTurnId(turnId);
    userData.setRole("user");
    userData.setModel("gpt-4");
    userData.setChatMode("chat");
    userData.setMessage(new MessageData("Test user message"));
    userData.setTimestamp(Instant.now());
    return userData;
  }

  private CopilotTurnData createTestCopilotTurnData(String turnId) {
    CopilotTurnData copilotData = new CopilotTurnData();
    copilotData.setTurnId(turnId);
    copilotData.setRole("copilot");
    copilotData.setTimestamp(Instant.now());

    ReplyData replyData = new ReplyData();
    replyData.setText("Test copilot message");
    copilotData.setReply(replyData);
    return copilotData;
  }

  private List<ConversationXmlData> createTestConversationXmlDataList() {
    List<ConversationXmlData> conversations = new ArrayList<>();
    conversations.add(new ConversationXmlData("00000000-0000-0000-0000-000000000001", "Conversation 1", Instant.now(),
        Instant.now()));
    conversations.add(new ConversationXmlData("00000000-0000-0000-0000-000000000002", "Conversation 2", Instant.now(),
        Instant.now()));
    return conversations;
  }

  private ChatProgressValue createTestChatProgressValue() {
    ChatProgressValue progress = new ChatProgressValue();
    progress.setKind(WorkDoneProgressKind.report);
    progress.setConversationId("00000000-0000-0000-0000-000000000000");
    progress.setTurnId("00000000-0000-0000-0000-000000000002");
    progress.setReply("Test reply");
    progress.setSuggestedTitle("Test Suggested Title");
    return progress;
  }
}
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.JsonSyntaxException;
import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.logger.CopilotForEclipseLogger;
import com.microsoft.copilot.eclipse.core.persistence.UserTurnData.MessageData;

@ExtendWith(MockitoExtension.class)
class ConversationPersistenceServiceTests {

  @TempDir
  Path tempDir;

  @Mock
  private AuthStatusManager mockAuthStatusManager;

  @Mock
  private CopilotCore mockCopilotCore;

  @Mock
  private IPath mockStateLocation;

  @Mock
  private CopilotForEclipseLogger mockLogger;

  private ConversationPersistenceService persistenceService;
  private MockedStatic<CopilotCore> mockedCopilotCore;

  @BeforeEach
  void setUp() {
    when(mockAuthStatusManager.getUserName()).thenReturn("testuser");

    mockedCopilotCore = Mockito.mockStatic(CopilotCore.class);
    mockedCopilotCore.when(CopilotCore::getPlugin).thenReturn(mockCopilotCore);
    when(mockCopilotCore.getStateLocation()).thenReturn(mockStateLocation);
    when(mockStateLocation.toOSString()).thenReturn(tempDir.toString());

    persistenceService = new ConversationPersistenceService(mockAuthStatusManager);
  }

  @AfterEach
  void tearDown() {
    if (mockedCopilotCore != null) {
      mockedCopilotCore.close();
    }
  }

  @Test
  void testSaveConversation_NewConversation() throws IOException {
    ConversationData conversationData = createTestConversationData("00000000-0000-0000-0000-000000000000");

    persistenceService.saveConversation(conversationData);

    // Verify conversation directory structure is created
    Path conversationsPath = tempDir.resolve("conversations").resolve("testuser");
    assertTrue(Files.exists(conversationsPath));

    // Verify JSON file is created
    Path jsonFile = conversationsPath.resolve("00000000-0000-0000-0000-000000000000.json");
    assertTrue(Files.exists(jsonFile));

    // Verify index XML file is created
    Path indexFile = conversationsPath.resolve("conversation_index.xml");
    assertTrue(Files.exists(indexFile));
  }

  @Test
  void testSaveConversation_ExistingConversation() throws IOException {
    ConversationData conversationData = createTestConversationData("00000000-0000-0000-0000-000000000000");

    // Save conversation first time
    persistenceService.saveConversation(conversationData);

    // Modify and save again
    conversationData.setTitle("Updated Title");
    persistenceService.saveConversation(conversationData);

    // Verify file is updated
    Path jsonFile = tempDir.resolve("conversations").resolve("testuser")
        .resolve("00000000-0000-0000-0000-000000000000.json");
    assertTrue(Files.exists(jsonFile));
    String content = Files.readString(jsonFile);
    assertTrue(content.contains("Updated Title"));
  }

  @Test
  void testLoadConversationFromPersistedJsonFile_Success() throws IOException {
    ConversationData originalData = createTestConversationData("00000000-0000-0000-0000-000000000000");
    persistenceService.saveConversation(originalData);

    ConversationData loadedData = persistenceService
        .loadConversationFromPersistedJsonFile("00000000-0000-0000-0000-000000000000");

    assertNotNull(loadedData);
    assertEquals("00000000-0000-0000-0000-000000000000", loadedData.getConversationId());
    assertEquals("Test Conversation", loadedData.getTitle());
    assertEquals("testuser", loadedData.getRequesterUsername());
  }

  @Test
  void testLoadConversationFromPersistedJsonFile_FileNotFound() {
    assertThrows(IOException.class, () -> {
      persistenceService.loadConversationFromPersistedJsonFile("00000000-0000-0000-0000-000000000000");
    });
  }

  @Test
  void testLoadConversationFromPersistedJsonFile_InvalidJson() throws IOException {
    // Create directory structure
    Path conversationsPath = tempDir.resolve("conversations").resolve("testuser");
    Files.createDirectories(conversationsPath);

    // Create invalid JSON file
    Path jsonFile = conversationsPath.resolve("00000000-0000-0000-0000-000000000000.json");
    Files.writeString(jsonFile, "{ invalid json content");

    assertThrows(JsonSyntaxException.class, () -> {
      persistenceService.loadConversationFromPersistedJsonFile("00000000-0000-0000-0000-000000000000");
    });
  }

  @Test
  void testListConversations_EmptyList() {
    List<ConversationXmlData> conversations = persistenceService.listConversations();

    assertNotNull(conversations);
    assertTrue(conversations.isEmpty());
  }

  @Test
  void testListConversations_WithConversations() throws IOException {
    // Save multiple conversations
    ConversationData conv1 = createTestConversationData("00000000-0000-0000-0000-000000000001");
    conv1.setTitle("First Conversation");
    conv1.setLastMessageDate(Instant.parse("2023-01-01T10:00:00Z"));

    ConversationData conv2 = createTestConversationData("00000000-0000-0000-0000-000000000002");
    conv2.setTitle("Second Conversation");
    conv2.setLastMessageDate(Instant.parse("2023-01-02T10:00:00Z"));

    persistenceService.saveConversation(conv1);
    persistenceService.saveConversation(conv2);

    List<ConversationXmlData> conversations = persistenceService.listConversations();

    assertEquals(2, conversations.size());
    // Should be sorted by last message date (most recent first)
    assertEquals("00000000-0000-0000-0000-000000000002", conversations.get(0).getConversationId());
    assertEquals("00000000-0000-0000-0000-000000000001", conversations.get(1).getConversationId());
  }

  @Test
  void testUpdatePersistedConversationId_Success() throws IOException {
    ConversationData originalData = createTestConversationData("00000000-0000-0000-0000-000000000000");
    persistenceService.saveConversation(originalData);

    persistenceService.updatePersistedConversationId("00000000-0000-0000-0000-000000000000",
        "00000000-0000-0000-0000-000000000001");

    // Verify old file is moved to new file
    Path oldFile = tempDir.resolve("conversations").resolve("testuser")
        .resolve("00000000-0000-0000-0000-000000000000.json");
    Path newFile = tempDir.resolve("conversations").resolve("testuser")
        .resolve("00000000-0000-0000-0000-000000000001.json");

    assertFalse(Files.exists(oldFile));
    assertTrue(Files.exists(newFile));

    // Verify conversation data is updated
    ConversationData updatedData = persistenceService
        .loadConversationFromPersistedJsonFile("00000000-0000-0000-0000-000000000001");
    assertEquals("00000000-0000-0000-0000-000000000001", updatedData.getConversationId());
  }

  @Test
  void testUpdatePersistedConversationId_NewFileAlreadyExists() throws IOException {
    ConversationData oldData = createTestConversationData("00000000-0000-0000-0000-000000000000");
    ConversationData newData = createTestConversationData("00000000-0000-0000-0000-000000000001");

    persistenceService.saveConversation(oldData);
    persistenceService.saveConversation(newData);

    persistenceService.updatePersistedConversationId("00000000-0000-0000-0000-000000000000",
        "00000000-0000-0000-0000-000000000001");

    // Should not move file if new file already exists
    Path oldFile = tempDir.resolve("conversations").resolve("testuser")
        .resolve("00000000-0000-0000-0000-000000000000.json");
    Path newFile = tempDir.resolve("conversations").resolve("testuser")
        .resolve("00000000-0000-0000-0000-000000000001.json");

    assertTrue(Files.exists(oldFile));
    assertTrue(Files.exists(newFile));
  }

  @Test
  void testListConversationsForUser_DifferentUser() throws IOException {
    // Save conversation for testuser
    ConversationData conv1 = createTestConversationData("00000000-0000-0000-0000-000000000001");
    persistenceService.saveConversation(conv1);

    // List conversations for different user
    List<ConversationXmlData> conversations = persistenceService.listConversationsForUser("otheruser");

    assertTrue(conversations.isEmpty());
  }

  @Test
  void testXmlIndexStructure() throws IOException {
    ConversationData conversationData = createTestConversationData("00000000-0000-0000-0000-000000000000");
    conversationData.setTitle("Test Conversation Title");
    conversationData.setCreationDate(Instant.parse("2023-01-01T10:00:00Z"));
    conversationData.setLastMessageDate(Instant.parse("2023-01-01T11:00:00Z"));

    persistenceService.saveConversation(conversationData);

    Path indexFile = tempDir.resolve("conversations").resolve("testuser").resolve("conversation_index.xml");
    assertTrue(Files.exists(indexFile));

    String indexContent = Files.readString(indexFile);
    assertTrue(indexContent.contains("username=\"testuser\""));
    assertTrue(indexContent.contains("conversationId=\"00000000-0000-0000-0000-000000000000\""));
    assertTrue(indexContent.contains("title=\"Test Conversation Title\""));
    assertTrue(indexContent.contains("creationDate=\"2023-01-01T10:00:00Z\""));
    assertTrue(indexContent.contains("lastMessageDate=\"2023-01-01T11:00:00Z\""));
  }

  @Test
  void testComplexConversationSerialization() throws IOException {
    ConversationData conversationData = createComplexConversationData("00000000-0000-0000-0000-000000000000");

    persistenceService.saveConversation(conversationData);
    ConversationData loadedData = persistenceService
        .loadConversationFromPersistedJsonFile("00000000-0000-0000-0000-000000000000");

    assertNotNull(loadedData);
    assertEquals("00000000-0000-0000-0000-000000000000", loadedData.getConversationId());
    assertEquals(2, loadedData.getTurns().size());

    // Verify user turn
    AbstractTurnData firstTurn = loadedData.getTurns().get(0);
    assertTrue(firstTurn instanceof UserTurnData);
    UserTurnData userTurn = (UserTurnData) firstTurn;
    assertEquals("00000000-0000-0000-0000-000000000001", userTurn.getTurnId());
    assertEquals("User message", userTurn.getMessage().getText());

    // Verify copilot turn
    AbstractTurnData secondTurn = loadedData.getTurns().get(1);
    assertTrue(secondTurn instanceof CopilotTurnData);
    CopilotTurnData copilotTurn = (CopilotTurnData) secondTurn;
    assertEquals("00000000-0000-0000-0000-000000000002", copilotTurn.getTurnId());
    assertEquals("Copilot response", copilotTurn.getReply().getText());
  }

  // Helper methods
  private ConversationData createTestConversationData(String conversationId) {
    ConversationData data = new ConversationData();
    data.setConversationId(conversationId);
    data.setTitle("Test Conversation");
    data.setRequesterUsername("testuser");
    data.setResponderUsername("GitHub Copilot");
    data.setCreationDate(Instant.now());
    data.setLastMessageDate(Instant.now());
    data.setTurns(new ArrayList<>());
    return data;
  }

  private ConversationData createComplexConversationData(String conversationId) {
    ConversationData data = createTestConversationData(conversationId);

    // Add user turn
    UserTurnData userTurn = new UserTurnData();
    userTurn.setTurnId("00000000-0000-0000-0000-000000000001");
    userTurn.setRole("user");
    userTurn.setMessage(new MessageData("User message"));
    userTurn.setTimestamp(Instant.now());
    userTurn.setModel("gpt-4");
    userTurn.setChatMode("chat");
    userTurn.setUserLanguage("en");
    userTurn.setSource("panel");
    userTurn.setReferences(new ArrayList<>());
    userTurn.setIgnoredSkills(new ArrayList<>());

    // Add copilot turn
    CopilotTurnData copilotTurn = new CopilotTurnData();
    copilotTurn.setTurnId("00000000-0000-0000-0000-000000000002");
    copilotTurn.setRole("copilot");
    copilotTurn.setTimestamp(Instant.now());
    CopilotTurnData.ReplyData replyData = new CopilotTurnData.ReplyData();
    replyData.setText("Copilot response");
    copilotTurn.setReply(replyData);

    data.getTurns().add(userTurn);
    data.getTurns().add(copilotTurn);

    return data;
  }
}
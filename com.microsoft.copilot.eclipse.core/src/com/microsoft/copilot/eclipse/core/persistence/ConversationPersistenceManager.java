package com.microsoft.copilot.eclipse.core.persistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.Turn;
import com.microsoft.copilot.eclipse.core.persistence.UserTurnData.MessageData;

/**
 * Manager service for conversation persistence operations. Handles all business logic, conversation lifecycle, state
 * management, and coordination between factory and persistence service.
 */
public class ConversationPersistenceManager {
  private final ConversationPersistenceService persistenceService;
  private final ConversationDataFactory dataFactory;
  private final Map<String, ConversationData> conversationCache;

  /**
   * Constructor for ConversationPersistenceManager.
   *
   * @param authStatusManager the authentication status manager
   */
  public ConversationPersistenceManager(AuthStatusManager authStatusManager) {
    this.persistenceService = new ConversationPersistenceService(authStatusManager);
    this.dataFactory = new ConversationDataFactory(authStatusManager);
    this.conversationCache = new ConcurrentHashMap<>();
  }

  /**
   * Loads a full conversation by ID.
   */
  public CompletableFuture<ConversationData> loadConversation(String conversationId) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return getConversationFromCacheOrLoadFromDisk(conversationId);
      } catch (IOException e) {
        CopilotCore.LOGGER.error("Failed to load conversation: " + conversationId, e);
        throw new RuntimeException("Failed to load conversation", e);
      }
    });
  }

  /**
   * Loads conversation turns as Turn[] array for the specified conversation.
   *
   * @param conversationId the ID of the conversation to load turns for
   * @return array of Turn objects converted from TurnData
   * @throws RuntimeException if loading fails
   */
  public List<Turn> loadConversationTurns(String conversationId) {
    try {
      ConversationData conversation = getConversationFromCacheOrLoadFromDisk(conversationId);
      List<AbstractTurnData> turnDataList = conversation != null ? conversation.getTurns() : List.of();
      return dataFactory.convertToTurns(turnDataList);
    } catch (IOException e) {
      CopilotCore.LOGGER.error("Failed to load conversation turns: " + conversationId, e);
      return new ArrayList<>();
    }
  }

  private ConversationData getConversationFromCacheOrLoadFromDisk(String conversationId) throws IOException {
    ConversationData cached = conversationCache.get(conversationId);
    if (cached != null) {
      return cached;
    }
    ConversationData loaded = persistenceService.loadConversationFromPersistedJsonFile(conversationId);
    if (loaded != null) {
      conversationCache.put(conversationId, loaded);
    }
    return loaded;
  }

  /**
   * Updates the conversation ID for a history record. The history record with old conversation ID will be updated to
   * the a new record with the new conversation ID (the old record will be removed). The conversation data in memory
   * cache will also be updated.
   *
   * @param newConversationId the new conversation ID to assign
   * @param historyConversationId the ID of the history record to update
   */
  public void updateConversationIdToHistoryRecord(String newConversationId, String historyConversationId) {
    ConversationData conversation = conversationCache.get(historyConversationId);
    if (conversation == null) {
      return;
    }
    conversationCache.remove(historyConversationId);
    conversation.setConversationId(newConversationId);
    conversationCache.put(newConversationId, conversation);
    try {
      persistenceService.updatePersistedConversationId(historyConversationId, newConversationId);
    } catch (IOException e) {
      CopilotCore.LOGGER.error("Failed to update conversation ID for history record: " + historyConversationId, e);
    }
  }

  /**
   * Lists all conversations in the persisted XML index document.
   *
   * @return list of ConversationXmlData objects representing all conversations
   */
  public List<ConversationXmlData> listConversations() {
    return persistenceService.listConversations();
  }

  /**
   * Persist the user turn info in a conversation.
   *
   * @param conversationId the ID of the conversation
   * @param turnId the ID of the turn to update
   * @param message the new message text for the user
   * @param model the model used for this turn
   * @param chatMode the chat mode for this turn
   */
  public CompletableFuture<ConversationData> persistUserTurnInfo(String conversationId, String turnId, String message,
      CopilotModel model, String chatMode, IFile currentFile, List<IResource> references) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        ConversationData conversation = getOrCreateNewConversationById(conversationId);

        UserTurnData userTurnData = findOrCreateUserTurn(conversation, turnId);

        if (userTurnData != null) {
          userTurnData.setMessage(new MessageData(message));
          if (model != null) {
            userTurnData.setModel(model.getModelName());
          }
          if (chatMode != null) {
            userTurnData.setChatMode(chatMode);
          }
          if (currentFile != null) {
            userTurnData.setCurrentDocument(currentFile);
          }
          if (references != null) {
            userTurnData.setReferences(references);
          }
          persistAndCacheConversation(conversation);
        }
        return conversation;
      } catch (IOException e) {
        CopilotCore.LOGGER.error("Failed to update user turn info for conversation: " + conversationId, e);
        throw new RuntimeException("Failed to update turn user info", e);
      }
    });
  }

  /**
   * Updates a conversation with progress data and caches it only (no disk persistence).
   */
  public CompletableFuture<Void> cacheConversationProgress(String conversationId, ChatProgressValue progress) {
    return CompletableFuture.runAsync(() -> {
      try {
        ConversationData conversationData = updateConversationProgress(conversationId, progress);
        conversationCache.put(conversationId, conversationData);
      } catch (IOException e) {
        CopilotCore.LOGGER.error("Failed to cache conversation progress: " + conversationId, e);
      }
    });
  }

  /**
   * Updates a conversation with progress data and persists it to disk.
   */
  public CompletableFuture<Void> persistConversationProgress(String conversationId, ChatProgressValue progress) {
    return CompletableFuture.runAsync(() -> {
      try {
        ConversationData conversationData = updateConversationProgress(conversationId, progress);
        persistAndCacheConversation(conversationData);
      } catch (IOException e) {
        CopilotCore.LOGGER.error("Failed to persist conversation progress: " + conversationId, e);
      }
    });
  }

  /**
   * Updates a conversation with progress data. This method is synchronous and handles all IO operations internally.
   */
  public ConversationData updateConversationProgress(String conversationId, ChatProgressValue progress)
      throws IOException {
    ConversationData conversationData = getOrCreateNewConversationById(conversationId);

    // Update conversation metadata using factory
    dataFactory.updateConversationMetadata(conversationData, progress);

    // Find or create turn and update it
    CopilotTurnData copilotTurnData = findOrCreateCopilotTurn(conversationData, progress.getTurnId());
    dataFactory.updateReplyFromProgress(copilotTurnData.getReply(), progress);

    // Update suggested title in CopilotTurnData if present
    if (StringUtils.isNotBlank(progress.getSuggestedTitle())) {
      copilotTurnData.setSuggestedTitle(progress.getSuggestedTitle());
    }

    return conversationData;
  }

  private UserTurnData findOrCreateUserTurn(ConversationData conversation, String turnId) {
    if (turnId != null) {
      AbstractTurnData existingTurn = findTurn(conversation, turnId);
      if (existingTurn != null && existingTurn instanceof UserTurnData userTurnData) {
        return userTurnData;
      }
    }

    UserTurnData turn = dataFactory.createUserTurnData(conversation.getConversationId(), turnId, "", null, null);
    conversation.getTurns().add(turn);
    return turn;
  }

  private CopilotTurnData findOrCreateCopilotTurn(ConversationData conversation, String turnId) {
    if (turnId != null) {
      AbstractTurnData existingTurn = findTurn(conversation, turnId);
      if (existingTurn != null && existingTurn instanceof CopilotTurnData copilotTurnData) {
        return copilotTurnData;
      }
    }

    CopilotTurnData turn = dataFactory.createCopilotTurnData(turnId);
    conversation.getTurns().add(turn);
    return turn;
  }

  /**
   * Finds a turn by ID in the conversation.
   */
  private AbstractTurnData findTurn(ConversationData conversation, String turnId) {
    if (conversation == null || turnId == null) {
      return null;
    }
    for (AbstractTurnData t : conversation.getTurns()) {
      if (turnId.equals(t.getTurnId())) {
        return t;
      }
    }
    return null;
  }

  private ConversationData getOrCreateNewConversationById(String conversationId) throws IOException {
    try {
      ConversationData existedConversation = getConversationFromCacheOrLoadFromDisk(conversationId);
      if (existedConversation != null) {
        return existedConversation;
      }
    } catch (IOException e) {
      // treat as missing and create below
    }

    ConversationData newConversation = dataFactory.createConversationData(conversationId);

    // must persist conversation and index here to make sure conversation title and last message date is up to date.
    persistAndCacheConversation(newConversation);
    return newConversation;
  }

  private void persistAndCacheConversation(ConversationData conversation) throws IOException {
    persistenceService.saveConversation(conversation);
    conversationCache.put(conversation.getConversationId(), conversation);
  }

  /**
   * Gets the data factory for data transformation operations.
   *
   * @return the ConversationDataFactory instance
   */
  public ConversationDataFactory getDataFactory() {
    return dataFactory;
  }
}
package com.microsoft.copilot.eclipse.core.persistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.Turn;
import com.microsoft.copilot.eclipse.core.lsp.protocol.codingagent.CodingAgentMessageRequestParams;
import com.microsoft.copilot.eclipse.core.persistence.UserTurnData.MessageData;

/**
 * Manager service for conversation persistence operations. Handles all business logic, conversation lifecycle, state
 * management, and coordination between factory and persistence service.
 */
public class ConversationPersistenceManager {
  private final ConversationPersistenceService persistenceService;
  private final ConversationDataFactory dataFactory;
  private final Map<String, ConversationData> conversationCache;
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  // Maximum number of conversations to keep persisted
  private static final int MAX_PERSISTED_CONVERSATIONS = 256;

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
      lock.readLock().lock();
      try {
        return getConversationFromCacheOrLoadFromDisk(conversationId);
      } catch (IOException e) {
        CopilotCore.LOGGER.error("Failed to load conversation: " + conversationId, e);
        throw new RuntimeException("Failed to load conversation", e);
      } finally {
        lock.readLock().unlock();
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
    lock.readLock().lock();
    try {
      ConversationData conversation = getConversationFromCacheOrLoadFromDisk(conversationId);
      List<AbstractTurnData> turnDataList = conversation != null ? conversation.getTurns() : List.of();
      return dataFactory.convertToTurns(turnDataList);
    } catch (IOException e) {
      CopilotCore.LOGGER.error("Failed to load conversation turns: " + conversationId, e);
      return new ArrayList<>();
    } finally {
      lock.readLock().unlock();
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
  public CompletableFuture<Void> updateConversationIdToHistoryRecord(String newConversationId,
      String historyConversationId) {
    if (Objects.equals(newConversationId, historyConversationId)) {
      // No change needed if IDs are the same
      return CompletableFuture.completedFuture(null);
    }

    return CompletableFuture.runAsync(() -> {
      lock.writeLock().lock();
      try {
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
      } finally {
        lock.writeLock().unlock();
      }
    });
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
   * @param customChatModeId the custom chat mode ID (if applicable)
   */
  public CompletableFuture<ConversationData> persistUserTurnInfo(String conversationId, String turnId, String message,
      CopilotModel model, String chatMode, String customChatModeId, IFile currentFile, List<IResource> references) {
    return CompletableFuture.supplyAsync(() -> {
      lock.writeLock().lock();
      try {
        ConversationData conversationData = getOrCreateNewConversationById(conversationId);

        UserTurnData userTurnData = findOrCreateUserTurn(conversationData, turnId);

        if (userTurnData != null) {
          userTurnData.setMessage(new MessageData(message));
          if (model != null) {
            userTurnData.setModel(model.getModelName());
          }
          if (chatMode != null) {
            userTurnData.setChatMode(chatMode);
          }
          if (customChatModeId != null) {
            userTurnData.setCustomChatModeId(customChatModeId);
          }
          if (currentFile != null) {
            userTurnData.setCurrentDocument(currentFile);
          }
          if (references != null) {
            userTurnData.setReferences(references);
          }
          if (StringUtils.isBlank(conversationData.getTitle()) && StringUtils.isNotBlank(message)) {
            // Set a temporary title if the conversation does not have one yet
            String trimedMsg = message.trim().replaceAll("\\s*[\\r\\n]+\\s*", " ");
            String tempTitle = trimedMsg.length() <= 50 ? trimedMsg : trimedMsg.substring(0, 50) + "...";
            conversationData.setTitle(tempTitle);
          }
          persistAndCacheConversation(conversationData);
        }
        return conversationData;
      } catch (IOException e) {
        CopilotCore.LOGGER.error("Failed to update user turn info for conversation: " + conversationId, e);
        throw new RuntimeException("Failed to update turn user info", e);
      } finally {
        lock.writeLock().unlock();
      }
    });
  }

  /**
   * Updates a conversation with progress data and caches it only (no disk persistence).
   */
  public CompletableFuture<Void> cacheConversationProgress(String conversationId, ChatProgressValue progress) {
    return CompletableFuture.runAsync(() -> {
      lock.writeLock().lock();
      try {
        ConversationData conversationData = updateConversationProgressInternal(conversationId, progress);
        conversationCache.put(conversationId, conversationData);
      } catch (Exception e) {
        CopilotCore.LOGGER.error("Failed to cache conversation progress: " + conversationId, e);
      } finally {
        lock.writeLock().unlock();
      }
    });
  }

  /**
   * Updates a conversation with progress data and persists it to disk.
   */
  public CompletableFuture<Void> persistConversationProgress(String conversationId, ChatProgressValue progress) {
    return CompletableFuture.runAsync(() -> {
      lock.writeLock().lock();
      try {
        ConversationData conversationData = updateConversationProgressInternal(conversationId, progress);
        persistAndCacheConversation(conversationData);
      } catch (IOException e) {
        CopilotCore.LOGGER.error("Failed to persist conversation progress: " + conversationId, e);
      } catch (Exception e) {
        CopilotCore.LOGGER.error("Failed to persist conversation progress: " + conversationId, e);
      } finally {
        lock.writeLock().unlock();
      }
    });
  }

  /**
   * Persists a cached conversation to disk if it exists in the cache.
   */
  public CompletableFuture<Void> persistCachedConversation(String conversationId) {
    return CompletableFuture.runAsync(() -> {
      lock.writeLock().lock();
      try {
        ConversationData conversationData = conversationCache.get(conversationId);
        if (conversationData != null) {
          persistAndCacheConversation(conversationData);
        }
      } catch (IOException e) {
        CopilotCore.LOGGER.error("Failed to persist cached conversation: " + conversationId, e);
      } finally {
        lock.writeLock().unlock();
      }
    });
  }

  /**
   * Updates a conversation with progress data. This method is synchronous and handles all IO operations internally.
   */
  public CompletableFuture<ConversationData> updateConversationProgress(String conversationId,
      ChatProgressValue progress) {
    return CompletableFuture.supplyAsync(() -> {
      lock.writeLock().lock();
      try {
        return updateConversationProgressInternal(conversationId, progress);
      } catch (IOException e) {
        CopilotCore.LOGGER.error("Failed to update conversation progress: " + conversationId, e);
        throw new RuntimeException("Failed to update conversation progress", e);
      } finally {
        lock.writeLock().unlock();
      }
    });
  }

  /**
   * Internal method to update conversation progress without locking (caller must hold write lock).
   */
  private ConversationData updateConversationProgressInternal(String conversationId, ChatProgressValue progress)
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

    UserTurnData turn = dataFactory.createUserTurnData(conversation.getConversationId(), turnId, "", null, null, null);
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

    // Check conversation count and remove oldest if needed before creating new one
    enforceConversationHistoryLimit();

    ConversationData newConversation = dataFactory.createConversationData(conversationId);

    // must persist conversation and index here to make sure conversation title and last message date is up to date.
    persistAndCacheConversation(newConversation);
    return newConversation;
  }

  /**
   * Enforces the conversation history limit by removing the oldest conversations when the count exceeds
   * MAX_PERSISTED_CONVERSATIONS.
   */
  private void enforceConversationHistoryLimit() {
    try {
      List<ConversationXmlData> allConversations = persistenceService.listConversations();

      if (allConversations.size() >= MAX_PERSISTED_CONVERSATIONS) {
        allConversations.sort((a, b) -> {
          if (a.getLastMessageDate() == null && b.getLastMessageDate() == null) {
            return 0;
          }
          if (a.getLastMessageDate() == null) {
            return -1; // null dates are considered oldest
          }
          if (b.getLastMessageDate() == null) {
            return 1;
          }
          return a.getLastMessageDate().compareTo(b.getLastMessageDate());
        });

        int conversationsToRemove = allConversations.size() - MAX_PERSISTED_CONVERSATIONS + 1;

        for (int i = 0; i < conversationsToRemove && i < allConversations.size(); i++) {
          ConversationXmlData oldestConversation = allConversations.get(i);
          String conversationIdToRemove = oldestConversation.getConversationId();

          try {
            persistenceService.deleteConversation(conversationIdToRemove);
            conversationCache.remove(conversationIdToRemove);
            CopilotCore.LOGGER.info("Removed oldest conversation to maintain history limit: " + conversationIdToRemove);
          } catch (IOException e) {
            CopilotCore.LOGGER.error("Failed to delete oldest conversation: " + conversationIdToRemove, e);
          }
        }
      }
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to enforce conversation history limit", e);
    }
  }

  private void persistAndCacheConversation(ConversationData conversation) throws IOException {
    persistenceService.saveConversation(conversation);
    conversationCache.put(conversation.getConversationId(), conversation);
  }

  /**
   * Removes a conversation by ID from both disk and in-memory cache.
   *
   * @param conversationId the ID of the conversation to remove
   */
  public CompletableFuture<Void> removeConversationById(String conversationId) {
    return CompletableFuture.runAsync(() -> {
      lock.writeLock().lock();
      try {
        persistenceService.deleteConversation(conversationId);
        conversationCache.remove(conversationId);
      } catch (IOException e) {
        CopilotCore.LOGGER.error("Failed to delete conversation: " + conversationId, e);
      } finally {
        lock.writeLock().unlock();
      }
    });
  }

  /**
   * Updates the title of a conversation and persists the change to disk.
   *
   * @param conversationId the ID of the conversation to update
   * @param newTitle the new title to set
   */
  public CompletableFuture<Void> updateConversationTitle(String conversationId, String newTitle) {
    return CompletableFuture.runAsync(() -> {
      lock.writeLock().lock();
      try {
        ConversationData conversation = getConversationFromCacheOrLoadFromDisk(conversationId);
        if (conversation != null) {
          conversation.setTitle(newTitle);
          persistAndCacheConversation(conversation);
        }
      } catch (IOException e) {
        CopilotCore.LOGGER.error("Failed to update conversation title: " + conversationId, e);
      } finally {
        lock.writeLock().unlock();
      }
    });
  }

  /**
   * Adds a coding agent message to a conversation turn and persists it.
   *
   * @param params the coding agent message request parameters
   * @param agentSlug the slug identifier of the coding agent
   */
  public CompletableFuture<Void> addCodingAgentMessage(CodingAgentMessageRequestParams params, String agentSlug) {
    return CompletableFuture.runAsync(() -> {
      lock.writeLock().lock();
      try {
        ConversationData conversation = getOrCreateNewConversationById(params.getConversationId());
        final CopilotTurnData copilotTurn = findOrCreateCopilotTurn(conversation, params.getTurnId());

        // Create agent message data
        CopilotTurnData.AgentMessageData agentMessage = new CopilotTurnData.AgentMessageData();
        agentMessage.setTitle(params.getTitle());
        agentMessage.setDescription(params.getDescription());
        agentMessage.setPrLink(params.getPrLink());
        agentMessage.setAgentSlug(agentSlug);

        // Add to the turn's reply data
        if (copilotTurn.getReply().getAgentMessages() == null) {
          copilotTurn.getReply().setAgentMessages(new ArrayList<>());
        }
        copilotTurn.getReply().getAgentMessages().add(agentMessage);

        // Persist the updated conversation
        persistAndCacheConversation(conversation);
      } catch (IOException e) {
        CopilotCore.LOGGER.error("Failed to add agent message: " + params.getConversationId(), e);
      } finally {
        lock.writeLock().unlock();
      }
    });
  }

  /**
   * Persists model information (model name and billing multiplier) for a Copilot turn.
   *
   * @param conversationId the ID of the conversation
   * @param turnId the ID of the turn
   * @param modelName the name of the model used
   * @param billingMultiplier the billing multiplier for the model
   */
  public CompletableFuture<Void> persistModelInfo(String conversationId, String turnId, String modelName,
      double billingMultiplier) {
    return CompletableFuture.runAsync(() -> {
      lock.writeLock().lock();
      try {
        ConversationData conversation = getOrCreateNewConversationById(conversationId);
        CopilotTurnData copilotTurn = findOrCreateCopilotTurn(conversation, turnId);

        // Set model information in the reply data
        if (copilotTurn.getReply() != null) {
          copilotTurn.getReply().setModelName(modelName);
          copilotTurn.getReply().setBillingMultiplier(billingMultiplier);
        }

        // Persist the updated conversation
        persistAndCacheConversation(conversation);
      } catch (IOException e) {
        CopilotCore.LOGGER.error("Failed to persist model info for conversation: " + conversationId, e);
      } finally {
        lock.writeLock().unlock();
      }
    });
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

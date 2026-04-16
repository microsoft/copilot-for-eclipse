// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.persistence;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IPath;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Service for persisting conversation data using IMemento pattern. Manages both the conversation index (XML) and
 * individual conversation files (JSON).
 */
public class ConversationPersistenceService {
  private static final String CONVERSATIONS_FOLDER_NAME = "conversations";
  private static final String INDEX_FILE_NAME = "conversation_index.xml";

  // XML element constants
  private static final String ELEMENT_USER = "user";
  private static final String ELEMENT_CONVERSATIONS = "conversations";
  private static final String ELEMENT_CONVERSATION = "conversation";

  // XML attribute constants
  private static final String ATTR_USERNAME = "username";
  private static final String ATTR_CONVERSATION_ID = "conversationId";
  private static final String ATTR_TITLE = "title";
  private static final String ATTR_CREATION_DATE = "creationDate";
  private static final String ATTR_LAST_MESSAGE_DATE = "lastMessageDate";

  // Additional constants
  private static final String JSON_EXTENSION = ".json";

  private final Gson gson;
  private final AuthStatusManager authStatusManager;

  /**
   * Constructor for ConversationPersistenceService.
   *
   * @param authStatusManager the authentication status manager
   */
  protected ConversationPersistenceService(AuthStatusManager authStatusManager) {
    this.authStatusManager = authStatusManager;
    this.gson = new GsonBuilder().registerTypeAdapter(Instant.class, new InstantTypeAdapter())
        .registerTypeAdapter(AbstractTurnData.class, new AbstractTurnDataDeserializer()).setPrettyPrinting().create();
  }

  /**
   * Saves the conversation data to conversation JSON file and updates the index if it's a new conversation or the
   * conversation title / lastMessageDate should be updated.
   */
  protected void saveConversation(ConversationData conversationData) throws IOException {
    String username = authStatusManager.getUserName();
    if (StringUtils.isBlank(username)) {
      CopilotCore.LOGGER.info("Cannot persist conversation: username is blank");
      return;
    }

    saveConversationJson(conversationData, username);
    updateConversationIndex(conversationData, username);
  }

  /** Gets the plugin state location path for the current workspace. */
  private Path getMetadataPath() {
    IPath stateLocation = CopilotCore.getPlugin().getStateLocation();
    return Paths.get(stateLocation.toOSString());
  }

  /**
   * Gets the conversations folder path for the current user. Persists files under: stateLocation/conversations/username
   */
  private Path getConversationsPath(String username) {
    Path baseConversationsPath = getMetadataPath().resolve(CONVERSATIONS_FOLDER_NAME);
    if (StringUtils.isBlank(username)) {
      CopilotCore.LOGGER
          .error(new IllegalStateException("Username is blank when getting and persisting conversations path"));
      return baseConversationsPath;
    }
    return baseConversationsPath.resolve(username);
  }

  /**
   * Saves the conversation data to a JSON file. Return true if a new conversation JSON file is created.
   */
  private boolean saveConversationJson(ConversationData conversationData, String username) throws IOException {
    Path conversationFilePath = getConversationFilePath(conversationData.getConversationId(), username);
    boolean newFileCreated = !Files.exists(conversationFilePath);

    // Ensure parent directories exist
    Path parentDir = conversationFilePath.getParent();
    if (parentDir != null && !Files.exists(parentDir)) {
      Files.createDirectories(parentDir);
    }

    String jsonContent = gson.toJson(conversationData);
    Files.write(conversationFilePath, jsonContent.getBytes(StandardCharsets.UTF_8));

    return newFileCreated;
  }

  /** Updates the conversation index XML file. */
  private void updateConversationIndex(ConversationData conversationData, String username) throws IOException {
    ConversationXmlData summary = new ConversationXmlData(conversationData.getConversationId(),
        conversationData.getTitle(), conversationData.getCreationDate(), conversationData.getLastMessageDate());

    try {
      Document doc = loadOrCreateIndexDocument(username);
      updateUserConversations(doc, username, summary);
      saveIndexDocument(doc, username);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new IOException("Failed to load conversation index document", e);
    } catch (TransformerException e) {
      throw new IOException("Failed to save conversation index document", e);
    }
  }

  /** Updates the conversations for a specific user in the index document. */
  private void updateUserConversations(Document doc, String username, ConversationXmlData summary) {
    Element userElement = getUserElement(doc, username, true);
    Element conversationsElement = getConversationsElement(userElement, true);

    Element conversationElement = findConversationElement(conversationsElement, summary.getConversationId());
    if (conversationElement == null) {
      conversationElement = doc.createElement(ELEMENT_CONVERSATION);
      conversationElement.setAttribute(ATTR_CONVERSATION_ID, summary.getConversationId());
      conversationsElement.appendChild(conversationElement);
    }

    setAttributeOrEmpty(conversationElement, ATTR_TITLE, summary.getTitle());
    setAttributeOrEmpty(conversationElement, ATTR_CREATION_DATE,
        summary.getCreationDate() != null ? summary.getCreationDate().toString() : null);
    setAttributeOrEmpty(conversationElement, ATTR_LAST_MESSAGE_DATE,
        summary.getLastMessageDate() != null ? summary.getLastMessageDate().toString() : null);
  }

  /** Gets the file path for a conversation JSON file. */
  private Path getConversationFilePath(String conversationId, String username) {
    return getConversationsPath(username).resolve(conversationId + JSON_EXTENSION);
  }

  /**
   * Updates the conversation ID in cache and persist it to both the index and the JSON file.
   */
  protected void updatePersistedConversationId(String oldConversationId, String newConversationId) throws IOException {
    if (!isValidConversationIdUpdate(oldConversationId, newConversationId)) {
      return;
    }

    String username = authStatusManager.getUserName();
    Path oldFilePath = getConversationFilePath(oldConversationId, username);
    Path newFilePath = getConversationFilePath(newConversationId, username);

    if (!Files.exists(oldFilePath) || Files.exists(newFilePath)) {
      return;
    }

    Files.move(oldFilePath, newFilePath);

    ConversationData conversationData = loadConversationFromPersistedJsonFile(newConversationId);
    if (conversationData != null) {
      conversationData.setConversationId(newConversationId);
      saveConversationJson(conversationData, username);
    }

    try {
      Document doc = loadOrCreateIndexDocument(username);
      Element userElement = getUserElement(doc, username, false);
      if (userElement != null) {
        Element conversationsElement = findChildElement(userElement, ELEMENT_CONVERSATIONS);
        if (conversationsElement != null) {
          Element conversationElement = findConversationElement(conversationsElement, oldConversationId);
          if (conversationElement != null) {
            conversationElement.setAttribute(ATTR_CONVERSATION_ID, newConversationId);
            saveIndexDocument(doc, username);
          }
        }
      }
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new IOException("Failed to update conversation ID", e);
    } catch (TransformerException e) {
      throw new IOException("Failed to save updated conversation index document", e);
    }
  }

  private boolean isValidConversationIdUpdate(String oldConversationId, String newConversationId) {
    return StringUtils.isNotBlank(oldConversationId) && StringUtils.isNotBlank(newConversationId)
        && !oldConversationId.equals(newConversationId);
  }

  /** Loads the existing index document or creates a new one for the given user. */
  private Document loadOrCreateIndexDocument(String username)
      throws ParserConfigurationException, SAXException, IOException {
    Path indexPath = getXmlIndexFilePath(username);
    if (Files.exists(indexPath)) {
      return loadIndexDocument(username);
    } else {
      return createIndexDocument(username);
    }
  }

  /** Loads an existing index document for the given user. */
  private Document loadIndexDocument(String username) throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilder builder = createSecureDocumentBuilder();

    Path indexPath = getXmlIndexFilePath(username);
    try (FileInputStream fis = new FileInputStream(indexPath.toFile())) {
      Document doc = builder.parse(fis);
      doc.normalize();
      return doc;
    }
  }

  /** Creates a new index document for the given user. */
  private Document createIndexDocument(String username) throws ParserConfigurationException {
    DocumentBuilder builder = createSecureDocumentBuilder();

    Document doc = builder.newDocument();
    Element root = doc.createElement(ELEMENT_USER);
    if (StringUtils.isNotBlank(username)) {
      root.setAttribute(ATTR_USERNAME, username);
    }
    doc.appendChild(root);
    return doc;
  }

  /** Creates a DocumentBuilder with secure configuration. */
  private DocumentBuilder createSecureDocumentBuilder() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setIgnoringElementContentWhitespace(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    return factory.newDocumentBuilder();
  }

  /** Saves the index document to file for the given user. */
  private void saveIndexDocument(Document doc, String username) throws TransformerException, IOException {
    Path indexPath = getXmlIndexFilePath(username);

    // Ensure parent directories exist
    Path parentDir = indexPath.getParent();
    if (parentDir != null && !Files.exists(parentDir)) {
      Files.createDirectories(parentDir);
    }

    // Use DOM Level 3 Load/Save API to serialize
    DOMImplementation impl = doc.getImplementation();
    Object lsFeature = impl.getFeature("LS", "3.0");
    if (!(lsFeature instanceof DOMImplementationLS)) {
      throw new IOException("DOM Level 3 Load/Save not supported by current DOM implementation");
    }

    DOMImplementationLS domImplLs = (DOMImplementationLS) lsFeature;
    LSSerializer serializer = domImplLs.createLSSerializer();
    try {
      serializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
      serializer.getDomConfig().setParameter("xml-declaration", Boolean.TRUE);
    } catch (DOMException ignored) {
      // If the underlying implementation doesn't support these parameters, proceed without failing
    }

    LSOutput lsOutput = domImplLs.createLSOutput();
    lsOutput.setEncoding(StandardCharsets.UTF_8.name());
    try (OutputStream os = Files.newOutputStream(indexPath)) {
      lsOutput.setByteStream(os);
      serializer.write(doc, lsOutput);
    }
  }

  /** Finds a child element by tag name. */
  private Element findChildElement(Element parent, String tagName) {
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
        return (Element) child;
      }
    }
    return null;
  }

  /** Finds a conversation element by conversation ID. */
  private Element findConversationElement(Element conversationsElement, String conversationId) {
    NodeList conversations = conversationsElement.getElementsByTagName(ELEMENT_CONVERSATION);
    for (int i = 0; i < conversations.getLength(); i++) {
      Element conversation = (Element) conversations.item(i);
      if (conversationId.equals(conversation.getAttribute(ATTR_CONVERSATION_ID))) {
        return conversation;
      }
    }
    return null;
  }

  /** Helper to set an attribute value or empty string if null. */
  private void setAttributeOrEmpty(Element element, String attributeName, String value) {
    element.setAttribute(attributeName, value != null ? value : "");
  }

  /** Lists all conversations for the current user. */
  protected List<ConversationXmlData> listConversations() {
    String username = authStatusManager.getUserName();
    if (StringUtils.isBlank(username)) {
      return new ArrayList<>();
    }
    return listConversationsForUser(username);
  }

  /** Lists all conversations for a specific user. */
  protected List<ConversationXmlData> listConversationsForUser(String username) {
    List<ConversationXmlData> conversations = new ArrayList<>();

    Path indexPath = getXmlIndexFilePath(username);
    if (!Files.exists(indexPath)) {
      return conversations;
    }

    try {
      Document doc = loadOrCreateIndexDocument(username);
      Element userElement = getUserElement(doc, username, false);

      if (userElement != null) {
        Element conversationsElement = findChildElement(userElement, ELEMENT_CONVERSATIONS);
        if (conversationsElement != null) {
          NodeList conversationNodes = conversationsElement.getElementsByTagName(ELEMENT_CONVERSATION);
          for (int i = 0; i < conversationNodes.getLength(); i++) {
            Element conversationElement = (Element) conversationNodes.item(i);
            ConversationXmlData summary = parseXmlConversationElement(conversationElement);
            conversations.add(summary);
          }
        }
      }
    } catch (ParserConfigurationException | SAXException | IOException e) {
      CopilotCore.LOGGER.error("Failed to list conversations", e);
      return new ArrayList<>();
    }

    // Sort conversations by last message date (most recent first)
    sortConversationsByLastMessageDate(conversations);
    return conversations;
  }

  private Path getXmlIndexFilePath(String username) {
    return getConversationsPath(username).resolve(INDEX_FILE_NAME);
  }

  private ConversationXmlData parseXmlConversationElement(Element conversationElement) {
    String conversationId = conversationElement.getAttribute(ATTR_CONVERSATION_ID);
    String title = conversationElement.getAttribute(ATTR_TITLE);
    String creationDateStr = conversationElement.getAttribute(ATTR_CREATION_DATE);
    String lastMessageDateStr = conversationElement.getAttribute(ATTR_LAST_MESSAGE_DATE);

    Instant creationDate = parseInstantOrNull(creationDateStr);
    Instant lastMessageDate = parseInstantOrNull(lastMessageDateStr);

    return new ConversationXmlData(conversationId, title, creationDate, lastMessageDate);
  }

  private void sortConversationsByLastMessageDate(List<ConversationXmlData> conversations) {
    conversations.sort((a, b) -> {
      if (a.getLastMessageDate() == null && b.getLastMessageDate() == null) {
        return 0;
      }
      if (a.getLastMessageDate() == null) {
        return 1;
      }
      if (b.getLastMessageDate() == null) {
        return -1;
      }
      return b.getLastMessageDate().compareTo(a.getLastMessageDate());
    });
  }

  /** Helper to parse an Instant from a string, returning null if empty or invalid. */
  private Instant parseInstantOrNull(String dateStr) {
    if (StringUtils.isBlank(dateStr)) {
      return null;
    }
    return Instant.parse(dateStr);
  }

  /**
   * Gets the user element, optionally creating it if it doesn't exist.
   */
  private Element getUserElement(Document doc, String username, boolean createIfMissing) {
    Element root = doc.getDocumentElement();

    if (root == null || !ELEMENT_USER.equals(root.getTagName())) {
      if (!createIfMissing) {
        return null;
      }
      Element userElement = doc.createElement(ELEMENT_USER);
      if (StringUtils.isNotBlank(username)) {
        userElement.setAttribute(ATTR_USERNAME, username);
      }
      doc.appendChild(userElement);
      return userElement;
    }

    return root;
  }

  /**
   * Gets the conversations element for a user, optionally creating it if it doesn't exist. This consolidates the common
   * pattern of finding/creating conversations elements.
   */
  private Element getConversationsElement(Element userElement, boolean createIfMissing) {
    Element conversationsElement = findChildElement(userElement, ELEMENT_CONVERSATIONS);

    if (conversationsElement == null && createIfMissing) {
      conversationsElement = userElement.getOwnerDocument().createElement(ELEMENT_CONVERSATIONS);
      userElement.appendChild(conversationsElement);
    }

    return conversationsElement;
  }

  /**
   * Loads a conversation by ID.
   */
  protected ConversationData loadConversationFromPersistedJsonFile(String conversationId)
      throws IOException, JsonSyntaxException {
    Path conversationFile = getConversationFilePath(conversationId, authStatusManager.getUserName());
    if (!Files.exists(conversationFile)) {
      throw new IOException("Conversation file not found: " + conversationId);
    }
    try {
      String json = new String(Files.readAllBytes(conversationFile), StandardCharsets.UTF_8);
      return gson.fromJson(json, ConversationData.class);
    } catch (JsonSyntaxException e) {
      throw new JsonSyntaxException("Failed to load conversation: " + conversationId, e);
    }
  }

  /**
   * Deletes a conversation by ID, removing both the JSON file and its entry in the index.
   */
  protected void deleteConversation(String conversationId) throws IOException {
    String username = authStatusManager.getUserName();
    if (StringUtils.isBlank(username) || StringUtils.isBlank(conversationId)) {
      return;
    }

    // Delete the conversation JSON file
    Path conversationFilePath = getConversationFilePath(conversationId, username);
    if (Files.exists(conversationFilePath)) {
      Files.delete(conversationFilePath);
    }

    // Update the index XML file
    Path indexPath = getXmlIndexFilePath(username);
    if (!Files.exists(indexPath)) {
      return;
    }

    try {
      Document doc = loadIndexDocument(username);
      if (doc == null) {
        return;
      }

      Element userElement = getUserElement(doc, username, false);
      if (userElement != null) {
        Element conversationsElement = findChildElement(userElement, ELEMENT_CONVERSATIONS);
        if (conversationsElement != null) {
          Element conversationElement = findConversationElement(conversationsElement, conversationId);
          if (conversationElement != null) {
            conversationsElement.removeChild(conversationElement);
            saveIndexDocument(doc, username);
          }
        }
      }
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new IOException("Failed to delete conversation from index", e);
    } catch (TransformerException e) {
      throw new IOException("Failed to save updated conversation index document", e);
    }
  }

  /** Custom Gson type adapter for Instant serialization. */
  private static class InstantTypeAdapter extends TypeAdapter<Instant> {
    @Override
    public void write(JsonWriter out, Instant value) throws IOException {
      if (value == null) {
        out.nullValue();
      } else {
        out.value(value.toString());
      }
    }

    @Override
    public Instant read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      String value = in.nextString();
      return (value == null || value.isEmpty()) ? null : Instant.parse(value);
    }
  }

  /**
   * Deserializer to handle polymorphic AbstractTurnData (UserTurnData vs CopilotTurnData).
   */
  private static class AbstractTurnDataDeserializer implements JsonDeserializer<AbstractTurnData> {
    @Override
    public AbstractTurnData deserialize(JsonElement json, java.lang.reflect.Type typeOfT,
        com.google.gson.JsonDeserializationContext context) throws JsonParseException {
      if (json == null || !json.isJsonObject()) {
        return null;
      }
      JsonObject obj = json.getAsJsonObject();
      // Prefer explicit role field if present
      String role = obj.has("role") && !obj.get("role").isJsonNull() ? obj.get("role").getAsString() : null;
      try {
        if (role != null) {
          if ("user".equalsIgnoreCase(role)) {
            UserTurnData utd = context.deserialize(obj, UserTurnData.class);
            // Ensure role preserved if missing in constructor
            if (utd.getRole() == null) {
              utd.setRole("user");
            }
            return utd;
          } else if ("copilot".equalsIgnoreCase(role) || "assistant".equalsIgnoreCase(role)) {
            CopilotTurnData ctd = context.deserialize(obj, CopilotTurnData.class);
            if (ctd.getRole() == null) {
              ctd.setRole(role.toLowerCase());
            }
            return ctd;
          }
        }
        // Fallback: inspect distinguishing fields
        if (obj.has("message")) {
          UserTurnData utd = context.deserialize(obj, UserTurnData.class);
          if (utd.getRole() == null) {
            utd.setRole("user");
          }
          return utd;
        }
        if (obj.has("reply")) {
          CopilotTurnData ctd = context.deserialize(obj, CopilotTurnData.class);
          if (ctd.getRole() == null) {
            ctd.setRole("copilot");
          }
          return ctd;
        }
      } catch (RuntimeException e) {
        throw new JsonParseException("Failed to deserialize turn: " + json, e);
      }
      throw new JsonParseException("Unknown turn type (missing role/message/reply) in JSON: " + json);
    }
  }
}

# CLS Session Persistence and Restoration for Conversation History

## Overview
This feature integrates CLS (Copilot Language Server) server-side session persistence so that when a conversation is restored the CLS receives the original `conversationId` and `restoreToTurnId`, enabling full context continuation rather than relying solely on IDE-side turn history. It also configures `<user home>/.copilot/eclipse` as the transcript directory (matching IntelliJ's convention).

> **Path note:** On macOS/Linux this directory is `~/.copilot/eclipse`; on Windows it is `%USERPROFILE%\.copilot\eclipse`.

---

## Test Cases

### TC-001: Conversation history displays correctly after session switch

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Copilot Chat is open
- At least one conversation with multiple turns exists in history

#### Steps
1. Create a conversation with at least 3 user–Copilot turn pairs
2. Open chat history and switch to a different conversation
3. Open chat history again and select the original conversation

#### Expected Result
- All user and Copilot turns are displayed in the correct order
- Each turn shows its full content (no missing or truncated messages)
- Turn pairing is correct: each user message is followed by its corresponding Copilot response

#### 📸 Key Screenshots
- [ ] **Before switch** — Full conversation with all turns visible
- [ ] **After restore** — Same conversation restored with all turns in correct order

---

### TC-002: Conversation history persists across Eclipse restarts

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Eclipse is open with Copilot Chat functional
- The `<user home>/.copilot/eclipse` directory does not exist (or is empty) before the test

#### Steps
1. Open Copilot Chat and send at least 2 messages; wait for responses
2. Close Eclipse completely (File → Exit or equivalent)
3. Verify that the `<user home>/.copilot/eclipse` directory exists and contains transcript files
4. Reopen Eclipse and open Copilot Chat
5. Open chat history and select the conversation from before the restart

#### Expected Result
- The conversation appears in chat history with all turns intact
- Continuing the conversation produces contextually aware responses (CLS uses transcript for context)

#### 📸 Key Screenshots
- [ ] **Transcript directory** — `<user home>/.copilot/eclipse` folder visible with transcript files
- [ ] **After restart** — Conversation restored in chat history after Eclipse reopen

---

### TC-003: New conversation starts without interference from restored state

**Type:** `Regression`
**Priority:** `P0`

#### Preconditions
- At least one existing conversation in chat history

#### Steps
1. Open chat history and restore an existing conversation
2. Click "New Chat" to start a fresh conversation
3. Send a message unrelated to any prior conversation
4. Verify Copilot responds without referencing prior conversations

#### Expected Result
- New conversation has a clean context; no cross-contamination from the restored conversation
- CLS receives a new `conversationId` (no `restoreToTurnId` for a brand-new conversation)

---

### TC-004: Transcript directory created on first startup

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- `<user home>/.copilot/eclipse` directory does not exist

#### Steps
1. Start Eclipse with the Copilot plugin installed
2. Wait for the Copilot language server to finish initializing (status bar shows Copilot ready)
3. Check the file system for `<user home>/.copilot/eclipse`

#### Expected Result
- `<user home>/.copilot/eclipse` directory is created automatically on startup
- The path mirrors IntelliJ's `<user home>/.copilot/jb` convention (different subdirectory, same parent)

#### 📸 Key Screenshots
- [ ] **Directory exists** — File explorer showing `<user home>/.copilot/eclipse` after startup

---

### TC-005: Partially completed conversation restores to the last completed turn

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions
- Copilot Chat is open in Agent mode

#### Steps
1. Send a message and wait for Copilot to begin responding
2. Click Cancel while the response is still streaming
3. Open chat history, switch to a different conversation
4. Open chat history again and select the original conversation
5. Send a new follow-up message

#### Expected Result
- The restored conversation shows the partial/cancelled turn in the UI
- The `restoreToTurnId` sent to CLS corresponds to the last *fully completed* turn, not the cancelled one
- Copilot's next response is coherent with the conversation up to the last completed turn

#### 📸 Key Screenshots
- [ ] **After cancel** — Conversation with cancelled/partial turn visible
- [ ] **After restore + new message** — New response coherent with completed context

---

### TC-006: No 400 Bad Request after restoring a tool-call conversation

**Type:** `Regression`
**Priority:** `P0`

#### Preconditions
- Copilot Chat is open in Agent mode
- A workspace with at least one source file is open

#### Steps
1. Send a message that triggers at least one tool call and gets a full assistant response (e.g. "list the files in this project and summarise what each one does")
2. Wait for the full multi-turn exchange to complete (tool calls executed, final assistant reply shown)
3. Restart Eclipse IDE
4. Open Copilot Chat and restore the previous conversation from history
5. Send a new follow-up message in the restored conversation

#### Expected Result
- The follow-up message is sent without any 400 Bad Request error
- The assistant replies successfully on the first attempt (no error banner)

#### 📸 Key Screenshots
- [ ] **Completed original turn** — Chat showing the tool-call exchange and final assistant reply before restart
- [ ] **After restore + reply** — Restored history with successful follow-up reply and no error

---

### TC-007: Restored history shows no duplicated user messages

**Type:** `Regression`
**Priority:** `P0`

#### Preconditions
- Copilot Chat is open in Agent mode
- A workspace with at least one source file is open

#### Steps
1. Send a message that triggers at least one tool call (e.g. "read the contents of README.md and explain it")
2. Wait for the full response including the assistant's explanation
3. Note the exact assistant response text
4. Restart Eclipse IDE
5. Open Copilot Chat and restore the conversation from history
6. Inspect the restored conversation turns

#### Expected Result
- The assistant's previous response text does not appear as a user message in the restored conversation
- Each turn shows the correct role (user prompt → tool call → assistant reply), with no duplicated content

#### 📸 Key Screenshots
- [ ] **Original conversation** — Chat showing the original tool-call turn and assistant reply
- [ ] **Restored conversation** — Restored history with correctly attributed messages and no duplicates

---

### TC-008: Cancelled terminal tool call stays cancelled after restoration

**Type:** `Regression`
**Priority:** `P0`

#### Preconditions
- Copilot Chat is open in Agent mode
- A workspace is open so the `run_in_terminal` tool can execute
- A model that supports tool calls is selected

#### Steps
1. Send a message that makes Copilot use the `run_in_terminal` tool to perform a longer-running task (e.g. "run a command that takes a while, such as `ping -n 30 127.0.0.1`")
2. While the terminal tool call is still running (spinner active on the tool status), click Cancel on the chat response
3. Verify the terminal tool call status updates to the **cancelled** icon (no longer spinning)
4. Click "New Chat" to start a fresh conversation
5. Open chat history and select the original conversation to restore it

#### Expected Result
- After restoration, the terminal tool call still shows the **cancelled** icon
- The tool call status does **not** revert to running/spinning (no rotating/ongoing indicator reappears)
- The cancelled status is consistent between the live cancel and the restored view

#### 📸 Key Screenshots
- [ ] **After cancel** — Terminal tool call showing the cancelled icon (not spinning)
- [ ] **After restore** — Same terminal tool call restored with the cancelled icon, not reverted to running

---

## Screenshots Checklist
> Consolidated list of all key screenshot moments.

- [ ] `TC-001` Full conversation before session switch
- [ ] `TC-001` Same conversation after restore, all turns in correct order
- [ ] `TC-002` `<user home>/.copilot/eclipse` directory with transcript files
- [ ] `TC-002` Conversation restored in history after Eclipse restart
- [ ] `TC-004` `<user home>/.copilot/eclipse` directory created on first startup
- [ ] `TC-005` Conversation with cancelled turn visible
- [ ] `TC-005` New response coherent with completed context after restore
- [ ] `TC-006` Completed tool-call turn before restart
- [ ] `TC-006` Restored history with successful follow-up and no 400 error
- [ ] `TC-007` Original conversation showing assistant reply
- [ ] `TC-007` Restored conversation with correctly attributed messages and no duplicates
- [ ] `TC-008` Cancelled terminal tool call showing cancelled icon after cancel
- [ ] `TC-008` Restored terminal tool call still cancelled, not reverted to running

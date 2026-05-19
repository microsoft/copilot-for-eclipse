# Subagent Progress Events Leak into Unrelated Conversation UI When Switching Sessions

## Overview
When a subagent is executing and the user switches to a different conversation via history, subagent progress events should not appear in the newly opened conversation. This verifies that `ChatView.onChatProgress()` correctly filters events by conversationId so only events belonging to the currently displayed conversation (including its active subagent) are rendered.

---

## Test Cases

### TC-001: Subagent progress does not bleed into a different conversation

**Type:** `Regression`
**Priority:** `P0`

#### Preconditions
- Copilot chat is open in Agent mode
- At least one prior conversation exists in chat history (or a second conversation can be started)

#### Steps
1. Send a message in Agent mode that triggers a subagent execution (e.g. "create a new file and write a hello world program")
2. Observe the subagent progress spinner appearing in the chat UI
3. While the subagent is still running (spinner visible), open the chat history panel
4. Select a different (previously existing) conversation to switch to it
5. Observe the chat UI for the newly opened conversation while the subagent continues running in the background

#### Expected Result
- The newly opened conversation displays only its own historical messages
- No subagent progress events, tool call results, or partial output from the background subagent appear in this conversation
- The background subagent completes without corrupting the currently displayed conversation

#### 📸 Key Screenshots
- [ ] **Subagent running** — Subagent spinner visible in the original conversation
- [ ] **After switch** — Newly opened conversation showing clean, uncontaminated history

---

### TC-002: Subagent progress still renders correctly in its own conversation

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Copilot chat is open in Agent mode

#### Steps
1. Send a message that triggers a subagent execution
2. Observe the subagent progress events appearing in the correct conversation
3. Wait for the subagent to complete
4. Verify all tool calls and output are rendered in the originating conversation

#### Expected Result
- All subagent progress events appear only in the conversation that initiated the subagent
- Tool call results and final output are correctly displayed in that conversation

#### 📸 Key Screenshots
- [ ] **Subagent output** — Completed subagent progress and results in the originating conversation

---

### TC-003: Switch back to the original conversation shows completed subagent results

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- Copilot chat is open in Agent mode
- At least one prior conversation exists in chat history

#### Steps
1. Send a message that triggers a subagent execution
2. While the subagent is running, switch to a different conversation via chat history
3. Wait for the subagent to finish (no UI indication expected in the switched conversation)
4. Open chat history and switch back to the original conversation

#### Expected Result
- The original conversation shows the completed subagent results and all progress events that occurred while the user was away
- The intermediate conversation that was viewed remains uncontaminated

#### 📸 Key Screenshots
- [ ] **After return** — Original conversation with complete subagent output intact

---

### TC-005: New conversation started while subagent runs in background

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions
- Copilot chat is open in Agent mode

#### Steps
1. Send a message that triggers a subagent execution
2. While the subagent is running, click "New Chat" to start a fresh conversation
3. Observe the new (empty) conversation UI while the subagent continues running
4. Send a message in the new conversation and wait for a response

#### Expected Result
- The new conversation remains empty (no subagent leak) until the user's own message is sent
- The new conversation's response is not mixed with the background subagent's output

#### 📸 Key Screenshots
- [ ] **New chat during subagent** — Empty new conversation with no leaked subagent events

---

## Screenshots Checklist
> Consolidated list of all key screenshot moments.

- [ ] `TC-001` Subagent spinner visible in original conversation
- [ ] `TC-001` Newly opened conversation uncontaminated after switch
- [ ] `TC-002` Completed subagent output in originating conversation
- [ ] `TC-003` Original conversation with full subagent results after switching back
- [ ] `TC-005` Empty new conversation showing no leaked subagent events

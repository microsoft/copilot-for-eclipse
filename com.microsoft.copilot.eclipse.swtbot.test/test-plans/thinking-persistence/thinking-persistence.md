# Thinking Block Persistence & Restoration

## Overview
Thinking blocks (the collapsible "Thinking" banners shown during model reasoning) are persisted to conversation history so they survive session switches and history restoration. This covers content, state (completed/cancelled), and generated titles for both main agent and subagent thinking blocks.

---

## Test Cases

### TC-001: Completed thinking restores after session switch

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Copilot chat is open in Agent mode
- A model that supports thinking is selected (e.g. Claude Sonnet 4.6)

#### Steps
1. Send a message that triggers thinking (e.g. "think about what improvements this file needs")
2. Wait for thinking to complete (spinner stops, title appears on the thinking block header)
3. Open chat history, select a different conversation (or "New Chat")
4. Open chat history again, select the original conversation

#### Expected Result
The thinking block appears collapsed with its generated title, expandable to show full thinking content.

#### 📸 Key Screenshots
- [ ] **Before switch** — Thinking block with title visible in completed state
- [ ] **After restore** — Same thinking block restored with title and expandable content

---

### TC-002: Cancelled thinking restores after session switch

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Copilot chat is open in Agent mode
- A model that supports thinking is selected

#### Steps
1. Send a message that triggers thinking
2. While thinking is streaming (spinner active), click Cancel
3. Verify the thinking block shows cancelled icon
4. Open chat history, select a different conversation
5. Open chat history again, select the original conversation

#### Expected Result
The cancelled thinking block restores with cancelled state and partial content visible on expand.

#### 📸 Key Screenshots
- [ ] **After cancel** — Thinking block with cancelled icon
- [ ] **After restore** — Same cancelled thinking block restored

---

### TC-003: Multiple thinking blocks in one turn

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Copilot chat is open in Agent mode
- A model that supports thinking is selected

#### Steps
1. Send a message that triggers multiple rounds of thinking interleaved with tool calls (e.g. a complex multi-step task)
2. Wait for the full response to complete
3. Verify multiple thinking blocks are shown, each with its own title
4. Open chat history, select a different conversation and switch back

#### Expected Result
All thinking blocks restore in correct order, each with its own title and content, positioned before their corresponding tool calls.

#### 📸 Key Screenshots
- [ ] **Before switch** — Multiple thinking blocks visible in the turn
- [ ] **After restore** — All thinking blocks restored in correct positions

---

### TC-004: Subagent thinking restores after session switch

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- Copilot chat is open in Agent mode
- A model that supports thinking and subagents is selected

#### Steps
1. Send a message that triggers a subagent with thinking (e.g. "use the test agent to analyze this file step by step")
2. Wait for the subagent to complete with thinking
3. Verify the subagent's thinking block appears inside the subagent message block
4. Open chat history, select a different conversation and switch back

#### Expected Result
The subagent's thinking block restores inside the subagent message block with title and content.

#### 📸 Key Screenshots
- [ ] **Before switch** — Subagent thinking block visible inside subagent block
- [ ] **After restore** — Subagent thinking block restored correctly

---

### TC-005: Cancelled subagent thinking persists

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions
- Copilot chat is open in Agent mode
- A model that supports thinking and subagents is selected

#### Steps
1. Send a message that triggers a subagent
2. While the subagent is thinking (spinner active inside subagent block), click Cancel
3. Verify the subagent thinking shows cancelled state
4. Open chat history, select a different conversation and switch back

#### Expected Result
The cancelled subagent thinking block restores with cancelled state.

#### 📸 Key Screenshots
- [ ] **After cancel** — Subagent thinking block with cancelled icon
- [ ] **After restore** — Cancelled subagent thinking block restored

## Screenshots Checklist
> Consolidated list of all key screenshot moments.

- [ ] `TC-001` Completed thinking before switch
- [ ] `TC-001` Completed thinking after restore
- [ ] `TC-002` Cancelled thinking after cancel
- [ ] `TC-002` Cancelled thinking after restore
- [ ] `TC-003` Multiple thinking blocks before switch
- [ ] `TC-003` Multiple thinking blocks after restore
- [ ] `TC-004` Subagent thinking before switch
- [ ] `TC-004` Subagent thinking after restore
- [ ] `TC-005` Cancelled subagent thinking after cancel
- [ ] `TC-005` Cancelled subagent thinking after restore

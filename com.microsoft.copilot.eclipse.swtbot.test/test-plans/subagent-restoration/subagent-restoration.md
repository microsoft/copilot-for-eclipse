# Subagent Turns Appear as Separate Assistant Messages After Restoration

## Overview
When a subagent executes during an agent-mode conversation, its turns were previously persisted as independent `CopilotTurnData` entries. On restoration from history, those turns rendered as standalone assistant messages instead of being nested inside the parent agent turn.

---

## Test Cases

### TC-001: Subagent turn restores nested inside parent agent turn

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Copilot chat is open in Agent mode
- A prompt that reliably triggers subagent execution is available (e.g. a task that invokes `run_subagent`)

#### Steps
1. Send a message in Agent mode that causes a subagent to execute (e.g. "analyze all Java files in the workspace using a subagent")
2. Wait for the subagent to complete and the full response to finish streaming
3. Note the structure of the response: the subagent output should appear nested/indented inside the parent agent turn, **not** as a separate assistant message below it
4. Open chat history (the history panel), select a different conversation or click "New Chat"
5. Open chat history again and select the original conversation to restore it

#### Expected Result
- The restored conversation shows the subagent output nested inside the parent agent turn, matching the structure visible before the session switch
- No additional standalone assistant message appears below the parent turn for the subagent execution

#### 📸 Key Screenshots
- [ ] **Before switch** — Full conversation view showing subagent output nested inside parent turn
- [ ] **After restore** — Same conversation restored with subagent output still nested, no orphaned message

---

### TC-002: Multiple subagent turns all restore nested correctly

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Copilot chat is open in Agent mode
- A prompt that triggers multiple sequential subagent invocations is available

#### Steps
1. Send a message that causes two or more subagent invocations in a single parent turn
2. Wait for all subagents to complete
3. Verify each subagent's output is shown nested inside the parent turn (no orphaned messages)
4. Switch to a different conversation via chat history, then switch back to restore the original

#### Expected Result
- All subagent turns restore nested inside the parent agent turn
- The number of visible assistant messages in the turn matches what was shown before the session switch
- No orphaned standalone assistant messages appear for any subagent

#### 📸 Key Screenshots
- [ ] **Before switch** — Multiple subagent outputs nested inside one parent turn
- [ ] **After restore** — All subagent outputs still nested, none appear as separate messages

---

### TC-003: Conversation with no subagent is unaffected

**Type:** `Regression`
**Priority:** `P0`

#### Preconditions
- Copilot chat is open in Agent mode

#### Steps
1. Send a message that produces a normal (non-subagent) agent response
2. Wait for the response to complete
3. Switch to a different conversation via chat history, then switch back to restore the original

#### Expected Result
- The restored conversation looks identical to before the session switch
- No duplicate or extra assistant messages appear

---

### TC-004: Partially-streamed subagent turn (cancelled) restores correctly

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions
- Copilot chat is open in Agent mode

#### Steps
1. Send a message that triggers a subagent execution
2. While the subagent is still streaming (before it completes), click the Cancel button
3. Observe the state of the conversation — the parent turn and any partial subagent output
4. Switch to a different conversation via chat history, then switch back

#### Expected Result
- The restored conversation reflects the cancelled state
- The partial subagent output (if any) is shown nested inside the parent turn, not as a separate assistant message

#### 📸 Key Screenshots
- [ ] **After cancel** — Conversation showing cancelled subagent output nested in parent turn
- [ ] **After restore** — Same nested structure restored after session switch

---

## Screenshots Checklist
> Consolidated list of all key screenshot moments.

- [ ] `TC-001` Subagent nested in parent turn before switch
- [ ] `TC-001` Subagent nested in parent turn after restore
- [ ] `TC-002` Multiple subagents nested before switch
- [ ] `TC-002` Multiple subagents nested after restore
- [ ] `TC-004` Cancelled subagent nested after cancel
- [ ] `TC-004` Cancelled subagent nested after restore

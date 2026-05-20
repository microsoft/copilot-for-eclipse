# Support Skills and Prompt Files

## Overview
This feature adds support for agent skills (`SKILL.md`) and custom prompt files (`.prompt.md`) in Agent Mode. Skills and prompts are discovered from workspace projects and user-global directories, exposed as slash commands in the chat input, and controlled by an **Enable Skills** toggle in `Window → Preferences → Copilot → Chat`.

---

## Test Cases

### TC-001: Enable Skills toggle appears in Chat preferences

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Eclipse is running with the Copilot plugin installed
- A valid GitHub Copilot subscription is active (client-preview feature flag must be on)

#### Steps
1. Open **Window → Preferences → Copilot → Chat**
2. Observe the preference page content

#### Expected Result
- An **Enable Skills** checkbox is visible
- A note below it reads "Controls whether agent skills can be used to enrich chat context."
- The checkbox is checked by default (enabled)

#### 📸 Key Screenshots
- [ ] **Preferences page** — Chat preferences showing the Enable Skills toggle (checked)

---

### TC-002: Workspace-scoped skill appears as slash command in Agent mode

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Eclipse is running with a project open in the workspace
- Copilot chat is open in **Agent** mode
- **Enable Skills** is checked in preferences

#### Steps
1. Create the directory `.github/skills/my-skill/` inside any open project
2. Create `SKILL.md` inside that directory with the following content:
   ```markdown
   ---
   name: My Skill
   description: A test skill for verification
   ---
   This skill provides test context.
   ```
3. In the Copilot chat input (Agent mode), type `/` and observe the slash command popup

#### Expected Result
- `my-skill` appears in the slash command list with description "A test skill for verification"
- The display name shown is `My Skill` (from `shortDescription`/name front matter)

#### 📸 Key Screenshots
- [ ] **Slash command popup** — `/my-skill` visible in the Agent mode autocomplete list

---

### TC-003: Prompt file appears as slash command in Agent mode

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Eclipse is running with a project open in the workspace
- Copilot chat is open in **Agent** mode
- **Enable Skills** is checked in preferences

#### Steps
1. Create a `.github/` directory (or any supported location) inside the project
2. Create a file named `review.prompt.md` in the project with some markdown content
3. In the Copilot chat input (Agent mode), type `/` and observe the slash command popup

#### Expected Result
- `review` (or the prompt file's ID) appears in the slash command list
- The template is listed alongside built-in slash commands

#### 📸 Key Screenshots
- [ ] **Slash command popup** — Custom prompt file visible as slash command in Agent mode

---

### TC-004: Skills do NOT appear in Ask mode

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- A workspace-scoped `SKILL.md` exists (as created in TC-002)
- **Enable Skills** is checked in preferences

#### Steps
1. Switch the Copilot chat to **Ask** mode
2. In the chat input, type `/` and observe the slash command popup

#### Expected Result
- The skill (`my-skill`) does **not** appear in the Ask mode slash command list
- Only built-in `chat-panel`-scoped commands are shown

#### 📸 Key Screenshots
- [ ] **Ask mode popup** — Skill absent from the slash command list in Ask mode

---

### TC-005: Disabling Skills hides skills from slash command list

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- A workspace-scoped `SKILL.md` exists and was visible in Agent mode (TC-002 completed)

#### Steps
1. Open **Window → Preferences → Copilot → Chat**
2. Uncheck **Enable Skills** and click **Apply and Close**
3. In the Copilot chat (Agent mode), type `/` and observe the slash command popup

#### Expected Result
- The skill is no longer listed in the slash command popup
- Built-in slash commands (e.g. `/fix`, `/explain`) still appear if scoped for Agent mode

#### 📸 Key Screenshots
- [ ] **Preferences page** — Enable Skills unchecked
- [ ] **Slash command popup** — Skill absent after disabling

---

### TC-006: Auto-refresh when SKILL.md is added or removed

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- Eclipse is running with a project open
- Copilot chat is open in Agent mode
- **Enable Skills** is checked

#### Steps
1. In the chat input, type `/` — note that the skill does not yet exist
2. In Eclipse's Project Explorer, create `.github/skills/refresh-skill/SKILL.md` with a description
3. Without restarting, return to the chat input and type `/` again
4. Then delete the `SKILL.md` file and type `/` once more

#### Expected Result
- After adding: `refresh-skill` appears in the slash command list without restarting Eclipse
- After deleting: `refresh-skill` disappears from the slash command list without restarting Eclipse

#### 📸 Key Screenshots
- [ ] **Before add** — Slash command popup without the skill
- [ ] **After add** — Slash command popup with the new skill
- [ ] **After delete** — Slash command popup with skill removed

---

### TC-007: User-global skill discovery

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- Copilot chat is open in Agent mode
- **Enable Skills** is checked

#### Steps
1. Create `~/.copilot/skills/global-skill/SKILL.md` with a name and description in YAML front matter
2. Restart Eclipse (or wait for auto-refresh)
3. In the Copilot chat input (Agent mode), type `/`

#### Expected Result
- `global-skill` appears in the slash command list, available regardless of which workspace is open

#### 📸 Key Screenshots
- [ ] **Slash command popup** — User-global skill visible in Agent mode

---

### TC-008: Skill prefix filtering in slash command autocomplete

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- At least two skills are available in Agent mode (e.g., `my-skill` and `other-skill`)

#### Steps
1. In the Copilot chat input (Agent mode), type `/my`
2. Observe the filtered slash command popup

#### Expected Result
- Only commands matching "my" are shown (e.g., `my-skill` visible, `other-skill` absent)

#### 📸 Key Screenshots
- [ ] **Filtered popup** — Autocomplete showing only matching skills for typed prefix

---

### TC-009: Skills settings persist across Eclipse restarts

**Type:** `Regression`
**Priority:** `P1`

#### Preconditions
- **Enable Skills** preference is visible

#### Steps
1. Open **Window → Preferences → Copilot → Chat** and uncheck **Enable Skills**
2. Click **Apply and Close**
3. Restart Eclipse
4. Open **Window → Preferences → Copilot → Chat**

#### Expected Result
- **Enable Skills** remains unchecked after restart, confirming the preference is persisted

---

## Screenshots Checklist
> Consolidated list of all key screenshot moments.

- [ ] `TC-001` Chat preferences showing the Enable Skills toggle (checked)
- [ ] `TC-002` Slash command popup with `/my-skill` visible in Agent mode
- [ ] `TC-003` Custom prompt file visible as slash command in Agent mode
- [ ] `TC-004` Ask mode popup without skill in slash command list
- [ ] `TC-005` Enable Skills unchecked in preferences
- [ ] `TC-005` Slash command popup without skill after disabling
- [ ] `TC-006` Slash command popup before skill is added
- [ ] `TC-006` Slash command popup after skill is added
- [ ] `TC-006` Slash command popup after skill is deleted
- [ ] `TC-007` User-global skill visible in Agent mode slash command popup
- [ ] `TC-008` Filtered autocomplete showing only matching skills for typed prefix

---
name: endgame
description: Orchestrate endgame verification for a GitHub milestone issue. Fetches the issue, parses assigned tasks, delegates each task to a subagent that researches the linked PR/issue and writes a test plan, and saves every plan to com.microsoft.copilot.eclipse.swtbot.test/test-plans/ following the project's standard test-plan format.
---

# Endgame Verification Skill

Use this skill to run an endgame verification pass for a GitHub milestone issue.
Each task in the issue is delegated to a subagent that researches the linked
PR/issue and writes a test plan under `com.microsoft.copilot.eclipse.swtbot.test/test-plans/`.

## Workflow

**⚠️ CRITICAL — YOU MUST FOLLOW THESE RULES:**

1. **Do NOT run `gh pr view` or `gh issue view` for individual tasks** — only
   run it once for the main endgame issue.
2. **Do NOT research or analyse any task yourself.**
3. **Do NOT create any test-plan files yourself.**
4. **IMMEDIATELY call `runSubagent` for each task** after parsing the issue —
   no delays, no research.

---

### Step 1 — Ask the user for inputs

Ask for:
- The GitHub endgame issue URL
  (e.g. `https://github.com/microsoft/copilot-for-eclipse/issues/XXXX`)
- The user's GitHub account name

---

### Step 2 — Fetch the endgame issue (ONE call only)

```shell
gh issue view <issue_number> --repo microsoft/copilot-for-eclipse --json body --jq '.body'
```

Parse the body to find **all tasks (checkboxes) assigned to the specified
user**. Extract each task's title and any linked PR/issue URL as plain text.

**STOP — do NOT fetch any of the linked PRs or issues.**

---

### Step 3 — Process each task via subagent

For each task, call `runSubagent` with:
- **description**: `"Endgame: <short_task_title>"`
- **prompt**: the template below, filled in with only the info you extracted.

#### Subagent prompt template

---
## Task Details
- Task Number: \<N\>
- Task Title: \<task_title\>
- Assignee: \<username\>
- Related Issue/PR: \<link_if_available\> (NOT YET FETCHED — you must fetch this)

## Your Mission

YOU (the subagent) must research this task AND create the test-plan file.

### Step 1: Research the task

- Fetch the linked PR or issue with `gh pr view` / `gh issue view`.
- Understand what feature or fix needs to be verified.
- **If anything is ambiguous or unclear after reading the PR/issue, ask the
  user for clarification before proceeding. Keep asking until you have enough
  information to write concrete, accurate test steps.**

### Step 2: Create the test-plan file

Determine a short `<feature-slug>` (lowercase, hyphens, no special chars)
derived from the task title (e.g. `chat-history-restore`).

Create the directory and file:

```
com.microsoft.copilot.eclipse.swtbot.test/test-plans/<feature-slug>/<feature-slug>.md
```

Use **exactly** this format, matching the project's existing test plans
(see `com.microsoft.copilot.eclipse.swtbot.test/test-plans/thinking-persistence/thinking-persistence.md` for a live
example):

```markdown
# <Feature / Task Title>

## Overview
<1–3 sentences: what is being verified and why it matters.>

---

## Test Cases

### TC-001: <Specific test case title>

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- <Specific state required before starting these steps>

#### Steps
1. <Detailed, concrete step>
2. <Next step>
3. <Continue as needed>

#### Expected Result
- <Observable outcome that proves the feature works>

#### 📸 Key Screenshots
- [ ] **<Label>** — <What to capture>

---

### TC-002: <Next scenario if needed>
<!-- Repeat TC block for each distinct scenario. Use TC-003, TC-004, … -->

---

## Screenshots Checklist
> Consolidated list of all key screenshot moments.

- [ ] `TC-001` <Label>
- [ ] `TC-002` <Label>
```

Guidelines:
- **Overview**: 1–3 sentences max. Do not repeat what is already covered by
  Preconditions or Steps.
- **Type** values: `Happy Path`, `Negative`, `Edge Case`, `Regression`.
- **Priority** values: `P0` (must-pass), `P1` (high), `P2` (medium).
- Use 3-digit zero-padded TC numbers: TC-001, TC-002, TC-003, …
- Omit `📸 Key Screenshots` within a TC block if there are no meaningful
  screenshots to capture for that case.
- Keep the `## Screenshots Checklist` section at the end — list every
  screenshot across all TC blocks.
- If you are still unsure about any step after researching, **ask the user**
  before writing that step — do not guess.

### Step 3: Return a brief summary

Return ONLY:
- File path created (e.g. `com.microsoft.copilot.eclipse.swtbot.test/test-plans/chat-history-restore/chat-history-restore.md`)
- One-line summary of what needs to be verified

---

### Step 4 — Final summary

After all subagents complete, provide:
- A table of all generated test-plan files
- Total tasks processed
- Any tasks that could not be processed (with reasons)

---

## Notes

- **NEVER run `gh pr view` or `gh issue view` on task links yourself** — only
  on the main endgame issue. Subagents handle the individual links.
- **NEVER analyse or research tasks yourself** — immediately delegate to
  subagents.
- Each subagent runs independently.
- Subagents should be concise — create the file and return a brief summary.
- If a task is still unclear after the user is asked, note the outstanding
  question inside the test-plan file.
- Use slugified task titles for the feature slug and directory name
  (lowercase, hyphens, no special characters).

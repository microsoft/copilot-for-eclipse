# Endgame Verification Prompt

## Instructions

**⚠️ CRITICAL - YOU MUST FOLLOW THESE RULES:**
1. **Do NOT run `gh pr view` or `gh issue view` for individual tasks** - only run it once for the main endgame issue
2. **Do NOT research or analyze any task yourself**
3. **Do NOT create any verification files yourself**
4. **IMMEDIATELY call `runSubagent` for each task** after parsing the issue - no delays, no research

Your workflow is ONLY:
1. Create initial todo: "Fetch endgame issue and parse tasks"
2. Fetch the endgame issue (ONE `gh issue view` call)
3. Parse to get task list, then ADD a todo item for each task found
4. Create output directory
5. Call `runSubagent` for each task - mark todo complete when subagent returns

### Steps

1. **Create initial todo list**:
   Use `manage_todo_list` to create the first todo:
   - Todo 1: "Fetch endgame issue" (mark as in-progress)

2. **Ask the user** for the following information:
   - The GitHub endgame issue link (e.g., `https://github.com/microsoft/copilot-for-eclipse/issues/XXXX`)
   - The user's GitHub account name

3. **Fetch the endgame issue** (this is the ONLY `gh issue view` you should run):
   ```shell
   gh issue view <issue_number> --repo microsoft/copilot-for-eclipse
   ```
   Parse the issue body to find all tasks (checkboxes) assigned to the specified user.
   Extract the task title and any linked PR/issue URL as plain text.
   **STOP - do NOT fetch any of the linked PRs or issues.**

4. **Update todo list with all tasks found**:
   Use `manage_todo_list` to:
   - Mark "Fetch endgame issue" as completed
   - ADD a new todo for each task found (e.g., "Task 1: <title>", "Task 2: <title>", etc.)
   - All new task todos should be "not-started"

5. **Create the output directory**:
   ```shell
   mkdir -p .github/endgame/<issue_number>
   ```

6. **For each task, mark todo as in-progress, then call `runSubagent`**:
   
   For each task:
   1. Update todo list - mark that task as "in-progress"
   2. Call `runSubagent` tool with:
      - **description**: "Endgame: <short_task_title>"
      - **prompt**: The template below filled in with only the info you extracted
   3. When subagent returns, mark that task's todo as "completed"
   
   **Subagent prompt template**:
   
   ---
   ## Task Details
   - Task Number: <N>
   - Task Title: <task_title>
   - Assignee: <username>
   - Related Issue/PR: <link_if_available> (NOT YET FETCHED - you must fetch this)
   
   ## Your Mission
   YOU (the subagent) must research this task AND create the verification file.
   
   ### Step 1: Research the Task
   - If there is a linked PR/issue, fetch it using `gh pr view` or `gh issue view`
   - Understand what feature/fix needs to be verified
   - **If anything is ambiguous or unclear after reading the PR/issue, ask the user
     for clarification before proceeding. Keep asking until you have enough
     information to write concrete, accurate test steps.**
   
   ### Step 2: Create Verification File (YOU must create this file)
   Create the file: `.github/endgame/<issue_number>/<N>_<task_slug>.md`
   
   Use **exactly** this format (modelled on the project test-plan style under
   `com.microsoft.copilot.eclipse.swtbot.test/test-plans/`):
   
   ```markdown
   # <Feature / Task Title>
   
   ## Overview
   <1-3 sentences: what is being verified and why it matters.>
   
   Entry points:
   - <Primary UI/menu path to reach the feature>
   
   Not exercised:
   - <Known out-of-scope items, or omit this block if none>
   
   ---
   
   ## Prerequisites
   
   - Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and activated.
   - <Additional prerequisite specific to this feature>
   
   ---
   
   ## 1. <Scenario group name, e.g. "Happy-path verification">
   
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
   - <Second outcome if needed>
   
   #### Key Screenshots
   - [ ] **<Label>** -- <What to capture in this screenshot>
   
   #### Notes on failure modes
   - <Common failure symptom> -- <Likely cause and where to look>
   ```
   
   Guidelines for filling in the template:
   - **Overview**: 1-3 sentences max. Do not repeat what is already covered by
     Prerequisites or the Steps.
   - **Type** values: `Happy Path`, `Negative`, `Edge Case`, `Regression`.
   - **Priority** values: `P0` (must-pass), `P1` (high), `P2` (medium).
   - Add additional `### TC-NNN` blocks (3-digit zero-padded: TC-002, TC-003, …) if multiple distinct scenarios are needed.
   - Omit `#### Notes on failure modes` if there are no obvious failure modes to call out.
   - If you are still unsure about any step after researching, **ask the user**
     before writing that step -- do not guess.
   
   ### Step 3: Return Summary
   Return ONLY:
   - File path created
   - One-line summary of what needs to be verified
   ---

7. **After all subagents complete**, provide:
   - Summary table of all generated verification files
   - Total tasks processed
   - Any tasks that could not be processed (with reasons)

---

## Notes

- **NEVER run `gh pr view` or `gh issue view` on task links** - only on the main endgame issue
- **NEVER analyze or research tasks yourself** - immediately delegate to subagents
- Each subagent runs independently
- Subagents should be concise - create the file and return a brief summary
- If a task is unclear after asking the user, note the outstanding question in the verification file
- Use slugified task titles for filenames (lowercase, hyphens, no special chars)

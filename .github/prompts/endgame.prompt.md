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
   - If there's a linked PR/issue, fetch it using `gh pr view` or `gh issue view`
   - Understand what feature/fix needs to be verified
   - Identify the affected areas of the codebase
   
   ### Step 2: Create Verification File (YOU must create this file)
   Create the file: `.github/endgame/<issue_number>/<N>_<task_slug>.md`
   
   Use this format:
   
   ### Task: <Task Title>
   **Assignee:** <Name>
   **Issue/PR:** <Link>
   
   #### Context
   [Brief description of what this task involves]
   
   #### Prerequisites
   - [ ] [Any setup needed before testing]
   
   #### Steps to Verify
   1. [ ] [Detailed step 1]
   2. [ ] [Detailed step 2]
   3. [ ] [Detailed step 3]
   
   #### Expected Results
   - [Expected outcome 1]
   - [Expected outcome 2]
   
   #### Edge Cases to Test
   - [Edge case 1]
   - [Edge case 2]
   
   #### Status
   - [ ] Not Started
   - [ ] In Progress
   - [ ] Completed
   - [ ] Blocked (reason: ___)
   
   ### Step 3: Return Summary
   Return ONLY:
   - File path created
   - One-line summary of what needs to be verified
   ---

7. **After all subagents complete**, provide:
   - Summary table of all generated verification files
   - Total tasks processed
   - Any tasks that couldn't be processed (with reasons)

---

## Notes

- **NEVER run `gh pr view` or `gh issue view` on task links** - only on the main endgame issue
- **NEVER analyze or research tasks yourself** - immediately delegate to subagents
- Each subagent runs independently
- Subagents should be concise - create the file and return a brief summary
- If a task is unclear, the subagent should note this in the verification file
- Use slugified task titles for filenames (lowercase, hyphens, no special chars)

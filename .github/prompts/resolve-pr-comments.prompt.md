---
agent: 'agent'
description: 'Resolve PR review comments one by one with user confirmation at each step'
---

# PR Review Comment Resolution

Resolve PR review comments for PR #${input:pr_number:Enter PR number}

## Critical Rules

**SEQUENTIAL PROCESSING IS MANDATORY**: You MUST fully complete ALL steps for one comment before starting the next.

**CONFIRMATION GATES ARE BLOCKING**: You MUST wait for explicit user confirmation at each gate. Do NOT proceed without user approval.

**TODO TRACKING**: Use the todo list to track progress. Only ONE comment should be "in-progress" at a time.

## Workflow

### 1. Verify Branch
- Check current branch: `git branch --show-current`
- If not on the PR's source branch, switch to it: `gh pr checkout ${input:pr_number}`

### 2. Get All Review Comments and Save to File
Use pagination to retrieve all comments (gh CLI defaults to 30 results) and save to a file for reference:
```shell
mkdir -p .github/pullrequests/${input:pr_number}
gh api repos/microsoft/copilot-eclipse/pulls/${input:pr_number}/comments --paginate --jq '.[] | {id: .id, path: .path, line: .line, body: .body, diff_hunk: .diff_hunk}' > .github/pullrequests/${input:pr_number}/comments.json
```

### 3. For Each Comment, Extract:
- `id`: The comment ID (needed for resolving)
- `path`: The file path where the comment was made
- `line`: The line number in the file
- `body`: The actual review comment content
- `diff_hunk`: The code context around the comment

### 4. Create TODO List for All Comments
**IMMEDIATELY after fetching comments**, create a TODO list with one item per comment:
- Title format: "Comment #N: <brief description from body>"
- All items start as "not-started"
- This provides visibility to the user on overall progress

### 5. Address Comments ONE AT A TIME (Sequential Processing)

**⚠️ CRITICAL: Do NOT batch process. Do NOT skip ahead. Complete each comment FULLY before moving to the next.**

For each comment in order:
1. **Mark the TODO as "in-progress"** (only one at a time)
2. **Follow the COMPLETE workflow** in [resolve-single-comment.prompt.md](resolve-single-comment.prompt.md)
3. **Wait for ALL confirmation gates** in that workflow:
   - Confirmation to apply the proposed fix
   - Final confirmation after showing the diff
   - Confirmation that commit/push/reply succeeded
4. **Mark the TODO as "completed"** only after step 9 (Commit and Reply) is done
5. **Then and ONLY then**, move to the next comment

**DO NOT:**
- ❌ Start analyzing comment #2 while comment #1 is still in-progress
- ❌ Skip the "Get User Confirmation" step (Step 4 in single-comment workflow)
- ❌ Skip the "Get Final Confirmation" step (Step 8 in single-comment workflow)
- ❌ Mark a comment as complete before committing and replying
- ❌ Process multiple comments in parallel

### 6. After Fixing All Comments
Run full verification:
```shell
.\mvnw clean verify
```

## Example TODO List Management

After fetching 3 comments, create:
```
TODO 1: "Comment #1: Use StringUtils.isNotBlank()" - not-started
TODO 2: "Comment #2: Add null check for parameter" - not-started  
TODO 3: "Comment #3: Extract method for reuse" - not-started
```

When starting comment #1:
```
TODO 1: "Comment #1: Use StringUtils.isNotBlank()" - in-progress ← ONLY this one
TODO 2: "Comment #2: Add null check for parameter" - not-started
TODO 3: "Comment #3: Extract method for reuse" - not-started
```

Only after commit/push/reply for comment #1:
```
TODO 1: "Comment #1: Use StringUtils.isNotBlank()" - completed ✓
TODO 2: "Comment #2: Add null check for parameter" - in-progress ← Now start this
TODO 3: "Comment #3: Extract method for reuse" - not-started
```

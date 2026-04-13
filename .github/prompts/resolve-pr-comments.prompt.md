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
gh api repos/microsoft/copilot-for-eclipse/pulls/${input:pr_number}/comments --paginate --jq '.[] | {id: .id, path: .path, line: .line, body: .body, in_reply_to_id: .in_reply_to_id, user: .user.login}' > .github/pullrequests/${input:pr_number}/comments.json
```

**Note**: The `line` field from the API points directly to the line in the current file where the comment was made. No diff parsing is needed - just read the file at that line number to see the code context.

### 3. Filter and Classify Comments
After fetching comments:
1. **Identify resolved comments** - If a comment has replies (other comments with `in_reply_to_id` matching its `id`) from the PR author starting with "Done." or similar, it's already resolved
2. **Skip reply comments** - Comments with `in_reply_to_id` set are replies, not original review comments
3. **Focus on actionable review comments** - Only create TODOs for original comments (no `in_reply_to_id`) that haven't been resolved

### 4. For Each Actionable Comment, Extract:
- `id`: The comment ID (needed for replying)
- `path`: The file path where the comment was made
- `line`: The line number in the file (use this directly to read code context)
- `body`: The actual review comment content
- `in_reply_to_id`: If set, this is a reply to another comment (skip these)
- `user`: The username who made the comment

### 5. Create TODO List for Actionable Comments
**IMMEDIATELY after fetching and filtering comments**, create a TODO list with one item per actionable comment:
- Title format: "Comment #{id}: <brief description from body>"
- All items start as "not-started"
- This provides visibility to the user on overall progress
- Exclude comments that are developer responses or already resolved

### 6. Address Comments ONE AT A TIME (Sequential Processing)

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

### 7. After Fixing All Comments
Run full verification:
```shell
.\mvnw clean verify
```

## Example TODO List Management

After fetching and filtering 3 actionable comments (e.g., skipping 2 "Done" responses), create:ionable comments (e.g., skipping 2 "Done" responses), create:
```
TODO 1: "Comment #2580177053: Use StringUtils.isNotBlank()" - not-started
TODO 2: "Comment #2580180721: Add null check for parameter" - not-started  
TODO 3: "Comment #2580191136: Extract method for reuse" - not-started
```

When starting comment #1:
```
TODO 1: "Comment #2580177053: Use StringUtils.isNotBlank()" - in-progress ← ONLY this one
TODO 2: "Comment #2580180721: Add null check for parameter" - not-started
TODO 3: "Comment #2580191136: Extract method for reuse" - not-started
```

Only after commit/push/reply for comment #1:
```
TODO 1: "Comment #2580177053: Use StringUtils.isNotBlank()" - completed ✓
TODO 2: "Comment #2580180721: Add null check for parameter" - in-progress ← Now start this
TODO 3: "Comment #2580191136: Extract method for reuse" - not-started
```

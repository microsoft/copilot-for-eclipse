---
agent: 'agent'
description: 'Resolve a single PR review comment with confirmation gates'
---

# Single PR Comment Resolution

## When to Apply
Apply this instruction when resolving an individual PR review comment. This is typically invoked as part of the broader PR comment resolution workflow.

## Critical Rules

**⚠️ THIS IS A BLOCKING WORKFLOW**: Each step must complete before proceeding to the next.

**CONFIRMATION STEPS ARE MANDATORY**: Steps 4 and 8 require explicit user approval. You MUST stop and wait for user input.

**DO NOT PROCEED WITHOUT CONFIRMATION**: If the user has not responded, DO NOT continue to the next step.

## Input
- `pr_number`: The PR number
- `comment_id`: The comment ID from the comments.json file

Read the comment details from `.github/pullrequests/{pr_number}/comments.json` using the `comment_id`:
- `id`: The comment ID
- `path`: The file path where the comment was made
- `line`: The line number in the file
- `body`: The review comment content
- `diff_hunk`: The code context around the comment

## Resolution Steps

### 1. Understand the Context
1. Read the file at the specified line and surrounding context (±10 lines)
2. Review the `diff_hunk` to understand what changed
3. Parse the comment `body` to understand what the reviewer is requesting

### 2. Classify the Comment Type
Determine the type of feedback:
- **Code style**: Formatting, naming conventions, import organization
- **Bug fix**: Logic error, null check, edge case handling
- **Refactoring**: Extract method, simplify logic, reduce duplication
- **Documentation**: Add/update Javadoc, comments, or explanations
- **Best practice**: Thread safety, resource management, error handling
- **Question/Clarification**: Reviewer needs explanation (may not require code change)

### 3. Propose the Fix
1. Identify the specific change needed
2. **Present the proposed change to the user** with:
   - What will be changed
   - Why this addresses the reviewer's feedback
   - Any related changes needed (e.g., updating callers)

### 4. Get User Confirmation ⛔ BLOCKING STEP

**⛔ STOP HERE AND WAIT FOR USER RESPONSE**

You MUST NOT proceed to step 5 until the user explicitly approves.

Present options to the user and WAIT:
- ✅ Apply the proposed fix
- 🔄 Modify the approach (discuss alternatives)
- ⏭️ Skip this comment (defer to later)
- ❓ Ask the reviewer for clarification

**After presenting options, END YOUR RESPONSE and wait for user input.**

### 5. Apply the Change
**Only proceed here after user confirms in step 4.**

Once confirmed:
1. Make the code change using appropriate edit tools
2. If the change affects method signatures or behavior:
   - Use `list_code_usages` to find all callers
   - Update callers to handle the new behavior
3. Format the code according to project style (2-space indent, 120 char lines)

### 6. Validate the Change
1. **Check against Best Practices**:
   - Follow Eclipse conventions (use Eclipse-native APIs)
   - Thread safety (no I/O on UI thread)
   - Resource management (dispose SWT resources)
   - Error handling (use Eclipse status objects)

2. **Check against Code Style**:
   - Google Java Style (enforced by Checkstyle)
   - Use `StringUtils.isNotBlank()` for string checks
   - Use simple class names with imports (avoid FQN)
   - Use `@Override` annotation

3. **Run verification** (if significant change):
   ```shell
   .\mvnw checkstyle:check -pl <module>
   ```

### 7. Document Learning (Optional)
If the comment reveals a pattern that should be remembered:
- Suggest adding it to `.github/copilot-instructions.md`
- Example patterns worth documenting:
  - Common API usage mistakes
  - Project-specific conventions
  - Recurring review feedback themes

### 8. Get Final Confirmation ⛔ BLOCKING STEP

**⛔ STOP HERE AND WAIT FOR USER RESPONSE**

You MUST NOT proceed to step 9 until the user explicitly accepts the changes.

Present the changes to the user for final approval:
- Show the diff of all changes made
- Ask: "Do you accept these changes? (yes/no)"

**After presenting the diff and question, END YOUR RESPONSE and wait for user input.**

If **not accepted**, iterate and improve the changes based on user feedback, then return to this step.

### 9. Commit and Reply
**Only proceed here after user accepts in step 8.**

Once the user accepts the changes:
1. Commit the changes:
   ```shell
   git add -A
   git commit -m "Address PR comment #{comment_id}: <brief description>"
   ```
2. Push the changes:
   ```shell
   git push
   ```
3. Reply to the PR comment:
   ```shell
   gh api -X POST /repos/microsoft/copilot-eclipse/pulls/{pr_number}/comments/{comment_id}/replies -f body="Done. <description of what was changed and why>"
   ```

**Only after step 9 completes successfully should this comment be marked as "completed" in the TODO list.**

## Example Resolution

### Input Comment
```json
{
  "path": "com.microsoft.copilot.eclipse.ui/src/.../MyClass.java",
  "line": 42,
  "body": "Use StringUtils.isNotBlank() instead of manual null/empty check",
  "diff_hunk": "@@ -40,3 +40,5 @@\n+    if (value != null && !value.isEmpty()) {"
}
```

### Resolution Flow (with blocking points)
1. **Understand**: Reviewer wants to use utility method instead of manual check
2. **Classify**: Code style / Best practice
3. **Propose**: Replace `value != null && !value.isEmpty()` with `StringUtils.isNotBlank(value)`
4. **⛔ WAIT**: Present options, STOP, wait for user to respond
   - User says: "✅ Apply the fix"
5. **Apply**: Edit the file (only after user approval)
6. **Validate**: Run checkstyle on the module
7. **Document**: Already in copilot-instructions.md ✓
8. **⛔ WAIT**: Show diff, STOP, wait for user to accept
   - User says: "yes"
9. **Commit and Reply**: Commit changes, push, reply with commit ID
10. **Mark TODO as completed** (only now!)

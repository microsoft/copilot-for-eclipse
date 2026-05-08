# Support Delegating File and Text Search to IDE

## Overview
Verify that Copilot Agent mode can satisfy language-server `workspace/findFiles` and `workspace/findTextInFiles`
requests by delegating file-name and text-content search to Eclipse. This matters for ABAP support because ADT resources
may use Eclipse-managed or virtual URIs that the Copilot language server cannot search directly from the local file system.

Entry points:
- Window -> Show View -> Other... -> Copilot -> Copilot Chat -> Agent mode

Not exercised:
- Manual unit-level invocation of `workspace/findFiles` or `workspace/findTextInFiles`; this plan verifies the
  user-visible Agent flow.
- Generic local-file search outside ABAP ADT resources.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and activated.
- The user is signed in to GitHub Copilot and Agent mode is available in the Copilot Chat view.
- ABAP Development Tools is installed and connected to an ABAP system.
- The workspace contains an imported ABAP project with a known package that has at least one ABAP development object
  visible in Project Explorer.
- A test ABAP development object is available that can be opened in ADT and contains, or can safely be updated to
  contain, a unique search token such as `ZCOPILOT_SEARCH_ABAP_144` in a comment or string literal.

---

## 1. ABAP ADT file and text search delegation

### TC-001: Agent finds a known token inside ABAP source through Eclipse text search

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- The Eclipse workbench is open with the ABAP project loaded and connected.
- A test ABAP development object in the target package contains the unique token `ZCOPILOT_SEARCH_ABAP_144` in a comment
  or string literal.
- The object containing the token is visible in Project Explorer and can be opened from ADT.
- Copilot Chat is open in Agent mode.

#### Steps
1. Open the ABAP development object that contains `ZCOPILOT_SEARCH_ABAP_144` and verify the token is present in the
   editor.
2. Open **Copilot Chat** from `Window -> Show View -> Other... -> Copilot -> Copilot Chat` if it is not already open.
3. Switch the chat mode selector to **Agent**.
4. Send a prompt that asks Agent mode to search the ABAP project or package for the token, for example: `Search ABAP
   project <project name> for the text ZCOPILOT_SEARCH_ABAP_144. Return the matching object path
   and line text.`
5. If Copilot asks for tool confirmation, approve the requested workspace text-search operation.
6. Wait for the Copilot response to complete.
7. Compare the returned object and line text with the ABAP editor content.

#### Expected Result
- Copilot completes the request without reporting that the ABAP source, ADT resource, `semanticfs` URI, or virtual file
  cannot be searched or read.
- The response includes the ABAP development object that contains `ZCOPILOT_SEARCH_ABAP_144`.
- The response includes the matching line text, or enough surrounding context to prove that the token was found inside
  the ABAP source object.
- The Eclipse error log has no `workspace/findTextInFiles`, `Invalid regex`, `Invalid glob`, or `Failed to search text`
  error for the selected ABAP resource.

#### Key Screenshots
- [ ] **ABAP token in editor** -- The ABAP development object open in ADT with `ZCOPILOT_SEARCH_ABAP_144` visible.
- [ ] **Agent text-search prompt** -- Copilot Chat in Agent mode with the token-search prompt visible.
- [ ] **Completed text-search response** -- The completed response showing the matching ABAP object and line text.

#### Notes on failure modes
- Copilot returns file-name matches but cannot find the token -- `workspace/findFiles` may work while
  `workspace/findTextInFiles` is not reading ABAP resources through Eclipse EFS.
- The match is missing when the token differs only by case -- text search should be case-insensitive, so check the
  regex/text-search handling path.
- The chat turn hangs after approval -- the text search may be blocked while reading an ADT-backed resource.

### TC-002: Agent handles no-match ABAP searches without surfacing errors

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions
- The Eclipse workbench is open with the ABAP project loaded and connected.
- Copilot Chat is open in Agent mode.
- A deliberately nonexistent ABAP object name or token is chosen, for example `ZCOPILOT_SEARCH_ABAP_144_DOES_NOT_EXIST`.

#### Steps
1. Ask Agent mode to search the target ABAP package for files or development objects matching the nonexistent name.
2. If Copilot asks for tool confirmation, approve the requested workspace search operation.
3. Wait for the response to complete.
4. Ask Agent mode to search the same ABAP package for the nonexistent text token.
5. If Copilot asks for tool confirmation, approve the requested workspace text-search operation.
6. Wait for the response to complete.
7. Open the Eclipse error log and check for errors emitted during both searches.

#### Expected Result
- Copilot completes both turns and explains that no matching ABAP files, objects, or text results were found.
- No error dialog is shown to the user.
- The Eclipse error log has no uncaught exception or stack trace from `workspace/findFiles`, `workspace/findTextInFiles`,
  glob matching, regex matching, or URI parsing.

#### Key Screenshots
- [ ] **No-match file-search response** -- Copilot Chat showing a completed response for the nonexistent ABAP object
  name.
- [ ] **No-match text-search response** -- Copilot Chat showing a completed response for the nonexistent ABAP token.

#### Notes on failure modes
- An invalid-pattern or URI error appears for an ordinary no-match search -- the delegated search should return an empty
  result rather than surfacing an exception.
- The response includes unrelated local files or source lines -- the search may not be scoped to the selected ABAP
  project or package.
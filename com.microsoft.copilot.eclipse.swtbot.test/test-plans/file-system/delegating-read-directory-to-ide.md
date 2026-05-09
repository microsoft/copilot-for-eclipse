# Support Delegating Read Directory to IDE

## Overview
Verify that Copilot Agent mode can satisfy language-server `workspace/readDirectory` requests by delegating
directory listing to Eclipse. This matters for ABAP support because ADT resources may use Eclipse-managed or virtual
URIs that the Copilot language server cannot list directly from the local file system.

Entry points:
- Window -> Show View -> Other... -> Copilot -> Copilot Chat -> Agent mode

Not exercised:
- General file and text search delegation, which is covered by the separate search delegation task.
- Manual unit-level invocation of `workspace/readDirectory`; this plan verifies the user-visible Agent flow.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and activated.
- The user is signed in to GitHub Copilot and Agent mode is available in the Copilot Chat view.
- ABAP Development Tools is installed and connected to an ABAP system.
- The workspace contains an imported ABAP project that has previously cached at least one ABAP development object
  locally (i.e., the object has been opened from Project Explorer at least once so the `.adt/...` workspace folder
  contains a corresponding `IFolder`/`IFile` for it).
- A folder under the project's locally cached ADT structure (e.g., `<project>/.adt/classlib/classes/<class_name>/`) is
  visible in Project Explorer with the **Show Hidden Resources** filter disabled, and has at least one child entry on
  disk.

---

## 1. ABAP ADT directory delegation

### TC-001: Agent lists a cached ADT directory through Eclipse

**Type:** `Happy Path`
**Priority:** `P0`

> Note: Virtual ABAP packages shown in Project Explorer are not Eclipse `IResource`s, and ADT only materializes
> development objects under `<project>/.adt/...` after they are opened. The delegated `workspace/readDirectory`
> implementation lists workspace resources, so this test targets a *cached* ADT folder where children are guaranteed to
> exist on disk.

#### Preconditions
- The Eclipse workbench is open with the ABAP project loaded and connected.
- A locally cached ADT folder (e.g., `<project>/.adt/classlib/classes/<class_name>/`) is visible in Project Explorer
  and contains at least one cached child entry (a sub-folder or a development object file such as `<name>.aclass`).
- Copilot Chat is open in a fresh or cleared conversation.

#### Steps
1. Open **Copilot Chat** from `Window -> Show View -> Other... -> Copilot -> Copilot Chat`.
2. Switch the chat mode selector to **Agent**.
3. In Project Explorer, expand the target ABAP project and create a folder under it.
4. Attach that folder to the chat context (drag-and-drop into the input area or use **Add Context...**) so Copilot
   sends its workspace URI to the language server.
5. Send a prompt that asks Agent mode to list its immediate children, for example:
   `List the immediate children of the attached folder. Do not search recursively.`
6. If Copilot asks for tool confirmation, approve the requested workspace read operation.
7. Wait for the Copilot response to complete.
8. Compare the listed entries in the response with the immediate children visible under the same folder in Project
   Explorer.

#### Expected Result
- Copilot completes the request without reporting that the folder URI, ADT resource, `platform:/resource` URI, or
  `semanticfs:` URI cannot be read.
- The response includes the immediate child entries that are visible on disk for the attached cached ADT folder.
- The response does not include recursive grandchildren when the prompt requested immediate children only.
- The Eclipse error log has no `workspace/readDirectory`, `Invalid container URI`, or `Failed to read directory` error
  for the selected ABAP resource.

#### Key Screenshots
- [ ] **Cached ADT folder in Project Explorer** -- The selected folder expanded to show the child entries used as the
  expected result.
- [ ] **Agent prompt** -- Copilot Chat in Agent mode with the folder attached and the listing prompt visible.
- [ ] **Completed Agent response** -- The completed response showing the matching child entries.

#### Notes on failure modes
- Copilot says the directory cannot be read or is unsupported -- Eclipse may not be resolving the ADT,
  `platform:/resource`, or `semanticfs:` URI through the delegated `workspace/readDirectory` path.
- The response is empty while Project Explorer shows children -- the IDE may have returned no accessible `IContainer`
  for the resource; verify the URI scheme is among the supported schemes (`PlatformUtils.getSupportedUriSchemes()`),
  the ABAP project is connected, and the target folder is genuinely cached on disk (a virtual ABAP *package* will
  legitimately return empty because it is not an `IResource`).
- The response includes unrelated workspace files -- the language server may have fallen back to local filesystem
  listing instead of the Eclipse workspace resource.

### TC-002: Agent handles an empty or inaccessible ABAP package without crashing

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions
- The Eclipse workbench is open with the ABAP project loaded.
- An empty ABAP package, a package with no visible child resources, or a package that can be temporarily
  disconnected/unloaded is available.
- Copilot Chat is open in Agent mode.

#### Steps
1. In Project Explorer, locate the empty, unloaded, or inaccessible ABAP package and note its displayed name.
2. Ask Agent mode to list the immediate children of that ABAP package.
3. If Copilot asks for tool confirmation, approve the requested workspace read operation.
4. Wait for the response to complete.
5. Open the Eclipse error log and check for errors emitted during the request.

#### Expected Result
- Copilot completes the turn and explains that no child entries are available, or that the resource could not be accessed, without hanging or crashing the chat view.
- No error dialog is shown to the user.
- The Eclipse error log has no uncaught exception or stack trace from `workspace/readDirectory` or directory URI parsing.

#### Key Screenshots
- [ ] **Empty or inaccessible ABAP package** -- Project Explorer showing the package state before the prompt.
- [ ] **Graceful Agent response** -- Copilot Chat showing a completed response without a crash or endless spinner.

#### Notes on failure modes
- The chat turn remains in progress indefinitely -- the `workspace/readDirectory` request may not be completing for unresolved ADT resources.
- An error dialog or stack trace appears -- URI handling should return an empty directory result for unresolved resources instead of surfacing an exception.

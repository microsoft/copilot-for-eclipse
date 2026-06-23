# Run In Terminal Tool

## Overview
Verify that Agent mode invokes `run_in_terminal` with commands that preserve multi-line shell syntax and execute
build commands from the correct imported project root. These cases guard against command flattening and wrong
working-directory selection in terminal tool calls.

---

## Test Cases

### TC-001: Multi-line PowerShell command executes correctly

**Type:** `Regression`
**Priority:** `P0`

#### Preconditions
- Eclipse IDE is open with the GitHub Copilot for Eclipse plugin installed and activated.
- The user is signed in to GitHub Copilot.
- Copilot Chat is open in Agent mode.
- The terminal shell used by `run_in_terminal` is PowerShell.
- Terminal tool confirmation is enabled, or the tester can inspect the generated command before allowing execution.

#### Steps
1. Start a new Copilot Chat conversation in Agent mode.
2. Send a prompt that asks the agent to run this exact multi-line PowerShell command in the terminal and report the
   output:
   ```powershell
   1..3 | ForEach-Object {
     Write-Output "item=$_"
   }
   ```
3. When the `run_in_terminal` confirmation appears, verify that the displayed command keeps the pipeline and block
   body as a multi-line command, including the opening `{`, the `Write-Output` line, and the closing `}`.
4. Allow the command to run.
5. Wait for the terminal tool call to complete and inspect the terminal output returned to the agent.

#### Expected Result
- The command executes successfully without PowerShell parser errors caused by dropped newlines, missing braces, or
  truncated block content.
- The output contains exactly these item lines, in order:
  ```text
  item=1
  item=2
  item=3
  ```
- The agent response reports the same three output lines and does not claim the command failed.

#### 📸 Key Screenshots
- [ ] **Multi-line command confirmation** — `run_in_terminal` confirmation showing the full multi-line PowerShell
  command.
- [ ] **Multi-line command output** — Terminal or agent output showing `item=1`, `item=2`, and `item=3`.

---

### TC-002: Maven verify command runs from imported project root

**Type:** `Regression`
**Priority:** `P0`

#### Preconditions
- Eclipse IDE is open with the GitHub Copilot for Eclipse plugin installed and activated.
- The user is signed in to GitHub Copilot.
- Copilot Chat is open in Agent mode.
- A Maven project is imported into the Eclipse workspace and visible in Project Explorer.
- The imported project root contains `pom.xml` and either a Maven wrapper (`mvnw` / `mvnw.cmd`) or uses a system
  `mvn` installation.
- Terminal tool confirmation is enabled, or the tester can inspect the generated command before allowing execution.

#### Steps
1. Import a Maven project into the Eclipse workspace if one is not already present.
2. Start a new Copilot Chat conversation in Agent mode.
3. Ask the agent to run Maven `verify` for the imported project.
4. When the `run_in_terminal` confirmation appears, inspect the generated command before allowing it.
5. Allow the command to run after confirming the tool will execute from the imported Maven project root. Valid evidence
   includes either an explicit `cd "<imported-maven-project-root>"` in the command or a terminal prompt/working directory
   that already points at the imported project root.
6. Wait for the terminal tool call to complete or reach the normal build result for that project.

#### Expected Result
- The terminal executes Maven `verify` from the imported Maven project root path.
- The execution directory is the project directory that contains the imported project's `pom.xml`, not the Eclipse
  workspace root, a parent folder, or an unrelated project.
- The tool satisfies this either by starting the terminal with the imported project root as its working directory or by
  using an equivalent explicit directory change before running `verify`, such as:
  ```powershell
  cd "<imported-maven-project-root>"
  .\mvnw.cmd verify
  ```
  or:
  ```powershell
  cd "<imported-maven-project-root>"
  mvn verify
  ```
- The terminal starts Maven from that project root, so Maven resolves the expected `pom.xml`.

#### 📸 Key Screenshots
- [ ] **Imported Maven project** — Project Explorer showing the selected/imported Maven project root with `pom.xml`.
- [ ] **Verify execution location** — `run_in_terminal` confirmation or terminal prompt showing the command will execute
  from the imported Maven project root.
- [ ] **Verify command output** — Terminal output showing Maven started from the selected project root.

---

## Screenshots Checklist
> Consolidated list of all key screenshot moments.

- [ ] `TC-001` Multi-line command confirmation
- [ ] `TC-001` Multi-line command output
- [ ] `TC-002` Imported Maven project
- [ ] `TC-002` Verify execution location
- [ ] `TC-002` Verify command output

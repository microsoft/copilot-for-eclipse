# GitHub Copilot for Eclipse - Copilot Instructions

## Project Overview

This is the **GitHub Copilot for Eclipse** plugin - an Eclipse IDE extension that brings AI-powered code completions, chat assistance, and intelligent coding features to Eclipse users.

### Key Features
- **Code Completions**: Real-time AI-powered code suggestions using ghost text and inline completions
- **Chat Interface**: Conversational AI assistance with agent mode, custom chat modes, and context-aware responses
- **Agent Tools**: Multi-step autonomous coding with tools for file operations, error analysis, and terminal execution
- **Language Server Integration**: LSP4E-based connection to GitHub Copilot language server
- **Authentication**: GitHub OAuth integration for Copilot subscription management
- **Multi-Platform Support**: Windows, macOS (x64/aarch64), and Linux (x64/aarch64)

### Tech Stack
- **Java**: Primary language (Java 17+, with Eclipse 4.31+ requiring Java 21+)
- **Eclipse RCP/OSGi**: Plugin architecture using Eclipse platform APIs
- **Maven/Tycho**: Build system for Eclipse plugin development
- **LSP4E**: Language Server Protocol integration for Eclipse
- **Node.js/JavaScript**: Copilot language server (agent binaries bundled per platform)
- **JDT**: Java Development Tools integration for Java-specific features
- **SWT**: Standard Widget Toolkit for UI components

## Validating Changes

You MUST verify compilation and code quality before declaring work complete!

1. **ALWAYS** run Maven build to check for compilation errors:
   ```shell
   .\mvnw clean verify
   ```

2. **CHECK** Checkstyle compliance (Google Java Style with custom rules):
   ```shell
   .\mvnw checkstyle:check
   ```

3. **RUN** tests before submitting changes:
   ```shell
   .\mvnw test
   ```

4. **FIX** all compilation errors, Checkstyle violations, and test failures before moving forward

### Maven Build Structure
- Root POM coordinates multi-module build with Tycho
- Each bundle/feature/test has its own POM inheriting from root
- Checkstyle validation runs during `verify` phase
- Target platform defined in `base.target`, `target-terminal.target`, `target-tm-terminal.target`

### Code Change Review Trigger

**When** code changes are complete (after creating, editing, or refactoring code files), automatically verify that the changes follow project best practices:

1. Review changes against the **Best Practices Summary** section above
2. Verify **Code Style Preferences** are followed (formatting, naming, imports)
3. **When modifying a function's behavior** (error handling, return type, async pattern) → check all callers using `list_code_usages` or `grep_search` to ensure they handle the new behavior correctly
4. Run build verification:
   - `.\mvnw checkstyle:check` to verify Checkstyle compliance
   - `.\mvnw clean verify` to ensure compilation succeeds
   - `.\mvnw test` to verify tests pass

**Action**: After completing code changes, review against this checklist and fix any violations before declaring work complete.

## Project Architecture

### Bundle Structure (OSGi Modules)

The project follows Eclipse plugin conventions with multiple OSGi bundles:

#### Core Bundles

**`com.microsoft.copilot.eclipse.core`** - Core functionality and language server integration
- **Package**: `com.microsoft.copilot.eclipse.core`
- **Purpose**: LSP client, authentication, chat/completion logic, persistence, utilities
- **Key Components**:
  - `lsp/` - Language server protocol client implementation
  - `chat/` - Chat modes (built-in, custom), conversation management
  - `completion/` - Code completion provider and job scheduling
  - `persistence/` - Conversation history storage with Gson serialization
  - `format/` - Language-specific formatting readers (Java, CDT)
  - `logger/` - Logging infrastructure with Eclipse console and telemetry handlers
  - `events/` - Event management for chat and auth status
  - `utils/` - Core utilities, file operations, resource management

**`com.microsoft.copilot.eclipse.ui`** - User interface and editor integration
- **Package**: `com.microsoft.copilot.eclipse.ui`
- **Purpose**: Chat view, completion UI, editor integration, agent tools
- **Key Components**:
  - `chat/` - Chat view widgets, message rendering, input handling
  - `completion/` - Ghost text rendering, code mining, completion managers
  - `chat/tools/` - Agent tools (file operations, debugging, terminal, errors)
  - `chat/services/` - Tool service manager, chat service coordination
  - `editors/` - Editor lifecycle management and integration
  - `handlers/` - Command handlers for Copilot actions
  - `preferences/` - Settings UI and preference pages
  - `quickstart/` - Onboarding and feature introduction
  - `swt/` - Custom SWT widgets (markdown rendering, code blocks)

**`com.microsoft.copilot.eclipse.ui.jobs`** - Copilot Jobs view integration
- **Package**: `com.microsoft.copilot.eclipse.ui.jobs`
- **Purpose**: Copilot Jobs view integration

**`com.microsoft.copilot.eclipse.branding`** - Product branding and about dialog
- **Package**: N/A (resources only)
- **Purpose**: Icons, about.ini, about.properties

#### Terminal Integration Bundles

**`com.microsoft.copilot.eclipse.terminal.api`** - Terminal tool API definitions
- **Package**: `com.microsoft.copilot.eclipse.terminal.api`
- **Purpose**: Interface definitions for terminal execution tools

**`com.microsoft.copilot.eclipse.ui.terminal`** - Terminal integration (modern)
- **Package**: `com.microsoft.copilot.eclipse.ui.terminal`
- **Purpose**: Implementation for modern Eclipse Terminal (Eclipse 4.37+)
- **Service**: `IRunInTerminalTool` implementation
- **Dependencies**: `org.eclipse.terminal.*` packages

**`com.microsoft.copilot.eclipse.ui.terminal.tm`** - TM Terminal integration (legacy)
- **Package**: `com.microsoft.copilot.eclipse.ui.terminal.tm`
- **Purpose**: Implementation for TM Terminal (Eclipse 4.36 and lower)
- **Service**: `IRunInTerminalTool` implementation
- **Dependencies**: `org.eclipse.tm.terminal.*` packages

#### Platform-Specific Agent Bundles

These bundles contain the Node.js-based Copilot language server agent for each platform:

- **`com.microsoft.copilot.eclipse.core.agent.win32`** - Windows x64
- **`com.microsoft.copilot.eclipse.core.agent.linux.x64`** - Linux x64
- **`com.microsoft.copilot.eclipse.core.agent.linux.aarch64`** - Linux ARM64
- **`com.microsoft.copilot.eclipse.core.agent.macosx.x64`** - macOS Intel
- **`com.microsoft.copilot.eclipse.core.agent.macosx.aarch64`** - macOS Apple Silicon

Each contains `copilot-agent/` directory with Node.js binary and agent code.

#### Packaging Bundles

**`com.microsoft.copilot.eclipse.feature`** - Eclipse feature definition
- Aggregates all required bundles into installable feature
- Defines feature.xml with dependencies and description

**`com.microsoft.copilot.eclipse.repository`** - P2 update site
- Creates installable P2 repository (update site)
- Defines category.xml for feature categorization

#### Test Bundles

**`com.microsoft.copilot.eclipse.core.test`** - Core bundle tests
- JUnit tests for core functionality
- Fragment of `com.microsoft.copilot.eclipse.core`

**`com.microsoft.copilot.eclipse.ui.test`** - UI bundle tests
- JUnit tests for UI components
- Fragment of `com.microsoft.copilot.eclipse.ui`
- Note: SWTBot integration is planned for future enhancement

### Key Architecture Patterns

#### OSGi Service Pattern
- Services registered via OSGi Declarative Services (DS) or programmatically
- Service tracking using `ServiceTracker` or DS references
- Loose coupling between bundles via service interfaces
- Example: Terminal tools use `TerminalServiceManager` with listener pattern

#### Extension Point Pattern
- Eclipse extension points for extensibility (e.g., terminal implementations)
- LSP4E extension point for language server registration
- Custom extension points could be added for tool extensibility

#### Language Server Protocol (LSP)
- LSP4E provides base LSP client infrastructure
- `CopilotLanguageClient` extends `LanguageClientImpl` for custom capabilities
- `CopilotLanguageServer` interface defines server capabilities
- Custom LSP extensions for Copilot-specific features (agent tools, chat)

#### Agent Tool Architecture
1. **Tool Registration**: Tools extend `BaseTool` and register with `AgentToolService`
2. **Tool Schema**: Each tool defines `LanguageModelToolInformation` with JSON schema
3. **Tool Invocation**: Server sends `InvokeClientToolParams`, adapter executes and returns `LanguageModelToolResult`
4. **Confirmation Flow**: Tools can require user confirmation via `needConfirmation()`
5. **Async Execution**: Tools use `CompletableFuture` for async operations

#### Chat System Architecture
1. **Chat Modes**: Built-in and custom chat modes with system prompts
2. **Conversation Storage**: Persistent conversation history with turn-based structure
3. **Message Rendering**: Custom SWT widgets for markdown, code blocks, tool invocations
4. **Context Management**: File context, selection context, workspace context

## Code Style Preferences

**Google Java Style with Customizations** (enforced by Checkstyle)

### Formatting

**When** indenting code → use 2 spaces (NOT tabs), +4 spaces for continuation lines

**When** writing lines → keep under 120 characters maximum

**When** using braces → always include them for control structures (if, else, for, while, do); use K&R style (opening brace on same line)

```java
// ✓ Correct
public void example() {
  if (condition) {
    doSomething();
  } else {
    doSomethingElse();
  }
}

// ✗ Wrong - missing braces
if (condition)
  doSomething();
```

### Naming

**When** naming classes/interfaces → use `PascalCase` (e.g., `CopilotLanguageClient`, `BaseTool`)

**When** naming methods/variables → use `camelCase`, prefer descriptive names over abbreviations (e.g., `getToolInformation` not `getToolInfo`)

**When** naming constants → use `UPPER_SNAKE_CASE` (e.g., `MAX_RETRIES`, `DEFAULT_TIMEOUT`)

**When** naming packages → use lowercase, no underscores (e.g., `com.microsoft.copilot.eclipse.core.lsp`)

### Annotations & Documentation

**When** overriding methods → always use `@Override` annotation

**When** deprecating → use `@Deprecated` annotation with Javadoc `@deprecated` tag explaining the alternative

**When** writing public APIs → add Javadoc with `@param`, `@return`, `@throws` as needed

**When** adding comments → only when the "why" isn't obvious from code; use `//` for implementation comments

```java
/**
 * Retrieves the tool information for language model integration.
 *
 * @return the tool information describing this tool's capabilities
 */
@Override
public LanguageModelToolInformation getToolInformation() {
  // Implementation
}
```

### Imports

**When** adding imports → group by: static imports → java/javax → third-party → Eclipse → project (blank line between groups)

**When** importing → use explicit imports, never wildcards (except in tests)

**When** reviewing code → remove unused imports

### File Organization

**When** organizing a class file → follow this order:
1. License header (if required)
2. Package statement
3. Import statements (organized as above)
4. Class Javadoc
5. Class declaration
6. Static fields → Instance fields → Constructors → Methods (public → protected → private) → Inner classes

### Eclipse Plugin Best Practices

#### Dependencies

**When** adding bundle dependencies → minimize them; only add what's necessary

**When** declaring dependencies → use `Require-Bundle` for essential dependencies, `Import-Package` for optional or version-flexible ones

**When** designing bundles → avoid circular dependencies between bundles

#### Threading

**When** updating SWT widgets → always run on UI thread using `Display.asyncExec()` or `Display.syncExec()`

**When** performing I/O or long operations → use Eclipse `Job` API or `CompletableFuture.runAsync()`, never block UI thread

**When** chaining async operations → use `.thenCompose()`, `.thenAccept()`, `.exceptionally()`; avoid `.join()`

```java
// ✓ Correct - UI update from background thread
CompletableFuture.supplyAsync(() -> {
  // Background work
  return result;
}).thenAccept(result -> {
  Display.getDefault().asyncExec(() -> {
    // Update UI on UI thread
    label.setText(result);
  });
});
```

#### Resource Management

**When** creating SWT resources (fonts, images) → dispose them when done; Colors do not need disposal in modern SWT

**When** using shared resources → do not dispose resources from other bundles

**When** child has a parent → resources with a defined parent (e.g., Label with parent Composite) do not need explicit disposal

**When** using streams/files → use try-with-resources for Eclipse resources (IFile, IDocument)

#### Error Handling

**When** reporting errors → use Eclipse `IStatus` and `Status` objects

**When** logging errors → use `CopilotCore.getPlugin().logError(message, exception)`

**When** showing errors to users → use `ErrorDialog` or `MessageDialog`

**When** catching exceptions → never swallow silently; always log or handle appropriately

### Testing Standards

**When** writing new tests → use JUnit 5 (Jupiter)

**When** naming test classes → use `<ClassName>Test` or `<ClassName>Tests`

**When** naming test methods → use descriptive names: `testMethodName_scenario_expectedOutcome`

**When** testing UI → use SWTBot; run in dedicated test fragments; clean up resources after tests

**When** testing async operations → use appropriate timeouts

**When** integration testing → test bundle activation, service registration, LSP communication with mock server

## Development Workflow

### GitHub Interaction

**Always use GitHub CLI (`gh`) for interacting with the repository at https://github.com/microsoft/copilot-for-eclipse**

**When** viewing PRs → use `gh pr view <number>`, `gh pr list`, `gh pr status`

**When** creating PRs → use `gh pr create`

**When** reviewing PRs → use `gh pr review <number>`, `gh pr checks <number>`

**When** viewing issues → use `gh issue view <number>`, `gh issue list`

**When** fetching PR diffs → use `gh pr diff <number>`

**When** checking out PRs → use `gh pr checkout <number>`

**When** commenting → use `gh pr comment <number>`, `gh issue comment <number>`

### Setup and Build

1. **Prerequisites**:
   - Java 21 or later
   - Maven 3.8+ (or use provided Maven wrapper)
   - Eclipse IDE 2024-03 or later (for development)
   - GitHub CLI (`gh`) for repository interactions

2. **Build Project**:
   ```shell
   .\mvnw clean package
   ```

3. **Run Checkstyle**:
   ```shell
   .\mvnw checkstyle:check
   ```

4. **Run Tests**:
   ```shell
   .\mvnw test
   ```

5. **Build Update Site**:
   ```shell
   .\mvnw clean verify
   # Output in com.microsoft.copilot.eclipse.repository/target/repository/
   ```

### Testing in Eclipse

1. **Launch as Eclipse Application**:
   - Use launch configurations in `launch/` directory
   - Right-click project → Run As → Eclipse Application

2. **Debug Plugin**:
   - Set breakpoints in code
   - Launch in Debug mode
   - New Eclipse instance opens with plugin loaded

### Key Entry Points for Edits

**When** extending LSP client → modify `core/lsp/CopilotLanguageClient.java`

**When** changing LSP connection → update `core/lsp/LsStreamConnectionProvider.java`

**When** adding LSP protocol types → add to `core/lsp/protocol/`

**When** modifying chat UI → edit widgets in `ui/chat/`

**When** changing chat modes → update `core/chat/` for mode logic

**When** editing message rendering → modify `ui/swt/` for markdown/code blocks

**When** changing conversation storage → edit `core/persistence/`

**When** modifying completion logic → update `core/completion/CompletionProvider.java`

**When** changing ghost text → modify `ui/completion/` for visual presentation

**When** adding/modifying agent tools → edit `ui/chat/tools/`

**When** registering tools → update `ui/chat/services/AgentToolService.java`

**When** changing auth flow → update `core/AuthStatusManager.java`

**When** modifying terminal integration → edit `terminal.api/` or specific terminal bundles

**When** adding preferences → update `ui/preferences/`

**When** changing bundle dependencies → edit `META-INF/MANIFEST.MF`

**When** adding extension points → update `plugin.xml` files

**When** modifying feature → edit `feature/feature.xml`

## Common Development Tasks

### Adding a New Agent Tool

1. Create tool class extending `BaseTool` in `com.microsoft.copilot.eclipse.ui/src/.../ui/chat/tools/`
2. Implement `getToolInformation()` defining JSON schema for tool parameters
3. Implement `invokeLanguageModelTool()` for tool execution logic
4. Override `needConfirmation()` to control confirmation behavior
5. Register tool in `AgentToolService.registerDefaultTools()`
6. Add necessary bundle dependencies to `META-INF/MANIFEST.MF`

### Adding a New Chat Mode

1. Create mode class extending `BaseChatMode` or `CustomChatMode` in core bundle
2. Define system prompt and capabilities in mode configuration
3. Register mode through custom mode service or built-in mode registry
4. Update UI to show mode selection if needed

### Adding a New UI Component

1. Create SWT widget extending `Composite` in `com.microsoft.copilot.eclipse.ui/src/.../ui/`
2. Implement constructor taking parent composite and style bits
3. Create layout and child widgets in constructor
4. Add event handlers for user interactions
5. Dispose resources in widget disposal listener
6. Use `SwtUtils` for common UI patterns

### Modifying LSP Protocol

1. Add protocol types to `com.microsoft.copilot.eclipse.core/src/.../core/lsp/protocol/`
2. Update `CopilotLanguageClient` or `CopilotLanguageServer` interfaces if needed
3. Implement request/notification handlers in `CopilotLanguageClient`
4. Send custom requests using LSP4E's request infrastructure

### Adding Tests

1. Create test class in appropriate test bundle fragment
2. Use JUnit 5 annotations: `@Test`, `@BeforeEach`, `@AfterEach`
3. For UI tests, extend SWTBot test base classes
4. Mock external dependencies (LSP server, file system, etc.)
5. Clean up resources in teardown methods

## Best Practices Summary

### Core Principles
1. **Follow Eclipse Conventions**: Use Eclipse-native APIs over LSP dependencies (`WorkspaceUtils.listWorkspaceFolders()` not `LSPEclipseUtils.getWorkspaceFolders()`)
2. **Thread Safety**: Never perform I/O on UI thread; use `CompletableFuture.runAsync()` or Eclipse `Job` API for blocking operations
3. **Resource Management**: Dispose all SWT resources, close streams, unregister listeners; close editors before deleting files
4. **Error Handling**: Use Eclipse status objects, log appropriately (INFO only for significant events, not routine operations)
5. **Code Style**: Follow Google Java Style enforced by Checkstyle; use simple class names with imports (avoid fully qualified names)

### API & Code Quality

**When** checking strings → use `StringUtils.isNotBlank()` instead of manual null/empty checks

**When** referring to workspace folders → use "Project" terminology in UI and code

**When** querying resources → use `IResource.DEPTH_ONE` instead of `DEPTH_INFINITE` when not recursing

**When** passing progress monitors → use `new NullProgressMonitor()` instead of `null`

**When** registering extensions → consolidate in `plugin.xml`; verify extension point IDs are correct

### Threading & Async Patterns

**When** chaining futures → avoid `.join()`; use `.thenCompose()`, `.thenAccept()`, `.exceptionally()`

**When** updating UI from async → wrap in `Display.asyncExec()`

**When** using shared collections → use `CopyOnWriteArrayList` instead of `ArrayList` for concurrent access

**When** using synchronous operations → document why they are needed

```java
// ✓ Correct async pattern
CompletableFuture.runAsync(() -> {
  file.delete(); // I/O in background
}).thenAccept(result -> {
  Display.getDefault().asyncExec(() -> {
    updateUI(); // UI update on UI thread
  });
});
```

### UI/UX Best Practices

**When** adding user-facing text → externalize in `Messages.properties` using Eclipse NLS pattern

**When** implementing context menus → prefer simple, predictable visibility logic

**When** opening preference dialogs → show only relevant pages

**When** modifying files externally → call `IResource.refreshLocal()` after changes

**When** refreshing layouts → use `requestLayout()` instead of `layout()`; avoid redundant refresh calls before disposal

### Code Organization

**When** duplicating logic → extract to centralized utility methods (e.g., `UiUtils.isAgentFile()`)

**When** declaring methods → keep `private` if only used internally; avoid unnecessary `public`

**When** refactoring → remove unreachable/dead code

**When** handling inputs → check existence before use; handle multi-project scenarios

### Documentation

**When** making non-obvious decisions → explain why blocking calls or fully qualified names are used

**When** creating complex structures → add JavaDoc for nested data structures

**When** breaking conventions → document the reason for the deviation

### Testing & Dependencies

**When** writing tests → use JUnit 5 for core logic; integration tests for Eclipse integration

**When** adding dependencies → only add necessary bundles; check `base.target` for Eclipse 2024-03 version constraints

**When** targeting Eclipse versions → support multiple versions when possible

**When** optimizing performance → use jobs for long operations, cache expensive computations, dispose resources promptly

## Continuous Improvement

**When** the user provides feedback, correction, or identifies a reusable pattern:
1. Apply the fix or improvement immediately
2. **Proactively suggest** adding the lesson to this instructions file if it's a pattern that should be remembered
3. Ask: "Would you like me to add this to the copilot instructions so it's remembered for future sessions?"

---

This plugin brings the power of GitHub Copilot to Eclipse users through a robust, well-architected extension that follows Eclipse best practices and integrates deeply with the Eclipse platform.

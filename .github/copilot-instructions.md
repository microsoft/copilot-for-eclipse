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

## Coding Standards

### Java Coding Style

**Google Java Style with Customizations** (enforced by Checkstyle):

#### Indentation and Formatting
- **Indentation**: 2 spaces (NOT tabs)
- **Line length**: 100 characters maximum
- **Block indentation**: +2 spaces
- **Continuation indentation**: +4 spaces for wrapped lines
- **Braces**: Required for all control structures (if, else, for, while, do)
- **Brace style**: K&R style (opening brace on same line)

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

#### Naming Conventions
- **Classes/Interfaces**: `PascalCase` (e.g., `CopilotLanguageClient`, `BaseTool`)
- **Methods/Variables**: `camelCase` (e.g., `getToolInformation`, `lsConnection`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_RETRIES`, `DEFAULT_TIMEOUT`)
- **Packages**: lowercase, no underscores (e.g., `com.microsoft.copilot.eclipse.core.lsp`)
- **Type parameters**: Single capital letter (e.g., `<T>`, `<E>`)

#### Annotations
- **Override**: Always use `@Override` for overridden methods
- **Deprecated**: Use `@Deprecated` with Javadoc `@deprecated` tag

```java
@Override
public String getName() {
  return name;
}
```

#### Javadoc
- **Required** for all public classes, interfaces, and methods
- **Format**: Standard Javadoc with `@param`, `@return`, `@throws` as needed
- Use `/**` for Javadoc, `//` for implementation comments

```java
/**
 * Retrieves the tool information for language model integration.
 *
 * @return the tool information describing this tool's capabilities
 */
public LanguageModelToolInformation getToolInformation() {
  // Implementation
}
```

#### Imports
- No wildcard imports (except in tests)
- Organize: Static imports → Java standard library → Third-party → Eclipse → Project
- Remove unused imports

#### File Organization
1. License header (if required)
2. Package statement
3. Import statements (organized)
4. Class Javadoc
5. Class declaration
6. Static fields
7. Instance fields
8. Constructors
9. Methods (public → protected → private)
10. Static methods
11. Inner classes/interfaces

### Eclipse Plugin Best Practices

#### Bundle Dependencies
- Minimize dependencies - only add what's necessary
- Use `Require-Bundle` for essential dependencies
- Use `Import-Package` for optional or version-flexible dependencies
- Avoid circular dependencies between bundles

#### OSGi Services
- Use Declarative Services (DS) when possible
- Register services in plugin activator if DS not suitable
- Clean up service registrations in `stop()` method
- Use `ServiceTracker` for dynamic service dependencies

#### Threading
- **UI Thread**: All SWT operations must run on UI thread
  - Use `Display.asyncExec()` or `Display.syncExec()` from background threads
  - Use `Display.getDefault()` to get display instance
- **Background Jobs**: Use Eclipse `Job` API for long-running operations
  - Set appropriate job priority and scheduling rules
  - Report progress with `IProgressMonitor`
- **CompletableFuture**: Use for async tool operations and LSP requests

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
- Dispose SWT resources that you create (fonts, images)
- Note: Colors do not need disposal in modern SWT versions
- Do not dispose shared resources (e.g., images from other bundles)
- Resources with a defined parent (e.g., Label with parent Composite) do not need explicit disposal
- Use try-with-resources for Eclipse resources (IFile, IDocument)
- Close streams and dispose listeners in finally blocks or with try-with-resources

#### Error Handling
- Use Eclipse `IStatus` and `Status` for error reporting
- Log errors using plugin logger: `CopilotCore.getPlugin().logError(message, exception)`
- Show user-facing errors with `ErrorDialog` or `MessageDialog`
- Never swallow exceptions silently

### Testing Standards

#### Unit Tests
- Use JUnit 5 (Jupiter) for new tests
- Test class naming: `<ClassName>Test` or `<ClassName>Tests`
- One test class per production class (generally)
- Use descriptive test method names: `testMethodName_scenario_expectedOutcome`

#### UI Tests
- Use SWTBot for UI testing
- Run tests in dedicated test fragments
- Clean up resources after tests
- Use appropriate timeouts for async operations

#### Integration Tests
- Test bundle activation and service registration
- Test LSP communication with mock server
- Test tool invocation end-to-end

## Key APIs and Integrations

### Eclipse Platform APIs
- **Resources**: `IWorkspace`, `IProject`, `IFile`, `IFolder`
- **Text Editing**: `IDocument`, `ITextViewer`, `ITextEditor`
- **UI**: `IViewPart`, `IEditorPart`, `IWorkbenchPage`, `IWorkbenchWindow`
- **Jobs**: `Job`, `IJobManager`, `IProgressMonitor`
- **Preferences**: `IPreferenceStore`, `IEclipsePreferences`

### Java Development Tools (JDT)
- **Debug**: `IJavaDebugTarget`, `IJavaStackFrame`, `IJavaVariable`
- **Core Model**: `IJavaProject`, `ICompilationUnit`, `IType`, `IMethod`
- **UI**: `JavaUI`, editor integration APIs

### Language Server Protocol (LSP4E)
- **Client**: `LanguageClientImpl` - base class for custom client
- **Server Connection**: `ProcessStreamConnectionProvider` - for process-based servers
- **Document Sync**: Automatic document synchronization
- **Custom Requests**: Use `LanguageServer.getTextDocumentService()` for custom LSP requests

### SWT (Standard Widget Toolkit)
- **Widgets**: `Composite`, `Label`, `Text`, `Button`, `Browser`
- **Layouts**: `GridLayout`, `RowLayout`, `FillLayout`
- **Events**: `SelectionListener`, `ModifyListener`, `PaintListener`
- **Resources**: `Color`, `Font`, `Image` - must be disposed!

### External Integrations
- **GitHub**: OAuth authentication, API access
- **Copilot Language Server**: Custom LSP extensions for agent tools and chat
- **Telemetry**: Usage analytics (if enabled)

## Development Workflow

### GitHub Interaction

**Always use GitHub CLI (`gh`) for interacting with the repository at https://github.com/microsoft/copilot-eclipse**

Use `gh` commands for:
- Viewing PRs: `gh pr view <number>`, `gh pr list`, `gh pr status`
- Creating PRs: `gh pr create`
- Reviewing PRs: `gh pr review <number>`, `gh pr checks <number>`
- Viewing issues: `gh issue view <number>`, `gh issue list`
- Fetching PR diffs: `gh pr diff <number>`
- Checking out PRs: `gh pr checkout <number>`
- Commenting: `gh pr comment <number>`, `gh issue comment <number>`

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

**Language Server Integration:**
- **LSP client extensions**: Modify `com.microsoft.copilot.eclipse.core/src/.../core/lsp/CopilotLanguageClient.java`
- **LSP connection management**: Update `com.microsoft.copilot.eclipse.core/src/.../core/lsp/LsStreamConnectionProvider.java`
- **Custom LSP protocol**: Add types to `com.microsoft.copilot.eclipse.core/src/.../core/lsp/protocol/`

**Chat Features:**
- **Chat UI**: Modify widgets in `com.microsoft.copilot.eclipse.ui/src/.../ui/chat/`
- **Chat modes**: Update `com.microsoft.copilot.eclipse.core/src/.../core/chat/` for chat mode logic
- **Message rendering**: Edit `com.microsoft.copilot.eclipse.ui/src/.../ui/swt/` for markdown/code blocks
- **Conversation storage**: Modify `com.microsoft.copilot.eclipse.core/src/.../core/persistence/`

**Code Completion:**
- **Completion logic**: Update `com.microsoft.copilot.eclipse.core/src/.../core/completion/CompletionProvider.java`
- **Ghost text rendering**: Modify `com.microsoft.copilot.eclipse.ui/src/.../ui/completion/` for visual presentation
- **Completion managers**: Edit completion managers for different Eclipse versions (legacy vs modern)
- **Next Edit Suggestions (NES)**: Modify `com.microsoft.copilot.eclipse.ui/src/.../ui/completion/` for NES-related features

**Agent Tools:**
- **Tool implementations**: Add/modify tools in `com.microsoft.copilot.eclipse.ui/src/.../ui/chat/tools/`
- **Tool registration**: Update `com.microsoft.copilot.eclipse.ui/src/.../ui/chat/services/AgentToolService.java`
- **Tool API**: Modify `com.microsoft.copilot.eclipse.ui/src/.../ui/chat/tools/BaseTool.java` for tool framework

**Authentication:**
- **Auth flow**: Update `com.microsoft.copilot.eclipse.core/src/.../core/AuthStatusManager.java`
- **Status management**: Modify `com.microsoft.copilot.eclipse.ui/src/.../ui/CopilotStatusManager.java`

**Terminal Integration:**
- **Terminal API**: Modify `com.microsoft.copilot.eclipse.terminal.api/`
- **Terminal implementations**: Update specific terminal bundles (`ui.terminal` or `ui.terminal.tm`)

**Preferences and Settings:**
- **Preference pages**: Update `com.microsoft.copilot.eclipse.ui/src/.../ui/preferences/`
- **Core preferences**: Modify `com.microsoft.copilot.eclipse.core/` preference constants

**Bundle Manifests and Configuration:**
- **Dependencies**: Edit `META-INF/MANIFEST.MF` in respective bundles
- **Extension points**: Update `plugin.xml` files
- **Feature definition**: Modify `com.microsoft.copilot.eclipse.feature/feature.xml`
- **Update site**: Edit `com.microsoft.copilot.eclipse.repository/category.xml`

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
- **Utility Methods**: Use `StringUtils.isNotBlank()` instead of manual null/empty checks
- **Eclipse Terminology**: Use "Project" not "Workspace folder" in UI and code
- **Resource Depth**: Use `IResource.DEPTH_ONE` instead of `DEPTH_INFINITE` when not recursing
- **Progress Monitors**: Use `new NullProgressMonitor()` instead of `null`
- **Extension Points**: Consolidate registrations in `plugin.xml`; verify extension point IDs are correct

### Threading & Async Patterns
- **No Blocking**: Avoid `.join()` on CompletableFutures; chain with `.thenCompose()`, `.thenAccept()`, `.exceptionally()`
- **UI Updates**: Wrap in `Display.asyncExec()` when updating from async callbacks
- **Thread-Safe Collections**: Use `CopyOnWriteArrayList` instead of `ArrayList` for concurrent access
- **Document Concurrency**: Explain why synchronous operations are needed

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
- **Externalize Strings**: All user-facing text in `Messages.properties` using Eclipse NLS pattern
- **Simplify Logic**: Don't over-complicate context menu visibility; prefer simple, predictable behavior
- **Limit Dialog Scope**: Show only relevant preference pages when opening programmatically
- **Refresh Workspace**: Call `IResource.refreshLocal()` after external file modifications
- **Optimize Refreshes**: Use `requestLayout()` instead of `layout()`; avoid redundant refresh calls before disposal

### Code Organization
- **Extract Utilities**: Create centralized methods to avoid duplication (e.g., `UiUtils.isAgentFile()`)
- **Method Visibility**: Keep methods `private` if only used internally; avoid unnecessary `public`
- **Remove Dead Code**: Delete unreachable code (e.g., after dialog close)
- **Validate Inputs**: Check existence before use; handle multi-project scenarios

### Documentation
- **Clarify Decisions**: Explain why blocking calls or fully qualified names are used
- **Document Structures**: Add JavaDoc for complex nested data structures
- **Explain Deviations**: When breaking rules, document the reason

### Testing & Dependencies
- **Unit Tests**: Write tests for core logic using JUnit 5; integration tests for Eclipse integration
- **Minimize Dependencies**: Only add necessary bundles; check `base.target` for Eclipse 2024-03 version constraints
- **Backward Compatibility**: Support multiple Eclipse versions when possible
- **Performance**: Use jobs for long operations, cache expensive computations, dispose resources promptly

---

This plugin brings the power of GitHub Copilot to Eclipse users through a robust, well-architected extension that follows Eclipse best practices and integrates deeply with the Eclipse platform. Understanding the OSGi bundle structure, Eclipse APIs, and LSP integration patterns is crucial for making effective contributions.

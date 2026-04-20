# Contributing to GitHub Copilot for Eclipse

Thank you for your interest in contributing to GitHub Copilot for Eclipse! This document provides guidelines and instructions for contributing to this project.

## Code of Conduct

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information, see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any questions or concerns.

## Contributor License Agreement (CLA)

Most contributions require you to agree to a Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us the rights to use your contribution. For details, visit [https://cla.opensource.microsoft.com](https://cla.opensource.microsoft.com).

When you submit a pull request, a CLA bot will automatically determine whether you need to provide a CLA and decorate the PR appropriately (e.g., status check, comment). Simply follow the instructions provided by the bot. You will only need to do this once across all repos using our CLA.

## Getting Started

### Prerequisites

- **Java 21** or later
- **Maven 3.8+** (or use the provided Maven wrapper `./mvnw`)
- **Eclipse IDE for Eclipse Committers 2024-03** or later (for development)

### Building the Project

Clone the repository and build with the Maven wrapper:

```shell
./mvnw clean package
```

### Running Tests

```shell
./mvnw test
```

### Building the Update Site

```shell
./mvnw clean verify
```

The installable P2 repository is generated in `com.microsoft.copilot.eclipse.repository/target/repository/`.

### Running in Eclipse

1. Import all modules into your Eclipse workspace.
2. Use the launch configurations in the `launch/` directory.

## How to Contribute

### Reporting Issues

- Search [existing issues](https://github.com/microsoft/copilot-for-eclipse/issues) before filing a new one to avoid duplicates.
- File bugs or feature requests as a new GitHub Issue.
- Include steps to reproduce, expected behavior, actual behavior, and your environment details (Eclipse version, OS, Java version).

### Submitting Pull Requests

1. Fork the repository and create a feature branch from `main`.
2. Make your changes following the code style and architecture guidelines below.
3. Ensure all checks pass before submitting:
   ```shell
   ./mvnw checkstyle:check   # Code style compliance
   ./mvnw clean verify       # Compilation and packaging
   ./mvnw test               # Unit tests
   ```
4. Open a pull request with a clear description of the change and its motivation.
5. Address any feedback from reviewers.

### Security Vulnerabilities

**Please do not report security vulnerabilities through public GitHub issues.** For security reporting information, please review the guidance at [https://aka.ms/SECURITY.md](https://aka.ms/SECURITY.md).

## Project Structure

The project is a multi-module Maven/Tycho build consisting of OSGi bundles:

| Module | Purpose |
|--------|---------|
| `com.microsoft.copilot.eclipse.core` | Core functionality: LSP client, authentication, chat/completion logic |
| `com.microsoft.copilot.eclipse.ui` | User interface: chat view, completion UI, agent tools |
| `com.microsoft.copilot.eclipse.ui.jobs` | Copilot Jobs view integration |
| `com.microsoft.copilot.eclipse.terminal.api` | Terminal tool API definitions |
| `com.microsoft.copilot.eclipse.ui.terminal` | Terminal integration (Eclipse 4.37+) |
| `com.microsoft.copilot.eclipse.ui.terminal.tm` | TM Terminal integration (Eclipse 4.36 and earlier) |
| `com.microsoft.copilot.eclipse.branding` | Product branding and about dialog |
| `com.microsoft.copilot.eclipse.core.agent.*` | Platform-specific Copilot language server agent bundles |
| `com.microsoft.copilot.eclipse.feature` | Eclipse feature definition |
| `com.microsoft.copilot.eclipse.repository` | P2 update site |
| `com.microsoft.copilot.eclipse.core.test` | Core bundle tests |
| `com.microsoft.copilot.eclipse.ui.test` | UI bundle tests |

## Code Style

This project enforces **Google Java Style** (with customizations) via Checkstyle. The configuration is in [`checkstyle.xml`](checkstyle.xml).

## Development Guidelines

### Threading

- **Never block the UI thread** with I/O or long-running operations.
- Use `CompletableFuture.runAsync()` or Eclipse `Job` API for background work.
- Always update SWT widgets on the UI thread using `Display.asyncExec()` or `Display.syncExec()`.

### Resource Management

- Dispose SWT resources (fonts, images) when done.
- Use try-with-resources for streams and Eclipse resources.
- Close editors before deleting files.

### Error Handling

- Use Eclipse `IStatus` / `Status` objects for error reporting.
- Log errors via `CopilotCore.getPlugin().logError(message, exception)`.
- Never silently swallow exceptions.

### Dependencies

- Minimize bundle dependencies — only add what is necessary.
- Avoid circular dependencies between bundles.
- Use `Require-Bundle` for essential dependencies, `Import-Package` for optional or version-flexible ones.

### Testing

- Use **JUnit 5** (Jupiter) for new tests.
- Name test classes `<ClassName>Test` or `<ClassName>Tests`.
- Name test methods descriptively: `testMethodName_scenario_expectedOutcome`.
- Clean up resources in teardown methods.

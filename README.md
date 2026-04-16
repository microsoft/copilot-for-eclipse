# GitHub Copilot for Eclipse

GitHub Copilot for Eclipse brings AI-assisted coding to the Eclipse IDE with these core capabilities:

- **Code completions** for in-editor suggestions from code context or natural-language comments.
- **Next Edit Suggestions** provide context-aware suggestions for your next code edits.
- **Agent Mode** for conversational help and more autonomous, project-aware assistance.
- **Model Context Protocol (MCP)** integration to connect Copilot with external tools and services.
- **Advanced Agentic Capabilities** include Custom Agents, Isolated Subagents, and Plan Agent, with more agentic capabilities coming soon.


## Getting access to GitHub Copilot

Sign up for [GitHub Copilot Free](https://github.com/settings/copilot?utm_source=vscode-chat-readme&utm_medium=second&utm_campaign=2025mar-em-MSFT-signup), or request access from your enterprise admin.

To use GitHub Copilot, an active subscription is required. Learn more about business and individual plans at [github.com/features/copilot](https://github.com/features/copilot?utm_source=vscode-chat&utm_medium=readme&utm_campaign=2025mar-em-MSFT-signup).

## Prerequisites

- [Eclipse IDE](https://www.eclipse.org/downloads/)
- An active [GitHub Copilot subscription](https://github.com/features/copilot)

## Install and set up

### Option 1: Eclipse Marketplace

1. Open [Eclipse Marketplace](https://marketplace.eclipse.org/) and go to the [GitHub Copilot plugin page](https://marketplace.eclipse.org/content/github-copilot).
2. Drag **Install** to your running Eclipse workspace.
3. Restart Eclipse.
4. Sign in to GitHub Copilot from Eclipse.

### Option 2: Install from update site

1. In Eclipse, open **Help → Install New Software…**
2. Add this update site URL: `https://azuredownloads-g3ahgwb5b8bkbxhd.b01.azurefd.net/github-copilot/`
3. Select **GitHub Copilot** and complete installation.
4. Restart Eclipse and sign in.

## Core capabilities

### Code completions

Inline suggestions (ghost text) appear as you type in the editor. Suggestions can range from small edits to multi-line changes.

### Next Edit Suggestions

Next Edit Suggestions predict your next edit location and propose the next change based on your recent edits and context.

### Agent and Ask Mode

**Ask Mode** provides conversational AI assistance for explaining code, generating code from requirements, suggesting refactors, and providing debugging guidance.

**Agent Mode** works autonomously across your project context to identify and fix issues, propose implementation steps, and support larger coding tasks with iterative guidance.

### Model Context Protocol (MCP)

MCP support enables integrating external tools and services into Copilot workflows where configured.

### Advanced Agentic Capabilities

- **Custom Agents** allow users to create personalized agents with specific instructions and behaviors.
- **Isolated Subagents** can be spawned by the main agent to handle specific tasks or contexts independently.
- **Plan Agent** can generate multi-step plans to accomplish complex tasks, breaking them down into manageable actions.

For other available features in Eclipse, see the [Copilot feature matrix](https://docs.github.com/en/copilot/reference/copilot-feature-matrix?tool=eclipse).

## Usage tips

- Keep prompts specific and include constraints
- Request small, reviewable edits
- Validate output with tests and static analysis
- Treat AI output as draft code and review before committing

## Privacy and responsible use

We follow responsible practices in accordance with our
[Privacy Statement](https://docs.github.com/en/site-policy/privacy-policies/github-privacy-statement).

To get the latest security fixes, please use the latest version of GitHub Copilot for Eclipse.

## Data and telemetry

The GitHub Copilot for Eclipse plugin collects usage data and sends it to Microsoft to help improve our products and services. Read our [privacy statement](https://privacy.microsoft.com/privacystatement) to learn more.

## Security

Please do not report security vulnerabilities in public issues.

See [SECURITY.md](SECURITY.md) for vulnerability reporting instructions.

## Support

For bug reports and feature requests, use this repository’s Issues.

For support guidance, see [SUPPORT.md](SUPPORT.md).

## Trademarks

This project may contain trademarks or logos for projects, products, or services. Authorized use of Microsoft
trademarks or logos is subject to and must follow
[Microsoft's Trademark & Brand Guidelines](https://www.microsoft.com/legal/intellectualproperty/trademarks/usage/general).
Use of Microsoft trademarks or logos in modified versions of this project must not cause confusion or imply Microsoft sponsorship.
Any use of third-party trademarks or logos are subject to those third-party's policies.

## License

Copyright (c) Microsoft Corporation. All rights reserved.

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
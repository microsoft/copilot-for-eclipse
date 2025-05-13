# GitHub Copilot 0.6.0 Release Notes

## Feature Highlights

With the introduction of agent mode in Eclipse, vibe coding becomes a reality. GitHub Copilot's new agent mode can iterate on its own code, detect errors, and fix them automatically with a set of tools. It can suggest terminal commands for you to execute and even analyze run-time errors with self-healing capabilities. With MCP, GitHub Copilot can connect to external tools, enabling more interactive and context-aware assistance directly in your IDE.

### Agent Mode
Agent mode transforms GitHub Copilot into a more proactive coding companion, capable of understanding your project context and taking autonomous actions to help you code more efficiently. This powerful feature enables GitHub Copilot to:

- **Autonomous Problem-Solving**: Independently analyze code issues and propose solutions
- **Self-Iterative Development**: Learn from feedback and improve its suggestions in real-time
- **Contextual Command Suggestions**: Recommend and execute relevant terminal commands based on your current task
- **Error Detection and Resolution**: Automatically identify runtime errors and provide fixes
- **Smart Code Navigation**: Help you explore and understand complex codebases more effectively

#### How to Enable Agent Mode
In the chat window, if you are in the ask mode, select the agent mode in the drop down list.

### Model Context Protocol (MCP)
MCP is an open standard that enables AI models to interact with external tools and services through a unified interface. With MCP support in Eclipse, it extends agent mode capabilities by integrating tools contributed by MCP servers, enhancing the agent's ability to assist across more workflows and tasks. Key benefits include:

- **Extensible Tool Integration**: Connect with various external tools and services seamlessly
- **Enhanced Context Awareness**: Access broader context from different sources to provide more accurate assistance
- **Custom Workflow Support**: Create and integrate specialized tools for specific development workflows
- **Real-time Tool Interaction**: Interact with external services directly from your IDE
- **Community-driven Ecosystem**: Leverage community-contributed tools and extensions through the MCP standard

#### How to Configure MCP
Click the GitHub Copilot icon on the lower right corner, select **Edit Preferences**, then find the MCP Servers section to configure your MCP settings.

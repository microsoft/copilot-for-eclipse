# GitHub Copilot 0.7.0 Release Notes

## Feature Highlights
This release introduces a **new billing model** with support for premium requests in GitHub Copilot for Eclipse, enhanced chat usability with **Chat input history navigation**, **quick access to Agent Mode tool configuration**, and a range of **bug fixes and performance improvements**.

### 💵 Billing for GitHub Copilot Update
Starting **June 4, 2025**, a new billing model and updated user interface will be introduced in GitHub Copilot for Eclipse for all plans.

Your Copilot plan now includes [Premium requests](https://docs.github.com/en/copilot/managing-copilot/monitoring-usage-and-entitlements/about-premium-requests#premium-features), which provide access to more advanced models and features. These requests count against your monthly premium request allowance, calculated based on the [Model multipliers](https://docs.github.com/en/copilot/managing-copilot/monitoring-usage-and-entitlements/about-premium-requests#model-multipliers).

Read more on [About premium requests](https://docs.github.com/en/copilot/managing-copilot/monitoring-usage-and-entitlements/about-premium-requests) and [About billing for GitHub Copilot](https://docs.github.com/en/billing/managing-billing-for-your-products/managing-billing-for-github-copilot/about-billing-for-github-copilot).

### ⬆️⬇️ Chat Input History Navigation
You can now use the `Up` and `Down` arrow keys to navigate through your previous chat inputs, making it easier to reuse or revise past prompts.

### 🛠️ Quick Access to Agent Mode Tool Configuration
Click the `Tools` icon in the chat input box to quickly open the preferences page and configure your MCP tools.

### 🐞 Bug Fixes & Improvements
This release also includes bug fixes and enhancements to improve overall stability and user experience.

# GitHub Copilot 0.6.0 Release Notes

## Feature Highlights

With the introduction of agent mode in Eclipse, vibe coding becomes a reality. GitHub Copilot's new agent mode can iterate on its own code, detect errors, and fix them automatically with a set of tools. It can suggest terminal commands for you to execute and even analyze run-time errors with self-healing capabilities. With MCP, GitHub Copilot can connect to external tools, enabling more interactive and context-aware assistance directly in your IDE.

### Agent Mode
Agent mode transforms GitHub Copilot into a more proactive coding companion, capable of understanding your project context and taking autonomous actions to help you code more efficiently.

#### How to Enable Agent Mode
In the chat window, if you are in the ask mode, select the agent mode in the drop down list.
Read more on the [Copilot agent mode](https://docs.github.com/en/copilot/using-github-copilot/copilot-chat/asking-github-copilot-questions-in-your-ide?tool=eclipse#copilot-agent-mode-1).

### Model Context Protocol (MCP)
MCP is an open standard that enables AI models to interact with external tools and services through a unified interface. With MCP support in Eclipse, it extends agent mode capabilities by integrating tools contributed by MCP servers, enhancing the agent's ability to assist across more workflows and tasks. 

#### How to Configure MCP
Click the GitHub Copilot icon on the lower right corner, select **Edit Preferences**, then find the MCP Servers section to configure your MCP settings.
Read more on the [Copilot chat with MCP](https://docs.github.com/en/copilot/customizing-copilot/extending-copilot-chat-with-mcp?tool=eclipse).

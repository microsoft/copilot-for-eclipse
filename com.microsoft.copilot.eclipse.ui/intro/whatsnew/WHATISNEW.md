# GitHub Copilot 0.13.0 Release Notes
### Custom Agent
Custom agents bring customization to your chat mode by letting you specify name, description, tools, and models. Create specialized AI teammates tailored to your workflows and coding standards in Eclipse. Define agents using Markdown files that specify prompts so you can pick them up and run in your Eclipse quickly.

<video controls="true" src="./0.13.0/custom_agent.mp4" title="Custom Agent" style="max-width: 800px; width: 100%; height: auto;"></video>

---

### Plan
`Plan` helps AI think before it acts. It creates a clear plan first, so you can review and adjust it to fit your needs — then let the AI get to work. Simple, smart, and under your control.

<video controls="true" src="./0.13.0/plan.mp4" title="Plan" style="max-width: 800px; width: 100%; height: auto;"></video>

---

### Sub-agent
With Sub-Agent, your custom agents can now work in harmony under the guidance of a main agent. Each sub-agent tackles a specific task within its own isolated context, free from distractions — delivering sharper, more accurate results. Think of it as a team of specialists, each focused on what they do best, all orchestrated for maximum impact.

<video controls="true" src="./0.13.0/sub_agent.mp4" title="Sub-agent" style="max-width: 800px; width: 100%; height: auto;"></video>

---

### Copilot Coding Agent
With Copilot coding agent, GitHub Copilot can work independently in the background to complete tasks, just like a human developer: creating pull requests to solve issues in your GitHub repos.

<video controls="true" src="./0.13.0/coding_agent.mp4" title="Copilot Coding Agent" style="max-width: 800px; width: 100%; height: auto;"></video>

Note: [Click here to check more information](https://aka.ms/learn-copilot-coding-agent)

---

### Auto Model
Auto optimizes for model availability, currently routing to GPT-5, GPT-5 mini, GPT-4.1, Sonnet 4.5, and Haiku 4.5, depending on your subscription type. More models are coming soon.

---

# GitHub Copilot 0.12.0 Release Notes
### Chat History is Here!
Now you can easily revisit your past conversations anytime. And you can also rename a chat to give it a meaningful title, or remove it with just a click.

<video controls="true" src="./0.12.0/chat_history.mp4" title="Chat History" style="max-width: 800px; width: 100%; height: auto;"></video>

---

### Bring Your Own Key (BYOK) - Now in Public Preview
Bring Your Own Key (BYOK) support is now in public preview. If you already have an API key from a supported model provider, you can connect it in just a minute and start using their models directly.

<video controls="true" src="./0.12.0/byok.mp4" title="Bring Your Own Key" style="max-width: 1000px; width: 100%; height: auto;"></video>

Note: BYOK is available only for individual plans - Free, Pro, and Pro+, with `Editor Preview Features` turned on in your [Copilot Settings](https://github.com/settings/copilot/features).

---

### Re-organized Preferences Page.
As Copilot continues to grow with exciting new features, we’ve redesigned the plug-in preferences page to make it cleaner, more intuitive, and easier for you to discover everything at a glance.

![Re-organized Preferences Page](./0.12.0/preferences_page.png)

---

# GitHub Copilot 0.11.0 Release Notes
### More Convenient Ways to Add Chat Context
Adding context to your chats just got easier and more intuitive!
You now have multiple ways to include context files:

#### Drag and Drop

<video controls="true" src="./0.11.0/dnd.mp4" title="Drag and Drop"></video>

#### Right-Click from Explorer

<video controls="true" src="./0.11.0/context_menu.mp4" title="Right-Click from Explorer"></video>

---

### Enhanced Colors and Layout for Chat View
We’ve given the chat view a fresh coat of paint — and it looks better than ever!

#### Dark Theme Improvements

![Dark Theme Improvements](./0.11.0/dark_1.jpg)

#### Light Theme Improvements

![Light Theme Improvements](./0.11.0/light_1.jpg)

---

### Reduced Plugin Size
By splitting platform-specific binaries into separate fragments, the overall plugin size has been greatly reduced, which means faster downloads and updates.

---

### New Public API to Start a Chat Session Programmatically

We’ve introduced a new public API that allows other plugins to seamlessly start a new ask session in the Copilot chat view. Plug-ins can now invoke the command: `com.microsoft.copilot.eclipse.commands.openChatView` with two optional parameters:

- `com.microsoft.copilot.eclipse.commands.openChatView.inputValue`: A string representing the initial content of the chat.
- `com.microsoft.copilot.eclipse.commands.openChatView.autoSend`: A boolean indicating whether to automatically submit the content.

This opens up exciting possibilities for plugin developers to trigger contextual Copilot interactions directly from their tools.

#### Example: Spring Tools Plug-in Integration

Here’s how the Spring Tools plug-in leverages the new API to launch a chat session:

<video controls="true" src="./0.11.0/api.mp4" title="Spring Tools Plug-in Integration"></video>

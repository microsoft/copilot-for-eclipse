# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 0.16.0
### Added
- Support tool calling in Ask Mode.
- Show detailed model information on dropdown hover.
- Add openChatView mode parameter and wire handoff to command.

### Changed
- Update chat mode selector and model selector to show more information.
- Remove 'Included' billing message and simplify multiplier to 0.
- Remove border for Add Context button and center layout in ActionBar.

### Fixed
- Fix NPE under updateCodeMinings() lambda if editor is already disposed
- Fix bind Ctrl++ shortcut additionally for increaseChatFontSize command
- Fix Binary LSP agent start failed
- Fix some languages does not have syntax highlighting
- Fix wrong line delimiter is generated in completion
- Fix "Copy and Open" does not copy
- Fix BadPositionCategoryException in RenderManager when closing markdown editor
- Fix use invisible OSC escape sequence for terminal command completion marker
- Fix URI comparison for similar URI schemes.
- Fix CLS "temperature and top_p cannot both be specified for this model".

## 0.15.0
### Added
- Add JDT debugger tool for agent.
- Support increasing or decreasing font size in chat view.
- Add ManageTodoList tool UI support.
- Support agent max request preference.
- Add current editor selection to chat context.
- Support custom scheme file creation, edit and get errors.
- Support commit instruction.

### Changed
- Update MCP registry dialog.
- Remove nightly check for the MCP registry feature.
- Update file change summary bar hover effect.

### Fixed
- Remove unimplemented ToolConfigurationQuickFixProcessor causing ClassNotFoundException.
- NPE from NES feature when working on an editor without text widget.
- Update Jobs View category to reflect correct labeling.
- Fix markup rendering under dark theme.
- Fix css for handoff container in dark mode.
- Support traverse through the chat view via Tab.
- Fix git repository detection when .git is excluded in .project.
- Fix ChatView input undo/redo functionality.
- Remove redundant focus listener for created buttons.
- Defer the status check until setting sync is finished.
- Prevent deadlock in updateCodeMinings by using asyncExec.
- Add focus visual hint for widgets in action bar.
- Linux terminal shell not working due to incorrect environment property.
- Add content type to the quickAssistProcessor extension point.
- Always update modeToolStatus even when no tools are defined.
- Refactor action area visibility handling in chat history viewer.
- Set model apply to always.
- IllegalArgumentException when parsing Windows file paths in chat hyperlinks.
- SWTException on shutdown when Chat view is open.

## 0.14.0
### Added
- Set a max file number for the FileChangeSummaryBar and make the bar scrollable.
- Add dialog prompting users about missing terminal dependencies.
- Enable CVE Remediator sub-agent (rollout progressively).

### Changed
- Update MCP registry API version to v0.1.
- Move the Coding Agent Jobs top buttons to view toolbar.
- Move the Chat view top buttons to view toolbar.
- Remove the allow list for MCP contribution extension point.

### Fixed
- Simplify the parameters for getting built-in chat modes.
- Do not show footer for coding agent turns.
- Completion not working in .agent.md files.
- Update feedback URL.
- NPE when initialize MCP registry dialog.
- Failed to connect to proxy when auth contains backslash.
- Support non-UTF-8 encoded files.
- Eclipse hangs when the workspace contains too many files.
- Exclude output files when collecting watched files.
- Enable horizontal scrolling for command text in tool confirmation box.
- Chat view is empty when opening it after plugin activated.
- Update the UI for organization managed settings.
- Revert workaround for free plan users default model.
- Tools status will not be updated when manually edit tool list.
- Prompt user to restart eclipse when sub-agent preference changes.
- Load custom chat modes asynchronously to prevent UI freeze.
- Directly open the created file when clicking it in file change summary bar.
- Cannot create new empty files in new workspace in agent mode.
- Tool list is not refresh after configure tools in an unsaved .agent.md file.
- Improve tool specification parsing to handle server names with slashes.
- Avoid blocking the thread when sync tools.
- Update prompt of run_in_terminal tool.
- Improve event handling in ChatView and FileToolService.
- Quota display rendering not correct on MacOS.
- Improve the perf when typing in chat view.
- Limited description length to 100 in AgentMessageWidget.
- Should prompt user when disposing file change summary bar.
- Changed files panel will not dispose when switching chat history.
- Added tool call status to the tool call reply.

## 0.13.1
### Fixed
- Chat View - NPE when rendering buttons in action bar.
- Completion - Invalid thread access when completion in Eclipse 2024-03.

## 0.13.0
### Added
- Support Next Edit Suggestion (NES).
- Support Custom Agent.
- Support Plan mode.
- Support Auto model.
- Support delegating tasks to coding agent and view the jobs.
- Support dynamic OAuth for MCP servers.
- Support allow list check for the MCP registry.

### Changed
- Update chat view icons.

### Fixed
- MCP - Sync proxy bypass settings to CLS.
- MCP Registry - Cannot restore MCP registry URL.
- MCP Registry - Auto load more not working on MacOS.
- MCP Registry - Check server ID and base URL for MCP servers from registry.
- MCP Registry - Dynamically set the table row height for MCP registry dialog.- MCP Registry - Only store the MCP registry URL to configuration scope.
- MCP Registry - Refresh the tool bar of MCP registry dialog after clicking.
- Chat History - Persisted chat history title contains line breaks.
- Chat History - Conversation with id does not exist.
- Chat View: Apply default TM theme for source viewer.
- Extension Point - Activate bundle when the checking the MCP registration.
- Extension Point - Allow plugin to remove the mcp registration.
- Extension Point - Displaying new MCP server registration found but none actually exists.
- Accessibility - Add name attribute to the widgets in chat view.
- Typo - typo in completion settings page.


## 0.12.0
### Added
- Support chat history.
- Support BYOK (Bring Your Own Keys), including Azure, OpenAI, Groq, Anthropic, OpenRouter and Gemini.
- (Preview) Support MCP Registry.
- (Preview) Add an extension point to allow MCP server registration from other plugins.

### Changed
- Show the generate commit message button to different places per Eclipse platform version.
- Re-organize the Copilot preference pages.
- Use new GitHub App ID.

### Fixed
- Improve focus indicator for buttons in chat view.
- Misleading description for custom instructions.
- SWT Resource was not properly disposed by run_in_terminal tool.
- java.nio.file.FileSystemException thrown by TerminalServiceManager.
- Rendering of the whats new page is broken on webkit.
- Consider product customization for what's new preferences.
- Get charset by file.
- Dedup the files from the add context file dialog.
- '&' is used as mnemonic character in SWT Label.
- Refine color of line separator in chat view.
- Validates the files before editing.
- Set right background color and hover listener for action items in summary bar.
- Do not trigger completion if code mining is disabled.
- UI bundle is started before CLS is activated.


## 0.11.0
### Added
- Support drag and drop resources to referenced files.
- Support adding resources to referenced files via context menu in Package Explorer and Project Explorer.
- Enhance the color design of chat view.
- Use fragment bundle to split Copilot Language Server binaries.
- Add public API to start a new ask session.
- Add Copilot chat view to JEE related perspectives.
- Use configuration scope to control whether to show what's new page and expose to preference dialog.
- Add copyright info and branding plugin.

### Fixed
- Input history in chat is wrong in a new conversation.
- Use configuration scope to control getting started walkthrough page display.
- Fix compatibility issue for terminal across different Eclipse platform versions.
- Typo in release note entry.
- Referenced files cannot be closed if the project is deleted.
- NPE when calling InputNavigation.
- Shift+Tab move from inputText to chatContent.

## 0.10.0
### Added
- Support custom instructions.
- Support MCP feature flag.
- Support GitHub MCP server OAuth.
- Support adding image to the chat context.
- Support adding folder to chat context.
- Add confirmation dialog for unhandled files when create a new conversation in agent mode.
- Add `Edit Preferences...` button into chat top banner.
- Show conversation title in chat top banner.

### Changed
- Improve the Copilot perspective with onboarding images and more shortcuts.
- Update chat view's icon.
- Merge all open url related commands into one command.

### Fixed
- Error 'Document for URI could not be found' during chat.
- Unexpected files are listed in the Search Attachments dialog.
- Correct the default index when build SignInDialog.
- Input history is not cleared after switching account.
- Preference will be cleared if username is not ready when start up.
- Delay the show hint invocation timing to avoid command not found error.
- Active model does not reset to default model when model list change.
- Welcome view does not render correctly when height is limited.
- Persist chat input when mode switches.
- Send MCP tools status notification after server started.

### Removed
- Remove CopilotAuthStatusListener from AvatarService.
- Remove CopilotAuthStatusListener from CopilotStatusManager.

## 0.9.3
### Fixed
- Update CLS to 1.348.0.

## 0.9.2
### Fixed
- Update CLS to 1.347.0.

## 0.9.1
### Fixed
- Reset history to avoid skipping the main section rendering.
- Updated bundle version to fit 2024-03.
- Fixed Linux rendering problem.
- Async open chat after closing welcome page.
- Use IPreferenceStore.getBoolean() to get the updated value.
- Perspective logo should support dark mode.

## 0.9.0
### Added
- Show MCP logs in Console View.
- Add welcome introduction page.
- Support workspace context (@workspace) in ask mode.
- Add open chat view command to perspectives' onboard command list.
- Add keyboard shortcut command for open chat view command.
- Add new Copilot perspective.
- Support generate git commit message.

### Changed
- Support Eclipse 2024-03 & 2024-06.
- Make agent mode as default chat mode.
- Improve the chat view layout.
- Improve the Copilot menu in menu bar and status bar.
- Remove the spinner when completing code.

### Fixed
- MCP tool configuration button should not be visible in ask mode.
- Use workbench job to avoid blocking shutdown action.
- Check if the project is accessible before scanning watched files.
- Fix quota rendering issue on MacOS and Linux.
- Wrong completion when IDE auto closed brackets.
- Entire settings are synced even just changing one item.
- Wrong welcome page displayed in chat view when user is not signed in.
- File with no extension cannot be attached in chat view.
- Error 'SWT Resource was not properly disposed' after sign in.

## 0.8.0
### Added
- Enable remote MCP server.
- Add up-sell link to the model picker for free plan accounts.

### Changed
- Make the chat view appear as a side bar by default.

### Fixed
- MCP tools are not visible.
- Validate duplicate keys in MCP preference page.
- Last line of the completion dialog in chat view is not visible.
- Support error status for tool invocation result.
- Fix rendering issue on Linux GTK.
- Cannot use arrow up key in the completion dialog in chat view.
- Decimal display incorrectly in usage quota.
- Invalid thread access when reuse compare editor.
- Reuse existing compare editor for create_file tool.
- Add timeout when fetching env during activation on MacOS.
- Check signin before get persisted path.

## 0.7.0
### Added
- New billing support and user interface update.
- Input history navigation.
- A button shortcut to open the MCP configuration page.

### Changed
- Update CLS to 1.327.0.
- Update Copilot status icon.

### Fixed
- Fix the memory leak issue that the document is not disconnected.
- Document for URI could not be found.
- No tools is displayed in MCP configuration page.
- NPE when resolve menu bar handler.
- Compare editor title cannot be rendered correctly.

## 0.6.1
### Fixed
- Correct the bundle version requirement to align with Eclipse 2024-09.

## 0.6.0
### Added
- Support agent mode with stdio mcp server integration in chat.

## 0.5.1
### Fixed
- Annotation model is null when triggering completion.
- Input text box shakes when sending message by hitting Enter-Key.
- SWTException when disposing completion manager.
- Timeout error shows late when fail to login.
- Improve auto scroll to bottom behavior.
- Fixed schema name copilotCapabilities.
- Wrong node runtime may be found.

## 0.5.0
### Added
- Added GitHub Copilot menu to the top menu bar.

### Changed
- Updated the LS to 1.290.0.

### Fixed
- Stop append INFO log when format preference changes.
- Should not attach bin files even it was opened in editor (behavior of VSCode).

## 0.4.0
### Added
- Support ABAP.

### Changed
- Mark org.eclipse.jdt.annotation to optional.

### Fixed
- NPE when IFile.getLocation() is null.
- Illegal state exception in Turn widget.
- SWT resources not disposed properly.
- Markdown viewer fallbacks to textviewer.
- Chat input cannot be rendered as multi line when input text in too long.
- Exception when deleting word leading with brackets in chat input box.

## 0.3.0
### Added
- Support chat feature
   - Support to create a new conversation
   - Support slash commands
   - Support to attach context files
   - Support cancel a conversation
   - Support model picker for chat

## 0.2.0
### Added
- Support C/C++ format options.

### Fixed
- Track uncaught exceptions.
- Invalid thread access when generating completion.
- NPE when authStatesManager is not ready.
- Noise error log when signin is cancelled.
- Hide the credential information in proxy log.
- Remove hard-coded plugin version in GithubPanicErrorReport.
- Move the update status icon logic to display thread.

## 0.1.0
### Added
- Support authentication from GitHub Copilot.
- Support free plan subscription.
- Support inline completion.
- Support accepting completion by word.
- Support fetching Java format options when triggering inline completion.
- Support proxy configuration.
- Support toggling auto inline completion.
- Support configuring key bindings from the status bar menu.
- Support opening feedback forum from the status bar menu.
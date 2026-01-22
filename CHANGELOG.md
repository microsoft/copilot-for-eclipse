# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 0.15.0
### Added
- Add JDT debugger tool for agent. [PR#1476](https://github.com/microsoft/copilot-eclipse/pull/1476)
- Support increasing or decreasing font size in chat view. [PR#1533](https://github.com/microsoft/copilot-eclipse/pull/1533), [PR#1543](https://github.com/microsoft/copilot-eclipse/pull/1543), [PR#1540](https://github.com/microsoft/copilot-eclipse/pull/1540)
- Add ManageTodoList tool UI support. [PR#1523](https://github.com/microsoft/copilot-eclipse/pull/1523), [PR#1535](https://github.com/microsoft/copilot-eclipse/pull/1535)
- Support agent max request preference. [PR#1518](https://github.com/microsoft/copilot-eclipse/pull/1518)
- Add current editor selection to chat context. [#1214](https://github.com/microsoft/copilot-eclipse/issues/1214)
- Support custom scheme file creation, edit and get errors. [PR#1531](https://github.com/microsoft/copilot-eclipse/pull/1531)
- Support commit instruction. [#1117](https://github.com/microsoft/copilot-eclipse/issues/1117)

### Changed
- Update MCP registry dialog. [PR#1504](https://github.com/microsoft/copilot-eclipse/pull/1504)
- Remove nightly check for the MCP registry feature. [PR#1516](https://github.com/microsoft/copilot-eclipse/pull/1516)
- Update file change summary bar hover effect. [#1486](https://github.com/microsoft/copilot-eclipse/issues/1486)

### Fixed
- NPE from NES feature when working on an editor without text widget. [PR#1553](https://github.com/microsoft/copilot-eclipse/pull/1553)
- Update Jobs View category to reflect correct labeling. [PR#1552](https://github.com/microsoft/copilot-eclipse/pull/1552)
- Fix markup rendering under dark theme. [#460](https://github.com/microsoft/copilot-eclipse/issues/460)
- Fix css for handoff container in dark mode. [PR#1536](https://github.com/microsoft/copilot-eclipse/pull/1536)
- Support traverse through the chat view via Tab. [PR#1524](https://github.com/microsoft/copilot-eclipse/pull/1524)
- Fix git repository detection when .git is excluded in .project. [#1521](https://github.com/microsoft/copilot-eclipse/issues/1521)
- Fix ChatView input undo/redo functionality. [#1437](https://github.com/microsoft/copilot-eclipse/issues/1437)
- Remove redundant focus listener for created buttons. [PR#1515](https://github.com/microsoft/copilot-eclipse/pull/1515)
- Defer the status check until setting sync is finished. [#1429](https://github.com/microsoft/copilot-eclipse/issues/1429)
- Prevent deadlock in updateCodeMinings by using asyncExec. [PR#1510](https://github.com/microsoft/copilot-eclipse/pull/1510)
- Add focus visual hint for widgets in action bar. [PR#1513](https://github.com/microsoft/copilot-eclipse/pull/1513)
- Linux terminal shell not working due to incorrect environment property. [PR#1508](https://github.com/microsoft/copilot-eclipse/pull/1508), [PR#1499](https://github.com/microsoft/copilot-eclipse/pull/1499)
- Add content type to the quickAssistProcessor extension point. [PR#1502](https://github.com/microsoft/copilot-eclipse/pull/1502)
- Always update modeToolStatus even when no tools are defined. [#1491](https://github.com/microsoft/copilot-eclipse/issues/1491)
- Refactor action area visibility handling in chat history viewer. [PR#1497](https://github.com/microsoft/copilot-eclipse/pull/1497)
- Set model apply to always. [PR#1494](https://github.com/microsoft/copilot-eclipse/pull/1494)
- IllegalArgumentException when parsing Windows file paths in chat hyperlinks. [#1539](https://github.com/microsoft/copilot-eclipse/issues/1539)

## 0.14.0
### Added
- Set a max file number for the FileChangeSummaryBar and make the bar scrollable. [#1339](https://github.com/microsoft/copilot-eclipse/issues/1339)
- Add dialog prompting users about missing terminal dependencies. [PR#1467](https://github.com/microsoft/copilot-eclipse/pull/1467)
- Enable CVE Remediator sub-agent (rollout progressively). [#1319](https://github.com/microsoft/copilot-eclipse/issues/1319) 

### Changed
- Update MCP registry API version to v0.1. [PR#1442](https://github.com/microsoft/copilot-eclipse/pull/1442)
- Move the Coding Agent Jobs top buttons to view toolbar. [PR#1418](https://github.com/microsoft/copilot-eclipse/pull/1418)
- Move the Chat view top buttons to view toolbar. [PR#1423](https://github.com/microsoft/copilot-eclipse/pull/1423)
- Remove the allow list for MCP contribution extension point. [PR#1427](https://github.com/microsoft/copilot-eclipse/pull/1427)

### Fixed
- Simplify the parameters for getting built-in chat modes. [PR#1403](https://github.com/microsoft/copilot-eclipse/pull/1403)
- Do not show footer for coding agent turns. [PR#1419](https://github.com/microsoft/copilot-eclipse/pull/1419)
- Completion not working in .agent.md files. [#1380](https://github.com/microsoft/copilot-eclipse/issues/1380)
- Update feedback URL. [PR#1421](https://github.com/microsoft/copilot-eclipse/pull/1421)
- NPE when initialize MCP registry dialog. [#1424](https://github.com/microsoft/copilot-eclipse/issues/1424)
- Failed to connect to proxy when auth contains backslash. [PR#1432](https://github.com/microsoft/copilot-eclipse/pull/1432)
- Support non-UTF-8 encoded files. [PR#1414](https://github.com/microsoft/copilot-eclipse/pull/1414)
- Eclipse hangs when the workspace contains too many files. [#1430](https://github.com/microsoft/copilot-eclipse/issues/1430)
- Exclude output files when collecting watched files. [PR#1439](https://github.com/microsoft/copilot-eclipse/pull/1439)
- Enable horizontal scrolling for command text in tool confirmation box. [PR#1440](https://github.com/microsoft/copilot-eclipse/pull/1440)
- Chat view is empty when opening it after plugin activated. [#1426](https://github.com/microsoft/copilot-eclipse/issues/1426)
- Update the UI for organization managed settings. [PR#1438](https://github.com/microsoft/copilot-eclipse/pull/1438)
- Revert workaround for free plan users default model. [PR#1447](https://github.com/microsoft/copilot-eclipse/pull/1447)
- Tools status will not be updated when manually edit tool list. [#1388](https://github.com/microsoft/copilot-eclipse/issues/1388)
- Prompt user to restart eclipse when sub-agent preference changes. [#1400](https://github.com/microsoft/copilot-eclipse/issues/1400)
- Load custom chat modes asynchronously to prevent UI freeze. [PR#1457](https://github.com/microsoft/copilot-eclipse/pull/1457)
- Directly open the created file when clicking it in file change summary bar. [#1464](https://github.com/microsoft/copilot-eclipse/issues/1464)
- Cannot create new empty files in new workspace in agent mode. [#1299](https://github.com/microsoft/copilot-eclipse/issues/1299)
- Tool list is not refresh after configure tools in an unsaved .agent.md file. [#1416](https://github.com/microsoft/copilot-eclipse/issues/1416)
- Improve tool specification parsing to handle server names with slashes. [PR#1471](https://github.com/microsoft/copilot-eclipse/pull/1471)
- Avoid blocking the thread when sync tools. [PR#1465](https://github.com/microsoft/copilot-eclipse/pull/1465)
- Update prompt of run_in_terminal tool. [PR#1477](https://github.com/microsoft/copilot-eclipse/pull/1477)
- Improve event handling in ChatView and FileToolService. [PR#1475](https://github.com/microsoft/copilot-eclipse/pull/1475)
- Quota display rendering not correct on MacOS. [#1456](https://github.com/microsoft/copilot-eclipse/issues/1456)
- Improve the perf when typing in chat view. [PR#1478](https://github.com/microsoft/copilot-eclipse/pull/1478)
- Limited description length to 100 in AgentMessageWidget. [PR#1480](https://github.com/microsoft/copilot-eclipse/pull/1480)
- Should prompt user when disposing file change summary bar. [#1473](https://github.com/microsoft/copilot-eclipse/issues/1473)
- Changed files panel will not dispose when switching chat history. [#1152](https://github.com/microsoft/copilot-eclipse/issues/1152)
- Added tool call status to the tool call reply. [#1484](https://github.com/microsoft/copilot-eclipse/pull/1484)

## 0.13.1
### Fixed
- Chat View - NPE when rendering buttons in action bar. [PR#1411](https://github.com/microsoft/copilot-eclipse/pull/1411)
- Completion - Invalid thread access when completion in Eclipse 2024-03. [PR#1412](https://github.com/microsoft/copilot-eclipse/pull/1412)

## 0.13.0
### Added
- Support Next Edit Suggestion (NES). [PR#1283](https://github.com/microsoft/copilot-eclipse/pull/1283)
- Support Custom Agent. [PR#1315](https://github.com/microsoft/copilot-eclipse/pull/1315), [PR#1329](https://github.com/microsoft/copilot-eclipse/pull/1329)
- Support Plan mode. [PR#1344](https://github.com/microsoft/copilot-eclipse/pull/1344), [PR#1345](https://github.com/microsoft/copilot-eclipse/pull/1345)
- Support Auto model. [#1303](https://github.com/microsoft/copilot-eclipse/issues/1303)
- Support delegating tasks to coding agent and view the jobs. [#1327](https://github.com/microsoft/copilot-eclipse/pull/1327)
- Support dynamic OAuth for MCP servers. [PR#1328](https://github.com/microsoft/copilot-eclipse/pull/1328)
- Support allow list check for the MCP registry. [PR#1245](https://github.com/microsoft/copilot-eclipse/pull/1245), [PR#1255](https://github.com/microsoft/copilot-eclipse/pull/1255)

### Changed
- Update chat view icons. [PR#1274](https://github.com/microsoft/copilot-eclipse/pull/1274)

### Fixed
- MCP - Sync proxy bypass settings to CLS. [PR#1314](https://github.com/microsoft/copilot-eclipse/pull/1314)
- MCP Registry - Cannot restore MCP registry URL. [PR#1248](https://github.com/microsoft/copilot-eclipse/pull/1248)
- MCP Registry - Auto load more not working on MacOS. [#1252](https://github.com/microsoft/copilot-eclipse/issues/1252)
- MCP Registry - Check server ID and base URL for MCP servers from registry. [PR#1263](https://github.com/microsoft/copilot-eclipse/pull/1263)
- MCP Registry - Dynamically set the table row height for MCP registry dialog. [#1208](https://github.com/microsoft/copilot-eclipse/issues/1208)- MCP Registry - Only store the MCP registry URL to configuration scope. [PR#1291](https://github.com/microsoft/copilot-eclipse/pull/1291)
- MCP Registry - Refresh the tool bar of MCP registry dialog after clicking. [PR#1290](https://github.com/microsoft/copilot-eclipse/pull/1290)
- Chat History - Persisted chat history title contains line breaks. [#1250](https://github.com/microsoft/copilot-eclipse/issues/1250)
- Chat History - Conversation with id does not exist. [#1261](https://github.com/microsoft/copilot-eclipse/issues/1261)
- Chat View: Apply default TM theme for source viewer. [PR#1287](https://github.com/microsoft/copilot-eclipse/pull/1287)
- Extension Point - Activate bundle when the checking the MCP registration. [PR#1262](https://github.com/microsoft/copilot-eclipse/pull/1262)
- Extension Point - Allow plugin to remove the mcp registration. [PR#1277](https://github.com/microsoft/copilot-eclipse/pull/1277)
- Extension Point - Displaying new MCP server registration found but none actually exists. [#1293](https://github.com/microsoft/copilot-eclipse/issues/1293)
- Accessibility - Add name attribute to the widgets in chat view. [PR#1312](https://github.com/microsoft/copilot-eclipse/pull/1312)
- Typo - typo in completion settings page. [#1270](https://github.com/microsoft/copilot-eclipse/issues/1270)


## 0.12.0
### Added
- Support chat history. [#246](https://github.com/microsoft/copilot-eclipse/issues/246)
- Support BYOK (Bring Your Own Keys), including Azure, OpenAI, Groq, Anthropic, OpenRouter and Gemini. [#1098](https://github.com/microsoft/copilot-eclipse/issues/1098), [#1099](https://github.com/microsoft/copilot-eclipse/issues/1099), [#1205](https://github.com/microsoft/copilot-eclipse/issues/1205)
- (Preview) Support MCP Registry. [PR#1210](https://github.com/microsoft/copilot-eclipse/pull/1210)
- (Preview) Add an extension point to allow MCP server registration from other plugins. [PR#1142](https://github.com/microsoft/copilot-eclipse/pull/1142)

### Changed
- Show the generate commit message button to different places per Eclipse platform version. [PR#1138](https://github.com/microsoft/copilot-eclipse/pull/1138)
- Re-organize the Copilot preference pages. [#1107](https://github.com/microsoft/copilot-eclipse/issues/1107)
- Use new GitHub App ID. [PR#1179](https://github.com/microsoft/copilot-eclipse/pull/1179)

### Fixed
- Improve focus indicator for buttons in chat view. [PR#1096](https://github.com/microsoft/copilot-eclipse/pull/1096)
- Misleading description for custom instructions. [#1139](https://github.com/microsoft/copilot-eclipse/issues/1139)
- SWT Resource was not properly disposed by run_in_terminal tool. [#1140](https://github.com/microsoft/copilot-eclipse/issues/1140)
- java.nio.file.FileSystemException thrown by TerminalServiceManager. [#1143](https://github.com/microsoft/copilot-eclipse/issues/1143)
- Rendering of the whats new page is broken on webkit. [PR#1161](https://github.com/microsoft/copilot-eclipse/pull/1161)
- Consider product customization for what's new preferences. [PR#1166](https://github.com/microsoft/copilot-eclipse/pull/1166)
- Get charset by file. [PR#1173](https://github.com/microsoft/copilot-eclipse/pull/1173)
- Dedup the files from the add context file dialog. [PR#1177](https://github.com/microsoft/copilot-eclipse/pull/1177)
- '&' is used as mnemonic character in SWT Label. [PR#1176](https://github.com/microsoft/copilot-eclipse/pull/1176)
- Refine color of line separator in chat view. [#1146](https://github.com/microsoft/copilot-eclipse/issues/1146)
- Validates the files before editing. [PR#1190](https://github.com/microsoft/copilot-eclipse/pull/1190)
- Set right background color and hover listener for action items in summary bar. [#710](https://github.com/microsoft/copilot-eclipse/issues/710)
- Do not trigger completion if code mining is disabled. [PR#1195](https://github.com/microsoft/copilot-eclipse/pull/1195), [PR#1200](https://github.com/microsoft/copilot-eclipse/pull/1200)
- UI bundle is started before CLS is activated. [PR#1230](https://github.com/microsoft/copilot-eclipse/pull/1230)


## 0.11.0
### Added
- Support drag and drop resources to referenced files. [PR#1059](https://github.com/microsoft/copilot-eclipse/pull/1059)
- Support adding resources to referenced files via context menu in Package Explorer and Project Explorer. [PR#1040](https://github.com/microsoft/copilot-eclipse/pull/1040)
- Enhance the color design of chat view. [PR#1081](https://github.com/microsoft/copilot-eclipse/pull/1081)
- Use fragment bundle to split Copilot Language Server binaries. [PR#1083](https://github.com/microsoft/copilot-eclipse/pull/1083), [PR#1085](https://github.com/microsoft/copilot-eclipse/pull/1085)
- Add public API to start a new ask session. [PR#1031](https://github.com/microsoft/copilot-eclipse/pull/1031)
- Add Copilot chat view to JEE related perspectives. [PR#1076](https://github.com/microsoft/copilot-eclipse/pull/1076)
- Use configuration scope to control whether to show what's new page and expose to preference dialog. [PR#1100](https://github.com/microsoft/copilot-eclipse/pull/1100)
- Add copyright info and branding plugin. [PR#1079](https://github.com/microsoft/copilot-eclipse/pull/1079), [PR#1074](https://github.com/microsoft/copilot-eclipse/pull/1074)

### Fixed
- Input history in chat is wrong in a new conversation. [#902](https://github.com/microsoft/copilot-eclipse/issues/902)
- Use configuration scope to control getting started walkthrough page display. [PR#1084](https://github.com/microsoft/copilot-eclipse/pull/1084)
- Fix compatibility issue for terminal across different Eclipse platform versions. [PR#1080](https://github.com/microsoft/copilot-eclipse/pull/1080)
- Typo in release note entry. [PR#1082](https://github.com/microsoft/copilot-eclipse/pull/1082)
- Referenced files cannot be closed if the project is deleted. [#1053](https://github.com/microsoft/copilot-eclipse/issues/1053)
- NPE when calling InputNavigation. [PR#1075](https://github.com/microsoft/copilot-eclipse/pull/1075)
- Shift+Tab move from inputText to chatContent. [PR#1077](https://github.com/microsoft/copilot-eclipse/pull/1077)

## 0.10.0
### Added
- Support custom instructions. [#576](https://github.com/microsoft/copilot-eclipse/issues/576)
- Support MCP feature flag. [PR#1010](https://github.com/microsoft/copilot-eclipse/pull/1010)
- Support GitHub MCP server OAuth. [PR#990](https://github.com/microsoft/copilot-eclipse/pull/990)
- Support adding image to the chat context. [#968](https://github.com/microsoft/copilot-eclipse/pull/968)
- Support adding folder to chat context. [PR#1032](https://github.com/microsoft/copilot-eclipse/pull/1032)
- Add confirmation dialog for unhandled files when create a new conversation in agent mode. [#977](https://github.com/microsoft/copilot-eclipse/pull/977)
- Add `Edit Preferences...` button into chat top banner. [PR#1019](https://github.com/microsoft/copilot-eclipse/pull/1019)
- Show conversation title in chat top banner. [PR#978](https://github.com/microsoft/copilot-eclipse/pull/978)

### Changed
- Improve the Copilot perspective with onboarding images and more shortcuts. [PR#986](https://github.com/microsoft/copilot-eclipse/pull/986)
- Update chat view's icon. [PR#981](https://github.com/microsoft/copilot-eclipse/pull/981)
- Merge all open url related commands into one command. [PR#1030](https://github.com/microsoft/copilot-eclipse/pull/1030)

### Fixed
- Error 'Document for URI could not be found' during chat. [#884](https://github.com/microsoft/copilot-eclipse/issues/884)
- Unexpected files are listed in the Search Attachments dialog. [#530](https://github.com/microsoft/copilot-eclipse/issues/530)
- Correct the default index when build SignInDialog. [PR#1025](https://github.com/microsoft/copilot-eclipse/pull/1025)
- Input history is not cleared after switching account. [#835](https://github.com/microsoft/copilot-eclipse/issues/835)
- Preference will be cleared if username is not ready when start up. [#1008](https://github.com/microsoft/copilot-eclipse/issues/1008)
- Delay the show hint invocation timing to avoid command not found error. [PR#998](https://github.com/microsoft/copilot-eclipse/pull/998)
- Active model does not reset to default model when model list change. [#987](https://github.com/microsoft/copilot-eclipse/issues/987)
- Welcome view does not render correctly when height is limited. [#895](https://github.com/microsoft/copilot-eclipse/issues/895)
- Persist chat input when mode switches. [#762](https://github.com/microsoft/copilot-eclipse/issues/762)
- Send MCP tools status notification after server started. [#1050](https://github.com/microsoft/copilot-eclipse/issues/1050)

### Removed
- Remove CopilotAuthStatusListener from AvatarService. [PR#1024](https://github.com/microsoft/copilot-eclipse/pull/1024)
- Remove CopilotAuthStatusListener from CopilotStatusManager. [PR#1014](https://github.com/microsoft/copilot-eclipse/pull/1014)

## 0.9.3
### Fixed
- Update CLS to 1.348.0.

## 0.9.2
### Fixed
- Update CLS to 1.347.0.

## 0.9.1
### Fixed
- Reset history to avoid skipping the main section rendering. [#970](https://github.com/microsoft/copilot-eclipse/pull/970)
- Updated bundle version to fit 2024-03. [#971](https://github.com/microsoft/copilot-eclipse/pull/971)
- Fixed Linux rendering problem. [#974](https://github.com/microsoft/copilot-eclipse/pull/974)
- Async open chat after closing welcome page. [#975](https://github.com/microsoft/copilot-eclipse/pull/975)
- Use IPreferenceStore.getBoolean() to get the updated value. [#976](https://github.com/microsoft/copilot-eclipse/pull/976)
- Perspective logo should support dark mode. [#980](https://github.com/microsoft/copilot-eclipse/pull/980)

## 0.9.0
### Added
- Show MCP logs in Console View. [#833](https://github.com/microsoft/copilot-eclipse/issues/833)
- Add welcome introduction page. [PR#904](https://github.com/microsoft/copilot-eclipse/pull/904)
- Support workspace context (@workspace) in ask mode. [PR#780](https://github.com/microsoft/copilot-eclipse/pull/780)
- Add open chat view command to perspectives' onboard command list. [PR#883](https://github.com/microsoft/copilot-eclipse/pull/883)
- Add keyboard shortcut command for open chat view command. [PR#882](https://github.com/microsoft/copilot-eclipse/pull/882)
- Add new Copilot perspective. [PR#912](https://github.com/microsoft/copilot-eclipse/pull/912)
- Support generate git commit message. [#794](https://github.com/microsoft/copilot-eclipse/issues/794)

### Changed
- Support Eclipse 2024-03 & 2024-06. [PR#876](https://github.com/microsoft/copilot-eclipse/pull/876)
- Make agent mode as default chat mode. [PR#959](https://github.com/microsoft/copilot-eclipse/pull/959)
- Improve the chat view layout. [PR#890](https://github.com/microsoft/copilot-eclipse/pull/890), [PR#879](https://github.com/microsoft/copilot-eclipse/pull/879)
- Improve the Copilot menu in menu bar and status bar. [PR#923](https://github.com/microsoft/copilot-eclipse/pull/923)
- Remove the spinner when completing code. [#788](https://github.com/microsoft/copilot-eclipse/issues/788)

### Fixed
- MCP tool configuration button should not be visible in ask mode. [#880](https://github.com/microsoft/copilot-eclipse/issues/880)
- Use workbench job to avoid blocking shutdown action. [PR#907](https://github.com/microsoft/copilot-eclipse/pull/907)
- Check if the project is accessible before scanning watched files. [PR#908](https://github.com/microsoft/copilot-eclipse/pull/908)
- Fix quota rendering issue on MacOS and Linux. [#822](https://github.com/microsoft/copilot-eclipse/issues/822), [#878](https://github.com/microsoft/copilot-eclipse/issues/878)
- Wrong completion when IDE auto closed brackets. [PR#892](https://github.com/microsoft/copilot-eclipse/pull/892)
- Entire settings are synced even just changing one item. [#877](https://github.com/microsoft/copilot-eclipse/issues/877)
- Wrong welcome page displayed in chat view when user is not signed in. [#851](https://github.com/microsoft/copilot-eclipse/issues/851)
- File with no extension cannot be attached in chat view. [#863](https://github.com/microsoft/copilot-eclipse/issues/863)
- Error 'SWT Resource was not properly disposed' after sign in. [#949](https://github.com/microsoft/copilot-eclipse/issues/949)

## 0.8.0
### Added
- Enable remote MCP server.
- Add up-sell link to the model picker for free plan accounts. [PR#840](https://github.com/microsoft/copilot-eclipse/pull/840)

### Changed
- Make the chat view appear as a side bar by default. [#336](https://github.com/microsoft/copilot-eclipse/issues/336)

### Fixed
- MCP tools are not visible. [#772](https://github.com/microsoft/copilot-eclipse/pull/772)
- Validate duplicate keys in MCP preference page. [#830](https://github.com/microsoft/copilot-eclipse/issues/830)
- Last line of the completion dialog in chat view is not visible. [#800](https://github.com/microsoft/copilot-eclipse/issues/800)
- Support error status for tool invocation result. [#842](https://github.com/microsoft/copilot-eclipse/issues/842)
- Fix rendering issue on Linux GTK. [#515](https://github.com/microsoft/copilot-eclipse/issues/515)
- Cannot use arrow up key in the completion dialog in chat view. [#838](https://github.com/microsoft/copilot-eclipse/issues/838)
- Decimal display incorrectly in usage quota. [#820](https://github.com/microsoft/copilot-eclipse/issues/820)
- Invalid thread access when reuse compare editor. [#844](https://github.com/microsoft/copilot-eclipse/issues/844)
- Reuse existing compare editor for create_file tool. [#841](https://github.com/microsoft/copilot-eclipse/issues/841)
- Add timeout when fetching env during activation on MacOS. [PR#839](https://github.com/microsoft/copilot-eclipse/pull/839)
- Check signin before get persisted path. [PR#837](https://github.com/microsoft/copilot-eclipse/pull/837)

## 0.7.0
### Added
- New billing support and user interface update.
- Input history navigation. [PR##785](https://github.com/microsoft/copilot-eclipse/pull/785)
- A button shortcut to open the MCP configuration page. [PR#766](https://github.com/microsoft/copilot-eclipse/pull/766)

### Changed
- Update CLS to 1.327.0. [PR#808](https://github.com/microsoft/copilot-eclipse/pull/808)
- Update Copilot status icon. [PR#792](https://github.com/microsoft/copilot-eclipse/pull/792)

### Fixed
- Fix the memory leak issue that the document is not disconnected. [PR#777](https://github.com/microsoft/copilot-eclipse/pull/777)
- Document for URI could not be found. [#274](https://github.com/microsoft/copilot-eclipse/issues/274)
- No tools is displayed in MCP configuration page. [#756](https://github.com/microsoft/copilot-eclipse/issues/756)
- NPE when resolve menu bar handler. [#655](https://github.com/microsoft/copilot-eclipse/issues/655)
- Compare editor title cannot be rendered correctly. [#763](https://github.com/microsoft/copilot-eclipse/issues/763)

## 0.6.1
### Fixed
- Correct the bundle version requirement to align with Eclipse 2024-09.

## 0.6.0
### Added
- Support agent mode with stdio mcp server integration in chat.

## 0.5.1
### Fixed
- Annotation model is null when triggering completion. [#468](https://github.com/microsoft/copilot-eclipse/issues/468)
- Input text box shakes when sending message by hitting Enter-Key. [#540](https://github.com/microsoft/copilot-eclipse/issues/540)
- SWTException when disposing completion manager. [#547](https://github.com/microsoft/copilot-eclipse/issues/547)
- Timeout error shows late when fail to login. [#482](https://github.com/microsoft/copilot-eclipse/issues/482)
- Improve auto scroll to bottom behavior. [#451](https://github.com/microsoft/copilot-eclipse/issues/451)
- Fixed schema name copilotCapabilities. [PR#556](https://github.com/microsoft/copilot-eclipse/pull/556)
- Wrong node runtime may be found. [#557](https://github.com/microsoft/copilot-eclipse/issues/557)

## 0.5.0
### Added
- Added GitHub Copilot menu to the top menu bar. [#242](https://github.com/microsoft/copilot-eclipse/issues/242)

### Changed
- Updated the LS to 1.290.0. [PR#529](https://github.com/microsoft/copilot-eclipse/pull/529)

### Fixed
- Stop append INFO log when format preference changes. [#298](https://github.com/microsoft/copilot-eclipse/issues/298)
- Should not attach bin files even it was opened in editor (behavior of VSCode). [#465](https://github.com/microsoft/copilot-eclipse/issues/465)

## 0.4.0
### Added
- Support ABAP. [#279](https://github.com/microsoft/copilot-eclipse/issues/279)

### Changed
- Mark org.eclipse.jdt.annotation to optional. [#411](https://github.com/microsoft/copilot-eclipse/issues/411)

### Fixed
- NPE when IFile.getLocation() is null. [#303](https://github.com/microsoft/copilot-eclipse/issues/303)
- Illegal state exception in Turn widget. [#496](https://github.com/microsoft/copilot-eclipse/issues/496)
- SWT resources not disposed properly. [#498](https://github.com/microsoft/copilot-eclipse/issues/498)
- Markdown viewer fallbacks to textviewer. [#401](https://github.com/microsoft/copilot-eclipse/issues/401)
- Chat input cannot be rendered as multi line when input text in too long. [#449](https://github.com/microsoft/copilot-eclipse/issues/449)
- Exception when deleting word leading with brackets in chat input box. [#480](https://github.com/microsoft/copilot-eclipse/issues/480)

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
- Support C/C++ format options. [#235](https://github.com/microsoft/copilot-eclipse/issues/235)

### Fixed
- Track uncaught exceptions. [PR#269](https://github.com/microsoft/copilot-eclipse/pull/269)
- Invalid thread access when generating completion. [#267](https://github.com/microsoft/copilot-eclipse/issues/267)
- NPE when authStatesManager is not ready. [#257](https://github.com/microsoft/copilot-eclipse/issues/257)
- Noise error log when signin is cancelled. [#263](https://github.com/microsoft/copilot-eclipse/issues/263)
- Hide the credential information in proxy log. [#233](https://github.com/microsoft/copilot-eclipse/issues/233)
- Remove hard-coded plugin version in GithubPanicErrorReport. [#229](https://github.com/microsoft/copilot-eclipse/issues/229)
- Move the update status icon logic to display thread. [PR#266](https://github.com/microsoft/copilot-eclipse/pull/266)

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
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
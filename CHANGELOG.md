# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
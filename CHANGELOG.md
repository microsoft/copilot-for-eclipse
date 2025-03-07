# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
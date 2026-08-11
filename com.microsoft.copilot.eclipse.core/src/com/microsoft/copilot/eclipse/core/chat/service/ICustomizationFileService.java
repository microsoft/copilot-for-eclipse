// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat.service;

import java.nio.file.Path;
import java.util.Set;

/**
 * Maintains an up-to-date view of the Copilot customization files (skills, prompts, instructions,
 * and agents) reported by the language server, keyed by their on-disk location.
 *
 */
public interface ICustomizationFileService {

  /** The customization kinds, each backed by a distinct language-server list request. */
  enum CustomizationType {
    SKILL, PROMPT, INSTRUCTION, AGENT
  }

  /**
   * Returns the absolute paths of single-file customization files (prompts, instructions, agents).
   * The returned set is an immutable snapshot.
   */
  Set<Path> getCustomizationFiles();

  /**
   * Returns the absolute paths of skill folders (the directory containing each {@code SKILL.md}).
   * A read of any file within one of these folders is a skill read. The returned set is an immutable
   * snapshot.
   */
  Set<Path> getSkillFolders();

  /**
   * Refreshes every customization type from the language server. Used for the initial load.
   * Fire-and-forget; the work completes asynchronously off the caller's thread.
   */
  void refreshAllAsync();
}

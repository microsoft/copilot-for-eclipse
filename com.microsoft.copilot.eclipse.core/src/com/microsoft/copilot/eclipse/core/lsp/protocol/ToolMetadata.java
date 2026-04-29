// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Tool-specific metadata provided by CLS as part of the confirmation request, used by the
 * auto-approve system to make confirmation decisions.
 */
public class ToolMetadata {

  private TerminalCommandData terminalCommandData;
  private SensitiveFileData sensitiveFileData;

  public TerminalCommandData getTerminalCommandData() {
    return terminalCommandData;
  }

  public void setTerminalCommandData(TerminalCommandData terminalCommandData) {
    this.terminalCommandData = terminalCommandData;
  }

  public SensitiveFileData getSensitiveFileData() {
    return sensitiveFileData;
  }

  public void setSensitiveFileData(SensitiveFileData sensitiveFileData) {
    this.sensitiveFileData = sensitiveFileData;
  }

  @Override
  public int hashCode() {
    return Objects.hash(terminalCommandData, sensitiveFileData);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ToolMetadata other = (ToolMetadata) obj;
    return Objects.equals(terminalCommandData, other.terminalCommandData)
        && Objects.equals(sensitiveFileData, other.sensitiveFileData);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("terminalCommandData", terminalCommandData);
    builder.append("sensitiveFileData", sensitiveFileData);
    return builder.toString();
  }

  /**
   * Data describing a terminal command and its sub-commands.
   */
  public static class TerminalCommandData {
    private String[] subCommands;
    private String[] commandNames;

    public String[] getSubCommands() {
      return subCommands;
    }

    public void setSubCommands(String[] subCommands) {
      this.subCommands = subCommands;
    }

    public String[] getCommandNames() {
      return commandNames;
    }

    public void setCommandNames(String[] commandNames) {
      this.commandNames = commandNames;
    }

    @Override
    public int hashCode() {
      return Objects.hash(Arrays.hashCode(subCommands), Arrays.hashCode(commandNames));
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      TerminalCommandData other = (TerminalCommandData) obj;
      return Arrays.equals(subCommands, other.subCommands)
          && Arrays.equals(commandNames, other.commandNames);
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("subCommands", subCommands);
      builder.append("commandNames", commandNames);
      return builder.toString();
    }
  }

  /**
   * Data describing a sensitive file that requires confirmation.
   */
  public static class SensitiveFileData {
    private String filePath;
    private String matchingRule;
    private String ruleDescription;
    private boolean isGlobal;

    public String getFilePath() {
      return filePath;
    }

    public void setFilePath(String filePath) {
      this.filePath = filePath;
    }

    public String getMatchingRule() {
      return matchingRule;
    }

    public void setMatchingRule(String matchingRule) {
      this.matchingRule = matchingRule;
    }

    public String getRuleDescription() {
      return ruleDescription;
    }

    public void setRuleDescription(String ruleDescription) {
      this.ruleDescription = ruleDescription;
    }

    public boolean isGlobal() {
      return isGlobal;
    }

    public void setGlobal(boolean isGlobal) {
      this.isGlobal = isGlobal;
    }

    @Override
    public int hashCode() {
      return Objects.hash(filePath, matchingRule, ruleDescription, isGlobal);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      SensitiveFileData other = (SensitiveFileData) obj;
      return Objects.equals(filePath, other.filePath)
          && Objects.equals(matchingRule, other.matchingRule)
          && Objects.equals(ruleDescription, other.ruleDescription)
          && isGlobal == other.isGlobal;
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("filePath", filePath);
      builder.append("matchingRule", matchingRule);
      builder.append("ruleDescription", ruleDescription);
      builder.append("isGlobal", isGlobal);
      return builder.toString();
    }
  }
}

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Params for textDocument/didShowInlineEdit notification.
 */
public class DidShowInlineEditParams {
  @NonNull
  private Item item;

  /**
   * Creates a new DidShowInlineEditParams.
   *
   * @param item the item containing the command wrapper
   */
  public DidShowInlineEditParams(Item item) {
    this.item = item;
  }

  /**
   * Creates a DidShowInlineEditParams from a suggestion UUID.
   *
   * @param uuid the suggestion UUID
   * @return the params object
   */
  public static DidShowInlineEditParams fromUuid(String uuid) {
    CommandWrapper cmd = new CommandWrapper(Collections.singletonList(uuid));
    Item item = new Item(cmd);
    return new DidShowInlineEditParams(item);
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).add("item", item).toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(item);
  }

  @Override
  public boolean equals(Object o) {
    return this == o || (o instanceof DidShowInlineEditParams r && Objects.equals(item, r.item));
  }

  /**
   * Item containing the command wrapper.
   */
  public static class Item {
    @NonNull
    private CommandWrapper command;

    /**
     * Creates a new Item.
     *
     * @param command the command wrapper
     */
    public Item(CommandWrapper command) {
      this.command = command;
    }

    public CommandWrapper getCommand() {
      return command;
    }

    public void setCommand(CommandWrapper command) {
      this.command = command;
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this).add("command", command).toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(command);
    }

    @Override
    public boolean equals(Object o) {
      return this == o || (o instanceof Item r && Objects.equals(command, r.command));
    }
  }

  /**
   * Wrapper for command arguments.
   */
  public static class CommandWrapper {
    @NonNull
    private List<String> arguments;

    /**
     * Creates a new CommandWrapper.
     *
     * @param arguments the list of command arguments
     */
    public CommandWrapper(List<String> arguments) {
      this.arguments = arguments;
    }

    public List<String> getArguments() {
      return arguments;
    }

    public void setArguments(List<String> arguments) {
      this.arguments = arguments;
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this).add("arguments", arguments).toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(arguments);
    }

    @Override
    public boolean equals(Object o) {
      return this == o || (o instanceof CommandWrapper r && Objects.equals(arguments, r.arguments));
    }
  }
}

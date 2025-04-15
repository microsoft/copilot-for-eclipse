package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchema;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchemaPropertyValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Tool for editing files.
 */
public class EditFileTool extends BaseTool {
  private static final String TOOL_NAME = "insert_edit_into_file";

  /**
   * Constructor for EditFileTool.
   */
  public EditFileTool() {
    this.name = TOOL_NAME;
  }

  @Override
  public LanguageModelToolInformation getToolInformation() {
    // Create a new instance of LanguageModelToolInformation
    LanguageModelToolInformation toolInfo = new LanguageModelToolInformation();

    // Set the name and description of the tool
    toolInfo.setName(TOOL_NAME);
    toolInfo.setDescription("""
        Insert new code into an existing file in the workspace.
        Use this tool once per file that needs to be modified, even if there are multiple changes for a file.
        Generate the \"explanation\" property first.
        The system is very smart and can understand how to apply your edits to the files,
        you just need to provide minimal hints.
        Avoid repeating existing code, instead use comments to represent regions of unchanged code.
        Be as concise as possible.
        For example:
        // ...existing code...
        { changed code }
        // ...existing code...
        { changed code }
        // ...existing code...
        Here is an example of how you should use format an edit to an existing Person class:
        class Person {
        \t// ...existing code...
        \tage: number;
        \t// ...existing code...
        \tgetAge() {
        \treturn this.age;
        \t}
        }
        """);

    // Define the input schema for the tool
    InputSchema inputSchema = new InputSchema();
    inputSchema.setType("object");

    // Define the properties of the input schema
    Map<String, InputSchemaPropertyValue> properties = new HashMap<>();
    properties.put("explanation",
        new InputSchemaPropertyValue("string", "A short explanation of the edit being made."));
    properties.put("filePath", new InputSchemaPropertyValue("string", "An absolute path to the file to edit."));
    properties.put("code", new InputSchemaPropertyValue("string", """
        The code change to apply to the file.
        The system is very smart and can understand how to apply your edits to the files,
        you just need to provide minimal hints.
        Avoid repeating existing code, instead use comments to represent regions of unchanged code.
        Be as concise as possible.
        For example:
        // ...existing code...
        { changed code }
        // ...existing code...
        { changed code }
        // ...existing code...
        Here is an example of how you should use format an edit to an existing Person class:
        class Person {
        \t// ...existing code...
        \tage: number;
        \t// ...existing code...
        \tgetAge() {
        \t\treturn this.age;
        \t}
        }
        """));

    // Set the properties and required fields for the input schema
    inputSchema.setProperties(properties);
    inputSchema.setRequired(Arrays.asList("explanation", "filePath", "code"));

    // Attach the input schema to the tool information
    toolInfo.setInputSchema(inputSchema);

    return toolInfo;
  }

  @Override
  public CompletableFuture<LanguageModelToolResult[]> invoke(Map<String, Object> input, ChatView chatView) {
    // Implementation for invoking the edit file tool
    CompletableFuture<LanguageModelToolResult[]> resultFuture = new CompletableFuture<>();
    if (input.get("filePath") instanceof String filePath) {
      IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(filePath));

      if (file == null || !file.exists()) {
        resultFuture.complete(new LanguageModelToolResult[] { new LanguageModelToolResult(
            "The file path provided does not exist. Please check the path and try again.") });
        return resultFuture;
      }

      if (input.get("code") instanceof String code) {
        try {
          compareStringWithFile(code, file);
          resultFuture.complete(
              new LanguageModelToolResult[] { new LanguageModelToolResult("File edit finished successfully.") });
        } catch (InvocationTargetException | InterruptedException e) {
          resultFuture.complete(new LanguageModelToolResult[] {
              new LanguageModelToolResult("Error occurred while comparing string with file: " + e.getMessage()) });
          CopilotCore.LOGGER.error("Error occurred while comparing copilot code with file", e);
        }
      } else {
        resultFuture.complete(new LanguageModelToolResult[] { new LanguageModelToolResult(
            "The code provided is not a valid string. Please check the code and try again.") });
      }
    } else {
      resultFuture.complete(new LanguageModelToolResult[] { new LanguageModelToolResult(
          "The file path provided is not a valid string. Please check the path and try again.") });
    }
    return resultFuture;
  }

  /**
   * Compares the given string with the content of the given file in a compare editor.
   *
   * @param stringContent The string content from the GitHub Copilot agent to compare.
   * @param file The user's file to compare with.
   * @throws InvocationTargetException If the operation is canceled.
   * @throws InterruptedException If the operation is canceled.
   */
  private void compareStringWithFile(String stringContent, IFile file)
      throws InvocationTargetException, InterruptedException {
    CompareConfiguration config = new CompareConfiguration();
    config.setLeftLabel(Messages.agent_tool_compareEditor_proposedChangesTitle.replaceAll("\"", ""));
    config.setRightLabel(file.getName());

    // Enable editing on both sides
    config.setLeftEditable(true);
    config.setRightEditable(true);

    // Set up the configuration to properly show differences
    config.setProperty(CompareConfiguration.USE_OUTLINE_VIEW, Boolean.TRUE);
    config.setProperty(CompareConfiguration.SHOW_PSEUDO_CONFLICTS, Boolean.TRUE);
    config.setProperty(CompareConfiguration.IGNORE_WHITESPACE, Boolean.FALSE);

    // Create a mutable string holder to capture edits
    StringHolder contentHolder = new StringHolder(stringContent);

    CompareEditorInput input = new CompareEditorInput(config) {
      @Override
      protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        monitor.beginTask("Calculating differences", 10);

        setTitle(Messages.agent_tool_compareEditor_TitlePrefix + file.getName());
        // By default, the CompareEditorInput can only save the content when user modifies the content. However, in the
        // agent mode, we should allow the user to save the Copilot proposed changes even if the user does not modify
        // the content. Set the CompareEditorInput to dirty by default.
        setDirty(true);

        // Keep proposedChanges virtual file's name and type same as the originalFile original file's name and type
        EditableStringCompareInput proposedChanges = new EditableStringCompareInput(contentHolder, file.getName(),
            file.getFileExtension());
        EditableFileCompareInput originalFile = new EditableFileCompareInput(file);

        // Create a diff node with proper configuration for text comparison
        DiffNode diffNode = new DiffNode(null, Differencer.CHANGE, null, proposedChanges, originalFile);

        monitor.done();
        return diffNode;
      }

      @Override
      public void saveChanges(IProgressMonitor monitor) throws CoreException {
        super.saveChanges(monitor);

        // After the normal save, write the StringHolder content back to the file
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(
            contentHolder.getContent().getBytes(StandardCharsets.UTF_8))) {
          file.setContents(inputStream, true, true, monitor);
        } catch (IOException e) {
          CopilotCore.LOGGER.error("Error saving agent changes to file", e);
        }

        setDirty(false);
      }
    };

    // TODO: Add a progress monitor to show the progress of the operation
    input.run(new NullProgressMonitor());
    SwtUtils.invokeOnDisplayThread(() -> {
      CompareUI.openCompareEditor(input);
    });
  }

  // Helper class to hold a mutable string
  private class StringHolder {
    private String content;

    public StringHolder(String content) {
      this.content = content;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }
  }

  // File input with edit support
  private class EditableFileCompareInput implements ITypedElement, IStreamContentAccessor, IEditableContent {
    private IFile file;
    private byte[] modifiedContent = null;

    public EditableFileCompareInput(IFile file) {
      this.file = file;
    }

    @Override
    public String getName() {
      return file.getName();
    }

    @Override
    public Image getImage() {
      return null;
    }

    @Override
    public String getType() {
      return file.getFileExtension();
    }

    @Override
    public InputStream getContents() throws CoreException {
      if (modifiedContent != null) {
        return new ByteArrayInputStream(modifiedContent);
      }
      return file.getContents();
    }

    @Override
    public boolean isEditable() {
      return true;
    }

    @Override
    public void setContent(byte[] newContent) {
      this.modifiedContent = newContent;
    }

    @Override
    public ITypedElement replace(ITypedElement dest, ITypedElement src) {
      if (src instanceof IStreamContentAccessor sca) {
        try (InputStream is = sca.getContents()) {
          // Just store changes in memory
          modifiedContent = is.readAllBytes();
        } catch (IOException | CoreException e) {
          CopilotCore.LOGGER.error("Error occurred while replacing file content", e);
        }
      }
      return this;
    }
  }

  // String input with edit support
  private class EditableStringCompareInput implements ITypedElement, IStreamContentAccessor, IEditableContent {
    private StringHolder contentHolder;
    private String name;
    private String type;

    public EditableStringCompareInput(StringHolder contentHolder, String name, String type) {
      this.contentHolder = contentHolder;
      this.name = name;
      this.type = type;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Image getImage() {
      return null;
    }

    @Override
    public String getType() {
      return type;
    }

    @Override
    public InputStream getContents() throws CoreException {
      return new ByteArrayInputStream(contentHolder.getContent().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean isEditable() {
      return true;
    }

    @Override
    public void setContent(byte[] newContent) {
      contentHolder.setContent(new String(newContent, StandardCharsets.UTF_8));
    }

    @Override
    public ITypedElement replace(ITypedElement dest, ITypedElement src) {
      if (src instanceof IStreamContentAccessor sca) {
        try (InputStream is = sca.getContents()) {
          byte[] content = is.readAllBytes();
          contentHolder.setContent(new String(content, StandardCharsets.UTF_8));
        } catch (IOException | CoreException e) {
          CopilotCore.LOGGER.error("Error occurred while replacing string content", e);
        }
      }
      return this;
    }
  }

}

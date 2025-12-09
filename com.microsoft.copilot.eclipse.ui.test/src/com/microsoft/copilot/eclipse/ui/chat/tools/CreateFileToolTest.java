package com.microsoft.copilot.eclipse.ui.chat.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult.ToolInvocationStatus;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

@ExtendWith(MockitoExtension.class)
class CreateFileToolTest {

  private static final String TEST_PROJECT_NAME = "TestProject";

  private CreateFileTool createFileTool;
  private Shell shell;
  private IProject testProject;

  @Mock
  private CopilotUi mockCopilotUi;
  @Mock
  private ChatServiceManager mockChatServiceManager;
  @Mock
  private FileToolService mockFileToolService;

  private MockedStatic<CopilotUi> mockedCopilotUi;

  @BeforeEach
  void setUp() throws Exception {
    // Setup SWT components
    createFileTool = new CreateFileTool();
    SwtUtils.invokeOnDisplayThread(() -> {
      shell = new Shell(Display.getDefault());
    });
  }

  @AfterEach
  void tearDown() throws Exception {
    // Close the static mock
    if (mockedCopilotUi != null) {
      mockedCopilotUi.close();
    }
    
    SwtUtils.invokeOnDisplayThread(() -> {
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
    
    // Clean up test project
    cleanupTestProject();
  }

  private IProject setupTestProject() throws Exception {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();
    testProject = root.getProject(TEST_PROJECT_NAME);
    
    if (!testProject.exists()) {
      testProject.create(null);
    }
    testProject.open(null);
    
    return testProject;
  }

  private void setupMocks() {
    mockedCopilotUi = mockStatic(CopilotUi.class);
    mockedCopilotUi.when(CopilotUi::getPlugin).thenReturn(mockCopilotUi);
    when(mockCopilotUi.getChatServiceManager()).thenReturn(mockChatServiceManager);
    when(mockChatServiceManager.getFileToolService()).thenReturn(mockFileToolService);
  }

  private void cleanupTestProject() throws Exception {
    if (testProject != null && testProject.exists()) {
      testProject.delete(true, true, null);
      testProject = null;
    }
  }

  private void assertErrorResult(LanguageModelToolResult[] results, String expectedMessageSubstring) {
    assertNotNull(results);
    assertEquals(1, results.length);
    assertEquals(ToolInvocationStatus.error, results[0].getStatus());
    assertTrue(results[0].getContent().get(0).getValue().contains(expectedMessageSubstring));
  }

  private void assertSuccessResult(LanguageModelToolResult[] results, String expectedMessageSubstring) {
    assertNotNull(results);
    assertEquals(1, results.length);
    assertEquals(ToolInvocationStatus.success, results[0].getStatus());
    assertTrue(results[0].getContent().get(0).getValue().contains(expectedMessageSubstring));
  }

  @Test
  void testGetToolInformation() {
    // Act
    LanguageModelToolInformation toolInfo = createFileTool.getToolInformation();

    // Assert
    assertNotNull(toolInfo);
    assertEquals(CreateFileTool.TOOL_NAME, toolInfo.getName());
    assertNotNull(toolInfo.getDescription());
    assertNotNull(toolInfo.getInputSchema());
    assertTrue(toolInfo.getInputSchema().getRequired().contains("filePath"));
    assertTrue(toolInfo.getInputSchema().getRequired().contains("content"));
  }

  @Test
  void testInvokeWithEmptyFilePathReturnsErrorStatus() throws InterruptedException, ExecutionException {
    // Arrange
    Map<String, Object> input = new HashMap<>();
    input.put("filePath", "");
    input.put("content", "test content");

    // Act
    CompletableFuture<LanguageModelToolResult[]> future = createFileTool.invoke(input, null);
    LanguageModelToolResult[] results = future.get();

    // Assert
    assertErrorResult(results, "Invalid file path: path cannot be empty");
  }

  @Test
  void testInvokeWithNullFilePathReturnsErrorStatus() throws InterruptedException, ExecutionException {
    // Arrange
    Map<String, Object> input = new HashMap<>();
    input.put("filePath", null);
    input.put("content", "test content");

    // Act
    CompletableFuture<LanguageModelToolResult[]> future = createFileTool.invoke(input, null);
    LanguageModelToolResult[] results = future.get();

    // Assert
    assertErrorResult(results, "Invalid file path");
  }

  @Test
  void testInvokeWithFileAlreadyExistsReturnsErrorStatus() throws Exception {
    // Arrange
    IProject project = setupTestProject();
    IFile existingFile = project.getFile("existingFile.txt");
    existingFile.create(new java.io.ByteArrayInputStream("existing content".getBytes()), true, null);
    
    Map<String, Object> input = new HashMap<>();
    input.put("filePath", existingFile.getLocation().toOSString());
    input.put("content", "new content");

    // Act
    CompletableFuture<LanguageModelToolResult[]> future = createFileTool.invoke(input, null);
    LanguageModelToolResult[] results = future.get();

    // Assert
    assertErrorResult(results, "file already exists");
  }

  @Test
  void testInvokeWithValidInputReturnsSuccessStatus() throws Exception {
    // Arrange
    IProject project = setupTestProject();
    setupMocks();
    
    IFile newFile = project.getFile("newFile.txt");
    
    Map<String, Object> input = new HashMap<>();
    input.put("filePath", newFile.getLocation().toOSString());
    input.put("content", "test content");

    // Act
    CompletableFuture<LanguageModelToolResult[]> future = createFileTool.invoke(input, null);
    LanguageModelToolResult[] results = future.get();

    // Assert
    assertSuccessResult(results, "File created at");
    assertTrue(newFile.exists());
  }

  @Test
  void testInvokeWithEmptyContentReturnsSuccessStatus() throws Exception {
    // Arrange
    IProject project = setupTestProject();
    setupMocks();
    
    IFile newFile = project.getFile("emptyFile.txt");
    
    Map<String, Object> input = new HashMap<>();
    input.put("filePath", newFile.getLocation().toOSString());
    input.put("content", "");

    // Act
    CompletableFuture<LanguageModelToolResult[]> future = createFileTool.invoke(input, null);
    LanguageModelToolResult[] results = future.get();

    // Assert - empty content is allowed, defaults to empty string
    assertNotNull(results);
    assertEquals(1, results.length);
    assertEquals(ToolInvocationStatus.success, results[0].getStatus());
    assertTrue(newFile.exists());
  }

  @Test
  void testInvokeWithNullContentReturnsSuccessStatus() throws Exception {
    // Arrange
    IProject project = setupTestProject();
    setupMocks();
    
    IFile newFile = project.getFile("nullContentFile.txt");
    
    Map<String, Object> input = new HashMap<>();
    input.put("filePath", newFile.getLocation().toOSString());
    input.put("content", null);

    // Act
    CompletableFuture<LanguageModelToolResult[]> future = createFileTool.invoke(input, null);
    LanguageModelToolResult[] results = future.get();

    // Assert - null content is allowed, defaults to empty string
    assertNotNull(results);
    assertEquals(1, results.length);
    assertEquals(ToolInvocationStatus.success, results[0].getStatus());
    assertTrue(newFile.exists());
  }

  @Test
  void testInvokeWithInvalidPathReturnsErrorStatus() throws InterruptedException, ExecutionException {
    // Arrange
    Map<String, Object> input = new HashMap<>();
    input.put("filePath", "/invalid/path/that/does/not/exist.txt");
    input.put("content", "test content");

    // Act
    CompletableFuture<LanguageModelToolResult[]> future = createFileTool.invoke(input, null);
    LanguageModelToolResult[] results = future.get();

    // Assert
    assertErrorResult(results, "does not exist in the workspace");
  }

  @Test
  void testNeedConfirmation() {
    // Act & Assert
    assertEquals(false, createFileTool.needConfirmation());
  }

  @Test
  void testToolName() {
    // Act & Assert
    assertEquals(CreateFileTool.TOOL_NAME, createFileTool.getToolName());
  }

  /**
   * Test coverage for ToolInvocationStatus:
   * - SUCCESS: Valid file creation with content, empty content, and null content
   * - ERROR: Empty/null file path, invalid path, file already exists
   * 
   * Note: CoreException and IOException scenarios are difficult to test in unit tests
   * without complex mocking and would be better covered by integration tests.
   */
}

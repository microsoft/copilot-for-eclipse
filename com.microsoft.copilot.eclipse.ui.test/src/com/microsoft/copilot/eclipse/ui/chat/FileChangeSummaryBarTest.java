package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.tools.FileToolService;
import com.microsoft.copilot.eclipse.ui.chat.tools.FileToolService.FileChangeProperty;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Tests for FileChangeSummaryBar, focusing on scroll behavior and file list rendering.
 */
@ExtendWith(MockitoExtension.class)
class FileChangeSummaryBarTest {

  @Mock
  private CopilotUi mockCopilotUi;
  @Mock
  private ChatServiceManager mockChatServiceManager;
  @Mock
  private FileToolService mockFileToolService;

  private Shell shell;
  private Composite parent;
  private FileChangeSummaryBar fileChangeSummaryBar;
  private MockedStatic<CopilotUi> mockedCopilotUi;

  @BeforeEach
  void setUp() {
    SwtUtils.invokeOnDisplayThread(() -> {
      setupSwtComponents();
      setupMocks();
    });
  }

  @AfterEach
  void tearDown() {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (fileChangeSummaryBar != null && !fileChangeSummaryBar.isDisposed()) {
        fileChangeSummaryBar.dispose();
      }
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
    if (mockedCopilotUi != null) {
      mockedCopilotUi.close();
    }
  }

  private void setupSwtComponents() {
    shell = new Shell(Display.getDefault());
    shell.setSize(800, 600);
    parent = new Composite(shell, SWT.NONE);
  }

  private void setupMocks() {
    mockedCopilotUi = mockStatic(CopilotUi.class);
    mockedCopilotUi.when(CopilotUi::getPlugin).thenReturn(mockCopilotUi);
    when(mockCopilotUi.getChatServiceManager()).thenReturn(mockChatServiceManager);
    when(mockChatServiceManager.getFileToolService()).thenReturn(mockFileToolService);
  }

  /**
   * Test that no ScrolledComposite is created when file count is less than or equal to MAX_VISIBLE_FILES (5).
   */
  @Test
  void testNoScrollForFewFiles() {
    SwtUtils.invokeOnDisplayThread(() -> {
      fileChangeSummaryBar = new FileChangeSummaryBar(parent, SWT.NONE);
      Map<IFile, FileChangeProperty> filesMap = createMockFilesMap(3, false);

      fileChangeSummaryBar.buildSummaryBarFor(filesMap);

      Object changedFiles = getFieldValue(fileChangeSummaryBar, "changedFiles");
      assertNotNull(changedFiles, "ChangedFiles should be created");

      ScrolledComposite scrolledComposite = getScrolledComposite(changedFiles);
      assertNull(scrolledComposite, "ScrolledComposite should not be created for 3 files");
    });
  }

  /**
   * Test that no ScrolledComposite is created when file count equals MAX_VISIBLE_FILES (5).
   */
  @Test
  void testNoScrollForExactlyMaxFiles() {
    SwtUtils.invokeOnDisplayThread(() -> {
      fileChangeSummaryBar = new FileChangeSummaryBar(parent, SWT.NONE);
      Map<IFile, FileChangeProperty> filesMap = createMockFilesMap(5, false);

      fileChangeSummaryBar.buildSummaryBarFor(filesMap);

      Object changedFiles = getFieldValue(fileChangeSummaryBar, "changedFiles");
      assertNotNull(changedFiles, "ChangedFiles should be created");

      ScrolledComposite scrolledComposite = getScrolledComposite(changedFiles);
      assertNull(scrolledComposite, "ScrolledComposite should not be created for exactly 5 files");
    });
  }

  /**
   * Test that ScrolledComposite is created when file count exceeds MAX_VISIBLE_FILES (5).
   */
  @Test
  void testScrollCreatedForManyFiles() {
    SwtUtils.invokeOnDisplayThread(() -> {
      fileChangeSummaryBar = new FileChangeSummaryBar(parent, SWT.NONE);
      Map<IFile, FileChangeProperty> filesMap = createMockFilesMap(10, false);

      fileChangeSummaryBar.buildSummaryBarFor(filesMap);

      Object changedFiles = getFieldValue(fileChangeSummaryBar, "changedFiles");
      assertNotNull(changedFiles, "ChangedFiles should be created");

      ScrolledComposite scrolledComposite = getScrolledComposite(changedFiles);
      assertNotNull(scrolledComposite, "ScrolledComposite should be created for 10 files");

      // Verify scroll properties
      assertTrue(scrolledComposite.getExpandHorizontal(), "ScrolledComposite should expand horizontally");
      assertTrue(scrolledComposite.getExpandVertical(), "ScrolledComposite should expand vertically");
    });
  }

  /**
   * Test that ScrolledComposite has proper height hint set based on MAX_VISIBLE_FILES.
   */
  @Test
  void testScrollHeightHintForManyFiles() {
    SwtUtils.invokeOnDisplayThread(() -> {
      fileChangeSummaryBar = new FileChangeSummaryBar(parent, SWT.NONE);
      Map<IFile, FileChangeProperty> filesMap = createMockFilesMap(8, false);

      fileChangeSummaryBar.buildSummaryBarFor(filesMap);

      Object changedFiles = getFieldValue(fileChangeSummaryBar, "changedFiles");
      assertNotNull(changedFiles, "ChangedFiles should be created");

      ScrolledComposite scrolledComposite = getScrolledComposite(changedFiles);
      assertNotNull(scrolledComposite, "ScrolledComposite should be created for 8 files");

      // Verify layout data has height hint
      Object layoutData = scrolledComposite.getLayoutData();
      assertTrue(layoutData instanceof GridData, "ScrolledComposite should have GridData layout");
      GridData gridData = (GridData) layoutData;
      assertTrue(gridData.heightHint > 0, "Height hint should be set for scrolled composite");
    });
  }

  /**
   * Test that all file rows are rendered correctly regardless of scroll presence.
   */
  @Test
  void testAllFileRowsRenderedWithScroll() {
    SwtUtils.invokeOnDisplayThread(() -> {
      fileChangeSummaryBar = new FileChangeSummaryBar(parent, SWT.NONE);
      int fileCount = 7;
      Map<IFile, FileChangeProperty> filesMap = createMockFilesMap(fileCount, false);

      fileChangeSummaryBar.buildSummaryBarFor(filesMap);

      Object changedFiles = getFieldValue(fileChangeSummaryBar, "changedFiles");
      assertNotNull(changedFiles, "ChangedFiles should be created");

      // Get the file rows list
      Object fileRowsList = getFieldValue(changedFiles, "fileRows");
      assertNotNull(fileRowsList, "File rows list should exist");

      assertTrue(fileRowsList instanceof List, "fileRows should be a List");

      List<?> fileRows = (List<?>) fileRowsList;
      assertEquals(fileCount, fileRows.size(), "All file rows should be created");
    });
  }

  /**
   * Test that content area is properly set as scrolled composite's content.
   */
  @Test
  void testContentAreaSetInScrolledComposite() {
    SwtUtils.invokeOnDisplayThread(() -> {
      fileChangeSummaryBar = new FileChangeSummaryBar(parent, SWT.NONE);
      Map<IFile, FileChangeProperty> filesMap = createMockFilesMap(8, false);

      fileChangeSummaryBar.buildSummaryBarFor(filesMap);

      Object changedFiles = getFieldValue(fileChangeSummaryBar, "changedFiles");
      assertNotNull(changedFiles, "ChangedFiles should be created");

      ScrolledComposite scrolledComposite = getScrolledComposite(changedFiles);
      assertNotNull(scrolledComposite, "ScrolledComposite should be created");

      Control content = scrolledComposite.getContent();
      assertNotNull(content, "ScrolledComposite should have content set");
      assertTrue(content instanceof Composite, "Content should be a Composite");

      // Verify it's the contentArea
      Object contentArea = getFieldValue(changedFiles, "contentArea");
      assertEquals(contentArea, content, "ScrolledComposite content should be the contentArea");
    });
  }

  /**
   * Test that minHeight is set correctly for ScrolledComposite.
   */
  @Test
  void testMinHeightSetForScrolledComposite() {
    SwtUtils.invokeOnDisplayThread(() -> {
      fileChangeSummaryBar = new FileChangeSummaryBar(parent, SWT.NONE);
      Map<IFile, FileChangeProperty> filesMap = createMockFilesMap(10, false);

      fileChangeSummaryBar.buildSummaryBarFor(filesMap);

      Object changedFiles = getFieldValue(fileChangeSummaryBar, "changedFiles");
      assertNotNull(changedFiles, "ChangedFiles should be created");

      ScrolledComposite scrolledComposite = getScrolledComposite(changedFiles);
      assertNotNull(scrolledComposite, "ScrolledComposite should be created");

      int minHeight = scrolledComposite.getMinHeight();
      assertTrue(minHeight > 0, "MinHeight should be set and greater than 0");
    });
  }

  /**
   * Test rebuilding summary bar with different file counts.
   */
  @Test
  void testRebuildSummaryBarChangesScrollBehavior() {
    SwtUtils.invokeOnDisplayThread(() -> {
      fileChangeSummaryBar = new FileChangeSummaryBar(parent, SWT.NONE);

      // First build with few files (no scroll)
      Map<IFile, FileChangeProperty> fewFiles = createMockFilesMap(3, false);
      fileChangeSummaryBar.buildSummaryBarFor(fewFiles);

      Object changedFiles1 = getFieldValue(fileChangeSummaryBar, "changedFiles");
      assertNotNull(changedFiles1, "ChangedFiles should be created");
      ScrolledComposite scroll1 = getScrolledComposite(changedFiles1);
      assertNull(scroll1, "No scroll should exist for 3 files");

      // Rebuild with many files (should have scroll)
      Map<IFile, FileChangeProperty> manyFiles = createMockFilesMap(10, false);
      fileChangeSummaryBar.buildSummaryBarFor(manyFiles);

      Object changedFiles2 = getFieldValue(fileChangeSummaryBar, "changedFiles");
      assertNotNull(changedFiles2, "ChangedFiles should be recreated");
      assertFalse(changedFiles1.equals(changedFiles2), "ChangedFiles should be a new instance");

      ScrolledComposite scroll2 = getScrolledComposite(changedFiles2);
      assertNotNull(scroll2, "Scroll should exist for 10 files");
    });
  }

  /**
   * Test that expand icon shows down arrow when expanded.
   */
  @Test
  void testExpandIconImageWhenExpanded() {
    SwtUtils.invokeOnDisplayThread(() -> {
      fileChangeSummaryBar = new FileChangeSummaryBar(parent, SWT.NONE);
      Map<IFile, FileChangeProperty> filesMap = createMockFilesMap(3, false);

      fileChangeSummaryBar.buildSummaryBarFor(filesMap);

      Object titleBar = getFieldValue(fileChangeSummaryBar, "titleBar");
      assertNotNull(titleBar, "TitleBar should be created");

      Object expandIcon = getFieldValue(titleBar, "expandIcon");
      assertNotNull(expandIcon, "ExpandIcon should exist");
      assertTrue(expandIcon instanceof Label, "ExpandIcon should be a Label");

      Label expandIconLabel = (Label) expandIcon;
      assertNotNull(expandIconLabel.getImage(), "ExpandIcon should have an image when expanded");

      // Verify it's the down arrow by checking that downArrowImage is not null
      Object downArrowImage = getFieldValue(titleBar, "downArrowImage");
      assertNotNull(downArrowImage, "Down arrow image should be created");
      assertEquals(downArrowImage, expandIconLabel.getImage(), "ExpandIcon should show down arrow when expanded");
    });
  }

  /**
   * Test that expand icon shows right arrow when collapsed.
   */
  @Test
  void testExpandIconImageWhenCollapsed() {
    SwtUtils.invokeOnDisplayThread(() -> {
      fileChangeSummaryBar = new FileChangeSummaryBar(parent, SWT.NONE);
      Map<IFile, FileChangeProperty> filesMap = createMockFilesMap(3, false);

      fileChangeSummaryBar.buildSummaryBarFor(filesMap);

      // Toggle to collapse
      boolean isExpanded = (Boolean) getFieldValue(fileChangeSummaryBar, "isExpanded");
      assertTrue(isExpanded, "Should be expanded initially");

      // Simulate collapse by calling toggleExpanded via reflection
      invokePrivateMethod(fileChangeSummaryBar, "toggleExpanded");

      isExpanded = (Boolean) getFieldValue(fileChangeSummaryBar, "isExpanded");
      assertFalse(isExpanded, "Should be collapsed after toggle");

      Object titleBar = getFieldValue(fileChangeSummaryBar, "titleBar");
      Object expandIcon = getFieldValue(titleBar, "expandIcon");
      Label expandIconLabel = (Label) expandIcon;

      // Verify it's the right arrow
      Object rightArrowImage = getFieldValue(titleBar, "rightArrowImage");
      assertNotNull(rightArrowImage, "Right arrow image should be created");
      assertEquals(rightArrowImage, expandIconLabel.getImage(), "ExpandIcon should show right arrow when collapsed");
    });
  }

  /**
   * Test tooltip text when expanded.
   */
  @Test
  void testTooltipTextWhenExpanded() {
    SwtUtils.invokeOnDisplayThread(() -> {
      fileChangeSummaryBar = new FileChangeSummaryBar(parent, SWT.NONE);
      Map<IFile, FileChangeProperty> filesMap = createMockFilesMap(3, false);

      fileChangeSummaryBar.buildSummaryBarFor(filesMap);

      Object titleBar = getFieldValue(fileChangeSummaryBar, "titleBar");
      assertNotNull(titleBar, "TitleBar should be created");

      Object expandIcon = getFieldValue(titleBar, "expandIcon");
      Object titleLabel = getFieldValue(titleBar, "titleLabel");

      assertTrue(expandIcon instanceof Label, "ExpandIcon should be a Label");
      assertTrue(titleLabel instanceof Label, "TitleLabel should be a Label");

      Label expandIconLabel = (Label) expandIcon;
      Label titleLabelWidget = (Label) titleLabel;

      String expandIconTooltip = expandIconLabel.getToolTipText();
      String titleLabelTooltip = titleLabelWidget.getToolTipText();

      assertNotNull(expandIconTooltip, "ExpandIcon should have tooltip when expanded");
      assertNotNull(titleLabelTooltip, "TitleLabel should have tooltip when expanded");

      // Both should have the same tooltip
      assertEquals(expandIconTooltip, titleLabelTooltip, "ExpandIcon and TitleLabel should have the same tooltip");

      // Tooltip should contain "collapse" when expanded
      assertTrue(expandIconTooltip.toLowerCase().contains("collapse"),
          "Tooltip should contain 'collapse' when expanded");

      // Tooltip should contain the file count
      assertTrue(expandIconTooltip.contains("3"), "Tooltip should contain file count");
    });
  }

  /**
   * Test tooltip text when collapsed.
   */
  @Test
  void testTooltipTextWhenCollapsed() {
    SwtUtils.invokeOnDisplayThread(() -> {
      fileChangeSummaryBar = new FileChangeSummaryBar(parent, SWT.NONE);
      Map<IFile, FileChangeProperty> filesMap = createMockFilesMap(5, false);

      fileChangeSummaryBar.buildSummaryBarFor(filesMap);

      // Toggle to collapse
      invokePrivateMethod(fileChangeSummaryBar, "toggleExpanded");

      boolean isExpanded = (Boolean) getFieldValue(fileChangeSummaryBar, "isExpanded");
      assertFalse(isExpanded, "Should be collapsed after toggle");

      Object titleBar = getFieldValue(fileChangeSummaryBar, "titleBar");
      Object expandIcon = getFieldValue(titleBar, "expandIcon");
      Object titleLabel = getFieldValue(titleBar, "titleLabel");

      Label expandIconLabel = (Label) expandIcon;
      Label titleLabelWidget = (Label) titleLabel;

      String expandIconTooltip = expandIconLabel.getToolTipText();
      String titleLabelTooltip = titleLabelWidget.getToolTipText();

      assertNotNull(expandIconTooltip, "ExpandIcon should have tooltip when collapsed");
      assertNotNull(titleLabelTooltip, "TitleLabel should have tooltip when collapsed");

      // Both should have the same tooltip
      assertEquals(expandIconTooltip, titleLabelTooltip, "ExpandIcon and TitleLabel should have the same tooltip");

      // Tooltip should contain "expand" when collapsed
      assertTrue(expandIconTooltip.toLowerCase().contains("expand"), "Tooltip should contain 'expand' when collapsed");

      // Tooltip should contain the file count
      assertTrue(expandIconTooltip.contains("5"), "Tooltip should contain file count");
    });
  }

  /**
   * Test that tooltip and image change correctly when toggling multiple times.
   */
  @Test
  void testTooltipAndImageToggleBehavior() {
    SwtUtils.invokeOnDisplayThread(() -> {
      fileChangeSummaryBar = new FileChangeSummaryBar(parent, SWT.NONE);
      Map<IFile, FileChangeProperty> filesMap = createMockFilesMap(4, false);

      fileChangeSummaryBar.buildSummaryBarFor(filesMap);

      Object titleBar = getFieldValue(fileChangeSummaryBar, "titleBar");
      Object expandIcon = getFieldValue(titleBar, "expandIcon");
      Label expandIconLabel = (Label) expandIcon;

      // Initially expanded - should have down arrow and collapse tooltip
      Object downArrowImage = getFieldValue(titleBar, "downArrowImage");
      assertEquals(downArrowImage, expandIconLabel.getImage(), "Should show down arrow initially");
      assertTrue(expandIconLabel.getToolTipText().toLowerCase().contains("collapse"),
          "Should have collapse tooltip initially");

      // Toggle to collapse
      invokePrivateMethod(fileChangeSummaryBar, "toggleExpanded");

      Object rightArrowImage = getFieldValue(titleBar, "rightArrowImage");
      assertEquals(rightArrowImage, expandIconLabel.getImage(), "Should show right arrow after first toggle");
      assertTrue(expandIconLabel.getToolTipText().toLowerCase().contains("expand"),
          "Should have expand tooltip after first toggle");

      // Toggle back to expand
      invokePrivateMethod(fileChangeSummaryBar, "toggleExpanded");

      assertEquals(downArrowImage, expandIconLabel.getImage(), "Should show down arrow after second toggle");
      assertTrue(expandIconLabel.getToolTipText().toLowerCase().contains("collapse"),
          "Should have collapse tooltip after second toggle");
    });
  }

  /**
   * Test tooltip contains correct file count for different numbers of files.
   */
  @Test
  void testTooltipContainsCorrectFileCount() {
    SwtUtils.invokeOnDisplayThread(() -> {
      fileChangeSummaryBar = new FileChangeSummaryBar(parent, SWT.NONE);

      // Test with 1 file
      Map<IFile, FileChangeProperty> oneFile = createMockFilesMap(1, false);
      fileChangeSummaryBar.buildSummaryBarFor(oneFile);

      Object titleBar = getFieldValue(fileChangeSummaryBar, "titleBar");
      Object expandIcon = getFieldValue(titleBar, "expandIcon");
      Label expandIconLabel = (Label) expandIcon;

      assertTrue(expandIconLabel.getToolTipText().contains("1"), "Tooltip should contain '1' for single file");
      assertTrue(expandIconLabel.getToolTipText().toLowerCase().contains("file"),
          "Tooltip should contain 'file' (singular)");

      // Test with 10 files
      Map<IFile, FileChangeProperty> tenFiles = createMockFilesMap(10, false);
      fileChangeSummaryBar.buildSummaryBarFor(tenFiles);

      titleBar = getFieldValue(fileChangeSummaryBar, "titleBar");
      expandIcon = getFieldValue(titleBar, "expandIcon");
      expandIconLabel = (Label) expandIcon;

      assertTrue(expandIconLabel.getToolTipText().contains("10"), "Tooltip should contain '10' for ten files");
      assertTrue(expandIconLabel.getToolTipText().toLowerCase().contains("files"),
          "Tooltip should contain 'files' (plural)");
    });
  }

  /**
   * Test that empty files map doesn't create ChangedFiles composite.
   */
  @Test
  void testEmptyFilesMapDoesNotCreateChangedFiles() {
    SwtUtils.invokeOnDisplayThread(() -> {
      fileChangeSummaryBar = new FileChangeSummaryBar(parent, SWT.NONE);
      Map<IFile, FileChangeProperty> emptyMap = new LinkedHashMap<>();

      fileChangeSummaryBar.buildSummaryBarFor(emptyMap);

      Object changedFiles = getFieldValue(fileChangeSummaryBar, "changedFiles");
      // ChangedFiles should not be created or should be disposed
      assertTrue(changedFiles == null || ((Composite) changedFiles).isDisposed(),
          "ChangedFiles should not exist for empty map");
    });
  }

  // Helper methods

  /**
   * Creates a map of mock files with the specified count.
   */
  private Map<IFile, FileChangeProperty> createMockFilesMap(int count, boolean isHandled) {
    Map<IFile, FileChangeProperty> filesMap = new LinkedHashMap<>();
    for (int i = 0; i < count; i++) {
      IFile mockFile = createMockFile("TestFile" + i + ".java");
      filesMap.put(mockFile, new FileChangeProperty(FileChangeType.Created));
    }
    return filesMap;
  }

  /**
   * Creates a mock IFile with the given name.
   */
  private IFile createMockFile(String fileName) {
    IFile mockFile = mock(IFile.class);
    when(mockFile.getName()).thenReturn(fileName);
    when(mockFile.getFullPath()).thenReturn(new Path("/project/" + fileName));
    return mockFile;
  }

  /**
   * Gets the ScrolledComposite from the ChangedFiles object using reflection.
   */
  private ScrolledComposite getScrolledComposite(Object changedFilesObj) {
    try {
      Field field = changedFilesObj.getClass().getDeclaredField("scrolledComposite");
      field.setAccessible(true);
      return (ScrolledComposite) field.get(changedFilesObj);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get scrolledComposite field", e);
    }
  }

  /**
   * Gets a field value from an object using reflection.
   */
  private Object getFieldValue(Object target, String fieldName) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(target);
    } catch (NoSuchFieldException e) {
      // Try parent class if field not found
      try {
        Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
      } catch (Exception ex) {
        throw new RuntimeException("Failed to get field " + fieldName, ex);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to get field " + fieldName, e);
    }
  }

  /**
   * Invokes a private method on an object using reflection.
   */
  private Object invokePrivateMethod(Object target, String methodName, Object... args) {
    try {
      Class<?>[] paramTypes = new Class<?>[args.length];
      for (int i = 0; i < args.length; i++) {
        paramTypes[i] = args[i].getClass();
      }
      java.lang.reflect.Method method = target.getClass().getDeclaredMethod(methodName, paramTypes);
      method.setAccessible(true);
      return method.invoke(target, args);
    } catch (NoSuchMethodException e) {
      // Try with no parameters if not found
      try {
        java.lang.reflect.Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
      } catch (Exception ex) {
        throw new RuntimeException("Failed to invoke method " + methodName, ex);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke method " + methodName, e);
    }
  }
}

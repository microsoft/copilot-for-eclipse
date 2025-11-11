package com.microsoft.copilot.eclipse.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.GetWatchedFilesRequest;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;

@ExtendWith(MockitoExtension.class)
class WatchedFileManagerTests {

  private WatchedFileManager watchedFileManager;

  @Mock
  private ResourcesPlugin mockResourcesPlugin;
  @Mock
  private IWorkspace mockWorkspace;
  @Mock
  private IWorkspaceRoot mockRoot;
  @Mock
  private IProject mockProject;
  @Mock
  private IFile mockFile;
  @Mock
  private IPath mockLocation;
  @Mock
  private CopilotCore mockCopilotPlugin;

  @BeforeEach
  void setUp() {
    watchedFileManager = new WatchedFileManager();
  }

  @Test
  void emptyWorkspaceReturnsEmptyFileList() {
    try (MockedStatic<ResourcesPlugin> mockedPlugin = mockStatic(ResourcesPlugin.class)) {
      mockedPlugin.when(ResourcesPlugin::getPlugin).thenReturn(mockResourcesPlugin);
      when(mockResourcesPlugin.getWorkspace()).thenReturn(mockWorkspace);
      when(mockWorkspace.getRoot()).thenReturn(mockRoot);
      when(mockRoot.getProjects()).thenReturn(new IProject[0]);

      GetWatchedFilesRequest request = new GetWatchedFilesRequest();
      request.setExcludeGitignoredFiles(false);

      List<String> results = watchedFileManager.getWatchedFiles(request);

      assertEquals(0, results.size());
    }
  }

  @Test
  void collectsFilesFromProject() throws CoreException {
    try (MockedStatic<ResourcesPlugin> mockedPlugin = mockStatic(ResourcesPlugin.class);
        MockedStatic<FileUtils> mockedUtil = mockStatic(FileUtils.class)) {
      IProject[] projects = new IProject[] { mockProject };
      IResource[] resources = new IResource[] { mockFile };

      mockedPlugin.when(ResourcesPlugin::getPlugin).thenReturn(mockResourcesPlugin);
      when(mockResourcesPlugin.getWorkspace()).thenReturn(mockWorkspace);
      when(mockWorkspace.getRoot()).thenReturn(mockRoot);
      when(mockRoot.getProjects()).thenReturn(projects);

      when(mockProject.exists()).thenReturn(true);
      when(mockProject.isAccessible()).thenReturn(true);
      when(mockProject.members()).thenReturn(resources);

      when(mockFile.exists()).thenReturn(true);
      when(mockFile.getLocation()).thenReturn(mockLocation);
      mockedUtil.when(() -> FileUtils.getResourceUri((IResource) any())).thenReturn("file:///test/file.txt");

      GetWatchedFilesRequest request = new GetWatchedFilesRequest();
      request.setExcludeGitignoredFiles(false);

      List<String> results = watchedFileManager.getWatchedFiles(request);

      assertEquals(1, results.size());
      assertEquals("file:///test/file.txt", results.get(0));
    }
  }

}

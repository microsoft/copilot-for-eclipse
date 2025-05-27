package com.microsoft.copilot.eclipse.ui.completion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.CopilotUi;

public class EditorLifecycleListenerIntegrationTests {
  private static final String FILENAME = "test.txt";
  private IProject testProject;
  private IFile file;
  private IEditorPart editor;
  private CopilotLanguageServerConnection serverConnectionSpy;
  private EditorLifecycleListener editorLifecycleListener;

  @BeforeEach
  void setUp() throws Exception {
    closeAllEditors();
    Job.getJobManager().join(CopilotUi.INIT_JOB_FAMILY, null);
    serverConnectionSpy = spy(CopilotCore.getPlugin().getCopilotLanguageServer());
    editorLifecycleListener = new EditorLifecycleListener(serverConnectionSpy,
        CopilotUi.getPlugin().getEditorsManager());
    IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
    for (IWorkbenchWindow window : windows) {
      window.getPartService().addPartListener(editorLifecycleListener);
    }

    testProject = createProject("Hello");
    file = createFile(testProject, FILENAME, "Hello");
    assertTrue(file.exists(), "Failed to create file: " + FILENAME + " in project " + testProject);

    Display.getDefault().syncExec(() -> {
      IWorkbench workbench = PlatformUI.getWorkbench();
      IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
      try {
        editor = IDE.openEditor(page, file, "editorForRefactorRenameTests", true);
      } catch (PartInitException e) {
      }
    });
    assertNotNull(editor, "Failed to open editor for file: " + file);
    assertTrue(editor instanceof EditorForRefactorRenameTests,
        "Editor is not of type EditorForRefactorRenameTests");
    waitForJobs(100, 1000);
  }

  @Test
  void testRename() throws Exception {
    renameResource();
  }

  @AfterEach
  void tearDown() throws Exception {
    closeAllEditors();
    waitForJobs(1000, 2000);
    verify(serverConnectionSpy, times(2)).connectDocument(any(), any());

    ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
    verify(serverConnectionSpy, times(2)).disconnectDocument(uriCaptor.capture());
    // Verify the URIs that were disconnected
    List<URI> disconnectedUris = uriCaptor.getAllValues();
    assertTrue(disconnectedUris.get(0).toString().endsWith(FILENAME),
        "First disconnected URI should end with " + FILENAME);
    assertTrue(disconnectedUris.get(1).toString().endsWith("renamed" + FILENAME),
        "Second disconnected URI should end with renamed" + FILENAME);

    IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
    for (IWorkbenchWindow window : windows) {
      window.getPartService().removePartListener(editorLifecycleListener);
    }
  }

  private static IProject createProject(String name) {
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
    try {
      if (!project.exists()) {
        project.create(null);
        project.open(null);
      }
    } catch (CoreException e) {
      fail("Don't catch in test code", e);
    }
    return project;
  }

  public static IFile createFile(final IContainer container, String path, String content) throws CoreException {
    final IFile result = container.getFile(new Path(path));
    ByteArrayInputStream bis = new ByteArrayInputStream(content.getBytes());
    result.create(bis, true, new NullProgressMonitor());
    // bis is always closed in File code
    return result;
  }

  private void renameResource() throws Exception {
    String newFileName = "renamed" + FILENAME;
    file.move(new Path(newFileName), true, new NullProgressMonitor());
    IFile renamedFile = testProject.getFile(newFileName);
    assertTrue(renamedFile.exists(), "resource rename failed");
    waitForJobs(500, 10000);
  }

  private static void closeAllEditors() {
    Display.getDefault().syncExec(() -> {
      IWorkbench workbench = PlatformUI.getWorkbench();
      IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
      for (IWorkbenchWindow window : windows) {
        IIntroManager introManager = workbench.getIntroManager();
        IIntroPart introPart = introManager.getIntro();
        if (introPart != null) {
          introManager.closeIntro(introPart);
        }
        IWorkbenchPage page = window.getActivePage();
        if (page != null) {
          page.closeAllEditors(false);
        }
      }
    });
    processUIEvents();
  }

  public static class EditorForRefactorRenameTests extends TextEditor {
    public EditorForRefactorRenameTests() {
      super();
      setDocumentProvider(new FileDocumentProvider());
    }
  }

  public static void waitForJobs(long minTimeMs, long maxTimeMs) {
    if (maxTimeMs < minTimeMs) {
      throw new IllegalArgumentException("Max time is smaller as min time!");
    }
    final long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < minTimeMs) {
      processUIEvents();
      sleep(10);
    }
    while (!Job.getJobManager().isIdle() && System.currentTimeMillis() - start < maxTimeMs) {
      processUIEvents();
      sleep(10);
    }
  }

  protected static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      return;
    }
  }

  private static void processUIEvents() {
    Display display = Display.getCurrent();
    if (display != null) {
      while (display.readAndDispatch()) {
        // process queued ui events
      }
    }
    sleep(100);
  }
}

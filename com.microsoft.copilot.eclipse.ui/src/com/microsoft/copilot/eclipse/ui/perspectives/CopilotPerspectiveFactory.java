package com.microsoft.copilot.eclipse.ui.perspectives;

import org.eclipse.tm.terminal.view.ui.interfaces.IUIConstants;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

import com.microsoft.copilot.eclipse.core.Constants;

/**
 * Copilot Perspective.
 */
public class CopilotPerspectiveFactory implements IPerspectiveFactory {
    
  @Override
  public void createInitialLayout(IPageLayout layout) {
    // Get the editor area id
    String editorArea = layout.getEditorArea();

    // Put Copilot ChatView to the right of the editor area
    IFolderLayout copilotLayout = layout.createFolder("right", IPageLayout.RIGHT, 0.75f, editorArea);
    copilotLayout.addView(Constants.CHAT_VIEW_ID);

    // Put Project Explorer view on the left side
    IFolderLayout projectExplorerLayout = layout.createFolder("left", IPageLayout.LEFT, 0.25f, editorArea);
    projectExplorerLayout.addView(IPageLayout.ID_PROJECT_EXPLORER);

    // Put Outline view and Mylyn tasks view at the bottom of Project Explorer view
    IFolderLayout outlineLayout = layout.createFolder("bottomLeft", IPageLayout.BOTTOM, 0.5f, "left");
    outlineLayout.addView(IPageLayout.ID_OUTLINE);
    outlineLayout.addView("org.eclipse.mylyn.tasks.ui.views.tasks");

    // Put Console view at the bottom of the editor area
    IFolderLayout consoleLayout = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.8f, editorArea);
    consoleLayout.addView(IPageLayout.ID_PROBLEM_VIEW);
    consoleLayout.addView(IConsoleConstants.ID_CONSOLE_VIEW);
    consoleLayout.addView(IUIConstants.ID);

    // Set the editor onboarding view
    layout.setEditorOnboardingText("Open a file or drop files here to open them");
    layout.setEditorOnboardingImageUri(
        "platform:/plugin/com.microsoft.copilot.eclipse.ui/icons/copilot_perspective.png");
    layout.addEditorOnboardingCommandId("com.microsoft.copilot.eclipse.commands.openChatView");
    layout.addEditorOnboardingCommandId("org.eclipse.ui.window.showKeyAssist");
  }
}

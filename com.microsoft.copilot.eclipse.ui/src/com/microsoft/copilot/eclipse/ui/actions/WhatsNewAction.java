package com.microsoft.copilot.eclipse.ui.actions;

import java.util.Properties;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Action to show the "What's New" information in the Eclipse IDE. This action is typically triggered from the What's
 * New section of the Welcome page.
 */
public class WhatsNewAction implements IIntroAction {

  private static final String COMMAND = "com.microsoft.copilot.eclipse.commands.showWhatIsNew"; //$NON-NLS-1$

  @Override
  public void run(IIntroSite site, Properties params) {
    closeWelcomePage();

    Shell currentShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    currentShell.getDisplay().asyncExec(() -> executeUpdateCommand(COMMAND));

  }

  void closeWelcomePage() {
    IIntroPart introPart = PlatformUI.getWorkbench().getIntroManager().getIntro();
    if (introPart != null) {
      PlatformUI.getWorkbench().getIntroManager().closeIntro(introPart);
    }
  }

  void executeUpdateCommand(String command) {
    ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
    IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
    Command cmd = commandService.getCommand(command);
    ExecutionEvent executionEvent = handlerService.createExecutionEvent(cmd, null);
    try {
      cmd.executeWithChecks(executionEvent);
    } catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e) {
      CopilotCore.LOGGER.error("Failed to execute command: " + command, e);
    }
  }

}

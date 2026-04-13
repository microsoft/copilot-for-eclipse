package com.microsoft.copilot.eclipse.ui.nes;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Action menu for accepting/rejecting suggestions.
 */
public class ActionMenu {

  private final StyledText text;
  private final IEventBroker eventBroker;
  private Menu activeMenu; // JFace created context menu
  private int maxWidth; // Cached max width for alignment
  private int spaceWidth; // Cached space width for alignment

  /**
   * Constructor.
   *
   * @param text the StyledText to attach the menu to
   */
  public ActionMenu(StyledText text) {
    this.text = text;
    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
  }

  /**
   * Checks if the action menu is currently open.
   */
  public boolean isOpen() {
    return activeMenu != null && !activeMenu.isDisposed() && activeMenu.isVisible();
  }

  /**
   * Disposes the active menu if it exists.
   */
  public void dispose() {
    if (activeMenu != null && !activeMenu.isDisposed()) {
      activeMenu.dispose();
      activeMenu = null;
    }
  }

  /**
   * Shows the action menu at the specified coordinates.
   */
  public void show(int x, int y) {
    if (text == null || text.isDisposed()) {
      return;
    }
    dispose();

    MenuManager menuManager = new MenuManager();
    String acceptLabel = buildAlignedMenuLabel(Messages.actionMenu_accept, "Tab");
    String rejectLabel = buildAlignedMenuLabel(Messages.actionMenu_reject, "Esc");

    ImageDescriptor acceptImage = UiUtils.buildImageDescriptorFromPngPath("/icons/chat/keyboard-tab.png");
    ImageDescriptor cancelImage = UiUtils.buildImageDescriptorFromPngPath("/icons/close.png");

    menuManager.add(new Action(acceptLabel, acceptImage) {
      @Override
      public void run() {
        if (eventBroker != null) {
          eventBroker.post(CopilotEventConstants.TOPIC_NES_ACCEPT_SUGGESTION, text);
        }
      }
    });
    menuManager.add(new Action(rejectLabel, cancelImage) {
      @Override
      public void run() {
        if (eventBroker != null) {
          eventBroker.post(CopilotEventConstants.TOPIC_NES_REJECT_SUGGESTION, text);
        }
      }
    });

    Point location = text.toDisplay(x, y);
    activeMenu = menuManager.createContextMenu(text.getShell());
    activeMenu.setLocation(location.x, location.y);

    activeMenu.setVisible(true);
  }

  /**
   * Builds an aligned menu label with consistent spacing. Pattern matches QuotaTextCalculator's getAlignedQuotaText
   * method.
   */
  private String buildAlignedMenuLabel(String base, String shortcut) {
    // Windows supports align the text via \t
    return base + "\t" + shortcut;
  }
}

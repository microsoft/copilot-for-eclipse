package com.microsoft.copilot.eclipse.ui.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

import com.microsoft.copilot.eclipse.ui.utils.ResourceUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;


/**
 * Handler to show context menu items for adding files or folders to references in package explorer/project explorer.
 */
public class ShowContextMenuHandler extends CompoundContributionItem {

  @Override
  protected IContributionItem[] getContributionItems() {
    IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    var sel = win != null ? win.getSelectionService().getSelection() : null;


    if (!(sel instanceof IStructuredSelection s) || s.isEmpty()) {
      return new IContributionItem[0];
    }
    
    IStructuredSelection selection = (IStructuredSelection) sel;

    ResourceUtils.SelectionStats stats = ResourceUtils.analyzeSelection(selection);

    if (!stats.hasOnlyValidResources()) {
      return new IContributionItem[0];
    }

    List<IContributionItem> items = new ArrayList<>();

    items.add(new Separator("com.microsoft.copilot.eclipse.ui.contextMenu.start"));

    ImageDescriptor menuIcon = UiUtils.buildImageDescriptorFromPngPath("/icons/github_copilot.png");

    MenuManager submenu = new MenuManager("Copilot", menuIcon, "com.microsoft.copilot.eclipse.ui.contextMenu");
    
    // Add "Add to References" command item
    CommandContributionItemParameter p = new CommandContributionItemParameter(
        win, null, "com.microsoft.copilot.eclipse.commands.addToReferences", CommandContributionItem.STYLE_PUSH);
    submenu.add(new CommandContributionItem(p));
    items.add(submenu);

    items.add(new GroupMarker("com.microsoft.copilot.eclipse.ui.contextMenu.end"));

    return items.toArray(IContributionItem[]::new);
  }
}

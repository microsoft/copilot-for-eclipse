// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.copilot.eclipse.ui.chat.services.ReferencedFileService;
import com.microsoft.copilot.eclipse.ui.utils.ResourceUtils;
import com.microsoft.copilot.eclipse.ui.utils.ResourceUtils.SelectionStats;

/**
 * Manages drag & drop of workspace resources into the ChatView to become referenced files.
 */
public class DragReferenceManager {
  private final ChatView chatView;
  private final ReferencedFileService referencedFileService;
  private DropTarget dropTarget;
  private Transfer[] transfers = new Transfer[] { LocalSelectionTransfer.getTransfer()};

  /**
   * Create a new DragReferenceManager.
   *
   * @param chatView the chat view
   */
  public DragReferenceManager(ChatView chatView, ReferencedFileService referencedFileService) {
    this.chatView = chatView;
    this.referencedFileService = referencedFileService;
  }

  /** Attach DnD to a composite. Re-attach will dispose the previous target. */
  public void attach(Composite parent) {
    if (parent == null || parent.isDisposed()) {
      return;
    }

    if (dropTarget != null && !dropTarget.isDisposed() && dropTarget.getControl() == parent) {
      return;
    }
    disposeDropTarget();

    dropTarget = new DropTarget(parent, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);
    dropTarget.setTransfer(transfers);
    dropTarget.addDropListener(new DropTargetListener() {
      @Override
      public void dragEnter(DropTargetEvent e) {
        if (!canAcceptDrop()) {
          e.detail = DND.DROP_NONE;
          return;
        }
        if (e.detail == DND.DROP_DEFAULT) {
          e.detail = DND.DROP_COPY;
        }
      }

      @Override
      public void dragOver(DropTargetEvent e) {
        if (!canAcceptDrop()) {
          e.detail = DND.DROP_NONE;
          return;
        }
        ISelection sel = LocalSelectionTransfer.getTransfer().getSelection();
        if (sel instanceof IStructuredSelection selection) {
          SelectionStats stats = ResourceUtils.analyzeSelection(selection);
          e.detail = stats.hasOnlyValidResources() ? DND.DROP_COPY : DND.DROP_NONE;
          e.feedback = stats.hasOnlyValidResources() ? (DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL) : DND.FEEDBACK_NONE;
          return;
        }
        e.detail = DND.DROP_NONE;
        e.feedback = DND.FEEDBACK_NONE;
      }

      @Override
      public void drop(DropTargetEvent event) {
        if (!canAcceptDrop()) {
          event.detail = DND.DROP_NONE;
          return;
        }
        if (!isSupportedType(event.currentDataType)) {
          event.detail = DND.DROP_NONE;
          return;
        }
        List<IResource> resources = extractValidWorkspaceResources(event);
        if (resources.isEmpty()) {
          return;
        }
        referencedFileService.addReferencedFiles(resources);
      }

      @Override
      public void dragLeave(DropTargetEvent event) {
      }

      @Override
      public void dragOperationChanged(DropTargetEvent event) {
        if (!canAcceptDrop()) {
          event.detail = DND.DROP_NONE;
        }
      }

      @Override
      public void dropAccept(DropTargetEvent event) {
        if (!canAcceptDrop()) {
          event.detail = DND.DROP_NONE;
        }
      }
    });
  }

  /**
   * Check if the chat view is in a state where it can accept file drops.
   *
   * @return true if the chat view can accept drops, false otherwise
   */
  private boolean canAcceptDrop() {
    Composite actionBar = chatView.getActionBar();
    return actionBar != null && !actionBar.isDisposed();
  }

  private List<IResource> extractValidWorkspaceResources(DropTargetEvent e) {
    ISelection sel = LocalSelectionTransfer.getTransfer().getSelection();
    if (sel instanceof IStructuredSelection selection) {
      return ResourceUtils.collectValidResources(selection);
    }
    return List.of();
  }

  private boolean isSupportedType(TransferData data) {
    return LocalSelectionTransfer.getTransfer().isSupportedType(data);
  }

  /**
   * Detach and dispose current DropTarget if any.
   */
  private void disposeDropTarget() {
    if (dropTarget != null && !dropTarget.isDisposed()) {
      dropTarget.dispose();
    }
    dropTarget = null;
  }

  /**
   * Dispose the drag reference manager.
   */
  public void dispose() {
    disposeDropTarget();
  }
}

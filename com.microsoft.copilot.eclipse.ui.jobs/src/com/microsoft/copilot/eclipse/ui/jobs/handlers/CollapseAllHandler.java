package com.microsoft.copilot.eclipse.ui.jobs.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import com.microsoft.copilot.eclipse.ui.jobs.events.JobsViewEvents;
import com.microsoft.copilot.eclipse.ui.jobs.views.JobsView;

/**
 * Handler for collapsing all nodes in the Jobs View.
 */
public class CollapseAllHandler {

  /**
   * Check if the collapse all command can be executed.
   *
   * @param part the active part
   * @return true if the part is a JobsView
   */
  @CanExecute
  public boolean canExecute(MPart part) {
    return part != null && part.getObject() instanceof JobsView;
  }

  /**
   * Execute the collapse all command by sending an event.
   *
   * @param eventBroker the event broker
   */
  @Execute
  public void execute(IEventBroker eventBroker) {
    eventBroker.post(JobsViewEvents.TOPIC_COLLAPSE_ALL, null);
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * A handler that does nothing when executed. This can be used as a placeholder command for the disabled Menubar menus
 * since it must need a handler for each item.
 */
public class DisabledDoNothingHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    return null;
  }

  @Override
  public boolean isEnabled() {
    return false;
  }

}

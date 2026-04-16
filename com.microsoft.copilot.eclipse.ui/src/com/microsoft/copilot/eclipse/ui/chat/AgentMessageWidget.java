// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.codingagent.CodingAgentMessageRequestParams;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Widget to display a coding agent message.
 */
public class AgentMessageWidget extends Composite {
  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param style the style
   * @param params the coding agent message parameters
   */
  public AgentMessageWidget(Composite parent, int style, CodingAgentMessageRequestParams params) {
    super(parent, style | SWT.BORDER);
    setLayout(new GridLayout(1, true));
    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    buildMessageLabelWithIcon(params);

    // Add button to view the PR if prLink is available
    if (StringUtils.isNotEmpty(params.getPrLink())) {
      buildViewPrButton(params.getPrLink());
    }

    requestLayout();
  }

  private void buildMessageLabelWithIcon(CodingAgentMessageRequestParams params) {
    // Create title composite with icon
    if (StringUtils.isNotEmpty(params.getTitle())) {
      Composite titleComposite = new Composite(this, SWT.NONE);
      titleComposite.setLayout(new GridLayout(2, false));
      titleComposite.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));

      // Pull request icon
      Label iconLabel = new Label(titleComposite, SWT.TOP);
      Image prImage = UiUtils.isDarkTheme() ? UiUtils.buildImageFromPngPath("/icons/jobs/pull_request_white.png")
          : UiUtils.buildImageFromPngPath("/icons/jobs/pull_request_black.png");
      iconLabel.setImage(prImage);
      iconLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
      iconLabel.addDisposeListener(e -> {
        if (prImage != null && !prImage.isDisposed()) {
          prImage.dispose();
        }
      });

      // Title label
      ChatMarkupViewer titleLabel = new ChatMarkupViewer(titleComposite, SWT.LEFT | SWT.WRAP);
      StyledText titleText = titleLabel.getTextWidget();
      titleText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      titleText.setEditable(false);
      titleLabel.setMarkup("**" + params.getTitle() + "**");
    }

    // Description
    if (StringUtils.isNotEmpty(params.getDescription())) {
      Composite descComposite = new Composite(this, SWT.NONE);
      descComposite.setLayout(new GridLayout(1, false));
      descComposite.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

      ChatMarkupViewer descLabel = new ChatMarkupViewer(descComposite, SWT.LEFT | SWT.WRAP);
      StyledText descText = descLabel.getTextWidget();
      descText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      descText.setEditable(false);

      String reducedDescription = StringUtils.isBlank(params.getDescription()) ? ""
          : params.getDescription().substring(0, 100) + "...";
      descLabel.setMarkup(reducedDescription);
    }

    requestLayout();
  }

  private void buildViewPrButton(String prLink) {
    Composite composite = new Composite(this, SWT.NONE);
    composite.setLayout(new RowLayout(SWT.HORIZONTAL));

    Button viewPrButton = new Button(composite, SWT.PUSH);
    viewPrButton.setText(Messages.agentMessageWidget_openInBrowserButton);
    viewPrButton.setToolTipText(Messages.agentMessageWidget_openInBrowserTooltip);
    viewPrButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
        UiUtils.openLink(prLink);
      }
    });
    viewPrButton.setData(CssConstants.CSS_CLASS_NAME_KEY, "btn-primary");

    Button viewJobListButton = new Button(composite, SWT.PUSH);
    viewJobListButton.setText(Messages.agentMessageWidget_openJobListButton);
    viewJobListButton.setToolTipText(Messages.agentMessageWidget_openJobListTooltip);
    viewJobListButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
        UiUtils.openE4Part(Constants.GITHUB_JOBS_VIEW_ID);
      }
    });
    viewJobListButton.setData(CssConstants.CSS_CLASS_NAME_KEY, "btn-secondary");
  }
}

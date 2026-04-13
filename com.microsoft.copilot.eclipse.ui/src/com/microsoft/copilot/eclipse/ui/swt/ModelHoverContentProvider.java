// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.swt;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.ModelUtils;

/**
 * Renders the full hover UI for model items in the model picker dropdown. The layout consists of the bold title
 * header, an optional degradation warning row, a separator, and model-specific details such as family and cost.
 */
public class ModelHoverContentProvider implements IDropdownItemHoverProvider {

  private static final int SECTION_SPACING = 3;
  private static final String POPUP_SECONDARY_TEXT_CLASS = "popup-secondary-text";

  private final CopilotModel model;
  private final IStylingEngine stylingEngine;

  /**
   * Creates a hover provider for the given model.
   *
   * @param model the model to display in the hover
   */
  public ModelHoverContentProvider(CopilotModel model) {
    this.model = model;
    this.stylingEngine = PlatformUI.getWorkbench().getService(IStylingEngine.class);
  }

  @Override
  public void configureHover(Composite parent, DropdownItem item) {
    renderHeader(parent, item);

    // Degradation warning
    if (StringUtils.isNotBlank(model.getDegradationReason())) {
      addWarningRow(parent, model.getDegradationReason());
    }

    addSeparator(parent);

    // Details section
    Composite detailsComp = new Composite(parent, SWT.NONE);
    GridData detailsGd = new GridData(SWT.FILL, SWT.FILL, true, false);
    detailsGd.verticalIndent = SECTION_SPACING;
    detailsComp.setLayoutData(detailsGd);
    GridLayout detailsLayout = new GridLayout(1, false);
    detailsLayout.marginWidth = 0;
    detailsLayout.marginHeight = 0;
    detailsLayout.marginBottom = SECTION_SPACING;
    detailsLayout.verticalSpacing = 2;
    detailsComp.setLayout(detailsLayout);

    // Family
    if (StringUtils.isNotBlank(model.getModelFamily())) {
      addDetailRow(detailsComp, Messages.model_hover_family, model.getModelFamily());
    }

    // Cost
    String cost = buildCostText();
    if (StringUtils.isNotBlank(cost)) {
      addDetailRow(detailsComp, Messages.model_hover_cost, cost);
    }
  }

  private void renderHeader(Composite parent, DropdownItem item) {
    Label titleLabel = new Label(parent, SWT.WRAP);
    titleLabel.setText(item.getLabel());
    titleLabel.setFont(createBoldFont(titleLabel));
    GridData headerGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
    headerGd.verticalIndent = SECTION_SPACING;
    titleLabel.setLayoutData(headerGd);
  }

  private Font createBoldFont(Label label) {
    Display display = label.getDisplay();
    FontData[] fontData = label.getFont().getFontData();
    for (FontData data : fontData) {
      data.setStyle(SWT.BOLD);
    }
    Font boldFont = new Font(display, fontData);
    label.addDisposeListener(event -> boldFont.dispose());
    return boldFont;
  }

  private void addWarningRow(Composite parent, String warningText) {
    Label warningLabel = new Label(parent, SWT.WRAP);
    warningLabel.setText(warningText);
    setCssClass(warningLabel, POPUP_SECONDARY_TEXT_CLASS);
    warningLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  }

  private void addDetailRow(Composite parent, String label, String value) {
    Label detailLabel = new Label(parent, SWT.NONE);
    detailLabel.setText(label + value);
    setCssClass(detailLabel, POPUP_SECONDARY_TEXT_CLASS);
    detailLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  }

  private void setCssClass(Label control, String className) {
    if (stylingEngine != null) {
      stylingEngine.setClassname(control, className);
      stylingEngine.style(control);
    } else {
      control.setData(CssConstants.CSS_CLASS_NAME_KEY, className);
    }
  }

  private String buildCostText() {
    String suffix = ModelUtils.getModelSuffix(model);
    if (model.getBilling() != null && model.getBilling().isPremium() && StringUtils.isNotBlank(suffix)) {
      return suffix + " " + Messages.model_hover_cost_premium;
    }
    return suffix;
  }

  private void addSeparator(Composite parent) {
    Composite separator = new Composite(parent, SWT.NONE);
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd.heightHint = 1;
    gd.verticalIndent = SECTION_SPACING;
    separator.setLayoutData(gd);
    Display display = parent.getDisplay();
    Color separatorColor = CssConstants.getSeparatorColor(display);
    separator.addPaintListener(e -> {
      Rectangle r = separator.getClientArea();
      e.gc.setBackground(separatorColor);
      e.gc.fillRectangle(0, 0, r.width, 1);
    });
  }
}

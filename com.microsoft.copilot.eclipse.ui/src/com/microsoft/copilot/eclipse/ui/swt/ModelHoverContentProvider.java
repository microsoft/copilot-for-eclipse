package com.microsoft.copilot.eclipse.ui.swt;

import org.apache.commons.lang3.StringUtils;
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

import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.ModelUtils;

/**
 * Provides rich hover UI for model items in the model picker dropdown. Displays model name,
 * family, cost, supported capabilities, and deprecation warnings.
 */
public class ModelHoverContentProvider implements IDropdownItemHoverProvider {

  private static final int SEPARATOR_V_MARGIN = 4;

  private final CopilotModel model;

  /**
  * Creates a hover provider for the given model.
   *
   * @param model the model to display in the hover
   */
  public ModelHoverContentProvider(CopilotModel model) {
    this.model = model;
  }

  @Override
  public void configureHover(Composite parent, DropdownItem item) {
    Display display = parent.getDisplay();

    // Model name (bold title)
    Label nameLabel = new Label(parent, SWT.NONE);
    nameLabel.setText(model.getModelName());
    FontData[] fontData = nameLabel.getFont().getFontData();
    for (FontData fd : fontData) {
      fd.setStyle(SWT.BOLD);
    }
    Font boldFont = new Font(display, fontData);
    nameLabel.setFont(boldFont);
    nameLabel.addDisposeListener(e -> boldFont.dispose());
    nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Separator
    addSeparator(parent);

    // Details section
    Composite detailsComp = new Composite(parent, SWT.NONE);
    detailsComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    GridLayout detailsLayout = new GridLayout(1, false);
    detailsLayout.marginWidth = 0;
    detailsLayout.marginHeight = 0;
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

  private void addDetailRow(Composite parent, String label, String value) {
    Label detailLabel = new Label(parent, SWT.NONE);
    detailLabel.setText(label + value);
    detailLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  }

  private String buildCostText() {
    String suffix = ModelUtils.getModelSuffix(model);
    if (model.getBilling() != null && model.getBilling().isPremium() && StringUtils.isNotBlank(suffix)) {
      return suffix + " " + Messages.model_hover_cost_premium;
    }
    return suffix;
  }

  private void addSeparator(Composite parent) {
    Display display = parent.getDisplay();
    final Color separatorColor = CssConstants.getSeparatorColor(display);
    Composite separator = new Composite(parent, SWT.NONE);
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd.heightHint = 1;
    gd.verticalIndent = SEPARATOR_V_MARGIN;
    separator.setLayoutData(gd);
    separator.addPaintListener(e -> {
      Rectangle r = separator.getClientArea();
      e.gc.setBackground(separatorColor);
      e.gc.fillRectangle(0, 0, r.width, 1);
    });
  }
}

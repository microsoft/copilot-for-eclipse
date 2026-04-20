// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.swt;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelBillingTokenPrices;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelCapabilitiesLimits;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.ModelUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Renders the full hover UI for model items in the model picker dropdown. The layout consists of the bold title header,
 * an optional degradation warning or preview badge, and model-specific details such as context size, token pricing, and
 * context window.
 */
public class ModelHoverContentProvider implements IDropdownItemHoverProvider {

  private static final int SECTION_SPACING = 3;
  private static final String POPUP_SECONDARY_TEXT_CLASS = "popup-secondary-text";

  private static Image arrowUpIcon;
  private static Image arrowDownIcon;

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

    if (StringUtils.isNotBlank(model.getModelPickerCategory())) {
      CategoryBadge.create(parent, model.getModelPickerCategory());
    }

    // Degradation warning
    if (StringUtils.isNotBlank(model.getDegradationReason())) {
      addWarningRow(parent, model.getDegradationReason());
    }

    CopilotModelCapabilitiesLimits limits = model.getCapabilities() != null ? model.getCapabilities().limits() : null;
    CopilotModelBillingTokenPrices tokenPrices = model.getBilling() != null ? model.getBilling().tokenPrices() : null;

    addContextSizeSection(parent, limits);
    addPricingSection(parent, tokenPrices);
    addContextWindowSection(parent, limits);
  }

  private void renderHeader(Composite parent, DropdownItem item) {
    Label titleLabel = new Label(parent, SWT.WRAP);
    titleLabel.setText(item.getLabel());
    titleLabel.setFont(createBoldFont(titleLabel));
    GridData headerGd = new GridData(SWT.FILL, SWT.NONE, true, false);
    titleLabel.setLayoutData(headerGd);
  }

  private void addContextSizeSection(Composite parent, CopilotModelCapabilitiesLimits limits) {
    if (limits == null || (limits.maxInputTokens() < 0 && limits.maxOutputTokens() < 0)) {
      return;
    }

    addSeparator(parent);

    Composite row = createKeyValueRow(parent);
    ((GridData) row.getLayoutData()).verticalIndent = SECTION_SPACING;

    // Context Size:
    Label keyLabel = createSecondaryTextLabel(row, Messages.model_hover_contextSize);
    keyLabel.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false));

    Composite valueComp = new Composite(row, SWT.NONE);
    valueComp.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, true, false));
    RowLayout valueLayout = new RowLayout(SWT.HORIZONTAL);
    valueLayout.marginTop = 0;
    valueLayout.marginBottom = 0;
    valueLayout.marginLeft = 0;
    valueLayout.marginRight = 0;

    // Add spacing between input and output token labels if both are present
    if (limits.maxInputTokens() >= 0 && limits.maxOutputTokens() >= 0) {
      valueLayout.spacing = 4;
    } else {
      valueLayout.spacing = 0;
    }
    valueComp.setLayout(valueLayout);

    // ex. ↑128k
    if (limits.maxInputTokens() >= 0) {
      addArrowTokenLabel(valueComp, true, ModelUtils.formatTokenCount(limits.maxInputTokens()));
    }
    // ex. ↓16k
    if (limits.maxOutputTokens() >= 0) {
      addArrowTokenLabel(valueComp, false, ModelUtils.formatTokenCount(limits.maxOutputTokens()));
    }
  }

  private void addPricingSection(Composite parent, CopilotModelBillingTokenPrices tokenPrices) {
    if (tokenPrices == null || !hasAnyPrice(tokenPrices)) {
      return;
    }

    addSeparator(parent);

    // Cost / Million Tokens header
    Label headerLabel = createSecondaryTextLabel(parent, Messages.model_hover_costPerMillionTokens);
    GridData headerGd = new GridData(SWT.FILL, SWT.NONE, true, false);
    headerGd.verticalIndent = SECTION_SPACING;
    headerLabel.setLayoutData(headerGd);

    // Price rows
    if (tokenPrices.inputPrice() > 0) {
      addKeyValueRow(parent, Messages.model_hover_inputPrice, ModelUtils.formatPriceValue(tokenPrices.inputPrice()));
    }
    if (tokenPrices.outputPrice() > 0) {
      addKeyValueRow(parent, Messages.model_hover_outputPrice, ModelUtils.formatPriceValue(tokenPrices.outputPrice()));
    }
    if (tokenPrices.cachePrice() > 0) {
      addKeyValueRow(parent, Messages.model_hover_cachedPrice, ModelUtils.formatPriceValue(tokenPrices.cachePrice()));
    }
  }

  private void addContextWindowSection(Composite parent, CopilotModelCapabilitiesLimits limits) {
    if (limits == null || limits.maxContextWindowTokens() < 0) {
      return;
    }

    addSeparator(parent);
    addKeyValueRow(parent, Messages.model_hover_contextWindow,
        ModelUtils.formatTokenCount(limits.maxContextWindowTokens()), SECTION_SPACING);
  }

  private void addKeyValueRow(Composite parent, String keyText, String valueText) {
    addKeyValueRow(parent, keyText, valueText, 0);
  }

  private void addKeyValueRow(Composite parent, String keyText, String valueText, int verticalIndent) {
    Composite row = createKeyValueRow(parent);
    ((GridData) row.getLayoutData()).verticalIndent = verticalIndent;

    Label keyLabel = createSecondaryTextLabel(row, keyText);
    keyLabel.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, true, false));

    Label valueLabel = createSecondaryTextLabel(row, valueText);
    valueLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false));
  }

  private Composite createKeyValueRow(Composite parent) {
    Composite row = new Composite(parent, SWT.NONE);
    row.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    row.setLayout(layout);
    return row;
  }

  private void addArrowTokenLabel(Composite parent, boolean isInput, String tokenText) {
    GridLayout pairLayout = new GridLayout(2, false);
    pairLayout.marginWidth = 0;
    pairLayout.marginHeight = 0;
    pairLayout.horizontalSpacing = 0;
    Composite pairComp = new Composite(parent, SWT.NONE);
    pairComp.setLayout(pairLayout);

    initArrowIcons(pairComp);
    Label arrowLabel = new Label(pairComp, SWT.NONE);
    Image arrowImage = isInput ? arrowUpIcon : arrowDownIcon;
    arrowLabel.setImage(arrowImage);

    createSecondaryTextLabel(pairComp, tokenText);
  }

  private static void initArrowIcons(Composite parent) {
    if (arrowUpIcon == null || arrowUpIcon.isDisposed()) {
      boolean isDark = UiUtils.isDarkTheme();
      arrowUpIcon = UiUtils.buildImageFromPngPath(isDark ? "/icons/dropdown/context_size_arrow_up_dark.png"
          : "/icons/dropdown/context_size_arrow_up_light.png");
      arrowDownIcon = UiUtils.buildImageFromPngPath(isDark ? "/icons/dropdown/context_size_arrow_down_dark.png"
          : "/icons/dropdown/context_size_arrow_down_light.png");
      parent.getDisplay().addListener(SWT.Dispose, e -> disposeStaticIcons());
    }
  }

  private static void disposeStaticIcons() {
    if (arrowUpIcon != null && !arrowUpIcon.isDisposed()) {
      arrowUpIcon.dispose();
      arrowUpIcon = null;
    }
    if (arrowDownIcon != null && !arrowDownIcon.isDisposed()) {
      arrowDownIcon.dispose();
      arrowDownIcon = null;
    }
  }

  private boolean hasAnyPrice(CopilotModelBillingTokenPrices prices) {
    return prices.inputPrice() > 0 || prices.outputPrice() > 0 || prices.cachePrice() > 0;
  }

  private Font createBoldFont(Label label) {
    Font boldFont = UiUtils.getBoldFont(label.getDisplay(), label.getFont());
    label.addDisposeListener(event -> boldFont.dispose());
    return boldFont;
  }

  private void addWarningRow(Composite parent, String warningText) {
    Label warningLabel = new Label(parent, SWT.WRAP);
    warningLabel.setText(warningText);
    UiUtils.applyCssClass(warningLabel, POPUP_SECONDARY_TEXT_CLASS, stylingEngine);
    warningLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
  }

  private Label createSecondaryTextLabel(Composite parent, String text) {
    Label label = new Label(parent, SWT.NONE);
    label.setText(text);
    UiUtils.applyCssClass(label, POPUP_SECONDARY_TEXT_CLASS, stylingEngine);
    return label;
  }

  private void addSeparator(Composite parent) {
    Composite separator = new Composite(parent, SWT.NONE);
    GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
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

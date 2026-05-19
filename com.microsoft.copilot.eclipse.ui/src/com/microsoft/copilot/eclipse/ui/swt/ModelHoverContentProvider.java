// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.swt;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelCapabilitiesLimits;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelCapabilitiesSupports;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ModelService;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.ModelUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Renders the full hover UI for model items in the model picker dropdown. The layout consists of the bold title header,
 * an optional category badge, an optional degradation warning, and model-specific details such as context size and
 * token pricing.
 */
public class ModelHoverContentProvider implements IDropdownItemHoverProvider {

  private static final int SECTION_SPACING = 3;
  private static final String POPUP_SECONDARY_TEXT_CLASS = "popup-secondary-text";

  /** Horizontal padding inside a thinking effort row, so the hover background has breathing room. */
  private static final int THINKING_EFFORT_ROW_H_PADDING = 4;
  /** Vertical padding inside a thinking effort row, so the hover background has breathing room. */
  private static final int THINKING_EFFORT_ROW_V_PADDING = 2;

  private static Image arrowUpIcon;
  private static Image arrowDownIcon;
  private static Image effortCheckIcon;

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
  public void configureHover(Composite parent, DropdownItem item, Runnable closeRequest) {
    renderHeader(parent, item);

    if (StringUtils.isNotBlank(model.getModelPickerCategory())) {
      CategoryBadge.create(parent, model.getModelPickerCategory());
    }

    // Degradation warning
    if (StringUtils.isNotBlank(model.getDegradationReason())) {
      addWarningRow(parent, model.getDegradationReason());
    }

    CopilotModelCapabilitiesLimits limits = model.getCapabilities() != null ? model.getCapabilities().limits() : null;

    addContextSizeSection(parent, limits);
    addPricingSection(parent, model.getModelPickerPriceCategory());
    addThinkingEffortSection(parent, closeRequest);
  }

  private void renderHeader(Composite parent, DropdownItem item) {
    Label titleLabel = new Label(parent, SWT.WRAP);
    titleLabel.setText(item.getLabel());
    titleLabel.setFont(createBoldFont(titleLabel));
    GridData headerGd = new GridData(SWT.FILL, SWT.NONE, true, false);
    titleLabel.setLayoutData(headerGd);
  }

  private void addContextSizeSection(Composite parent, CopilotModelCapabilitiesLimits limits) {
    if (limits == null) {
      return;
    }
    boolean hasInput = isPositive(limits.maxInputTokens());
    boolean hasOutput = isPositive(limits.maxOutputTokens());
    if (!hasInput && !hasOutput) {
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
    if (hasInput && hasOutput) {
      valueLayout.spacing = 4;
    } else {
      valueLayout.spacing = 0;
    }
    valueComp.setLayout(valueLayout);

    // ex. ↑128K
    if (hasInput) {
      addArrowTokenLabel(valueComp, true, ModelUtils.formatTokenCount(limits.maxInputTokens()));
    }
    // ex. ↓16K
    if (hasOutput) {
      addArrowTokenLabel(valueComp, false, ModelUtils.formatTokenCount(limits.maxOutputTokens()));
    }
  }

  private void addPricingSection(Composite parent, String priceCategory) {
    String costSymbols = ModelUtils.formatPriceCategory(priceCategory);
    if (StringUtils.isBlank(costSymbols)) {
      return;
    }

    addSeparator(parent);
    addKeyValueRow(parent, Messages.model_hover_cost, costSymbols);
  }

  private void addThinkingEffortSection(Composite parent, Runnable closeRequest) {
    if (!ModelUtils.supportsReasoningEffortLevel(model)) {
      return;
    }
    CopilotModelCapabilitiesSupports supports = model.getCapabilities().supports();
    List<String> efforts = supports.reasoningEfforts();
    if (efforts == null || efforts.isEmpty()) {
      return;
    }

    addSeparator(parent);

    Composite section = new Composite(parent, SWT.NONE);
    section.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    GridLayout sectionLayout = new GridLayout(1, false);
    sectionLayout.marginWidth = 0;
    sectionLayout.marginHeight = 0;
    sectionLayout.verticalSpacing = 2;
    ((GridData) section.getLayoutData()).verticalIndent = SECTION_SPACING;
    section.setLayout(sectionLayout);

    Label keyLabel = createSecondaryTextLabel(section, Messages.model_hover_thinkingEffort);
    keyLabel.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, true, false));

    Composite options = new Composite(section, SWT.NONE);
    options.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    GridLayout optionsLayout = new GridLayout(1, false);
    optionsLayout.marginWidth = 0;
    optionsLayout.marginHeight = 0;
    optionsLayout.verticalSpacing = 2;
    options.setLayout(optionsLayout);

    populateThinkingEffortOptions(options, efforts, closeRequest);
  }

  private void populateThinkingEffortOptions(Composite options, List<String> efforts, Runnable closeRequest) {
    ModelService modelService = resolveModelService();
    String selected = modelService != null ? modelService.getSelectedReasoningEffort(model) : null;
    String defaultEffort = ModelUtils.resolveDefaultReasoningEffort(model);
    // Show the user's selection when present, otherwise pre-mark the default so the hover always communicates
    // which effort the request will use.
    String effective = StringUtils.isNotBlank(selected) ? selected : defaultEffort;

    for (String effort : efforts) {
      if (StringUtils.isBlank(effort)) {
        continue;
      }
      addThinkingEffortOption(options, modelService, effort, effort.equals(effective), effort.equals(defaultEffort),
          closeRequest);
    }
    options.requestLayout();
  }

  private void addThinkingEffortOption(Composite parent, ModelService modelService, String effort, boolean isSelected,
      boolean isDefault, Runnable closeRequest) {
    String displayText = ModelUtils.formatReasoningEffortLevel(effort);
    if (displayText == null) {
      return;
    }

    // Three-column layout mirroring the model item row in DropdownPopup: a fixed-width leading icon column that
    // reserves space for the selection check mark, the left-aligned effort label, and the right-aligned secondary
    // description that grows to fill the remaining width.
    GridLayout rowLayout = new GridLayout(3, false);
    rowLayout.marginWidth = THINKING_EFFORT_ROW_H_PADDING;
    rowLayout.marginHeight = THINKING_EFFORT_ROW_V_PADDING;
    rowLayout.horizontalSpacing = 6;
    Composite row = new Composite(parent, SWT.NONE);
    row.setLayout(rowLayout);
    row.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    Image checkIcon = getCheckIcon(parent);
    Label iconLabel = new Label(row, SWT.NONE);
    GridData iconGd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
    if (checkIcon != null) {
      Rectangle iconBounds = checkIcon.getBounds();
      iconGd.widthHint = iconBounds.width;
      iconGd.heightHint = iconBounds.height;
    } else {
      iconGd.widthHint = 12;
    }
    iconLabel.setLayoutData(iconGd);
    if (isSelected && checkIcon != null) {
      iconLabel.setImage(checkIcon);
    }

    String labelText = isDefault ? NLS.bind(Messages.model_hover_thinkingEffort_default_suffix, displayText)
        : displayText;
    Label optionLabel = new Label(row, SWT.NONE);
    optionLabel.setText(labelText);
    // Primary text color (default Label foreground); left-aligned in the middle column.
    optionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    String description = ModelUtils.formatReasoningEffortDescription(effort);
    Label descriptionLabel = null;
    if (StringUtils.isNotBlank(description)) {
      descriptionLabel = new Label(row, SWT.NONE);
      descriptionLabel.setText(description);
      // Right-aligned and grabs the remaining horizontal space so the description hugs the right edge of the
      // hover popup while the effort label stays anchored to the left.
      descriptionLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
      setCssClass(descriptionLabel, POPUP_SECONDARY_TEXT_CLASS);
    }

    Cursor handCursor = parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND);
    row.setCursor(handCursor);
    iconLabel.setCursor(handCursor);
    optionLabel.setCursor(handCursor);
    if (descriptionLabel != null) {
      descriptionLabel.setCursor(handCursor);
    }

    // Attach the shared row controller: seeds CSS state on the row + its descendants and installs the
    // focused-background paint listener so hover/keyboard focus can flip the entire subtree to the
    // focused color via the shared CSS rules.
    ItemController rowController = ItemController.attach(row, stylingEngine, ItemController.CSS_DEFAULT_ID);
    rowController.installHoverFocus(iconLabel, optionLabel, descriptionLabel);

    MouseAdapter clickHandler = new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        if (e.button != 1 || modelService == null) {
          return;
        }
        // Persist the chosen effort first, then activate this model. Activating triggers the picker button
        // to update its label/suffix so the dropdown control reflects the (model, effort) pair the user just
        // chose -- even when they clicked an effort on a non-active model.
        modelService.setSelectedReasoningEffort(model, effort);
        modelService.setActiveModel(model.getModelName());
        // Close the entire dropdown (hover + main popup) via the host-provided callback so the user sees an
        // immediate dismiss. Next time the dropdown opens, refreshBoundModelPickers (invoked from
        // setSelectedReasoningEffort) has updated the model row's suffix to reflect the newly selected effort.
        if (closeRequest != null) {
          closeRequest.run();
        }
      }
    };

    Control[] interactiveControls = descriptionLabel != null
        ? new Control[] { row, iconLabel, optionLabel, descriptionLabel }
        : new Control[] { row, iconLabel, optionLabel };
    for (Control c : interactiveControls) {
      c.addMouseListener(clickHandler);
    }
  }

  private static ModelService resolveModelService() {
    CopilotUi plugin = CopilotUi.getPlugin();
    if (plugin == null || plugin.getChatServiceManager() == null) {
      return null;
    }
    return plugin.getChatServiceManager().getModelService();
  }

  private void addKeyValueRow(Composite parent, String keyText, String valueText) {
    Composite row = createKeyValueRow(parent);
    ((GridData) row.getLayoutData()).verticalIndent = SECTION_SPACING;

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
    if (effortCheckIcon != null && !effortCheckIcon.isDisposed()) {
      effortCheckIcon.dispose();
      effortCheckIcon = null;
    }
  }

  /**
   * Returns the cached check-mark image used to indicate the selected thinking effort row, lazily loaded on first
   * access. The icon shares the asset used by the dropdown popup so the leading column lines up visually with the
   * checkmarks shown next to selected model items.
   */
  private static Image getCheckIcon(Composite parent) {
    if (effortCheckIcon == null || effortCheckIcon.isDisposed()) {
      effortCheckIcon = UiUtils.isDarkTheme()
          ? UiUtils.buildImageFromPngPath("/icons/dropdown/dropdown_complete_status_dark.png")
          : UiUtils.buildImageFromPngPath("/icons/dropdown/dropdown_complete_status.png");
      if (parent != null && !parent.isDisposed()) {
        parent.getDisplay().addListener(SWT.Dispose, e -> disposeStaticIcons());
      }
    }
    return effortCheckIcon;
  }

  private static boolean isPositive(Integer value) {
    return value != null && value > 0;
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
    warningLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
  }

  private Label createSecondaryTextLabel(Composite parent, String text) {
    Label label = new Label(parent, SWT.NONE);
    label.setText(text);
    setCssClass(label, POPUP_SECONDARY_TEXT_CLASS);
    return label;
  }

  private void setCssClass(Label control, String className) {
    if (stylingEngine != null) {
      stylingEngine.setClassname(control, className);
      stylingEngine.style(control);
    } else {
      control.setData(CssConstants.CSS_CLASS_NAME_KEY, className);
    }
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

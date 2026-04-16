// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.quickstart;

import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * GitHub Copilot feature page for the Quick Start dialog. Contains the main UI components and their update logic.
 */
public class FeaturePage extends Composite {

  /**
   * Enum for different feature types in the quick start dialog.
   */
  public enum Feature {
    AGENT, ASK, COMPLETION
  }

  private Label rightPanelContent;
  private Composite selectedCard;
  private Color normalBackgroundColor;
  private Color selectedBackgroundColor;
  private Image currentContentImage;
  private Display display;
  private boolean isDarkTheme;

  private static final int ALIGNED_MARGIN = 24;

  /**
   * Creates a FeaturePage with colors appropriate for the current theme.
   */
  public FeaturePage(Composite parent) {
    super(parent, SWT.NONE);

    // Cache commonly used objects
    this.display = Display.getCurrent();
    this.isDarkTheme = UiUtils.isDarkTheme();

    initializeColors();

    this.addDisposeListener(e -> disposeColors());

    createContent(parent);
  }

  /**
   * Initializes theme-appropriate colors.
   */
  private void initializeColors() {
    if (isDarkTheme) {
      // Dark theme colors
      normalBackgroundColor = new Color(display, 47, 48, 48); // #2F3030
      selectedBackgroundColor = new Color(display, 72, 72, 76); // #48484C
    } else {
      // Light theme colors
      normalBackgroundColor = new Color(display, 255, 255, 255); // #FFFFFF
      selectedBackgroundColor = new Color(display, 241, 241, 242); // #F1F1F2
    }
  }

  /**
   * Creates the main content area with all UI components.
   */
  private void createContent(Composite parent) {
    GridLayout containerLayout = new GridLayout(1, false);
    containerLayout.marginWidth = 0;
    containerLayout.verticalSpacing = 0;
    this.setLayout(containerLayout);
    this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-container");

    // Title section
    createTitleSection(this);

    // Main content area
    createMainContent(this);
  }

  private void createTitleSection(Composite parent) {
    GridLayout titleLayout = new GridLayout(1, false);
    titleLayout.marginWidth = 0;
    titleLayout.marginHeight = 0;
    titleLayout.marginBottom = ALIGNED_MARGIN;
    Composite titleComposite = new Composite(parent, SWT.NONE);
    titleComposite.setLayout(titleLayout);
    titleComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    titleComposite.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-container");

    Composite titleAndCloseComposite = new Composite(titleComposite, SWT.NONE);
    GridLayout titleAndCloseLayout = new GridLayout(2, false);
    titleAndCloseLayout.marginLeft = 20;
    titleAndCloseComposite.setLayout(titleAndCloseLayout);
    titleAndCloseComposite.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-container");
    titleAndCloseComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label titleLabel = new Label(titleAndCloseComposite, SWT.NONE);
    titleLabel.setText(Messages.quickStart_title);
    titleLabel.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-container");
    titleLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
    FontData fontData = new FontData();
    fontData.setHeight(14);
    fontData.setStyle(SWT.BOLD);
    Font titleFont = new Font(Display.getCurrent(), fontData);
    titleLabel.setFont(titleFont);
    titleLabel.addDisposeListener(e -> {
      if (titleFont != null && !titleFont.isDisposed()) {
        titleFont.dispose();
      }
    });

    createCloseButton(titleAndCloseComposite);

    Label subtitleLabel = new Label(titleComposite, SWT.WRAP | SWT.CENTER);
    subtitleLabel.setText(Messages.quickStart_description);
    subtitleLabel.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-container");
    subtitleLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
  }

  private void createMainContent(Composite parent) {
    GridLayout mainLayout = new GridLayout(2, false);
    mainLayout.horizontalSpacing = 12;
    mainLayout.marginHeight = 0;
    mainLayout.marginWidth = 0;
    Composite mainComposite = new Composite(parent, SWT.NONE);
    mainComposite.setLayout(mainLayout);
    GridData mainLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    mainLayoutData.minimumHeight = 327;
    mainComposite.setLayoutData(mainLayoutData);
    mainComposite.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-container");

    // Left panel with feature cards
    createLeftPanel(mainComposite);

    // Right panel
    createRightPanel(mainComposite);

    // Set initial selection to Agent feature after panel creation
    selectCard(selectedCard, Feature.AGENT);
  }

  private void createLeftPanel(Composite parent) {
    GridLayout leftPanelLayout = new GridLayout(1, false);
    leftPanelLayout.marginWidth = 0;
    leftPanelLayout.marginHeight = 0;
    leftPanelLayout.verticalSpacing = 8;
    Composite leftPanel = new Composite(parent, SWT.NONE);

    leftPanel.setLayout(leftPanelLayout);
    GridData leftPanelData = new GridData(SWT.FILL, SWT.FILL, false, true);
    leftPanelData.widthHint = 245;
    leftPanel.setLayoutData(leftPanelData);
    leftPanel.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-container");

    // Create clickable feature cards (Agent selected by default)
    selectedCard = createClickableFeatureCard(leftPanel, "/icons/github_copilot.png", Messages.quickStart_agent_title,
        Messages.quickStart_agent_description, Feature.AGENT);
    createClickableFeatureCard(leftPanel, "/icons/chat/chatview_icon_chat.png", Messages.quickStart_ask_title,
        Messages.quickStart_ask_description, Feature.ASK);
    createClickableFeatureCard(leftPanel, "/icons/chat/chatview_icon_code.png", Messages.quickStart_completion_title,
        Messages.quickStart_completion_description, Feature.COMPLETION);
  }

  private void createRightPanel(Composite parent) {
    Canvas rightPanel = createRoundedPanel(parent);
    GridData rightPanelData = new GridData(SWT.FILL, SWT.FILL, true, true);
    rightPanelData.widthHint = 520; // Set width to 520 pixels
    rightPanelData.heightHint = 327; // Set height to 327 pixels
    rightPanel.setLayoutData(rightPanelData);
    rightPanel.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-feature-card");

    // Content area that will change based on selection
    rightPanelContent = new Label(rightPanel, SWT.WRAP | SWT.CENTER);
    rightPanelContent.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-feature-card");
    rightPanelContent.addDisposeListener(e -> {
      if (currentContentImage != null && !currentContentImage.isDisposed()) {
        currentContentImage.dispose();
      }
    });

    // Add resize listener to properly size the content
    rightPanel.addListener(SWT.Resize, e -> {
      Rectangle bounds = rightPanel.getBounds();
      rightPanelContent.setBounds(25, 25, bounds.width - 30, bounds.height - 50);
    });

    updateRightPanelContent(Feature.AGENT);
  }

  private Composite createClickableFeatureCard(Composite parent, String imagePath, String title, String description,
      Feature feature) {
    // Create a canvas for custom rounded painting
    Canvas cardCanvas = new Canvas(parent, SWT.NONE);
    cardCanvas.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-container");

    GridData canvasData = new GridData(SWT.FILL, SWT.FILL, true, true);
    canvasData.widthHint = 245;
    canvasData.heightHint = 102;
    cardCanvas.setLayoutData(canvasData);
    cardCanvas.setSize(245, 102);

    // Add paint listener for rounded border
    cardCanvas.addPaintListener(new PaintListener() {
      @Override
      public void paintControl(PaintEvent e) {
        GC gc = e.gc;
        // Set anti-aliasing for smoother curves
        gc.setAntialias(SWT.ON);

        // Get background color from stored data, fallback to canvas background
        Color bgColor = (Color) cardCanvas.getData("bgColor");
        if (bgColor == null) {
          bgColor = cardCanvas.getBackground();
        }

        // Fill rounded rectangle background with proper color
        gc.setBackground(bgColor);
        Rectangle bounds = cardCanvas.getBounds();
        gc.fillRoundRectangle(2, 2, bounds.width - 5, bounds.height - 5, 15, 15);
      }
    });

    // Create the content composite inside the canvas
    GridLayout cardLayout = new GridLayout(1, false);
    cardLayout.marginWidth = 0;
    cardLayout.marginHeight = 0;
    cardLayout.verticalSpacing = 0;
    Composite card = new Composite(cardCanvas, SWT.NONE);
    card.setLayout(cardLayout);
    card.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-feature-card");

    // Add resize listener to properly size the card content
    cardCanvas.addListener(SWT.Resize, e -> {
      Rectangle bounds = cardCanvas.getBounds();
      Point cardSize = card.computeSize(bounds.width - 16, SWT.DEFAULT);
      int x = 8;
      int y = (bounds.height - cardSize.y) / 2; // Center vertically
      card.setBounds(x, y, bounds.width - 16, cardSize.y);
    });

    // Make components clickable
    MouseAdapter clickHandler = new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        selectCard(card, feature);
      }
    };

    addClickableToControls(clickHandler, cardCanvas, card);

    // Icon and title composite
    GridLayout iconTitleLayout = new GridLayout(2, false);
    iconTitleLayout.marginHeight = 0;
    iconTitleLayout.marginWidth = 0;
    iconTitleLayout.horizontalSpacing = 8;
    Composite iconTitleComposite = new Composite(card, SWT.NONE);
    iconTitleComposite.setLayout(iconTitleLayout);
    iconTitleComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    iconTitleComposite.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-feature-card");

    // Icon - Load image from path using UiUtils
    Image iconImage = UiUtils.buildImageFromPngPath(imagePath);
    Label iconLabel = new Label(iconTitleComposite, SWT.NONE);
    iconLabel.setImage(iconImage);
    iconLabel.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-feature-card");
    GridData iconData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
    iconData.widthHint = 20;
    iconData.heightHint = 20;
    iconLabel.setLayoutData(iconData);

    // Dispose image when label is disposed
    iconLabel.addDisposeListener(e -> {
      if (iconImage != null && !iconImage.isDisposed()) {
        iconImage.dispose();
      }
    });

    // Make icon clickable too
    addClickableToControls(clickHandler, iconLabel);

    Label titleLabel = new Label(iconTitleComposite, SWT.NONE);
    titleLabel.setText(title);
    titleLabel.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-feature-card");
    titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Make title clickable too
    addClickableToControls(clickHandler, titleLabel);

    // Make title bold
    FontData[] fontDataArray = titleLabel.getFont().getFontData();
    for (FontData fontData : fontDataArray) {
      fontData.setStyle(SWT.BOLD);
      fontData.setHeight(PlatformUtils.isMac() ? 15 : 10);
    }
    Font cardTitleFont = new Font(display, fontDataArray);
    titleLabel.setFont(cardTitleFont);

    // Add dispose listener for title font
    titleLabel.addDisposeListener(e -> {
      if (cardTitleFont != null && !cardTitleFont.isDisposed()) {
        cardTitleFont.dispose();
      }
    });

    // Description composite
    GridLayout descLayout = new GridLayout(1, false);
    descLayout.marginHeight = 0;
    descLayout.marginWidth = 0;
    descLayout.marginLeft = 28; // Align with title text (icon width + spacing)
    Composite descComposite = new Composite(card, SWT.NONE);
    descComposite.setLayout(descLayout);
    descComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    descComposite.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-feature-card");

    // Make description composite clickable
    addClickableToControls(clickHandler, descComposite);

    Label descLabel = new Label(descComposite, SWT.WRAP);
    descLabel.setText(description);
    descLabel.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-feature-card");
    descLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    if (PlatformUtils.isMac()) {
      // Set font height for description label on macOS
      FontData[] descFontDataArray = descLabel.getFont().getFontData();
      for (FontData fontData : descFontDataArray) {
        fontData.setHeight(13);
      }
      Font descFont = new Font(Display.getCurrent(), descFontDataArray);
      descLabel.setFont(descFont);

      // Add dispose listener for description font
      descLabel.addDisposeListener(e -> {
        if (descFont != null && !descFont.isDisposed()) {
          descFont.dispose();
        }
      });
    }

    // Make description clickable too
    addClickableToControls(clickHandler, descLabel);

    return card;
  }

  private Canvas createRoundedPanel(Composite parent) {
    Canvas canvas = new Canvas(parent, SWT.NONE);
    canvas.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-feature-card");

    canvas.addPaintListener(new PaintListener() {
      @Override
      public void paintControl(PaintEvent e) {
        GC gc = e.gc;

        // Set anti-aliasing for smoother curves
        gc.setAntialias(SWT.ON);
        gc.setBackground(selectedBackgroundColor);

        Rectangle bounds = canvas.getBounds();
        gc.fillRoundRectangle(2, 2, bounds.width - 5, bounds.height - 5, 15, 15);
      }
    });

    return canvas;
  }

  private void selectCard(Composite card, Feature feature) {
    // Update the previously selected card back to normal
    if (selectedCard != null) {
      updateControlBackground(selectedCard, false);
    }

    // Update the newly selected card
    selectedCard = card;
    updateControlBackground(card, true);

    // Update right panel content
    updateRightPanelContent(feature);
  }

  private void updateControlBackground(Control card, boolean selected) {
    Color backgroundColor = selected ? selectedBackgroundColor : normalBackgroundColor;

    // Find the parent canvas and set the bgColor data there
    Control parent = card.getParent();
    if (parent instanceof Canvas) {
      parent.setData("bgColor", backgroundColor);
      parent.redraw(); // Redraw the canvas to trigger the paint listener
    }

    // Update CSS ID for styling consistency
    updateChildrenControlBackground(card, selected);

    IStylingEngine engine = PlatformUI.getWorkbench().getService(IStylingEngine.class);
    if (engine != null) {
      engine.setId(card, selected ? "quick-start-feature-card-selected" : "quick-start-feature-card");
    }
  }

  private void updateChildrenControlBackground(Control control, boolean selected) {
    control.setData(CSSSWTConstants.CSS_ID_KEY,
        selected ? "quick-start-feature-card-selected" : "quick-start-feature-card");
    if (control instanceof Composite composite) {
      for (Control child : composite.getChildren()) {
        updateChildrenControlBackground(child, selected);
      }
    }
  }

  private void updateRightPanelContent(Feature feature) {
    // Dispose previous image if it exists
    if (currentContentImage != null && !currentContentImage.isDisposed()) {
      currentContentImage.dispose();
      currentContentImage = null;
    }

    String imagePath;
    switch (feature) {
      case ASK:
        imagePath = "/intro/quickstart/quick_start_ask.png";
        break;
      case COMPLETION:
        imagePath = "/intro/quickstart/quick_start_completion.png";
        break;
      case AGENT:
      default:
        imagePath = "/intro/quickstart/quick_start_agent.png";
    }

    // Load and set the new image
    currentContentImage = UiUtils.buildImageFromPngPath(imagePath);
    rightPanelContent.setImage(currentContentImage);
    rightPanelContent.requestLayout();
  }

  /**
   * Closes the dialog containing this feature page.
   */
  private void closeDialog() {
    // Find the parent shell and close it
    getShell().close();
  }

  /**
   * Disposes of all color resources.
   */
  private void disposeColors() {
    if (normalBackgroundColor != null && !normalBackgroundColor.isDisposed()) {
      normalBackgroundColor.dispose();
    }
    if (selectedBackgroundColor != null && !selectedBackgroundColor.isDisposed()) {
      selectedBackgroundColor.dispose();
    }
  }

  /**
   * Creates a close button with hover effects.
   */
  private Label createCloseButton(Composite parent) {
    Image normalImage = UiUtils
        .buildImageFromPngPath(isDarkTheme ? "/intro/quickstart/close_dark.png" : "/intro/quickstart/close_light.png");
    Label closeButton = new Label(parent, SWT.NONE);
    closeButton.setImage(normalImage);
    closeButton.setData(CSSSWTConstants.CSS_ID_KEY, "quick-start-container");
    GridData closeButtonData = new GridData(SWT.RIGHT, SWT.TOP, false, false);
    closeButtonData.verticalIndent = 0;
    closeButton.setLayoutData(closeButtonData);
    closeButton.setToolTipText(Messages.quickStart_closeButton_tooltip);

    // Add click listener to close the dialog
    closeButton.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        closeDialog();
      }
    });

    Image hoverImage = UiUtils.buildImageFromPngPath(
        isDarkTheme ? "/intro/quickstart/close_hover_dark.png" : "/intro/quickstart/close_hover_light.png");
    // Add dispose listener for close button images
    closeButton.addDisposeListener(e -> {
      if (normalImage != null && !normalImage.isDisposed()) {
        normalImage.dispose();
      }
      if (hoverImage != null && !hoverImage.isDisposed()) {
        hoverImage.dispose();
      }
    });

    // Add hover effect for close button
    closeButton.addMouseTrackListener(new MouseTrackAdapter() {
      @Override
      public void mouseEnter(MouseEvent e) {
        closeButton.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
        closeButton.setImage(hoverImage);
        closeButton.redraw();
      }

      @Override
      public void mouseExit(MouseEvent e) {
        closeButton.setCursor(null);
        closeButton.setImage(normalImage);
        closeButton.redraw();
      }
    });

    return closeButton;
  }

  /**
   * Adds a click handler to multiple controls.
   */
  private void addClickableToControls(MouseAdapter clickHandler, Control... controls) {
    for (Control control : controls) {
      control.addMouseListener(clickHandler);
    }
  }
}
package com.microsoft.copilot.eclipse.ui.quickstart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
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
  private Color fontColor;
  private Color featureCardFontColor;
  private Image currentContentImage;

  private static final int ALIGNED_MARGIN = 24;

  /**
   * Creates a FeaturePage with colors appropriate for the current theme.
   */
  public FeaturePage(Composite parent) {
    super(parent, SWT.NONE);
    if (UiUtils.isDarkTheme()) {
      // Dark theme colors
      normalBackgroundColor = new Color(Display.getCurrent(), 47, 48, 48); // #2F3030
      selectedBackgroundColor = new Color(Display.getCurrent(), 72, 72, 76); // #48484C
      fontColor = new Color(Display.getCurrent(), 237, 238, 238); // #EDEEEE
      featureCardFontColor = new Color(Display.getCurrent(), 237, 238, 238); // #EDEEEE
    } else {
      // Light theme colors
      normalBackgroundColor = new Color(Display.getCurrent(), 255, 255, 255); // #FFFFFF
      selectedBackgroundColor = new Color(Display.getCurrent(), 241, 241, 242); // #F1F1F2
      fontColor = new Color(Display.getCurrent(), 90, 90, 90); // #5A5A5A
      featureCardFontColor = new Color(Display.getCurrent(), 0, 0, 0); // #000000
    }

    this.addDisposeListener(e -> disposeColors());

    createContent(parent);
  }

  /**
   * Creates the main content area with all UI components.
   */
  private void createContent(Composite parent) {
    GridLayout containerLayout = new GridLayout(1, false);
    containerLayout.marginWidth = ALIGNED_MARGIN;
    containerLayout.verticalSpacing = ALIGNED_MARGIN;
    this.setLayout(containerLayout);
    this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    this.setBackground(normalBackgroundColor);

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
    titleComposite.setBackground(normalBackgroundColor);

    Label titleLabel = new Label(titleComposite, SWT.NONE);
    titleLabel.setText(Messages.quickStart_title);
    titleLabel.setBackground(normalBackgroundColor);
    titleLabel.setForeground(fontColor);
    titleLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
    FontData fontData = new FontData();
    fontData.setHeight(16);
    fontData.setStyle(SWT.BOLD);
    Font titleFont = new Font(Display.getCurrent(), fontData);
    titleLabel.setFont(titleFont); // This line applies the bold font to the label

    // Add dispose listener for title font
    titleLabel.addDisposeListener(e -> {
      if (titleFont != null && !titleFont.isDisposed()) {
        titleFont.dispose();
      }
    });

    Label subtitleLabel = new Label(titleComposite, SWT.WRAP | SWT.CENTER);
    subtitleLabel.setText(Messages.quickStart_description);
    subtitleLabel.setBackground(normalBackgroundColor);
    subtitleLabel.setForeground(fontColor);
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
    mainComposite.setBackground(normalBackgroundColor);

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
    leftPanel.setBackground(normalBackgroundColor);

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

    // Content area that will change based on selection
    rightPanelContent = new Label(rightPanel, SWT.WRAP | SWT.CENTER);
    rightPanelContent.setBackground(normalBackgroundColor);
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
    cardCanvas.setBackground(normalBackgroundColor);

    GridData canvasData = new GridData(SWT.FILL, SWT.FILL, true, true);
    cardCanvas.setLayoutData(canvasData);

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
    card.setBackground(normalBackgroundColor);

    // Add resize listener to properly size the card content
    cardCanvas.addListener(SWT.Resize, e -> {
      Rectangle bounds = cardCanvas.getBounds();
      Point cardSize = card.computeSize(bounds.width - 16, SWT.DEFAULT);
      int x = 8;
      int y = (bounds.height - cardSize.y) / 2; // Center vertically
      card.setBounds(x, y, bounds.width - 16, cardSize.y);
    });

    // Make the canvas clickable
    cardCanvas.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        selectCard(card, feature);
      }
    });

    // Make the content composite clickable
    card.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        selectCard(card, feature);
      }
    });

    // Icon and title composite
    GridLayout iconTitleLayout = new GridLayout(2, false);
    iconTitleLayout.marginHeight = 0;
    iconTitleLayout.marginWidth = 0;
    iconTitleLayout.horizontalSpacing = 8;
    Composite iconTitleComposite = new Composite(card, SWT.NONE);
    iconTitleComposite.setLayout(iconTitleLayout);
    iconTitleComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    iconTitleComposite.setBackground(normalBackgroundColor);

    // Icon - Load image from path using UiUtils
    Image iconImage = UiUtils.buildImageFromPngPath(imagePath);
    Label iconLabel = new Label(iconTitleComposite, SWT.NONE);
    iconLabel.setImage(iconImage);
    iconLabel.setBackground(normalBackgroundColor);
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
    iconLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        selectCard(card, feature);
      }
    });

    Label titleLabel = new Label(iconTitleComposite, SWT.NONE);
    titleLabel.setText(title);
    titleLabel.setBackground(normalBackgroundColor);
    titleLabel.setForeground(featureCardFontColor);
    titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Make title clickable too
    titleLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        selectCard(card, feature);
      }
    });

    // Make title bold
    FontData[] fontDataArray = titleLabel.getFont().getFontData();
    for (FontData fontData : fontDataArray) {
      fontData.setStyle(SWT.BOLD);
      fontData.setHeight(PlatformUtils.isMac() ? 15 : 10);
    }
    Font titleFont = new Font(Display.getCurrent(), fontDataArray);
    titleLabel.setFont(titleFont);

    // Add dispose listener for title font
    titleLabel.addDisposeListener(e -> {
      if (titleFont != null && !titleFont.isDisposed()) {
        titleFont.dispose();
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
    descComposite.setBackground(normalBackgroundColor);

    // Make description composite clickable
    descComposite.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        selectCard(card, feature);
      }
    });

    Label descLabel = new Label(descComposite, SWT.WRAP);
    descLabel.setText(description);
    descLabel.setBackground(normalBackgroundColor);
    descLabel.setForeground(featureCardFontColor);
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
    descLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        selectCard(card, feature);
      }
    });

    // Store reference to canvas for background updates
    card.setData("canvas", cardCanvas);

    return card;
  }

  private Canvas createRoundedPanel(Composite parent) {
    Canvas canvas = new Canvas(parent, SWT.NONE);
    canvas.setBackground(normalBackgroundColor);

    canvas.addPaintListener(new PaintListener() {
      @Override
      public void paintControl(PaintEvent e) {
        GC gc = e.gc;
        Rectangle bounds = canvas.getBounds();

        // Set anti-aliasing for smoother curves
        gc.setAntialias(SWT.ON);

        // Fill rounded rectangle background first
        gc.setBackground(selectedBackgroundColor);
        gc.fillRoundRectangle(2, 2, bounds.width - 5, bounds.height - 5, 15, 15);
      }
    });

    return canvas;
  }

  private void selectCard(Composite card, Feature feature) {
    // Update the previously selected card back to normal
    if (selectedCard != null) {
      updateCardSelection(selectedCard, false);
    }

    // Update the newly selected card
    selectedCard = card;
    updateCardSelection(card, true);

    // Update right panel content
    updateRightPanelContent(feature);
  }

  private void updateCardSelection(Composite card, boolean selected) {
    Color bgColor = selected ? selectedBackgroundColor : normalBackgroundColor;

    // Update card background
    card.setBackground(bgColor);

    // Update the canvas background and trigger repaint
    Canvas canvas = (Canvas) card.getData("canvas");
    if (canvas != null) {
      // Store the background color as data instead of setting it directly
      canvas.setData("bgColor", bgColor);
      canvas.redraw(); // Trigger repaint with new background color
    }

    // Update all child controls recursively
    updateControlBackground(card, bgColor);
  }

  private void updateControlBackground(Control control, Color bgColor) {
    control.setBackground(bgColor);
    // Also update foreground color for labels to maintain proper contrast
    if (control instanceof Label) {
      ((Label) control).setForeground(featureCardFontColor);
    }
    if (control instanceof Composite) {
      Composite composite = (Composite) control;
      for (Control child : composite.getChildren()) {
        updateControlBackground(child, bgColor);
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
        imagePath = "/icons/quickStart/quick_start_ask.png";
        break;
      case COMPLETION:
        imagePath = "/icons/quickStart/quick_start_completion.png";
        break;
      case AGENT:
      default:
        imagePath = "/icons/quickStart/quick_start_agent.png";
    }

    // Load and set the new image
    currentContentImage = UiUtils.buildImageFromPngPath(imagePath);
    rightPanelContent.setImage(currentContentImage);
    rightPanelContent.getParent().layout();
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
    if (fontColor != null && !fontColor.isDisposed()) {
      fontColor.dispose();
    }
    if (featureCardFontColor != null && !featureCardFontColor.isDisposed()) {
      featureCardFontColor.dispose();
    }
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CheckQuotaResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.QuotaChangeNotification;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.QuotaSnapshotParams;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.preferences.UsageStatusPreferencePage;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.PreferencesUtils;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A widget that displays a quota usage indicator in the chat view header.
 *
 * <p>For non-CBCE plans (CFI), it shows a static icon (active/approaching/exhausted) based on session/weekly usage. For
 * CBCE plans (business/enterprise), it renders a dynamic 14x14 pie chart based on premium interactions, or a static
 * icon when the quota is unlimited.
 */
public class QuotaUsageIndicator extends Composite {

  private static final double EXHAUSTED_THRESHOLD = 95;
  private static final int APPROACHING_THRESHOLD = 75;
  private static final int START_ANGLE = 90;
  private static final int PIE_SIZE = 14;
  private static final int PIE_BORDER_WIDTH = 1;

  // Minimum full size to draw the pie chart with correct effect.
  // The visual size of the pie itself is still 14x14 with a 2-pixel border.
  private static final int PIE_FULL_SIZE = 18;

  private double usedPercent;
  private RenderMode renderMode = RenderMode.STATIC;
  private Image currentImage;
  private Canvas pieCanvas;
  private Label percentLabel;
  private String labelText;
  private final QuotaIndicatorPopup popup = new QuotaIndicatorPopup();
  private IEventBroker eventBroker;
  private EventHandler quotaStatusChangedHandler;

  /**
   * Static mode shows a pre-rendered icon; dynamic mode custom-paints a pie chart.
   */
  private enum RenderMode {
    STATIC, DYNAMIC
  }

  /**
   * Creates a new quota indicator widget.
   *
   * @param parent the parent composite
   * @param style the SWT style bits
   */
  public QuotaUsageIndicator(Composite parent, int style) {
    super(parent, style);
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.horizontalSpacing = 4;
    setLayout(layout);
    addInteractionListeners(this);

    this.pieCanvas = new Canvas(this, SWT.NONE);
    GridData canvasData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
    canvasData.widthHint = PIE_FULL_SIZE;
    canvasData.heightHint = PIE_FULL_SIZE;
    this.pieCanvas.setLayoutData(canvasData);
    this.pieCanvas.addPaintListener(this::paint);
    addInteractionListeners(this.pieCanvas);

    this.percentLabel = new Label(this, SWT.NONE);
    this.percentLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    this.percentLabel.setVisible(false);
    GridData labelData = (GridData) this.percentLabel.getLayoutData();
    labelData.exclude = true;
    addInteractionListeners(this.percentLabel);

    setVisible(false);

    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    this.quotaStatusChangedHandler = event -> {
      Object data = event.getProperty(IEventBroker.DATA);
      if (data instanceof QuotaChangeNotification notification) {
        SwtUtils.invokeOnDisplayThreadAsync(() -> {
          if (!isDisposed()) {
            update(notification);
            setVisible(true);
            getParent().requestLayout();
          }
        }, this);
      }
    };
    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_QUOTA_SNAPSHOT_CHANGED, this.quotaStatusChangedHandler);

    // Trigger initial quota load so the indicator is populated immediately
    CopilotCore.getPlugin().getAuthStatusManager().checkQuota();

    this.addDisposeListener(e -> {
      if (this.eventBroker != null && this.quotaStatusChangedHandler != null) {
        this.eventBroker.unsubscribe(this.quotaStatusChangedHandler);
      }
      popup.close();
      disposeImage();
    });
  }

  /**
   * Update the indicator with new quota data.
   *
   * @param notification the quota change notification from the language server
   */
  private void update(QuotaChangeNotification notification) {
    CopilotPlan plan = notification.copilotPlan();
    boolean isCbce = plan == CopilotPlan.business || plan == CopilotPlan.enterprise;

    popup.updateNotification(notification);

    if (isCbce) {
      updateCopilotBusinessAndEnterprise(notification);
    } else {
      updateCopilotForIndividual(notification);
    }

    updatePercentLabel();
    this.pieCanvas.redraw();
    this.requestLayout();
  }

  private void updateCopilotBusinessAndEnterprise(QuotaChangeNotification notification) {
    QuotaSnapshotParams premium = notification.premiumInteractions();
    this.renderMode = RenderMode.DYNAMIC;

    if (premium == null) {
      // No premium interactions data yet — show default pie at 0%
      this.usedPercent = 0;
      disposeImage();
    } else if (premium.unlimited()) {
      // Unlimited — show static icon with "Unlimited" label
      this.renderMode = RenderMode.STATIC;
      this.usedPercent = 0;
      this.labelText = Messages.quota_popup_unlimited;
      disposeImage();
      String theme = UiUtils.isDarkTheme() ? "dark" : "light";
      this.currentImage = UiUtils.buildImageFromPngPath("/icons/quota/quota_active_" + theme + ".png");
    } else {
      // Limited — dynamic pie based on premium interactions
      this.usedPercent = 100.0 - premium.percentRemaining();
      this.labelText = (int) Math.round(usedPercent) + "%";
      disposeImage();
    }
  }

  private void updateCopilotForIndividual(QuotaChangeNotification notification) {
    this.renderMode = RenderMode.STATIC;
    this.labelText = null;

    QuotaSnapshotParams immediate = notification.immediateUsageInterval();
    QuotaSnapshotParams extended = notification.extendedUsageInterval();

    // Show active icon if both intervals are unlimited
    boolean immediateUnlimited = immediate != null && immediate.unlimited();
    boolean extendedUnlimited = extended != null && extended.unlimited();
    if (immediateUnlimited && extendedUnlimited) {
      String theme = UiUtils.isDarkTheme() ? "dark" : "light";
      this.currentImage = UiUtils.buildImageFromPngPath("/icons/quota/quota_active_" + theme + ".png");
      return;
    }

    // Otherwise show quota status icon based on higher of session vs weekly usage
    double sessionUsed = immediate != null && !immediateUnlimited ? 100.0 - immediate.percentRemaining() : 0.0;
    double weeklyUsed = extended != null && !extendedUnlimited ? 100.0 - extended.percentRemaining() : 0.0;
    this.usedPercent = Math.max(sessionUsed, weeklyUsed);

    disposeImage();
    String theme = UiUtils.isDarkTheme() ? "dark" : "light";
    String status;
    if (usedPercent >= EXHAUSTED_THRESHOLD) {
      status = "exhausted";
    } else if (usedPercent >= APPROACHING_THRESHOLD) {
      status = "approaching";
    } else {
      status = "active";
    }
    this.currentImage = UiUtils.buildImageFromPngPath("/icons/quota/quota_" + status + "_" + theme + ".png");
  }

  private void updatePercentLabel() {
    boolean showLabel = this.labelText != null;
    this.percentLabel.setVisible(showLabel);
    ((GridData) this.percentLabel.getLayoutData()).exclude = !showLabel;
    if (showLabel) {
      this.percentLabel.setText(this.labelText);
      this.percentLabel.setForeground(CssConstants.getTopBannerTextColor(getDisplay()));
      this.percentLabel.requestLayout();
    }
  }

  private void paint(PaintEvent e) {
    if (this.renderMode == RenderMode.STATIC) {
      paintStaticIcon(e.gc);
    } else {
      paintDynamicPie(e.gc);
    }
  }

  private void paintStaticIcon(GC gc) {
    if (currentImage != null && !currentImage.isDisposed()) {
      Rectangle bounds = pieCanvas.getClientArea();
      Rectangle imgBounds = currentImage.getBounds();
      int x = (bounds.width - imgBounds.width) / 2;
      int y = (bounds.height - imgBounds.height) / 2;
      gc.drawImage(currentImage, x, y);
    }
  }

  private void paintDynamicPie(GC gc) {
    gc.setAdvanced(true);
    gc.setAntialias(SWT.ON);

    Rectangle bounds = pieCanvas.getClientArea();
    int x = (bounds.width - PIE_SIZE) / 2;
    int y = (bounds.height - PIE_SIZE) / 2;

    Color pieColor = getDynamicPieColor();

    // Draw border circle
    gc.setForeground(pieColor);
    gc.setLineWidth(PIE_BORDER_WIDTH);
    gc.drawOval(x, y, PIE_SIZE - 1, PIE_SIZE - 1);

    // Draw filled arc for usage inside the border
    if (usedPercent > 0) {
      int inset = PIE_BORDER_WIDTH;
      int innerSize = PIE_SIZE - 2 * inset;
      gc.setBackground(pieColor);
      int arcAngle = (int) Math.round(-usedPercent * 360.0 / 100.0);
      gc.fillArc(x + inset, y + inset, innerSize, innerSize, START_ANGLE, arcAngle);
    }
  }

  private Color getDynamicPieColor() {
    if (usedPercent >= EXHAUSTED_THRESHOLD) {
      return CssConstants.getQuotaExhaustedColor(getDisplay());
    } else if (usedPercent >= APPROACHING_THRESHOLD) {
      return CssConstants.getQuotaApproachingColor(getDisplay());
    }
    return CssConstants.getQuotaPieActiveColor(getDisplay());
  }

  private void addInteractionListeners(Control control) {
    control.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    control.addMouseTrackListener(new MouseTrackAdapter() {
      @Override
      public void mouseEnter(MouseEvent e) {
        if (popup.isOpen()) {
          return;
        }
        CheckQuotaResult status = CopilotCore.getPlugin().getAuthStatusManager().getQuotaStatus();
        if (status.getCopilotPlan() != null) {
          popup.open(QuotaUsageIndicator.this, QuotaChangeNotification.fromCheckQuotaResult(status));
        }
      }
    });
    control.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {
        Shell shell = getShell();
        popup.close();
        PreferencesUtil
            .createPreferenceDialogOn(shell, UsageStatusPreferencePage.ID, PreferencesUtils.getAllPreferenceIds(), null)
            .open();
      }
    });
  }

  private void disposeImage() {
    if (this.currentImage != null && !this.currentImage.isDisposed()) {
      this.currentImage.dispose();
      this.currentImage = null;
    }
  }
}

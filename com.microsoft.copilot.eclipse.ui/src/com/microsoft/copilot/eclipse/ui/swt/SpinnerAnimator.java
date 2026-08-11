// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.swt;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.ui.CopilotImages;

/**
 * Drives a rotating spinner animation on a target {@link Label}.
 *
 * <p>Frames are taken from the image registry ({@link CopilotImages}) and must never be disposed.
 * {@link #stop()} only detaches the current frame from the label so the caller can swap
 * in a final image (e.g. a "completed" icon) on the same label.
 *
 * <p>The animator hooks the target label's dispose listener so the animation is cancelled
 * automatically when the label goes away.
 */
public final class SpinnerAnimator {
  /** Total number of frames. */
  private static final int TOTAL_FRAMES = CopilotImages.SPINNER_FRAME_COUNT;
  /** Per-frame interval in milliseconds. */
  private static final int FRAME_INTERVAL_MS = 100;

  private final Label target;
  private Image currentFrameImage;
  private int currentFrame = 1;
  private Runnable animationRunnable;

  /**
   * Create an animator that will rotate spinner frames on the given label.
   *
   * @param target the label to update with each frame; must not be {@code null}
   */
  public SpinnerAnimator(Label target) {
    this.target = target;
    target.addDisposeListener(e -> stop());
  }

  /**
   * Start (or restart) the animation. Safe to call when already running — the existing animation
   * is cancelled first.
   */
  public void start() {
    if (target.isDisposed()) {
      return;
    }
    stop();
    currentFrame = 1;
    final Display display = target.getDisplay();
    animationRunnable = new Runnable() {
      @Override
      public void run() {
        if (target.isDisposed()) {
          return;
        }
        currentFrameImage = buildFrame(currentFrame);
        target.setImage(currentFrameImage);
        // Request layout so the icon scale stays correct as frames change.
        target.requestLayout();
        currentFrame = (currentFrame % TOTAL_FRAMES) + 1;
        display.timerExec(FRAME_INTERVAL_MS, this);
      }
    };
    display.timerExec(0, animationRunnable);
  }

  /**
   * Stop the animation and detach the current frame from the target label.
   * Frames owned by the image registry are never disposed. Safe to call repeatedly.
   */
  public void stop() {
    if (animationRunnable != null && !target.isDisposed()) {
      target.getDisplay().timerExec(-1, animationRunnable);
    }
    animationRunnable = null;
    // Detach the registry-owned frame from the label. Callers that want a final icon
    // (completed/cancelled/error) set it immediately after stop(), avoiding any visible flicker.
    if (!target.isDisposed() && target.getImage() == currentFrameImage) {
      target.setImage(null);
    }
    currentFrameImage = null;
  }

  private static Image buildFrame(int frame) {
    return CopilotImages.getSpinnerFrame(frame);
  }
}

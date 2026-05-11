// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.swt;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Drives a rotating spinner animation on a target {@link Label}.
 *
 * <p>The animator owns the lifecycle of the per-frame {@link Image} resources: each new frame is
 * loaded, the previous one is disposed, and {@link #stop()} guarantees that the label no longer
 * holds a reference to a disposed image. After {@link #stop()} the caller is free to swap in a
 * final image (e.g. a "completed" icon) on the same label.
 *
 * <p>The animator hooks the target label's dispose listener so the animation is cancelled and the
 * running frame is freed automatically when the label goes away.
 */
public final class SpinnerAnimator {
  /** Total number of frames in the spinner animation under {@code /icons/spinner/}. */
  private static final int TOTAL_FRAMES = 8;
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
        // Dispose the previous frame before loading the next one.
        if (currentFrameImage != null && !currentFrameImage.isDisposed()) {
          currentFrameImage.dispose();
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
   * Stop the animation and release the frame image. Detaches the image from the target label
   * before disposing it so the label never points at a disposed image. Safe to call repeatedly.
   */
  public void stop() {
    if (animationRunnable != null && !target.isDisposed()) {
      target.getDisplay().timerExec(-1, animationRunnable);
    }
    animationRunnable = null;
    // Detach the image from the label before disposing it so the label never points at a
    // disposed image. Callers that want a final icon (completed/cancelled/error) set it
    // immediately after stop(), avoiding any visible flicker.
    if (!target.isDisposed() && target.getImage() == currentFrameImage) {
      target.setImage(null);
    }
    if (currentFrameImage != null && !currentFrameImage.isDisposed()) {
      currentFrameImage.dispose();
    }
    currentFrameImage = null;
  }

  private static Image buildFrame(int frame) {
    return UiUtils.buildImageFromPngPath(String.format("/icons/spinner/%d.png", frame));
  }
}

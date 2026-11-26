// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.ui.CopilotImages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Manages avatars for chat messages.
 */
public class AvatarService {
  private static final String AVATAR_URL = "https://avatars.githubusercontent.com/%s?s=24&v=4";
  private static final Duration RETRY_DELAY = Duration.ofMinutes(5);
  private Map<String, Image> avatarCache = new ConcurrentHashMap<>();
  private Map<String, Job> jobs = new ConcurrentHashMap<>();
  private Map<String, Instant> retryTimes = new ConcurrentHashMap<>();

  private Image defaultGithubAvatar;
  private Image defaultUserAvatar;
  private AuthStatusManager authStatusManager;
  private IEventBroker eventBroker;
  private EventHandler authStatusChangedEventHandler;

  /**
   * Avatar Service.
   */
  public AvatarService(AuthStatusManager authStatusManager) {
    this.authStatusManager = authStatusManager;
    this.defaultGithubAvatar = CopilotImages.getImage(CopilotImages.IMG_CHAT_COPILOT_AVATAR);
    this.defaultUserAvatar = CopilotImages.getImage(CopilotImages.IMG_CHAT_USER_AVATAR);
    this.authStatusChangedEventHandler = event -> {
      Object property = event.getProperty(IEventBroker.DATA);
      if (property instanceof CopilotStatusResult statusResult && statusResult.isSignedIn()) {
        this.getAvatarForCurrentUser(SwtUtils.getDisplay());
      }

    };

    eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    if (eventBroker != null) {
      eventBroker.subscribe(CopilotEventConstants.TOPIC_AUTH_STATUS_CHANGED, authStatusChangedEventHandler);
    }

    this.getAvatarForCurrentUser(SwtUtils.getDisplay());
  }

  /**
   * Gets the avatar for the current user or a default user icon.
   * The returned {@link Image} must never be disposed by the caller.
   *
   * @param display the display
   * @return the avatar
   */
  public Image getAvatarForCurrentUser(Display display) {
    final String user = this.authStatusManager.getUserName();
    return getAvatar(display, user);
  }

  /**
   * Gets the avatar for the copilot.
   */
  public Image getAvatarForCopilot() {
    return defaultGithubAvatar;
  }

  /**
   * Gets the avatar icon for a user or a default user icon.
   * The returned {@link Image} must never be disposed by the caller.
   *
   * @param display the display
   * @param user the user
   * @return the avatar
   */
  public synchronized Image getAvatar(Display display, String user) {
    if (StringUtils.isBlank(user)) {
      return defaultUserAvatar;
    }

    Image image = avatarCache.get(user);
    if (image != null) {
      return image;
    } else {
      Instant retryTime = retryTimes.get(user);
      if (!jobs.containsKey(user) && (retryTime == null || !Instant.now().isBefore(retryTime))) {
        Job downloadJob = new Job("Download avatar for " + user) {
          @Override
          protected IStatus run(IProgressMonitor monitor) {
            try {
              URL url = new URL(String.format(AVATAR_URL, user));
              Image downloadedImage;
              try (var stream = url.openStream()) {
                downloadedImage = new Image(display, stream);
              }
              Image result = downloadedImage;
              // as the image is not always 24x24 even we set it in parameters, resize here if not
              if (result.getBounds().width != 24 || result.getBounds().height != 24) {
                result = UiUtils.resizeImage(display, downloadedImage, 24, 24);
                downloadedImage.dispose();
              }
              avatarCache.put(user, result);
            } catch (IOException | SWTException e) {
              CopilotCore.LOGGER.error(e);
            } finally {
              jobs.remove(user);
            }
            return Status.OK_STATUS;
          }
        };
        jobs.put(user, downloadJob);
        retryTimes.put(user, Instant.now().plus(RETRY_DELAY));
        downloadJob.schedule();
      }
      return defaultUserAvatar;
    }
  }

  /**
   * Disposes the resources.
   */
  public void dispose() {
    avatarCache.values().forEach(Image::dispose);
    jobs.values().forEach(Job::cancel);
    if (this.eventBroker != null) {
      this.eventBroker.unsubscribe(this.authStatusChangedEventHandler);
    }
  }

}

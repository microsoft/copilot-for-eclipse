package com.microsoft.copilot.eclipse.ui.chat.services;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Manages avatars for chat messages.
 */
public class AvatarService {
  private static final String AVATAR_URL = "https://avatars.githubusercontent.com/%s?s=24&v=4";
  private static final String DEFAULT_COPILOT_AVATAR_NAME = "/icons/chat/chat_message_copilot_avatar.png";
  private static final String DEFAULT_USER_AVATAR_NAME = "/icons/chat/chat_message_user_avatar.png";

  private Map<String, Image> avatarCache = new ConcurrentHashMap<>();
  private Map<String, Job> jobs = new ConcurrentHashMap<>();

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
    this.defaultGithubAvatar = UiUtils.buildImageFromPngPath(DEFAULT_COPILOT_AVATAR_NAME);
    this.defaultUserAvatar = UiUtils.buildImageFromPngPath(DEFAULT_USER_AVATAR_NAME);
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
   * Gets the avatar for the current user.
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
   * Gets the avatar for a user.
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
      if (!jobs.containsKey(user)) {
        Job downloadJob = new Job("Download avatar for " + user) {
          @Override
          protected IStatus run(IProgressMonitor monitor) {
            Image downloadedImage = null;
            try {
              URL url = new URL(String.format(AVATAR_URL, user));
              try (var stream = url.openStream()) {
                downloadedImage = new Image(display, stream);
              }
            } catch (IOException e) {
              CopilotCore.LOGGER.error(e);
            }
            if (downloadedImage != null) {
              Image result = downloadedImage;
              // as the image is not always 24x24 even we set it in parameters, resize here if not
              if (result.getBounds().width != 24 || result.getBounds().height != 24) {
                result = UiUtils.resizeImage(display, downloadedImage, 24, 24);
                downloadedImage.dispose();
              }
              avatarCache.put(user, result);
              jobs.remove(user);
            }
            return Status.OK_STATUS;
          }
        };
        jobs.put(user, downloadJob);
        downloadJob.schedule();
      }
      return defaultUserAvatar;
    }
  }

  /**
   * Disposes the resources.
   */
  public void dispose() {
    defaultGithubAvatar.dispose();
    defaultUserAvatar.dispose();
    avatarCache.values().forEach(Image::dispose);
    jobs.values().forEach(Job::cancel);
    if (this.eventBroker != null) {
      this.eventBroker.unsubscribe(this.authStatusChangedEventHandler);
    }
  }

}

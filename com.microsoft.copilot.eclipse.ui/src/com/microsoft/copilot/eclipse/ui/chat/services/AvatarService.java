// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import java.io.ByteArrayOutputStream;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.utils.BundleUtils;
import com.microsoft.copilot.eclipse.core.utils.DataUriUtils;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Manages avatars for chat messages.
 */
public class AvatarService {
  private static final String AVATAR_URL = "https://avatars.githubusercontent.com/%s?s=24&v=4";

  /** Bundle-relative path of the default Copilot avatar icon. */
  public static final String DEFAULT_COPILOT_AVATAR_PATH = "/icons/chat/chat_message_copilot_avatar.png";

  /** Bundle-relative path of the default user avatar icon. */
  public static final String DEFAULT_USER_AVATAR_PATH = "/icons/chat/chat_message_user_avatar.png";

  private Map<String, Image> avatarCache = new ConcurrentHashMap<>();
  private Map<String, String> avatarDataUriCache = new ConcurrentHashMap<>();
  private Map<String, Job> jobs = new ConcurrentHashMap<>();

  private Image defaultGithubAvatar;
  private Image defaultUserAvatar;
  private String defaultGithubAvatarDataUri;
  private String defaultUserAvatarDataUri;
  private AuthStatusManager authStatusManager;
  private IEventBroker eventBroker;
  private EventHandler authStatusChangedEventHandler;

  /**
   * Avatar Service.
   */
  public AvatarService(AuthStatusManager authStatusManager) {
    this.authStatusManager = authStatusManager;
    this.defaultGithubAvatar = UiUtils.buildImageFromPngPath(DEFAULT_COPILOT_AVATAR_PATH);
    this.defaultUserAvatar = UiUtils.buildImageFromPngPath(DEFAULT_USER_AVATAR_PATH);
    Bundle bundle = FrameworkUtil.getBundle(getClass());
    this.defaultGithubAvatarDataUri =
        BundleUtils.readResourceAsDataUri(bundle, DEFAULT_COPILOT_AVATAR_PATH, "image/png");
    this.defaultUserAvatarDataUri =
        BundleUtils.readResourceAsDataUri(bundle, DEFAULT_USER_AVATAR_PATH, "image/png");
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
   * Gets the display name of the current user, or a default fallback label when the user name is
   * blank (e.g. signed out). Shared by both conversation widgets.
   *
   * @return the current user's name, or the default user label if it is blank
   */
  public String getUserName() {
    String user = this.authStatusManager.getUserName();
    return StringUtils.isNotBlank(user) ? user : Messages.chat_turnWidget_user;
  }

  /**
   * Gets the display name for the Copilot responder. Shared by both conversation widgets.
   *
   * @return the Copilot display name
   */
  public String getCopilotName() {
    return Messages.chat_turnWidget_copilot;
  }

  /**
   * Gets the copilot avatar as a {@code data:} URI, for embedding in HTML (browser renderer).
   *
   * @return the copilot avatar as a {@code data:image/png} URI
   */
  public String getAvatarForCopilotAsDataUri() {
    return defaultGithubAvatarDataUri;
  }

  /**
   * Gets the avatar for the current user as a {@code data:} URI, for embedding in HTML.
   *
   * @return the current user's avatar as a {@code data:image/png} URI, or the default user avatar
   *     while the real one is still being downloaded
   */
  public String getAvatarForCurrentUserAsDataUri() {
    return getAvatarAsDataUri(this.authStatusManager.getUserName());
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
    }
    scheduleAvatarDownload(display, user);
    return defaultUserAvatar;
  }

  /**
   * Gets the avatar for a user as a {@code data:} URI, for embedding in HTML (browser renderer).
   *
   * <p>Shares the same cache and asynchronous download as {@link #getAvatar(Display, String)}: on a
   * cache miss the default user avatar is returned immediately and the real avatar is fetched in the
   * background, so later renders pick it up (matching the StyledText renderer's behavior).
   *
   * @param user the user
   * @return the user's avatar as a {@code data:image/png} URI, or the default user avatar while the
   *     real one is still being downloaded
   */
  public synchronized String getAvatarAsDataUri(String user) {
    if (StringUtils.isBlank(user)) {
      return defaultUserAvatarDataUri;
    }
    String dataUri = avatarDataUriCache.get(user);
    if (dataUri != null) {
      return dataUri;
    }
    scheduleAvatarDownload(SwtUtils.getDisplay(), user);
    return defaultUserAvatarDataUri;
  }

  private void scheduleAvatarDownload(Display display, String user) {
    if (jobs.containsKey(user)) {
      return;
    }
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
          avatarDataUriCache.put(user, toPngDataUri(result));
          jobs.remove(user);
        }
        return Status.OK_STATUS;
      }
    };
    jobs.put(user, downloadJob);
    downloadJob.schedule();
  }

  private static String toPngDataUri(Image image) {
    ImageLoader loader = new ImageLoader();
    loader.data = new ImageData[] {image.getImageData()};
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    loader.save(out, SWT.IMAGE_PNG);
    return DataUriUtils.toDataUri(out.toByteArray(), "image/png");
  }

  /**
   * Disposes the resources.
   */
  public void dispose() {
    defaultGithubAvatar.dispose();
    defaultUserAvatar.dispose();
    avatarCache.values().forEach(Image::dispose);
    avatarDataUriCache.clear();
    jobs.values().forEach(Job::cancel);
    if (this.eventBroker != null) {
      this.eventBroker.unsubscribe(this.authStatusChangedEventHandler);
    }
  }

}

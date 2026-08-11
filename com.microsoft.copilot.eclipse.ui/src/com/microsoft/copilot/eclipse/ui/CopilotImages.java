// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui;

import java.net.URL;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Centralized access to all static icons in the Copilot UI bundle and to Eclipse shared images.
 *
 * <p>Bundle images are owned by the plugin's {@link ImageRegistry} and disposed automatically when
 * the plugin stops. Callers must <em>not</em> dispose images returned by {@link #getImage}.
 */
public final class CopilotImages {

  // Path prefixes
  private static final String ICONS_ROOT = "icons/";
  private static final String ICONS_CHAT = ICONS_ROOT + "chat/";
  private static final String ICONS_DROPDOWN = ICONS_ROOT + "dropdown/";
  private static final String ICONS_JOBS = ICONS_ROOT + "jobs/";
  private static final String ICONS_MCP = ICONS_ROOT + "mcp/";
  private static final String ICONS_QUOTA = ICONS_ROOT + "quota/";
  private static final String ICONS_QUICKSTART = "intro/quickstart/";
  private static final String ICONS_SPINNER = ICONS_ROOT + "spinner/";

  // ---- Root icons ----------------------------------------------------------
  public static final String IMG_BLANK = ICONS_ROOT + "blank.png";
  public static final String IMG_CANCEL_STATUS = ICONS_ROOT + "cancel_status.png";
  public static final String IMG_CLOSE = ICONS_ROOT + "close.png";
  public static final String IMG_COMPLETE_STATUS = ICONS_ROOT + "complete_status.png";
  public static final String IMG_EDIT_KEYBOARD_SHORTCUTS = ICONS_ROOT + "edit_keyboard_shortcuts.png";
  public static final String IMG_EDIT_PREFERENCES = ICONS_ROOT + "edit_preferences.png";
  public static final String IMG_FEEDBACK_FORUM = ICONS_ROOT + "feedback_forum.png";
  public static final String IMG_GITHUB_COPILOT = ICONS_ROOT + "github_copilot.png";
  public static final String IMG_GITHUB_COPILOT_ERROR = ICONS_ROOT + "github_copilot_error.png";
  public static final String IMG_GITHUB_COPILOT_NOT_AUTHORIZED = ICONS_ROOT + "github_copilot_not_authorized.png";
  public static final String IMG_GITHUB_COPILOT_NOT_SIGNED_IN = ICONS_ROOT + "github_copilot_not_signed_in.png";
  public static final String IMG_GITHUB_COPILOT_SIGNED_IN = ICONS_ROOT + "github_copilot_signed_in.png";
  public static final String IMG_INFORMATION = ICONS_ROOT + "information.png";
  public static final String IMG_SIGNIN = ICONS_ROOT + "signin.png";
  public static final String IMG_SIGNOUT = ICONS_ROOT + "signout.png";

  // ---- Chat icons ----------------------------------------------------------
  public static final String IMG_CHAT_ATTACH_CONTEXT = ICONS_CHAT + "attach_context.png";
  public static final String IMG_CHAT_BACK_ARROW = ICONS_CHAT + "back_arrow.png";
  public static final String IMG_CHAT_BACK_ARROW_GREY = ICONS_CHAT + "back_arrow_grey.png";
  public static final String IMG_CHAT_BREAKPOINT_AUTO = ICONS_CHAT + "breakpoint_auto.png";
  public static final String IMG_CHAT_BREAKPOINT_AUTO_DISABLED = ICONS_CHAT + "breakpoint_auto_disabled.png";
  public static final String IMG_CHAT_HISTORY_EDIT = ICONS_CHAT + "chat_history_edit.png";
  public static final String IMG_CHAT_COPILOT_AVATAR = ICONS_CHAT + "chat_message_copilot_avatar.png";
  public static final String IMG_CHAT_USER_AVATAR = ICONS_CHAT + "chat_message_user_avatar.png";
  public static final String IMG_CHATVIEW_ICON_CHAT = ICONS_CHAT + "chatview_icon_chat.png";
  public static final String IMG_CHATVIEW_ICON_CODE = ICONS_CHAT + "chatview_icon_code.png";
  public static final String IMG_CHATVIEW_ICON_LOADING = ICONS_CHAT + "chatview_icon_loading.png";
  public static final String IMG_CHATVIEW_ICON_NOT_AUTHORIZED = ICONS_CHAT + "chatview_icon_not_authorized.png";
  public static final String IMG_CHATVIEW_ICON_WELCOME = ICONS_CHAT + "chatview_icon_welcome.png";
  public static final String IMG_CLEAR_TODO = ICONS_CHAT + "clear_todo.png";
  public static final String IMG_CLEAR_TODO_DISABLED = ICONS_CHAT + "clear_todo_disable.png";
  public static final String IMG_CHAT_DOWN_ARROW = ICONS_CHAT + "down_arrow.png";
  public static final String IMG_CHAT_ENTER = ICONS_CHAT + "enter.png";
  public static final String IMG_CHAT_EYE = ICONS_CHAT + "eye.png";
  public static final String IMG_CHAT_EYE_DARK = ICONS_CHAT + "eye_dark.png";
  public static final String IMG_CHAT_EYE_CLOSED = ICONS_CHAT + "eye_closed.png";
  public static final String IMG_CHAT_EYE_CLOSED_DARK = ICONS_CHAT + "eye_closed_dark.png";
  public static final String IMG_CHAT_GUTTER_ARROW = ICONS_CHAT + "gutter-arrow.png";
  public static final String IMG_CHAT_INSERT_TEMPLATE = ICONS_CHAT + "insert_template.png";
  public static final String IMG_CHAT_KEEP = ICONS_CHAT + "keep.png";
  public static final String IMG_CHAT_KEYBOARD_TAB = ICONS_CHAT + "keyboard-tab.png";
  public static final String IMG_CHAT_RED_NOTICE = ICONS_CHAT + "red_notice.png";
  public static final String IMG_CHAT_RIGHT_ARROW = ICONS_CHAT + "right_arrow.png";
  public static final String IMG_CHAT_SEND = ICONS_CHAT + "send.png";
  public static final String IMG_CHAT_SEND_DISABLED = ICONS_CHAT + "send_disabled.png";
  public static final String IMG_CHAT_SEND_TO_JOB = ICONS_CHAT + "send_to_job.png";
  public static final String IMG_CHAT_SEND_TO_JOB_DISABLED = ICONS_CHAT + "send_to_job_disabled.png";
  public static final String IMG_CHAT_TOOLS = ICONS_CHAT + "tools.png";
  public static final String IMG_CHAT_TOOLS_DETECTED = ICONS_CHAT + "tools_detected.png";
  public static final String IMG_CHAT_TOOLS_DISABLED = ICONS_CHAT + "tools_disabled.png";

  // ---- Todo icons ----------------------------------------------------------
  public static final String IMG_TODOS_FINISH = ICONS_CHAT + "todos_finish.png";
  public static final String IMG_TODOS_FINISH_DARK = ICONS_CHAT + "todos_finish_dark.png";
  public static final String IMG_TODOS_RUNNING = ICONS_CHAT + "todos_running.png";
  public static final String IMG_TODOS_RUNNING_DARK = ICONS_CHAT + "todos_running_dark.png";
  public static final String IMG_TODOS_WAITING = ICONS_CHAT + "todos_waiting.png";
  public static final String IMG_TODOS_WAITING_DARK = ICONS_CHAT + "todos_waiting_dark.png";

  // ---- Dropdown icons ------------------------------------------------------
  public static final String IMG_DROPDOWN_DOWN_ARROW = ICONS_DROPDOWN + "down_arrow.png";
  public static final String IMG_DROPDOWN_DOWN_ARROW_DARK = ICONS_DROPDOWN + "down_arrow_dark.png";
  public static final String IMG_DROPDOWN_COMPLETE_STATUS = ICONS_DROPDOWN + "dropdown_complete_status.png";
  public static final String IMG_DROPDOWN_COMPLETE_STATUS_DARK = ICONS_DROPDOWN + "dropdown_complete_status_dark.png";
  public static final String IMG_DROPDOWN_WARNING = ICONS_DROPDOWN + "dropdown_warning.png";
  public static final String IMG_DROPDOWN_WARNING_DARK = ICONS_DROPDOWN + "dropdown_warning_dark.png";

  // ---- Jobs pull-request icons (used from UI bundle Java code) -------------
  public static final String IMG_JOBS_PULL_REQUEST_BLACK = ICONS_JOBS + "pull_request_black.png";
  public static final String IMG_JOBS_PULL_REQUEST_WHITE = ICONS_JOBS + "pull_request_white.png";

  // ---- MCP icons -----------------------------------------------------------
  public static final String IMG_MCP_DEFAULT_ICON = ICONS_MCP + "mcp_default_icon.png";
  public static final String IMG_MCP_MARKETPLACE_ICON = ICONS_MCP + "mcp_marketplace_icon.png";
  public static final String IMG_MCP_REGISTRY = ICONS_MCP + "mcp_registry.png";
  public static final String IMG_MCP_REFRESH = ICONS_MCP + "refresh.png";
  public static final String IMG_MCP_HISTORY = ICONS_MCP + "history.png";
  public static final String IMG_MCP_HISTORY_DARK = ICONS_MCP + "history_dark.png";
  public static final String IMG_MCP_REPOSITORY = ICONS_MCP + "repository.png";
  public static final String IMG_MCP_REPOSITORY_DARK = ICONS_MCP + "repository_dark.png";
  public static final String IMG_MCP_UPDATE = ICONS_MCP + "update.png";
  public static final String IMG_MCP_UPDATE_DARK = ICONS_MCP + "update_dark.png";
  public static final String IMG_MCP_VERSIONS = ICONS_MCP + "versions.png";
  public static final String IMG_MCP_VERSIONS_DARK = ICONS_MCP + "versions_dark.png";

  // ---- Quota icons ---------------------------------------------------------
  public static final String IMG_QUOTA_UPGRADE = ICONS_QUOTA + "upgrade.png";
  public static final String IMG_QUOTA_USAGE_BLUE = ICONS_QUOTA + "usage_blue.png";
  public static final String IMG_QUOTA_USAGE_RED = ICONS_QUOTA + "usage_red.png";
  public static final String IMG_QUOTA_USAGE_YELLOW = ICONS_QUOTA + "usage_yellow.png";

  // ---- Quick Start icons ---------------------------------------------------
  public static final String IMG_QUICKSTART_AGENT = ICONS_QUICKSTART + "quick_start_agent.png";
  public static final String IMG_QUICKSTART_ASK = ICONS_QUICKSTART + "quick_start_ask.png";
  public static final String IMG_QUICKSTART_COMPLETION = ICONS_QUICKSTART + "quick_start_completion.png";
  public static final String IMG_QUICKSTART_CLOSE_LIGHT = ICONS_QUICKSTART + "close_light.png";
  public static final String IMG_QUICKSTART_CLOSE_DARK = ICONS_QUICKSTART + "close_dark.png";
  public static final String IMG_QUICKSTART_CLOSE_HOVER_LIGHT = ICONS_QUICKSTART + "close_hover_light.png";
  public static final String IMG_QUICKSTART_CLOSE_HOVER_DARK = ICONS_QUICKSTART + "close_hover_dark.png";

  // ---- Spinner frames (1-based index, see getSpinnerFrame) -----------------
  private static final String IMG_SPINNER_1 = ICONS_SPINNER + "1.png";
  private static final String IMG_SPINNER_2 = ICONS_SPINNER + "2.png";
  private static final String IMG_SPINNER_3 = ICONS_SPINNER + "3.png";
  private static final String IMG_SPINNER_4 = ICONS_SPINNER + "4.png";
  private static final String IMG_SPINNER_5 = ICONS_SPINNER + "5.png";
  private static final String IMG_SPINNER_6 = ICONS_SPINNER + "6.png";
  private static final String IMG_SPINNER_7 = ICONS_SPINNER + "7.png";
  private static final String IMG_SPINNER_8 = ICONS_SPINNER + "8.png";

  private static final String[] SPINNER_FRAMES = {
      IMG_SPINNER_1, IMG_SPINNER_2, IMG_SPINNER_3, IMG_SPINNER_4,
      IMG_SPINNER_5, IMG_SPINNER_6, IMG_SPINNER_7, IMG_SPINNER_8
  };

  /** Number of spinner animation frames (1-based indices run from 1 to this value). */
  public static final int SPINNER_FRAME_COUNT = SPINNER_FRAMES.length;

  private CopilotImages() {
    // prevent instantiation
  }

  /**
   * Returns the plugin's image registry.
   */
  static ImageRegistry getImageRegistry() {
    return CopilotUi.getPlugin().getImageRegistry();
  }

  /**
   * Registers all static icon descriptors. Called once from
   * {@link CopilotUi#initializeImageRegistry(ImageRegistry)}.
   */
  static void initialize(ImageRegistry registry) {
    register(registry, IMG_BLANK);
    register(registry, IMG_CANCEL_STATUS);
    register(registry, IMG_CLOSE);
    register(registry, IMG_COMPLETE_STATUS);
    register(registry, IMG_EDIT_KEYBOARD_SHORTCUTS);
    register(registry, IMG_EDIT_PREFERENCES);
    register(registry, IMG_FEEDBACK_FORUM);
    register(registry, IMG_GITHUB_COPILOT);
    register(registry, IMG_GITHUB_COPILOT_ERROR);
    register(registry, IMG_GITHUB_COPILOT_NOT_AUTHORIZED);
    register(registry, IMG_GITHUB_COPILOT_NOT_SIGNED_IN);
    register(registry, IMG_GITHUB_COPILOT_SIGNED_IN);
    register(registry, IMG_INFORMATION);
    register(registry, IMG_SIGNIN);
    register(registry, IMG_SIGNOUT);
    register(registry, IMG_CHAT_ATTACH_CONTEXT);
    register(registry, IMG_CHAT_BACK_ARROW);
    register(registry, IMG_CHAT_BACK_ARROW_GREY);
    register(registry, IMG_CHAT_BREAKPOINT_AUTO);
    register(registry, IMG_CHAT_BREAKPOINT_AUTO_DISABLED);
    register(registry, IMG_CHAT_HISTORY_EDIT);
    register(registry, IMG_CHAT_COPILOT_AVATAR);
    register(registry, IMG_CHAT_USER_AVATAR);
    register(registry, IMG_CHATVIEW_ICON_CHAT);
    register(registry, IMG_CHATVIEW_ICON_CODE);
    register(registry, IMG_CHATVIEW_ICON_LOADING);
    register(registry, IMG_CHATVIEW_ICON_NOT_AUTHORIZED);
    register(registry, IMG_CHATVIEW_ICON_WELCOME);
    register(registry, IMG_CLEAR_TODO);
    register(registry, IMG_CLEAR_TODO_DISABLED);
    register(registry, IMG_CHAT_DOWN_ARROW);
    register(registry, IMG_CHAT_ENTER);
    register(registry, IMG_CHAT_EYE);
    register(registry, IMG_CHAT_EYE_DARK);
    register(registry, IMG_CHAT_EYE_CLOSED);
    register(registry, IMG_CHAT_EYE_CLOSED_DARK);
    register(registry, IMG_CHAT_GUTTER_ARROW);
    register(registry, IMG_CHAT_INSERT_TEMPLATE);
    register(registry, IMG_CHAT_KEEP);
    register(registry, IMG_CHAT_KEYBOARD_TAB);
    register(registry, IMG_CHAT_RED_NOTICE);
    register(registry, IMG_CHAT_RIGHT_ARROW);
    register(registry, IMG_CHAT_SEND);
    register(registry, IMG_CHAT_SEND_DISABLED);
    register(registry, IMG_CHAT_SEND_TO_JOB);
    register(registry, IMG_CHAT_SEND_TO_JOB_DISABLED);
    register(registry, IMG_CHAT_TOOLS);
    register(registry, IMG_CHAT_TOOLS_DETECTED);
    register(registry, IMG_CHAT_TOOLS_DISABLED);
    register(registry, IMG_TODOS_FINISH);
    register(registry, IMG_TODOS_FINISH_DARK);
    register(registry, IMG_TODOS_RUNNING);
    register(registry, IMG_TODOS_RUNNING_DARK);
    register(registry, IMG_TODOS_WAITING);
    register(registry, IMG_TODOS_WAITING_DARK);
    register(registry, IMG_DROPDOWN_DOWN_ARROW);
    register(registry, IMG_DROPDOWN_DOWN_ARROW_DARK);
    register(registry, IMG_DROPDOWN_COMPLETE_STATUS);
    register(registry, IMG_DROPDOWN_COMPLETE_STATUS_DARK);
    register(registry, IMG_DROPDOWN_WARNING);
    register(registry, IMG_DROPDOWN_WARNING_DARK);
    register(registry, IMG_JOBS_PULL_REQUEST_BLACK);
    register(registry, IMG_JOBS_PULL_REQUEST_WHITE);
    register(registry, IMG_MCP_DEFAULT_ICON);
    register(registry, IMG_MCP_MARKETPLACE_ICON);
    register(registry, IMG_MCP_REGISTRY);
    register(registry, IMG_MCP_REFRESH);
    register(registry, IMG_MCP_HISTORY);
    register(registry, IMG_MCP_HISTORY_DARK);
    register(registry, IMG_MCP_REPOSITORY);
    register(registry, IMG_MCP_REPOSITORY_DARK);
    register(registry, IMG_MCP_UPDATE);
    register(registry, IMG_MCP_UPDATE_DARK);
    register(registry, IMG_MCP_VERSIONS);
    register(registry, IMG_MCP_VERSIONS_DARK);
    register(registry, IMG_QUOTA_UPGRADE);
    register(registry, IMG_QUOTA_USAGE_BLUE);
    register(registry, IMG_QUOTA_USAGE_RED);
    register(registry, IMG_QUOTA_USAGE_YELLOW);
    register(registry, IMG_QUICKSTART_AGENT);
    register(registry, IMG_QUICKSTART_ASK);
    register(registry, IMG_QUICKSTART_COMPLETION);
    register(registry, IMG_QUICKSTART_CLOSE_LIGHT);
    register(registry, IMG_QUICKSTART_CLOSE_DARK);
    register(registry, IMG_QUICKSTART_CLOSE_HOVER_LIGHT);
    register(registry, IMG_QUICKSTART_CLOSE_HOVER_DARK);
    register(registry, IMG_SPINNER_1);
    register(registry, IMG_SPINNER_2);
    register(registry, IMG_SPINNER_3);
    register(registry, IMG_SPINNER_4);
    register(registry, IMG_SPINNER_5);
    register(registry, IMG_SPINNER_6);
    register(registry, IMG_SPINNER_7);
    register(registry, IMG_SPINNER_8);
  }

  private static void register(ImageRegistry registry, String path) {
    URL url = CopilotImages.class.getResource("/" + path);
    ImageDescriptor descriptor = url != null
        ? ImageDescriptor.createFromURL(url) : ImageDescriptor.getMissingImageDescriptor();
    registry.put(path, descriptor);
  }

  /**
   * Returns the image for the given key. The returned image is owned by the plugin registry;
   * callers must <em>not</em> dispose it.
   *
   * @param key one of the {@code IMG_*} constants defined in this class
   * @return the registry-owned image for the given key
   */
  public static Image getImage(String key) {
    return getImageRegistry().get(key);
  }

  /**
   * Returns the {@link ImageDescriptor} for the given key. Useful where a descriptor is required,
   * e.g. for {@code Action.setImageDescriptor()}.
   *
   * @param key one of the {@code IMG_*} constants defined in this class
   * @return the image descriptor for the given key
   */
  public static ImageDescriptor getImageDescriptor(String key) {
    return getImageRegistry().getDescriptor(key);
  }

  /**
   * Returns the theme-correct image for icons that come in light/dark variants.
   * Uses {@link UiUtils#isDarkTheme()} to pick the right key.
   * The returned image is owned by the plugin registry;
   * callers must <em>not</em> dispose it.
   *
   * <p>Does not track live theme changes; callers re-render on theme-change events as needed.
   *
   * @param lightKey key to use in light theme
   * @param darkKey  key to use in dark theme
   * @return the registry-owned image for the current theme
   */
  public static Image getThemedImage(String lightKey, String darkKey) {
    return getImage(UiUtils.isDarkTheme() ? darkKey : lightKey);
  }

  private static String getSpinnerFrameImageKey(int frame) {
    Assert.isLegal(frame >= 1 && frame <= SPINNER_FRAMES.length,
        "Spinner frame must be in [1, " + SPINNER_FRAMES.length + "], got: " + frame);
    return SPINNER_FRAMES[frame - 1];
  }

  /**
   * Returns the image for the given 1-based spinner frame (1–{@link SPINNER_FRAME_COUNT}).
   * The returned image is owned by the plugin registry;
   * callers must <em>not</em> dispose it.
   *
   * @param frame 1-based index of the spinner frame
   * @return the registry-owned image for the requested spinner frame
   * @throws IllegalArgumentException if {@code frame} is outside [1, {@link SPINNER_FRAME_COUNT}]
   */
  public static Image getSpinnerFrame(int frame) {
    return getImage(getSpinnerFrameImageKey(frame));
  }

  /**
   * Returns the image descriptor for the given 1-based spinner frame (1–{@link SPINNER_FRAME_COUNT}).
   *
   * @param frame 1-based index of the spinner frame
   * @return the image descriptor for the requested spinner frame
   * @throws IllegalArgumentException if {@code frame} is outside [1, {@link SPINNER_FRAME_COUNT}]
   */
  public static ImageDescriptor getSpinnerFrameDescriptor(int frame) {
    return getImageDescriptor(getSpinnerFrameImageKey(frame));
  }

  /**
   * Convenience access to Eclipse's workbench shared images.
   * The returned image is owned by the owning plugin's registry;
   * callers must <em>not</em> dispose it.
   *
   * @param imageId a constant from {@link ISharedImages}, e.g. {@link ISharedImages#IMG_OBJS_ERROR_TSK}
   * @return the shared workbench image for the given id
   */
  public static Image getSharedImage(String imageId) {
    return PlatformUI.getWorkbench().getSharedImages().getImage(imageId);
  }

}

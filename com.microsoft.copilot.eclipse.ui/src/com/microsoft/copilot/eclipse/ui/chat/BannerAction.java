// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

/**
 * A clickable action rendered below the message of a {@link StaticBanner}. Each action is a localized label paired
 * with the URL that should be opened when the user activates it.
 *
 * @param text the visible link label
 * @param url the target URL opened on activation
 */
public record BannerAction(String text, String url) {
}

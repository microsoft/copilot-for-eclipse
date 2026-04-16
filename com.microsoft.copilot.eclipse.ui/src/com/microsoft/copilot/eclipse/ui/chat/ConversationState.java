// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

/**
 * Enum representing the different conversation states in the chat view.
 */
public enum ConversationState {
    /**
     * A brand new conversation with no history.
     */
    NEW_CONVERSATION,

    /**
     * Continuing an existing conversation.
     */
    CONTINUED_CONVERSATION,

    /**
     * A new conversation based on a history conversation.
     */
    NEW_HISTORY_BASED_CONVERSATION
}
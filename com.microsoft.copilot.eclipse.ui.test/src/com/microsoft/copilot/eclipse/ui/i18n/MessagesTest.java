package com.microsoft.copilot.eclipse.ui.i18n;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class MessagesTest {

    @Test
    void testMessagesInitialization() {
        // Ensure that the static fields are initialized
        assertNotNull(Messages.INFO_signToGitHub);
        assertNotNull(Messages.INFO_signOutFromGitHub);
    }
}
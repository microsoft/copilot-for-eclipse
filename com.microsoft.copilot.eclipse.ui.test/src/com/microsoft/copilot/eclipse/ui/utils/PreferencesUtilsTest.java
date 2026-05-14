// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.chat.CustomInstructionsChatLoadScope;

class PreferencesUtilsTest {

	private IPreferenceStore store;

	@BeforeEach
	void setUp() {
		store = new PreferenceStore();
		store.setDefault(Constants.CUSTOM_INSTRUCTIONS_CHAT_LOAD_SCOPE,
				CustomInstructionsChatLoadScope.DEFAULT_VALUE.getValue());
	}

	@AfterEach
	void tearDown() {
		store = null;
	}

	@ParameterizedTest
	@EnumSource(CustomInstructionsChatLoadScope.class)
	void testGetCustomInstructionsChatLoadScope_returnsStoredValue(CustomInstructionsChatLoadScope scope) {
		store.setValue(Constants.CUSTOM_INSTRUCTIONS_CHAT_LOAD_SCOPE, scope.getValue());

		CustomInstructionsChatLoadScope result = PreferencesUtils.getCustomInstructionsChatLoadScope(store);

		assertEquals(scope, result);
	}

	@Test
	void testGetCustomInstructionsChatLoadScope_fallsBackToDefaultInCaseOfInvalidValueInStore() {
		store.setValue(Constants.CUSTOM_INSTRUCTIONS_CHAT_LOAD_SCOPE, "invalid_value");

		CustomInstructionsChatLoadScope result = PreferencesUtils.getCustomInstructionsChatLoadScope(store);

		assertEquals(CustomInstructionsChatLoadScope.DEFAULT_VALUE, result);
		assertEquals(CustomInstructionsChatLoadScope.DEFAULT_VALUE.getValue(),
				store.getString(Constants.CUSTOM_INSTRUCTIONS_CHAT_LOAD_SCOPE),
				"The invalid stored value should have been corrected to the default");
	}

	@Test
	void testGetCustomInstructionsChatLoadScopeDefault_returnsConfiguredDefault() {
		// explicitly store varying values for InstanceScope and DefaultScope
		store.setValue(Constants.CUSTOM_INSTRUCTIONS_CHAT_LOAD_SCOPE,
				CustomInstructionsChatLoadScope.ALL_PROJECTS.getValue());
		store.setDefault(Constants.CUSTOM_INSTRUCTIONS_CHAT_LOAD_SCOPE,
				CustomInstructionsChatLoadScope.REFERENCED_PROJECTS.getValue());

		CustomInstructionsChatLoadScope result = PreferencesUtils.getCustomInstructionsChatLoadScopeDefault(store);

		// Should return the default value, not the stored value
		assertEquals(CustomInstructionsChatLoadScope.REFERENCED_PROJECTS, result);
	}

	@Test
	void testGetCustomInstructionsChatLoadScopeDefault_fallsBackToValidDefaultInCaseOfInvalidValue() {
		String invalidDefaultValue = "invalid_default_value";
		String instanceScopValue = CustomInstructionsChatLoadScope.REFERENCED_PROJECTS.getValue();
		store.setDefault(Constants.CUSTOM_INSTRUCTIONS_CHAT_LOAD_SCOPE, invalidDefaultValue);
		store.setValue(Constants.CUSTOM_INSTRUCTIONS_CHAT_LOAD_SCOPE, instanceScopValue);

		CustomInstructionsChatLoadScope result = PreferencesUtils.getCustomInstructionsChatLoadScopeDefault(store);

		assertEquals(CustomInstructionsChatLoadScope.DEFAULT_VALUE, result);
		assertEquals(invalidDefaultValue,
				store.getDefaultString(Constants.CUSTOM_INSTRUCTIONS_CHAT_LOAD_SCOPE),
				"The stored DefaultScope value should not change, even if it's invalid. "
				+ "That is repsonsibility of a PreferenceInitializer.");
		assertEquals(instanceScopValue,
				store.getString(Constants.CUSTOM_INSTRUCTIONS_CHAT_LOAD_SCOPE),
				"The stored InstanceScope value should not change");
	}
}

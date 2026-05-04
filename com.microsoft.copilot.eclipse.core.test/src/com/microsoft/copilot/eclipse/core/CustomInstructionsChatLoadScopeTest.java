// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.microsoft.copilot.eclipse.core.chat.CustomInstructionsChatLoadScope;

class CustomInstructionsChatLoadScopeTest {

  @ParameterizedTest
  @EnumSource(CustomInstructionsChatLoadScope.class)
  void testStringToEnumEntryConversion(CustomInstructionsChatLoadScope enumEntry) {
    String inputValue = enumEntry.getValue();

    CustomInstructionsChatLoadScope actualResult = CustomInstructionsChatLoadScope.fromValue(inputValue);

    assertEquals(enumEntry, actualResult);
  }

  @ParameterizedTest
  @ValueSource(strings = { "wrongValue" })
  @NullSource
  void testStringToEnumEntryConversionThrowsExceptionForWrongValues(String value) {
    assertThrows(IllegalArgumentException.class, () -> CustomInstructionsChatLoadScope.fromValue(value));
  }

}

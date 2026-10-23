// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ActionBarActionButtonStateTest {
  private static final String SEND_ENABLED = "SEND_ENABLED";
  private static final String SEND_DISABLED = "SEND_DISABLED";
  private static final String CANCEL_ENABLED = "CANCEL_ENABLED";
  private static final String TURN_STARTED = "TURN_STARTED";
  private static final String TURN_FINISHED = "TURN_FINISHED";
  private static final String INPUT_CHANGED = "INPUT_CHANGED";
  private static final Class<?> STATE_TYPE = loadStateType();
  private static final Class<?> EVENT_TYPE = loadEventType();

  @ParameterizedTest
  @ValueSource(strings = { SEND_ENABLED, SEND_DISABLED, CANCEL_ENABLED })
  void testOnTurnStarted_anyState_returnsCancelEnabled(String stateName) {
    assertEquals(CANCEL_ENABLED, stateName(nextState(stateName, TURN_STARTED, false)));
  }

  @ParameterizedTest
  @ValueSource(strings = { SEND_ENABLED, SEND_DISABLED, CANCEL_ENABLED })
  void testOnTurnFinished_withInput_returnsSendEnabled(String stateName) {
    assertEquals(SEND_ENABLED, stateName(nextState(stateName, TURN_FINISHED, true)));
  }

  @ParameterizedTest
  @ValueSource(strings = { SEND_ENABLED, SEND_DISABLED, CANCEL_ENABLED })
  void testOnTurnFinished_withoutInput_returnsSendDisabled(String stateName) {
    assertEquals(SEND_DISABLED, stateName(nextState(stateName, TURN_FINISHED, false)));
  }

  @Test
  void testOnInputChanged_runningTurn_keepsCancelEnabled() {
    assertEquals(CANCEL_ENABLED, stateName(nextState(CANCEL_ENABLED, INPUT_CHANGED, true)));
    assertEquals(CANCEL_ENABLED, stateName(nextState(CANCEL_ENABLED, INPUT_CHANGED, false)));
  }

  @Test
  void testOnInputChanged_idleTurn_reflectsInputPresence() {
    assertEquals(SEND_ENABLED, stateName(nextState(SEND_DISABLED, INPUT_CHANGED, true)));
    assertEquals(SEND_DISABLED, stateName(nextState(SEND_ENABLED, INPUT_CHANGED, false)));
  }

  @Test
  void testRepeatedTransitions_sameInput_areIdempotent() {
    Object startedTwice = nextState(nextState(CANCEL_ENABLED, TURN_STARTED, false), TURN_STARTED, false);
    Object enabledTwice = nextState(nextState(SEND_ENABLED, TURN_FINISHED, true), TURN_FINISHED, true);
    Object disabledTwice = nextState(nextState(SEND_DISABLED, TURN_FINISHED, false), TURN_FINISHED, false);

    assertEquals(CANCEL_ENABLED, stateName(startedTwice));
    assertEquals(SEND_ENABLED, stateName(enabledTwice));
    assertEquals(SEND_DISABLED, stateName(disabledTwice));
  }

  @Test
  void testIsTurnRunning_onlyCancelEnabled_returnsTrue() {
    assertTrue(invokeBoolean(state(CANCEL_ENABLED), "isTurnRunning"));
    assertFalse(invokeBoolean(state(SEND_ENABLED), "isTurnRunning"));
    assertFalse(invokeBoolean(state(SEND_DISABLED), "isTurnRunning"));
  }

  private static Class<?> loadStateType() {
    return loadNestedType("ActionButtonState");
  }

  private static Class<?> loadEventType() {
    return loadNestedType("ActionButtonEvent");
  }

  private static Class<?> loadNestedType(String simpleName) {
    try {
      return Class.forName(ActionBar.class.getName() + "$" + simpleName);
    } catch (ClassNotFoundException e) {
      throw new AssertionError(simpleName + " is missing", e);
    }
  }

  private static Object enumConstant(Class<?> enumType, String name) {
    for (Object value : enumType.getEnumConstants()) {
      if (name.equals(stateName(value))) {
        return value;
      }
    }
    throw new AssertionError("Unknown " + enumType.getSimpleName() + ": " + name);
  }

  private static String stateName(Object state) {
    return ((Enum<?>) state).name();
  }

  private static Object nextState(String stateName, String eventName, boolean hasInput) {
    return nextState(enumConstant(STATE_TYPE, stateName), eventName, hasInput);
  }

  private static Object nextState(Object state, String eventName, boolean hasInput) {
    try {
      Method method = ActionBar.class.getDeclaredMethod("nextActionButtonState", STATE_TYPE, EVENT_TYPE,
          boolean.class);
      method.setAccessible(true);
      return method.invoke(null, state, enumConstant(EVENT_TYPE, eventName), hasInput);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Failed to invoke ActionBar.nextActionButtonState", e);
    }
  }

  private static boolean invokeBoolean(Object state, String methodName) {
    try {
      Method method = STATE_TYPE.getDeclaredMethod(methodName);
      method.setAccessible(true);
      return (Boolean) method.invoke(state);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Failed to invoke ActionButtonState." + methodName, e);
    }
  }

  private static Object state(String name) {
    return enumConstant(STATE_TYPE, name);
  }
}

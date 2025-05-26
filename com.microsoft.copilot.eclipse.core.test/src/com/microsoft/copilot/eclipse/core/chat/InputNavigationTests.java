package com.microsoft.copilot.eclipse.core.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InputNavigationTests {

  private InputNavigation navigation;

  @BeforeEach
  void setUp() {
    navigation = new InputNavigation();
  }

  @Test
  void testDefaultConstructor() {
    assertEquals(0, navigation.size());
    assertEquals(0, navigation.currentPosition);
    assertTrue(navigation.atBottom());
    assertTrue(navigation.atTop());
  }

  @Test
  void testConstructorWithNullInputs() {
    navigation = new InputNavigation(null);
    assertEquals(0, navigation.size());
    assertEquals(0, navigation.currentPosition);
  }

  @Test
  void testConstructorWithInputs() {
    List<String> inputs = Arrays.asList("input1", "input2");
    navigation = new InputNavigation(inputs);
    assertEquals(2, navigation.size());
    assertEquals(2, navigation.currentPosition);
  }

  @Test
  void testConstructorWithOverflowInputs() {
    List<String> inputs = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      inputs.add("input" + i);
    }
    navigation = new InputNavigation(inputs);
    assertEquals(16, navigation.size());
    assertEquals("input4", navigation.getInputHistoryList().get(0));
    assertEquals("input19", navigation.getInputHistoryList().get(15));
  }

  @Test
  void testAdd() {
    navigation.add("input1");
    assertEquals(1, navigation.size());
    assertEquals(1, navigation.currentPosition);

    navigation.add("input2");
    assertEquals(2, navigation.size());
    assertEquals(2, navigation.currentPosition);
  }

  @Test
  void testAddBlank() {
    navigation.add("");
    navigation.add(" ");
    assertEquals(0, navigation.size());
  }

  @Test
  void testNavigateUp() {
    navigation.add("input1");
    navigation.add("input2");
    assertEquals("input2", navigation.navigateUp());
    assertEquals(1, navigation.currentPosition);
    assertEquals("input1", navigation.navigateUp());
  }

  @Test
  void testNavigateDown() {
    navigation.add("input1");
    navigation.add("input2");
    navigation.navigateUp();
    navigation.navigateUp();
    assertEquals("input2", navigation.navigateDown());
    assertEquals(1, navigation.currentPosition);
    assertEquals("input2", navigation.navigateDown());
  }

  @Test
  void testAtBottomAndAtTop() {
    assertTrue(navigation.atBottom());
    assertTrue(navigation.atTop());

    navigation.add("input1");
    navigation.add("input2");

    assertTrue(navigation.atBottom());
    assertFalse(navigation.atTop());

    navigation.navigateUp();
    assertFalse(navigation.atBottom());
    assertFalse(navigation.atTop());

    navigation.navigateUp();
    assertFalse(navigation.atBottom());
    assertTrue(navigation.atTop());
  }

  @Test
  void testGetLatestInput() {
    assertEquals(StringUtils.EMPTY, navigation.getLatestInput());

    navigation.add("input1");
    assertEquals("input1", navigation.getLatestInput());

    navigation.add("input2");
    assertEquals("input2", navigation.getLatestInput());
  }

  @Test
  void testUpdateCursorPosition() {
    navigation.add("input1");
    navigation.add("input2");

    navigation.updateCursorPosition(0);
    assertEquals(0, navigation.currentPosition);

    navigation.updateCursorPosition(2);
    assertEquals(2, navigation.currentPosition);
  }

  @Test
  void testUpdateCursorPositionOutOfBounds() {
    navigation.add("input1");

    assertThrows(IndexOutOfBoundsException.class, () -> navigation.updateCursorPosition(-1));
    assertThrows(IndexOutOfBoundsException.class, () -> navigation.updateCursorPosition(2));
  }

}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * InputNavigation is a class that manages the navigation of user inputs in a chat interface. It maintains a history of
 * inputs and allows for navigation through them.
 */
public class InputNavigation {

  private static final int MAX_HISTORY_SIZE = 16;

  private List<String> inputHistory;
  int currentPosition;

  /**
   * Constructs a new InputNavigation object.
   */
  public InputNavigation() {
    inputHistory = new ArrayList<>();
    currentPosition = 0;
  }

  /**
   * Constructs a new InputNavigation object with the specified list of inputs.
   *
   * @param inputs the list of inputs to initialize the navigation with
   */
  public InputNavigation(List<String> inputs) {
    if (inputs == null) {
      inputs = Collections.emptyList();
    }
    if (inputs.size() > MAX_HISTORY_SIZE) {
      inputs = inputs.subList(inputs.size() - MAX_HISTORY_SIZE, inputs.size());
    }
    inputHistory = new ArrayList<>(inputs);
    currentPosition = inputHistory.size();
  }

  public List<String> getInputHistoryList() {
    return inputHistory;
  }

  /**
   * Add a new input to the history and update the cursor.
   *
   * @param input the input text to add.
   */
  public void add(String input) {
    if (StringUtils.isBlank(input) || Objects.equals(input, getLatestInput())) {
      currentPosition = inputHistory.size();
      return;
    }
    if (inputHistory.size() == MAX_HISTORY_SIZE) {
      inputHistory.remove(0);
    }
    inputHistory.add(input);
    currentPosition = inputHistory.size();
  }

  /**
   * Navigate to the last input in the history.
   *
   * @return the previous input in the history, or an empty string if unavailable.
   */
  public String navigateUp() {
    if (inputHistory.isEmpty() || atTop()) {
      return StringUtils.EMPTY;
    }
    currentPosition--;
    ensureValidPosition();

    return inputHistory.get(currentPosition);
  }

  /**
   * Navigate to the next input in the history.
   *
   * @return the next input in the history, or an empty string if unavailable.
   */
  public String navigateDown() {
    if (inputHistory.isEmpty() || atBottom()) {
      return StringUtils.EMPTY;
    }
    currentPosition++;
    ensureValidPosition();

    return inputHistory.get(currentPosition);
  }

  /**
   * Check if the current position is at the bottom(latest) of the history.
   *
   * @return true if the current position is at the bottom of the history.
   */
  public boolean atBottom() {
    return inputHistory.isEmpty() || currentPosition == inputHistory.size();
  }

  /**
   * Check if the current position is at the top(oldest) of the history.
   *
   * @return true if the current position is at the top of the history.
   */
  public boolean atTop() {
    return inputHistory.isEmpty() || currentPosition == 0;
  }

  /**
   * Get the latest input from the history.
   *
   * @return the latest input, or an empty string if the history is empty.
   */
  public String getLatestInput() {
    if (inputHistory.isEmpty()) {
      return StringUtils.EMPTY;
    }
    return inputHistory.get(inputHistory.size() - 1);
  }

  /**
   * Get the size of the input history.
   *
   * @return the number of inputs in the history.
   */
  public int size() {
    return inputHistory.size();
  }

  /**
   * Update the current cursor position in the input history.
   *
   * @param position the new cursor position.
   */
  public void updateCursorPosition(int position) {
    if (position < 0 || position > inputHistory.size()) {
      throw new IndexOutOfBoundsException("Position out of bounds: " + position);
    }
    currentPosition = position;
  }

  private void ensureValidPosition() {
    if (currentPosition < 0) {
      currentPosition = 0;
    } else if (currentPosition >= inputHistory.size()) {
      currentPosition = inputHistory.size() - 1;
    }
  }

}

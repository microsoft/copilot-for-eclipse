package com.microsoft.copilot.eclipse.ui.nes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for computing character-level text differences using Myers algorithm.
 */
public class TextDiffCalculator {
  /**
   * Combined span describing a difference region in both original and new text. Positions are in ORIGINAL
   * (non-normalized) text coordinates.
   */
  public static class DualDiffSpan {
    public final DiffSegment.Type type; // DELETE / INSERT / REPLACE
    public final int origStart; // start in original text (before normalization)
    public final int origLength; // length in original text (before normalization)
    public final int newStart; // start in replacement text (before normalization)
    public final int newLength; // length in replacement text (before normalization)
    public final String origText; // original differing text (original newlines)
    public final String newText; // replacement differing text (original newlines)

    /**
     * Constructor.
     */
    public DualDiffSpan(DiffSegment.Type type, int origStart, int origLength, int newStart, int newLength,
        String origText, String newText) {
      this.type = type;
      this.origStart = origStart;
      this.origLength = origLength;
      this.newStart = newStart;
      this.newLength = newLength;
      this.origText = origText;
      this.newText = newText;
    }
  }

  /** Result of dual diff calculation. */
  public static class DualDiffResult {
    public final List<DualDiffSpan> spans = new ArrayList<>();

    /** Original text (before normalization). */
    public final String originalText;

    /** Replacement text (before normalization). */
    public final String replacementText;

    /** Constructor. */
    public DualDiffResult(String originalText, String replacementText) {
      this.originalText = originalText;
      this.replacementText = replacementText;
    }
  }

  /**
   * Diff segment type enumeration.
   */
  public static class DiffSegment {
    /** Diff segment types. */
    public enum Type {
      EQUAL, DELETE, INSERT, REPLACE
    }
  }

  /**
   * Normalized text with mapping back to original positions.
   * This is used to handle line break normalization (\r, \n, \r\n) consistently.
   */
  private static class NormalizedText {
    final String normalized;
    final int[] originalPositions; // originalPositions[i] = original position of normalized[i]

    NormalizedText(String original) {
      if (StringUtils.isBlank(original)) {
        this.normalized = "";
        this.originalPositions = new int[0];
        return;
      }

      StringBuilder sb = new StringBuilder(original.length());
      List<Integer> positions = new ArrayList<>(original.length());

      for (int i = 0; i < original.length(); i++) {
        char c = original.charAt(i);

        // Handle \r\n and \n\r as single newline
        if (c == '\r' && i + 1 < original.length() && original.charAt(i + 1) == '\n') {
          sb.append('\n');
          positions.add(i);
          i++;
        } else if (c == '\n' && i + 1 < original.length() && original.charAt(i + 1) == '\r') {
          sb.append('\n');
          positions.add(i);
          i++;
        } else if (c == '\r') {
          sb.append('\n');
          positions.add(i);
        } else {
          sb.append(c);
          positions.add(i);
        }
      }

      this.normalized = sb.toString();
      this.originalPositions = positions.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Map normalized index to original index. */
    int toOriginalIndex(int normalizedIndex) {
      if (normalizedIndex < 0 || normalizedIndex >= originalPositions.length) {
        return -1;
      }
      return originalPositions[normalizedIndex];
    }

    /** Get original length of substring [normStart, normEnd). */
    int getOriginalLength(int normStart, int normEnd, String originalText) {
      if (normStart >= normEnd || normStart >= originalPositions.length) {
        return 0;
      }

      int origStart = originalPositions[normStart];

      // Calculate original end position
      int origEnd;
      if (normEnd >= originalPositions.length) {
        origEnd = originalText.length();
      } else {
        origEnd = originalPositions[normEnd];
      }

      return origEnd - origStart;
    }
  }

  /**
   * Main entry point for calculating differences between original and replacement text. Uses Myers algorithm for
   * accurate character-level diff with newline normalization.
   *
   * @param original Original text
   * @param replacement Replacement text
   * @return DualDiffResult with spans in ORIGINAL text coordinates
   */
  public static DualDiffResult calculateDiff(String original, String replacement) {
    if (original == null) {
      original = "";
    }
    if (replacement == null) {
      replacement = "";
    }

    // Normalize texts
    NormalizedText normOrig = new NormalizedText(original);
    NormalizedText normRepl = new NormalizedText(replacement);

    // Run Myers diff on normalized texts
    List<Edit> edits = myersDiff(normOrig.normalized, normRepl.normalized);

    // Convert edits to DualDiffSpans with original coordinates
    DualDiffResult result = new DualDiffResult(original, replacement);

    for (Edit edit : edits) {
      if (edit.type == DiffSegment.Type.EQUAL) {
        continue;
      }
      // Map normalized positions back to original
      // Handle edge case: if original is empty, origStart should be 0, not -1
      int origStart = 0;
      if (edit.origLength > 0) {
        int mappedStart = normOrig.toOriginalIndex(edit.origStart);
        origStart = mappedStart >= 0 ? mappedStart : 0;
      }
      int origLength = normOrig.getOriginalLength(edit.origStart, edit.origStart + edit.origLength, original);

      // Handle edge case: if replacement is empty, newStart should be 0, not -1
      int newStart = 0;
      if (edit.newLength > 0) {
        int mappedStart = normRepl.toOriginalIndex(edit.newStart);
        newStart = mappedStart >= 0 ? mappedStart : 0;
      }
      int newLength = normRepl.getOriginalLength(edit.newStart, edit.newStart + edit.newLength, replacement);

      // Extract original text
      String origText = "";
      if (origLength > 0 && origStart >= 0 && origStart + origLength <= original.length()) {
        origText = original.substring(origStart, origStart + origLength);
      }

      String newText = "";
      if (newLength > 0 && newStart >= 0 && newStart + newLength <= replacement.length()) {
        newText = replacement.substring(newStart, newStart + newLength);
      }
      result.spans.add(new DualDiffSpan(edit.type, origStart, origLength, newStart, newLength, origText, newText));
    }

    return result;
  }

  /**
   * Calculate character-level differences (legacy method, delegates to calculateDiff).
   */
  public static DualDiffResult calculateDualCharacterDiff(String original, String replacement) {
    return calculateDiff(original, replacement);
  }

  /**
   * Calculate character-level differences (legacy method with ignore flag).
   */
  public static DualDiffResult calculateDualCharacterDiff(String original, String replacement,
      boolean ignoreLineEndingDiff) {
    // ignoreLineEndingDiff is now always true (built into normalization)
    return calculateDiff(original, replacement);
  }

  /**
   * Edit operation for Myers algorithm.
   */
  private static class Edit {
    final DiffSegment.Type type;
    final int origStart; // Position in normalized original
    final int origLength; // Length in normalized original
    final int newStart; // Position in normalized replacement
    final int newLength; // Length in normalized replacement

    Edit(DiffSegment.Type type, int origStart, int origLength, int newStart, int newLength) {
      this.type = type;
      this.origStart = origStart;
      this.origLength = origLength;
      this.newStart = newStart;
      this.newLength = newLength;
    }
  }

  /**
   * Simplified Myers diff algorithm. Returns list of Edit operations in normalized text coordinates.
   */
  private static List<Edit> myersDiff(String a, String b) {
    int n = a.length();
    int m = b.length();

    if (n == 0 && m == 0) {
      return new ArrayList<>();
    }
    if (n == 0) {
      List<Edit> result = new ArrayList<>();
      result.add(new Edit(DiffSegment.Type.INSERT, 0, 0, 0, m));
      return result;
    }

    if (m == 0) {
      List<Edit> result = new ArrayList<>();
      result.add(new Edit(DiffSegment.Type.DELETE, 0, n, 0, 0));
      return result;
    }

    // Myers algorithm
    int max = n + m;
    int[] v = new int[2 * max + 1];
    int[][] trace = new int[max + 1][];

    for (int d = 0; d <= max; d++) {
      for (int k = -d; k <= d; k += 2) {
        int x;
        // Choose direction
        boolean down = (k == -d || (k != d && v[k - 1 + max] < v[k + 1 + max]));
        if (down) {
          x = v[k + 1 + max];
        } else {
          x = v[k - 1 + max] + 1;
        }
        int y = x - k;
        // Extend diagonal
        while (x < n && y < m && a.charAt(x) == b.charAt(y)) {
          x++;
          y++;
        }
        v[k + max] = x;
        // Found solution
        if (x >= n && y >= m) {
          // Save final trace before backtracking
          trace[d] = Arrays.copyOf(v, v.length);
          return backtrack(a, b, trace, d);
        }
      }
      // Save trace after processing all k for this depth
      trace[d] = Arrays.copyOf(v, v.length);
    }
    List<Edit> result = new ArrayList<>();
    if (n > 0) {
      result.add(new Edit(DiffSegment.Type.DELETE, 0, n, 0, 0));
    }
    if (m > 0) {
      result.add(new Edit(DiffSegment.Type.INSERT, 0, 0, 0, m));
    }
    return result;
  }

  /**
   * Backtrack through Myers algorithm trace to build edit list.
   */
  private static List<Edit> backtrack(String a, String b, int[][] trace, int d) {
    List<Edit> edits = new ArrayList<>();

    int x = a.length();
    int y = b.length();

    for (int depth = d; depth > 0; depth--) {
      int[] v = trace[depth];
      int[] prevV = trace[depth - 1];
      int k = x - y;
      int max = a.length() + b.length();
      int prevX;
      boolean down = (k == -depth || (k != depth && prevV[k - 1 + max] < prevV[k + 1 + max]));
      if (down) {
        prevX = v[k + 1 + max];
      } else {
        prevX = v[k - 1 + max];
      }
      int prevY = prevX - (down ? (k + 1) : (k - 1));
      // Collect diagonal (equal region)
      while (x > prevX && y > prevY) {
        x--;
        y--;
      }
      // Record the edit
      if (down) {
        // Insert
        if (y > prevY) {
          edits.add(0, new Edit(DiffSegment.Type.INSERT, x, 0, prevY, y - prevY));
        }
        y = prevY;
      } else {
        // Delete
        if (x > prevX) {
          edits.add(0, new Edit(DiffSegment.Type.DELETE, prevX, x - prevX, y, 0));
        }
        x = prevX;
      }
    }
    return mergeEdits(edits);
  }

  /**
   * Merge adjacent DELETE+INSERT into REPLACE.
   */
  private static List<Edit> mergeEdits(List<Edit> edits) {
    List<Edit> result = new ArrayList<>();

    for (int i = 0; i < edits.size(); i++) {
      Edit current = edits.get(i);
      if (i + 1 < edits.size()) {
        Edit next = edits.get(i + 1);

        // Merge DELETE followed by INSERT at same position into REPLACE
        if (current.type == DiffSegment.Type.DELETE && next.type == DiffSegment.Type.INSERT
            && current.origStart + current.origLength == next.origStart 
            && current.newStart == next.newStart) {
          // Merge into REPLACE
          result.add(
              new Edit(DiffSegment.Type.REPLACE, current.origStart, current.origLength, next.newStart, next.newLength));
          i++;
          continue;
        }
      }

      result.add(current);
    }

    return result;
  }
}

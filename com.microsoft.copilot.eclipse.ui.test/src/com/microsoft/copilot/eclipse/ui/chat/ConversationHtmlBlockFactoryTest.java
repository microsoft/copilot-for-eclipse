// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import com.google.gson.Gson;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationAction;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationActionScope;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationContent;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentToolCall;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ThinkingBlockData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ToolCallData;


class ConversationHtmlBlockFactoryTest {

  private static final Gson GSON = new Gson();

  private ConversationHtmlBlockFactory factory;

  @BeforeEach
  void setUp() {
    factory = new ConversationHtmlBlockFactory();
  }

  @Nested
  class EscapeHtmlTests {

    @Test
    void returnsEmptyStringForNull() {
      assertEquals("", ConversationHtmlBlockFactory.escapeHtml(null));
    }

    @ParameterizedTest(name = "escapeHtml(\"{0}\") = \"{1}\"")
    @CsvSource({
        "'<script>', '&lt;script&gt;'",
        "'a & b', 'a &amp; b'",
        "'\"quoted\"', '&quot;quoted&quot;'",
        "'<a href=\"x\">&', '&lt;a href=&quot;x&quot;&gt;&amp;'",
        "'normal text', 'normal text'",
        "'Hello, world! 123 äöü', 'Hello, world! 123 äöü'"
    })
    void escapesSpecialHtmlCharacters(String input, String expected) {
      assertEquals(expected, ConversationHtmlBlockFactory.escapeHtml(input));
    }

    @Test
    void returnsEmptyForEmptyInput() {
      assertEquals("", ConversationHtmlBlockFactory.escapeHtml(""));
    }
  }

  @Nested
  class RenderMarkdownTests {

    @ParameterizedTest
    @NullAndEmptySource
    void returnsEmptyForNullOrEmpty(String input) {
      assertEquals("", factory.renderMarkdown(input));
    }

    @Test
    void rendersGfmTable() {
      String md = """
          | A | B |
          |---|---|
          | 1 | 2 |
          """;
      String html = factory.renderMarkdown(md);
      assertTrue(html.contains("<table"), "Should contain <table>");
      assertTrue(html.contains("</table>"), "Should contain </table>");
      assertTrue(html.contains("<th>A</th>"), "Should contain header cell A");
      assertTrue(html.contains("<th>B</th>"), "Should contain header cell B");
      assertTrue(html.contains("<td>1</td>"), "Should contain data cell 1");
      assertTrue(html.contains("<td>2</td>"), "Should contain data cell 2");
      assertTrue(html.contains("<thead>"), "Should have thead");
      assertTrue(html.contains("<tbody>"), "Should have tbody");
    }

    @Test
    void rendersFencedCodeBlockWithLanguageClass() {
      String md = """
          ```java
          int x = 1;
          ```
          """;
      String html = factory.renderMarkdown(md);
      assertTrue(html.contains("<pre>"), "Should contain <pre>");
      assertTrue(html.contains("language-java"), "Should have language class");
      assertTrue(html.contains("int x = 1;"), "Should contain code content");
    }

    @Test
    void fencedCodeBlockGetsActionButtons() {
      String md = """
          ```
          code
          ```
          """;
      String html = factory.renderMarkdown(md);
      assertTrue(html.contains("data-action=\"copy\""), "Should inject copy button");
      assertTrue(html.contains("data-action=\"insert\""), "Should inject insert button");
    }

    @Test
    void rendersTaskListItems() {
      String md = """
          - [x] Done
          - [ ] Pending
          """;
      String html = factory.renderMarkdown(md);
      assertTrue(html.contains("type=\"checkbox\""), "Should contain checkbox");
      assertTrue(html.contains("checked"), "Should mark done item as checked");
    }

    @Test
    void rendersInlineCodeWithoutActionButtons() {
      String html = factory.renderMarkdown("Use `foo()` here");
      assertTrue(html.contains("<code>foo()</code>"), "Should wrap inline code");
      assertFalse(html.contains("data-action=\"copy\""),
          "Inline code should NOT get copy/insert buttons");
    }

    @Test
    void rendersBasicMarkdown() {
      String result = factory.renderMarkdown("**bold** text");
      assertTrue(result.contains("<strong>bold</strong>"),
          "Should render bold as <strong>");
    }

    @Test
    void escapesHtmlEntitiesInCodeBlocks() {
      String md = """
          ```
          if (a < b && c > d) {}
          ```
          """;
      String html = factory.renderMarkdown(md);
      assertTrue(html.contains("&lt;"), "Should escape < in code");
      assertTrue(html.contains("&amp;"), "Should escape & in code");
    }
  }

  @Nested
  class MarkdownSecurityAndEdgeCaseTests {

    @Test
    void scriptTagInMarkdown_isEscapedNotExecutable() {
      String html = factory.renderMarkdown("<script>alert('xss')</script>");
      assertFalse(html.contains("<script>"), "Raw <script> must not appear");
      assertTrue(html.contains("&lt;script&gt;"), "Script tag must be entity-escaped");
    }

    @Test
    void rawHtmlDivInMarkdown_isEscapedNotRendered() {
      String html = factory.renderMarkdown("<div onclick=\"evil()\">click me</div>");
      assertFalse(html.contains("<div"), "Raw HTML div must not be rendered");
      assertTrue(html.contains("&lt;div"), "Div tag must be entity-escaped");
    }

    @Test
    void imgTagWithOnerror_isEscaped() {
      String html = factory.renderMarkdown("<img src=x onerror=alert(1)>");
      assertFalse(html.contains("<img"), "Raw img tag must not be rendered");
      assertTrue(html.contains("&lt;img"), "Img tag must be entity-escaped");
    }

    @Test
    void javascriptUrlInLink_isSanitized() {
      String html = factory.renderMarkdown("[click](javascript:alert(1))");
      assertFalse(html.contains("javascript:"), "javascript: URL must be removed");
      assertTrue(html.contains("click"), "Link text should still appear");
    }

    @Test
    void codeBlockInsideListItem() {
      String md = """
          - Item with code:
            ```java
            int x = 1;
            ```
          - Second item""";
      String html = factory.renderMarkdown(md);
      assertTrue(html.contains("<li>"), "Should render list items");
      assertTrue(html.contains("<pre>"), "Should render code block");
      assertTrue(html.contains("int x = 1;"), "Should contain code content");
      assertTrue(html.contains("Second item"), "Should render second item");
    }

    @Test
    void tableWithSpecialCharsInCells() {
      String md = """
          | Expression    | Result |
          |---------------|--------|
          | `a < b`       | true   |
          | `x & y`       | false  |
          | pipe \\| char  | ok     |
          """;
      String html = factory.renderMarkdown(md);
      assertTrue(html.contains("<table"), "Should render table");
      assertTrue(html.contains("<code>a &lt; b</code>"), "Should escape < inside code in cell");
      assertTrue(html.contains("true"), "Should contain result column values");
    }

    @Test
    void emptyFencedCodeBlock() {
      String md = """
          ```
          ```""";
      String html = factory.renderMarkdown(md);
      assertTrue(html.contains("<pre>"), "Should render empty code block");
      assertTrue(html.contains("<code>"), "Should contain code element");
    }

    @Test
    void whitespaceOnlyInput_returnsEmpty() {
      assertEquals("", factory.renderMarkdown("   \n\t\n  "));
    }

    @Test
    void nestedEmphasisInsideBlockquote() {
      String html = factory.renderMarkdown("> **bold** and *italic* in quote");
      assertTrue(html.contains("<blockquote>"), "Should render blockquote");
      assertTrue(html.contains("<strong>bold</strong>"), "Should render bold");
      assertTrue(html.contains("<em>italic</em>"), "Should render italic");
    }

    @Test
    void multipleConsecutiveCodeBlocks() {
      String md = """
          ```python
          print("hello")
          ```

          ```javascript
          console.log("world")
          ```""";
      String html = factory.renderMarkdown(md);
      assertTrue(html.contains("language-python"), "Should have python language class");
      assertTrue(html.contains("language-javascript"), "Should have javascript language class");
      // Both blocks should get action buttons
      int copyCount = countOccurrences(html, "data-action=\"copy\"");
      assertEquals(2, copyCount, "Both code blocks should get copy buttons");
    }

    private int countOccurrences(String text, String sub) {
      int count = 0;
      int idx = 0;
      while ((idx = text.indexOf(sub, idx)) != -1) {
        count++;
        idx += sub.length();
      }
      return count;
    }
  }

  @Nested
  class InjectCodeBlockButtonsTests {

    @Test
    void insertsButtonsBeforeClosingPreTag() {
      String input = "<pre><code>int x = 1;</code></pre>";
      String result = factory.injectCodeBlockButtons(input);
      assertTrue(result.contains("code-action-btn"), "Should inject action buttons");
      assertTrue(result.contains("data-action=\"copy\""), "Should have copy button");
      assertTrue(result.contains("data-action=\"insert\""), "Should have insert button");
      assertTrue(result.contains("int x = 1;"), "Should preserve code content");
      // Buttons must appear before </pre>
      assertTrue(result.indexOf("code-actions") < result.indexOf("</pre>"));
    }

    @Test
    void handlesMultiplePreBlocks() {
      String input = "<pre><code>a</code></pre><p>text</p><pre><code>b</code></pre>";
      String result = factory.injectCodeBlockButtons(input);
      int first = result.indexOf("code-actions");
      int second = result.indexOf("code-actions", first + 1);
      assertTrue(second > first, "Should inject into both <pre> blocks");
    }

    @Test
    void leavesHtmlWithoutPreUnchanged() {
      String html = "<p>No code here</p>";
      assertEquals(html, factory.injectCodeBlockButtons(html));
    }
  }

  @Nested
  class ToolCallBlockTests {

    @Test
    void completedStatus_showsCheckmarkAndSuccessClass() {
      AgentToolCall tc = agentToolCall("file_search", "completed", "Found 3 files");
      String html = factory.createToolCallHtmlBlock("t1-tc-0", tc);

      assertTrue(html.contains("tc-completed"), "Should have completed class");
      assertTrue(html.contains("tc-success"), "Should have success icon class");
      assertTrue(html.contains("&#x2713;"), "Should show checkmark");
      assertTrue(html.contains("file_search"), "Should show tool name");
      assertTrue(html.contains("Found 3 files"), "Should show progress");
    }

    @Test
    void errorStatus_showsCrossAndFailedClass() {
      AgentToolCall tc = agentToolCall("edit_file", "error", "Permission denied");
      String html = factory.createToolCallHtmlBlock("t1-tc-1", tc);

      assertTrue(html.contains("tc-failed"), "Should have tc-failed class");
      assertTrue(html.contains("tc-error"), "Should have error icon class");
      assertTrue(html.contains("&#x2717;"), "Should show cross");
      assertTrue(html.contains("Permission denied"), "Should show progress");
    }

    @Test
    void cancelledStatus_showsCrossAndFailedClass() {
      AgentToolCall tc = agentToolCall("grep_search", "cancelled", null);
      String html = factory.createToolCallHtmlBlock("t1-tc-2", tc);

      assertTrue(html.contains("tc-failed"), "Should have tc-failed class");
      assertTrue(html.contains("tc-cancelled"), "Should have cancelled icon class");
      assertTrue(html.contains("&#x2717;"), "Should show cross");
    }

    @Test
    void runningStatus_showsSpinner() {
      AgentToolCall tc = agentToolCall("grep_search", "running", null);
      String html = factory.createToolCallHtmlBlock("t1-tc-3", tc);

      assertTrue(html.contains("tc-running"), "Should have running icon class");
      assertTrue(html.contains("thinking-spinner"), "Should show spinner");
      assertFalse(html.contains("tc-completed"), "Should NOT have completed class");
      assertFalse(html.contains("tool-call-progress"),
          "Should NOT show progress span when message is null");
    }

    @Test
    void escapesToolNameAndProgressMessage() {
      AgentToolCall tc = agentToolCall("<script>alert(1)</script>", "completed",
          "path with & special <chars>");
      String html = factory.createToolCallHtmlBlock("t1-tc-4", tc);

      assertTrue(html.contains("&lt;script&gt;"), "Tool name should be escaped");
      assertTrue(html.contains("&amp; special &lt;chars&gt;"),
          "Progress should be escaped");
    }

    @Test
    void restoredToolCallBlock_matchesLiveToolCallBehavior() {
      ToolCallData tc = new ToolCallData();
      tc.setName("readFile");
      tc.setStatus("completed");
      tc.setProgressMessage("src/main.java");

      String html = factory.createRestoredToolCallHtmlBlock("t1-tc-5", tc);

      assertTrue(html.contains("tc-completed"));
      assertTrue(html.contains("&#x2713;"));
      assertTrue(html.contains("readFile"));
      assertTrue(html.contains("src/main.java"));
    }
  }

  @Nested
  class ModelInfoBlockTests {

    @Test
    void rendersModelNameOnly_whenBillingIsOneAndNoEffort() {
      String html = factory.createModelInfoHtmlBlock("b1", "GPT-5 mini", 1.0, null);

      assertTrue(html.contains("GPT-5 mini"), "Should show model name");
      assertFalse(html.contains("billing-multiplier"),
          "Should omit billing when multiplier is 1.0");
      assertFalse(html.contains("reasoning-effort"),
          "Should omit effort when null");
    }

    @Test
    void includesBillingMultiplier_whenNotOne() {
      String html = factory.createModelInfoHtmlBlock("b1", "gpt-4o", 2.0, null);

      assertTrue(html.contains("(2.0x)"), "Should show multiplier");
      assertTrue(html.contains("billing-multiplier"));
    }

    @Test
    void includesCapitalizedReasoningEffort() {
      String html = factory.createModelInfoHtmlBlock("b1", "GPT-5", 1.0, "medium");

      assertTrue(html.contains("- Medium"), "Should capitalize and prefix with dash");
      assertTrue(html.contains("reasoning-effort"));
    }

    @Test
    void omitsBillingMultiplier_whenZero() {
      String html = factory.createModelInfoHtmlBlock("b1", "Model", 0, "high");

      assertFalse(html.contains("billing-multiplier"),
          "Should omit billing when multiplier is 0");
      assertTrue(html.contains("- High"));
    }

    @Test
    void omitsEffort_whenEmpty() {
      String html = factory.createModelInfoHtmlBlock("b1", "Model", 1.0, "");

      assertFalse(html.contains("reasoning-effort"),
          "Should omit effort when empty string");
    }
  }

  @Nested
  class ConfirmationBlockTests {

    @Test
    void rendersFullConfirmation_withCommandAndButtons() {
      ConfirmationContent content = new ConfirmationContent(
          "Run in terminal?",
          "This will execute a command.",
          List.of(
              ConfirmationAction.allowOnce("Allow"),
              ConfirmationAction.skip("Deny")));

      Map<String, Object> input = Map.of(
          "command", "npm install",
          "explanation", "Installs dependencies");

      String html = factory.createConfirmationHtmlBlock("confirm-1", content, input);

      assertTrue(html.contains("Run in terminal?"), "Should show title");
      assertTrue(html.contains("This will execute a command."), "Should show message");
      assertTrue(html.contains("npm install"), "Should show command");
      assertTrue(html.contains("Installs dependencies"), "Should show explanation");
      assertTrue(html.contains("btn-primary"), "Primary button should have class");
      assertTrue(html.contains("window.acceptToolAction(0)"),
          "Accept button should call acceptToolAction");
      assertTrue(html.contains("window.dismissToolAction()"),
          "Deny button should call dismissToolAction");
      assertTrue(html.contains("Allow"), "Should show accept label");
      assertTrue(html.contains("Deny"), "Should show deny label");
    }

    @Test
    void omitsOptionalFields_whenAbsent() {
      ConfirmationContent content = new ConfirmationContent(
          "Confirm action?", null, List.of());

      String html = factory.createConfirmationHtmlBlock("confirm-2", content, "not-a-map");

      assertTrue(html.contains("Confirm action?"), "Should show title");
      assertFalse(html.contains("confirmation-message"),
          "Should omit message div when null");
      assertFalse(html.contains("confirmation-command"),
          "Should omit command when input is not a Map");
    }

    @Test
    void escapesHtmlInTitleAndMessage() {
      ConfirmationContent content = new ConfirmationContent(
          "<b>Title</b>", "msg with & special", List.of());

      String html = factory.createConfirmationHtmlBlock("confirm-3", content, null);

      assertTrue(html.contains("&lt;b&gt;Title&lt;/b&gt;"),
          "Title should be escaped");
      assertTrue(html.contains("msg with &amp; special"),
          "Message should be escaped");
    }

    @Test
    void rendersPlainPrimaryButton_whenSingleAcceptAction() {
      ConfirmationContent content = new ConfirmationContent(
          "Run in terminal?", null,
          List.of(
              ConfirmationAction.allowOnce("Allow"),
              ConfirmationAction.skip("Deny")));

      String html = factory.createConfirmationHtmlBlock("confirm-4", content, null);

      assertTrue(html.contains("btn-confirm btn-primary"),
          "Single accept action should be a plain primary button");
      assertFalse(html.contains("split-button"),
          "No split-button wrapper without alternative accept actions");
      assertFalse(html.contains("dropdown-menu"),
          "No dropdown without alternative accept actions");
      assertTrue(html.contains("window.acceptToolAction(0)"),
          "Primary button should accept action index 0");
      assertTrue(html.contains("window.dismissToolAction()"),
          "Deny button should call dismissToolAction");
    }

    @Test
    void rendersSplitButtonDropdown_whenMultipleAcceptActions() {
      ConfirmationContent content = new ConfirmationContent(
          "Run in terminal?", null,
          List.of(
              ConfirmationAction.allowOnce("Allow once"),
              new ConfirmationAction("Allow for session", true,
                  ConfirmationActionScope.SESSION, null, false),
              new ConfirmationAction("Always allow", true,
                  ConfirmationActionScope.GLOBAL, null, false),
              ConfirmationAction.skip("Deny")));

      String html = factory.createConfirmationHtmlBlock("confirm-5", content, null);

      assertTrue(html.contains("class=\"split-button\""),
          "Multiple accept actions should produce a split-button");
      assertTrue(html.contains("split-primary"), "Primary sub-button should be present");
      assertTrue(html.contains("data-dropdown-toggle"), "Caret toggle should be present");
      assertTrue(html.contains("class=\"dropdown-menu\" hidden"),
          "Dropdown menu should start hidden");
      // Primary (index 0) is the button; alternatives (indexes 1 and 2) are dropdown items.
      assertTrue(html.contains("window.acceptToolAction(0)"), "Primary uses index 0");
      assertTrue(html.contains("window.acceptToolAction(1)"), "First alternative uses index 1");
      assertTrue(html.contains("window.acceptToolAction(2)"), "Second alternative uses index 2");
      assertTrue(html.contains("Allow for session") && html.contains("Always allow"),
          "Dropdown should list the alternative accept actions");
      assertTrue(html.contains("window.dismissToolAction()"),
          "Dismiss stays a separate button");
    }
  }

  @Nested
  class ThinkingBlockTests {

    @Test
    void activeThinkingBlock_isOpenWithSpinner() {
      String html = factory.createThinkingHtmlBlock("think-1", "Analyzing code...");

      assertTrue(html.contains("<details open>"), "Should be open during streaming");
      assertTrue(html.contains("thinking-spinner"), "Should show spinner");
      assertTrue(html.contains("Thinking&#x2026;"), "Should have 'Thinking...' summary");
      assertTrue(html.contains("Analyzing code..."), "Should include thinking text");
      assertTrue(html.contains("class=\"thinking-block\""));
    }

    @Test
    void sealedThinkingBlock_isClosedWithCustomTitle() {
      String html = factory.createSealedThinkingHtmlBlock(
          "think-2", "Done.", "Analysis Complete");

      assertTrue(html.contains("<details>"), "Should have <details>");
      assertFalse(html.contains("<details open>"), "Should NOT be open");
      assertFalse(html.contains("thinking-spinner"), "Should NOT show spinner");
      assertTrue(html.contains("Analysis Complete"), "Should show custom title");
      assertTrue(html.contains("Done."), "Should have content");
    }

    @Test
    void sealedThinkingBlock_usesDefaultTitle_whenBlank() {
      String html = factory.createSealedThinkingHtmlBlock("think-3", "text", null);
      assertTrue(html.contains("Thinking&#x2026;"),
          "Should fall back to default title when null");

      html = factory.createSealedThinkingHtmlBlock("think-4", "text", "");
      assertTrue(html.contains("Thinking&#x2026;"),
          "Should fall back to default title when empty");
    }

    @Test
    void restoredThinkingBlock_delegatesToSealed() {
      ThinkingBlockData data = new ThinkingBlockData("tb-1", "Analyzing...");
      data.setTitle("Code Analysis");

      String html = factory.createRestoredThinkingHtmlBlock("think-5", data);

      assertTrue(html.contains("Code Analysis"));
      assertTrue(html.contains("<details>"));
      assertFalse(html.contains("<details open>"));
      assertTrue(html.contains("Analyzing..."));
    }

    @Test
    void escapesHtmlInThinkingContent() {
      String html = factory.createThinkingHtmlBlock("think-6", "if (a < b) { }");
      assertTrue(html.contains("a &lt; b"), "Should escape HTML in thinking text");
    }

    @Test
    void activeThinkingBlock_rendersMarkdownBold() {
      String html = factory.createThinkingHtmlBlock("think-7", "This is **important** text");
      assertTrue(html.contains("<strong>important</strong>"),
          "Bold Markdown in a streaming thinking body should render as <strong>");
    }

    @Test
    void sealedThinkingBlock_rendersMarkdownInBodyAndTitle() {
      String html = factory.createSealedThinkingHtmlBlock(
          "think-8", "Considered **options** and **trade-offs**", "**Bold** title");

      assertTrue(html.contains("<strong>options</strong>"),
          "Bold Markdown in the thinking body should render as <strong>");
      assertTrue(html.contains("<strong>Bold</strong> title"),
          "Bold Markdown in the thinking title should render inline as <strong>");
    }

    @Test
    void renderMarkdownInline_stripsWrappingParagraph() {
      String html = factory.renderMarkdownInline("**Bold** title");
      assertEquals("<strong>Bold</strong> title", html,
          "Inline render should strip the wrapping <p> commonmark emits for a lone paragraph");
    }

    @Test
    void renderMarkdownInline_returnsEmpty_forNullOrBlank() {
      assertEquals("", factory.renderMarkdownInline(null));
      assertEquals("", factory.renderMarkdownInline(""));
    }
  }

  @Nested
  class TurnContainerAndOtherBlockTests {

    @Test
    void createTurnContainerHtmlBlock_copilotTurn() {
      String html = factory.createTurnContainerHtmlBlock(
          "turn-123", true, "data:image/png;base64,ABC", "GitHub Copilot");

      assertTrue(html.contains("id=\"turn-123-copilot\""));
      assertTrue(html.contains("turn-copilot"));
      assertTrue(html.contains("turn-avatar"));
      assertTrue(html.contains("GitHub Copilot"));
      assertTrue(html.contains("id=\"turn-123-copilot-content\""));
    }

    @Test
    void createTurnContainerHtmlBlock_userTurn() {
      String html = factory.createTurnContainerHtmlBlock(
          "turn-456", false, null, "TestUser");

      assertTrue(html.contains("id=\"turn-456-user\""));
      assertTrue(html.contains("turn-user"));
      assertTrue(html.contains("TestUser"));
      assertTrue(html.contains("id=\"turn-456-user-content\""));
      assertFalse(html.contains("turn-avatar"),
          "Should omit avatar img when dataUri is null");
    }

    @Test
    void sameTurnId_producesDifferentContainerIds() {
      String user = factory.createTurnContainerHtmlBlock("t1", false, null, "U");
      String copilot = factory.createTurnContainerHtmlBlock("t1", true, null, "C");

      assertTrue(user.contains("id=\"t1-user\""));
      assertTrue(copilot.contains("id=\"t1-copilot\""));
      assertFalse(user.contains("-copilot"));
      assertFalse(copilot.contains("-user\""));
    }

    @Test
    void createCopilotReplyHtmlBlock_rendersMarkdown() {
      String html = factory.createCopilotReplyHtmlBlock("b1", "**hello**");

      assertTrue(html.contains("id=\"b1\""));
      assertTrue(html.contains("class=\"response\""));
      assertTrue(html.contains("<strong>hello</strong>"));
    }

    @Test
    void createUserRequestHtmlBlock_rendersMarkdown() {
      String html = factory.createUserRequestHtmlBlock("b2", "How do I **sort**?");

      assertTrue(html.contains("id=\"b2\""));
      assertTrue(html.contains("class=\"user-request\""));
      assertTrue(html.contains("<strong>sort</strong>"));
    }

    @Test
    void createErrorMessageHtmlBlock() {
      String html = factory.createErrorMessageHtmlBlock("e1", "Rate limit exceeded");

      assertTrue(html.contains("id=\"e1\""));
      assertTrue(html.contains("class=\"warning-message\""));
      assertTrue(html.contains("Rate limit exceeded"));
    }

    @Test
    void createWarningMessageHtmlBlockWithActions() {
      List<QuotaActions.QuotaAction> actions = List.of(
          new QuotaActions.QuotaAction("Upgrade Plan", "Upgrade tooltip",
              "https://example.com/upgrade", true),
          new QuotaActions.QuotaAction("Enable Usage", "Usage tooltip",
              "https://example.com/usage", false));

      String html = factory.createWarningMessageHtmlBlock("w1", "Quota exceeded", actions);

      assertTrue(html.contains("id=\"w1\""));
      assertTrue(html.contains("class=\"warning-message\""));
      assertTrue(html.contains("Quota exceeded"));
      assertTrue(html.contains("class=\"warning-actions\""));
      assertTrue(html.contains("btn-confirm btn-primary"));
      assertTrue(html.contains("Upgrade Plan"));
      assertTrue(html.contains("Enable Usage"));
      assertTrue(html.contains("data-copilot-action=\"openLink\""));
      assertTrue(html.contains("data-copilot-param=\"https://example.com/upgrade\""));
      assertFalse(html.contains("onclick="));
    }

    @Test
    void createWarningMessageHtmlBlockWithActions_keepsApostrophesInDataAttribute() {
      List<QuotaActions.QuotaAction> actions = List.of(
          new QuotaActions.QuotaAction("Upgrade Plan", "It's available",
              "https://example.com/plan's/upgrade", true));

      String html = factory.createWarningMessageHtmlBlock("w1", "Quota exceeded", actions);

      assertTrue(html.contains("data-copilot-action=\"openLink\""));
      assertTrue(html.contains("data-copilot-param=\"https://example.com/plan's/upgrade\""));
      assertTrue(html.contains("title=\"It's available\""));
      assertFalse(html.contains("onclick="));
    }

    @Test
    void createWarningMessageHtmlBlockWithActions_escapesDoubleQuotesInDataAttribute() {
      List<QuotaActions.QuotaAction> actions = List.of(
          new QuotaActions.QuotaAction("Upgrade Plan", "Say \"hi\"",
              "https://example.com/a?q=\"x\"", true));

      String html = factory.createWarningMessageHtmlBlock("w1", "Quota exceeded", actions);

      // The URL delimiter is a double quote, so a double quote in the value must be entity-escaped
      // to keep the attribute well-formed.
      assertTrue(html.contains("data-copilot-param=\"https://example.com/a?q=&quot;x&quot;\""));
      assertFalse(html.contains("data-copilot-param=\"https://example.com/a?q=\"x\"\""));
      assertTrue(html.contains("title=\"Say &quot;hi&quot;\""));
      assertFalse(html.contains("onclick="));
    }

    @Test
    void createWarningMessageHtmlBlockWithoutActions() {
      String html = factory.createWarningMessageHtmlBlock("w2", "Generic error", List.of());

      assertTrue(html.contains("id=\"w2\""));
      assertTrue(html.contains("class=\"warning-message\""));
      assertTrue(html.contains("Generic error"));
      assertFalse(html.contains("warning-actions"));
    }

    @Test
    void createStreamingIndicatorHtmlBlock() {
      String html = factory.createStreamingIndicatorHtmlBlock("s1");

      assertTrue(html.contains("id=\"s1\""));
      assertTrue(html.contains("class=\"streaming-indicator\""));
      assertTrue(html.contains("class=\"dot\""));
    }

    @Test
    void createCompactingStatusHtmlBlock() {
      String html = factory.createCompactingStatusHtmlBlock("c1");

      assertTrue(html.contains("id=\"c1\""));
      assertTrue(html.contains("class=\"compacting-status\""));
      assertTrue(html.contains("Compacting"));
    }

    @Test
    void createAgentMessageHtmlBlock() {
      String html = factory.createAgentMessageHtmlBlock(
          "msg-1", "PR Created", "Description", "https://github.com/pr/1");

      assertTrue(html.contains("id=\"msg-1\""));
      assertTrue(html.contains("class=\"message-card agent-message\""));
      assertTrue(html.contains("PR Created"));
      assertTrue(html.contains("Description"));
      assertTrue(html.contains("data-copilot-action=\"openLink\""));
      assertTrue(html.contains("data-copilot-param=\"https://github.com/pr/1\""));
      assertTrue(html.contains("data-copilot-action=\"openJobList\""));
      assertFalse(html.contains("onclick="));
    }

    @Test
    void createAgentMessageHtmlBlock_keepsApostrophesInActionParameter() {
      String html = factory.createAgentMessageHtmlBlock(
          "msg-1", "PR Created", "Description", "https://github.com/pr/it's-1");

      assertTrue(html.contains("data-copilot-param=\"https://github.com/pr/it's-1\""));
      assertFalse(html.contains("onclick="));
    }

    @Test
    void createAgentMessageHtmlBlock_escapesDoubleQuotesInActionParameter() {
      String html = factory.createAgentMessageHtmlBlock(
          "msg-1", "PR Created", "Description", "https://github.com/pr?q=\"x\"");

      // The URL delimiter is a double quote, so a double quote in the value must be entity-escaped.
      assertTrue(html.contains("data-copilot-param=\"https://github.com/pr?q=&quot;x&quot;\""));
      assertFalse(html.contains("onclick="));
    }
  }

  @Nested
  class BlockIdTests {

    @Test
    void userAndCopilotContainerIds_doNotCollide() {
      assertEquals("t1-user", ConversationHtmlBlockFactory.userTurnContainerId("t1"));
      assertEquals("t1-copilot",
          ConversationHtmlBlockFactory.copilotTurnContainerId("t1"));
    }

    @Test
    void contentBlockId_scopedByRole() {
      assertEquals("t1-user-content",
          ConversationHtmlBlockFactory.contentBlockId("t1", false));
      assertEquals("t1-copilot-content",
          ConversationHtmlBlockFactory.contentBlockId("t1", true));
    }

    @Test
    void otherIdFormats() {
      assertEquals("t1-5", ConversationHtmlBlockFactory.copilotChildBlockId("t1", 5));
      assertEquals("t1-tc-3", ConversationHtmlBlockFactory.toolCallBlockId("t1", 3));
      assertEquals("t1-compacting", ConversationHtmlBlockFactory.compactingBlockId("t1"));
      assertEquals("t1-model-info", ConversationHtmlBlockFactory.modelInfoBlockId("t1"));
      assertEquals("t1-streaming",
          ConversationHtmlBlockFactory.streamingIndicatorId("t1"));
      assertEquals("t1-confirm",
          ConversationHtmlBlockFactory.confirmationBlockId("t1"));
    }
  }

  // Creates an AgentToolCall using Gson (class has no setters).
  private static AgentToolCall agentToolCall(String name, String status,
      String progressMessage) {
    String json = GSON.toJson(Map.of(
        "name", name,
        "status", status,
        "progressMessage", progressMessage != null ? progressMessage : ""));
    AgentToolCall tc = GSON.fromJson(json, AgentToolCall.class);
    // Gson sets empty string; clear to null if that's what we want
    if (progressMessage == null) {
      // Use Gson again with explicit null
      String jsonNull = "{\"name\":\"" + name + "\",\"status\":\"" + status + "\"}";
      tc = GSON.fromJson(jsonNull, AgentToolCall.class);
    }
    return tc;
  }

  @Nested
  class SubagentBlockTests {

    @Test
    void subagentBlockId_combinesParentTurnAndToolCall() {
      assertEquals("turn-1-subagent-tc-9",
          ConversationHtmlBlockFactory.subagentBlockId("turn-1", "tc-9"));
    }

    @Test
    void subagentContentAreaId_suffixesBlockIdWithContent() {
      assertEquals("turn-1-subagent-tc-9-content",
          ConversationHtmlBlockFactory.subagentContentAreaId("turn-1", "tc-9"));
    }

    @Test
    void createSubagentBlockHtmlBlock_nestsContentAreaInsideBorderedBlock() {
      String blockId = ConversationHtmlBlockFactory.subagentBlockId("turn-1", "tc-9");
      String contentAreaId = ConversationHtmlBlockFactory.subagentContentAreaId("turn-1", "tc-9");
      String html = factory.createSubagentBlockHtmlBlock(
          blockId, contentAreaId, "data:image/png;base64,AAAA", "Investigated the failing test");

      assertTrue(html.contains("id=\"turn-1-subagent-tc-9\""), "Outer block carries its id");
      assertTrue(html.contains("class=\"message-card subagent-message-block\""),
          "Shares SWT subagent CSS class for parity");
      assertTrue(html.contains("id=\"turn-1-subagent-tc-9-content\""),
          "Inner content area carries its id");
      assertTrue(html.contains("class=\"subagent-content\""), "Inner content area is styled");
      assertTrue(html.contains("class=\"block-title\""), "Card has a header");
      assertTrue(html.contains("class=\"subagent-avatar\""), "Header shows the copilot avatar");
      assertTrue(html.contains("Investigated the failing test"), "Header shows the title");
      // Content area must be nested inside the outer block.
      assertTrue(html.indexOf(blockId) < html.indexOf(contentAreaId),
          "Content area is nested inside the outer block");
    }

    @Test
    void createSubagentBlockHtmlBlock_fallsBackToDefaultTitle() {
      String blockId = ConversationHtmlBlockFactory.subagentBlockId("turn-1", "tc-9");
      String contentAreaId = ConversationHtmlBlockFactory.subagentContentAreaId("turn-1", "tc-9");
      String html = factory.createSubagentBlockHtmlBlock(blockId, contentAreaId, null, null);

      assertTrue(html.contains("Subagent"), "Falls back to a default title");
      assertFalse(html.contains("class=\"subagent-avatar\""), "No avatar img when uri is blank");
    }
  }
}

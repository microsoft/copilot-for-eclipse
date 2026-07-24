// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.lsp4j.WorkDoneProgressKind;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.mockito.Mockito;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationAction;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationActionScope;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationContent;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.Thinking;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CheckQuotaResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.core.persistence.AbstractTurnData;
import com.microsoft.copilot.eclipse.core.persistence.ConversationDataFactory;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.AgentMessageData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.EditAgentRoundData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ErrorData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ErrorMessageData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ReplyData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ThinkingBlockData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ThinkingBlockState;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ToolCallData;
import com.microsoft.copilot.eclipse.core.persistence.UserTurnData;
import com.microsoft.copilot.eclipse.core.persistence.UserTurnData.MessageData;
import com.microsoft.copilot.eclipse.ui.chat.services.AvatarService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatFontService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;

/**
 * Side-by-side visual comparison of {@link BrowserConversationWidget} (left) and
 * {@link StyledTextConversationWidget} (right) rendering identical conversation content.
 *
 * <p><b>How to run:</b> Launch the test Eclipse Application (include the
 * {@code com.microsoft.copilot.eclipse.ui.test} bundle), then open the Copilot menu and select
 * "Compare Chat View Rendering Alternatives".
 *
 * <p>Both widgets require a running Eclipse workbench (PlatformUI, OSGi bundle classloaders).
 * This handler cannot run as a standalone Java application.
 */
public class ConversationRendererVisualDemoSideBySide extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    openSideBySide(Display.getCurrent());
    return null;
  }

  /**
   * Opens the side-by-side comparison shell.
   */
  public static void openSideBySide(Display display) {
    Shell shell = new Shell(display);
    shell.setText("Conversation Renderer \u2014 Side-by-Side Comparison");
    shell.setSize(1600, 1000);
    shell.setLayout(new GridLayout(1, false));

    // --- Theme toggle buttons ---
    Composite toolbar = new Composite(shell, SWT.NONE);
    toolbar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    toolbar.setLayout(new GridLayout(3, false));
    Label themeLabel = new Label(toolbar, SWT.NONE);
    themeLabel.setText("Theme:");
    Button darkButton = new Button(toolbar, SWT.PUSH);
    darkButton.setText("Dark");
    Button lightButton = new Button(toolbar, SWT.PUSH);
    lightButton.setText("Light");

    // --- SashForm with both widgets ---
    SashForm sash = new SashForm(shell, SWT.HORIZONTAL);
    sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    ChatServiceManager demoServiceManager = createDemoChatServiceManager();

    // --- Left: Browser-based renderer ---
    Composite leftPane = createLabeledPane(sash,
        "BrowserConversationWidget (HTML/CSS)");
    BrowserConversationWidget browserWidget =
        new BrowserConversationWidget(leftPane, demoServiceManager);

    // --- Right: StyledText-based renderer ---
    Composite rightPane = createLabeledPane(sash,
        "StyledTextConversationWidget (SWT StyledText)");
    StyledTextConversationWidget[] styledTextHolder = {
        new StyledTextConversationWidget(rightPane, demoServiceManager)};

    sash.setWeights(50, 50);

    // Theme toggle listeners — recreate StyledText widget to pick up new theme colors
    darkButton.addListener(SWT.Selection, e -> {
      Browser browser = (Browser) browserWidget.getControl();
      browser.execute("window.setTheme('dark')");
      switchEclipseTheme("org.eclipse.e4.ui.css.theme.e4_dark");
      styledTextHolder[0] = recreateStyledTextWidget(
          rightPane, demoServiceManager, styledTextHolder[0]);
    });
    lightButton.addListener(SWT.Selection, e -> {
      Browser browser = (Browser) browserWidget.getControl();
      browser.execute("window.setTheme('light')");
      switchEclipseTheme("org.eclipse.e4.ui.css.theme.e4_default");
      styledTextHolder[0] = recreateStyledTextWidget(
          rightPane, demoServiceManager, styledTextHolder[0]);
    });

    // Feed identical content to both widgets after the browser page has loaded.
    // BrowserConversationWidget needs its page loaded before insertHtmlBlockChild works,
    // so we schedule the content injection asynchronously.
    display.asyncExec(() -> feedDemoContent(browserWidget, styledTextHolder[0]));

    shell.open();
  }

  // --- Demo content constants (shared between both rendering paths) ---

  private static final String USER_MSG_1 =
      "How do I create a **GFM table** in Markdown? Also show me a code example.";

  private static final String COPILOT_REPLY_TABLE = """
      Here's a GFM table example:

      | Feature        | StyledText | Browser |
      |----------------|:----------:|:-------:|
      | Tables         | \u274c         | \u2705      |
      | Code blocks    | \u2705         | \u2705      |
      | Task lists     | \u274c         | \u2705      |
      | Inline code    | \u2705         | \u2705      |
      | Bold/Italic    | \u2705         | \u2705      |

      And here's a Java code example:

      ```java
      public class HelloWorld {
        public static void main(String[] args) {
          System.out.println("Hello, world!");
          if (args.length > 0) {
            System.out.println("Args: " + String.join(", ", args));
          }
        }
      }
      ```

      You can also use **inline code** like `System.out.println()` in your text.

      Here's a task list:

      - [x] Create table structure
      - [x] Add code block
      - [ ] Test rendering
      - [ ] Submit PR""";

  private static final String THINKING_CONTENT =
      "The user wants to know about GFM tables and code examples. "
          + "I'll provide a table example and a fenced code block with Java.";

  private static final String THINKING_TITLE = "Planning response";

  private static final String USER_MSG_2 =
      "Thanks! Can you also show me how bold and italic text render?";

  private static final String COPILOT_REPLY_FORMATTING = """
      Sure! Here are some formatting examples:

      - **Bold text** is wrapped in double asterisks
      - *Italic text* uses single asterisks
      - ***Bold and italic*** uses triple asterisks
      - ~~Strikethrough~~ uses tildes

      > This is a blockquote that can span
      > multiple lines.

      And a numbered list:

      1. First item
      2. Second item
      3. Third item""";

  private static final String ERROR_MSG = "Let me try a different approach...";
  private static final String ERROR_DETAIL =
      "Context window exceeded. The conversation is too long.";

  private static final String USER_MSG_FIX_TEST =
      "The login test is failing. Investigate it with a subagent and open a PR with the fix.";
  private static final String USER_MSG_BUILD =
      "Great. Now run the build to make sure everything still compiles.";
  private static final String USER_MSG_ERROR =
      "Summarize this entire 500-page design document in a single response.";
  private static final String USER_MSG_QUOTA =
      "Generate the full implementation using the most capable premium model.";
  private static final String USER_MSG_INVESTIGATE =
      "Before you make changes, investigate the current setup and run whatever tools you need.";
  private static final String USER_MSG_LIVE =
      "Walk me through the migration approach step by step.";

  private static final String SUBAGENT_THINKING_CONTENT =
      "The failing test points at the login flow. I'll delegate a focused investigation to a "
          + "subagent, then summarize the finding and open a pull request.";
  private static final String BUILD_REPLY_TEXT =
      "I'll run the build now. This needs to execute a terminal command:";

  private static final String AGENT_MSG_TITLE = "Pull Request Created";
  private static final String AGENT_MSG_DESC =
      "Implemented the requested feature with comprehensive unit tests and integration tests. "
          + "All existing tests continue to pass. The PR includes documentation updates as well.";
  private static final String AGENT_MSG_PR_LINK =
      "https://github.com/microsoft/copilot-for-eclipse/pull/330";

  private static final String CONFIRM_TITLE = "Run command in terminal?";
  private static final String CONFIRM_MESSAGE =
      "Copilot wants to execute a shell command.";
  private static final String CONFIRM_COMMAND = "npm install && npm run build";
  private static final String CONFIRM_COMMAND_NAME = "npm";
  private static final String CONFIRM_EXPLANATION =
      "Install dependencies and build the project.";

  private static final String THINKING_ACTIVE_TEXT =
      "Analyzing the project structure to determine the best approach. "
          + "I need to check if there are existing test files and "
          + "understand the module dependencies before making changes...";

  private static final String QUOTA_REPLY_TEXT = "I was unable to complete the request.";
  private static final String QUOTA_ERROR_MSG =
      "You've used all your premium requests for the month. "
          + "Consider upgrading your plan for continued access.";

  private static final String SUBAGENT_PARENT_REPLY =
      "I'll delegate the investigation to a subagent.";
  private static final String SUBAGENT_REPLY_TEXT =
      "Searched the codebase and found the failing assertion in `LoginServiceTest`. "
          + "The mock returns `null` where a session token is expected.";

  private static Composite createLabeledPane(Composite parent, String title) {
    Composite pane = new Composite(parent, SWT.NONE);
    pane.setLayout(new GridLayout(1, false));
    Label label = new Label(pane, SWT.NONE);
    label.setText(title);
    label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    Composite content = new Composite(pane, SWT.NONE);
    content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    content.setLayout(new FillLayout());
    return content;
  }

  /**
   * Creates a mock {@link ChatServiceManager} that reuses the real {@link AvatarService} from the
   * running Eclipse instance (for a real user avatar) and a real {@link ChatFontService}, but
   * returns a deterministic {@link CheckQuotaResult} so the quota-exceeded turn reliably renders its
   * "Upgrade Plan" action button regardless of the live account's plan or billing state.
   *
   * <p>The auth manager is a {@link Mockito#spy(Object) spy} of the real one: identity-related calls
   * (e.g. {@code getUserName()}) still return real data, while {@code getQuotaStatus()} is stubbed to
   * a {@code free} plan with token-based billing enabled and {@code canUpgradePlan = true}, which
   * {@link QuotaActions#forPlan} maps to a single primary "Upgrade Plan" action.
   */
  private static ChatServiceManager createDemoChatServiceManager() {
    ChatServiceManager mockManager = Mockito.mock(ChatServiceManager.class);

    // Spy the real AuthStatusManager so identity (avatar, user name) stays real, but override the
    // quota status with a deterministic value so the demo's Upgrade Plan button always appears.
    AuthStatusManager realAuth = CopilotCore.getPlugin().getAuthStatusManager();
    AuthStatusManager spyAuth = Mockito.spy(realAuth);
    CheckQuotaResult demoQuota = new CheckQuotaResult(
        null, null, null, null, null, CopilotPlan.free, true, Boolean.TRUE);
    Mockito.doReturn(demoQuota).when(spyAuth).getQuotaStatus();
    Mockito.when(mockManager.getAuthStatusManager()).thenReturn(spyAuth);

    // Use a real AvatarService backed by the real auth (loads GitHub avatar)
    AvatarService realAvatarService = new AvatarService(realAuth);
    Mockito.when(mockManager.getAvatarService()).thenReturn(realAvatarService);

    // ChatFontService — real instance (no OSGi dependencies in constructor)
    ChatFontService fontService = new ChatFontService();
    Mockito.when(mockManager.getChatFontService()).thenReturn(fontService);

    return mockManager;
  }

  /**
   * Feeds identical synthetic conversation data to both widgets. The conversation interleaves user
   * turns with copilot turns (so every copilot turn gets its own header). History (restored) turns
   * and live (streamed) turns are kept in separate turns: restoring a turn and then streaming into
   * the same turn id would duplicate the browser turn container and collide child-block ids. The
   * active (non-sealed) thinking block is therefore a live-only turn placed mid-conversation (never
   * the trailing round, which would otherwise reserve empty space below it in StyledText). Covers
   * all block types: text with formatting, tables, code blocks, task lists, thinking blocks (sealed
   * and active), tool calls (all statuses), tool confirmation, error and quota warnings, subagent
   * nesting, agent messages, and model info.
   */
  private static void feedDemoContent(
      BrowserConversationWidget browser, StyledTextConversationWidget styledText) {
    feedWidgets(browser, styledText);
  }

  /**
   * Feeds demo content to one or both widgets. When {@code browser} is {@code null}, only the
   * StyledText widget is populated (used after theme-switch recreation).
   */
  private static void feedWidgets(
      BrowserConversationWidget browser, StyledTextConversationWidget styledText) {

    // ConversationDataFactory needs an AuthStatusManager for tool call conversion
    AuthStatusManager mockAuth = Mockito.mock(AuthStatusManager.class);
    Mockito.when(mockAuth.getUserName()).thenReturn("demo-user");
    ConversationDataFactory dataFactory = new ConversationDataFactory(mockAuth);

    // The conversation interleaves user turns with copilot turns so every copilot turn gets its own
    // header. History (restored) turns and the single live (streamed) turn are kept separate.

    // --- Exchange 1 (turns 1-2): table + code + task list (reply only) ---
    restoreUser(browser, styledText, dataFactory, "turn-1", USER_MSG_1);

    CopilotTurnData tableTurn = new CopilotTurnData();
    tableTurn.setTurnId("turn-2");
    ReplyData tableReply = tableTurn.getReply();
    tableReply.setText(COPILOT_REPLY_TABLE);
    tableReply.setModelName("GPT-5 mini");
    tableReply.setBillingMultiplier(1.0);
    tableReply.setReasoningEffort("medium");
    restoreBoth(browser, styledText, tableTurn, dataFactory);

    // --- Exchange 2 (turns 3-4): markdown formatting examples ---
    restoreUser(browser, styledText, dataFactory, "turn-3", USER_MSG_2);

    CopilotTurnData formattingTurn = new CopilotTurnData();
    formattingTurn.setTurnId("turn-4");
    ReplyData formattingReply = formattingTurn.getReply();
    formattingReply.setText(COPILOT_REPLY_FORMATTING);
    formattingReply.setModelName("Claude Opus 4");
    formattingReply.setBillingMultiplier(2.5);
    formattingReply.setReasoningEffort("high");
    restoreBoth(browser, styledText, formattingTurn, dataFactory);

    // --- Exchange 3 (turns 5-6): sealed thinking block with tool-call examples (restored) ---
    restoreUser(browser, styledText, dataFactory, "turn-5", USER_MSG_INVESTIGATE);

    ThinkingBlockData thinkingBlock = new ThinkingBlockData();
    thinkingBlock.setId("turn-6-thinking");
    thinkingBlock.setContent(THINKING_CONTENT);
    thinkingBlock.setTitle(THINKING_TITLE);
    thinkingBlock.setState(ThinkingBlockState.COMPLETED);

    EditAgentRoundData thinkingRound = new EditAgentRoundData();
    thinkingRound.setRoundId(1);
    thinkingRound.setThinkingBlock(thinkingBlock);
    thinkingRound.setToolCalls(List.of(
        toolCall("tc-1", "file_search", "completed", "Found 3 results in src/"),
        toolCall("tc-2", "run_in_terminal", "cancelled", "User cancelled"),
        toolCall("tc-3", "edit_file", "error", "Permission denied: /etc/hosts"),
        toolCall("tc-4", "grep_search", "running", "Searching...")));

    CopilotTurnData thinkingTurn = new CopilotTurnData();
    thinkingTurn.setTurnId("turn-6");
    ReplyData thinkingReply = thinkingTurn.getReply();
    thinkingReply.setEditAgentRounds(List.of(thinkingRound));
    thinkingReply.setModelName("GPT-5 mini");
    thinkingReply.setBillingMultiplier(1.0);
    thinkingReply.setReasoningEffort("medium");
    restoreBoth(browser, styledText, thinkingTurn, dataFactory);

    // --- Exchange 4 (turns 7-8): active (non-sealed) thinking block via the live API ---
    // Kept in its own live-only turn (never mixed with a restored turn, which would duplicate the
    // browser turn container and collide child-block ids). Positioned mid-conversation so it is not
    // the trailing round — a short trailing active-thinking turn would reserve empty scroller space
    // below it in the StyledText windowed renderer.
    restoreUser(browser, styledText, dataFactory, "turn-7", USER_MSG_LIVE);

    if (browser != null) {
      browser.beginTurn("turn-8", true, false);
    }
    styledText.beginTurn("turn-8", true, false);

    ChatProgressValue thinkingEvent = new ChatProgressValue();
    thinkingEvent.setKind(WorkDoneProgressKind.report);
    thinkingEvent.setTurnId("turn-8");
    thinkingEvent.setThinking(new Thinking("turn-8-active", THINKING_ACTIVE_TEXT, null));
    if (browser != null) {
      browser.processTurnEvent(thinkingEvent);
    }
    styledText.processTurnEvent(thinkingEvent);

    // --- Exchange 5 (turns 9-10): reply + thinking + nested subagent + PR agent message ---
    restoreUser(browser, styledText, dataFactory, "turn-9", USER_MSG_FIX_TEST);

    ThinkingBlockData subThinking = new ThinkingBlockData();
    subThinking.setId("turn-10-thinking");
    subThinking.setContent(SUBAGENT_THINKING_CONTENT);
    subThinking.setTitle("Delegating investigation");
    subThinking.setState(ThinkingBlockState.COMPLETED);

    EditAgentRoundData subRound = new EditAgentRoundData();
    subRound.setRoundId(1);
    subRound.setThinkingBlock(subThinking);
    subRound.setToolCalls(List.of(
        toolCall("tc-subagent", "run_subagent", "completed", "Investigated the failing test")));

    CopilotTurnData subParentTurn = new CopilotTurnData();
    subParentTurn.setTurnId("turn-10");
    ReplyData subParentReply = subParentTurn.getReply();
    subParentReply.setText(SUBAGENT_PARENT_REPLY);
    subParentReply.setEditAgentRounds(List.of(subRound));
    AgentMessageData prMessage = new AgentMessageData();
    prMessage.setTitle(AGENT_MSG_TITLE);
    prMessage.setDescription(AGENT_MSG_DESC);
    prMessage.setPrLink(AGENT_MSG_PR_LINK);
    prMessage.setAgentSlug("github-copilot-coding-agent");
    subParentReply.setAgentMessages(List.of(prMessage));
    subParentReply.setModelName("Claude Opus 4");
    subParentReply.setBillingMultiplier(2.5);
    subParentReply.setReasoningEffort("high");
    restoreBoth(browser, styledText, subParentTurn, dataFactory);

    // Nested subagent turn (parentTurnId + subagentToolCallId route it into the subagent block).
    CopilotTurnData subChildTurn = new CopilotTurnData();
    subChildTurn.setTurnId("turn-10-sub");
    subChildTurn.setParentTurnId("turn-10");
    subChildTurn.setSubagentToolCallId("tc-subagent");
    subChildTurn.getReply().setText(SUBAGENT_REPLY_TEXT);
    restoreBoth(browser, styledText, subChildTurn, dataFactory);

    // --- Exchange 6 (turns 11-12): running tool with a live tool-confirmation request ---
    restoreUser(browser, styledText, dataFactory, "turn-11", USER_MSG_BUILD);

    CopilotTurnData confirmTurn = new CopilotTurnData();
    confirmTurn.setTurnId("turn-12");
    ReplyData confirmReply = confirmTurn.getReply();
    confirmReply.setText(BUILD_REPLY_TEXT);
    EditAgentRoundData confirmRound = new EditAgentRoundData();
    confirmRound.setRoundId(1);
    confirmRound.setToolCalls(List.of(
        toolCall("tc-confirm", "run_in_terminal", "running", "Awaiting confirmation...")));
    confirmReply.setEditAgentRounds(List.of(confirmRound));
    confirmReply.setModelName("GPT-5 mini");
    confirmReply.setBillingMultiplier(1.0);
    restoreBoth(browser, styledText, confirmTurn, dataFactory);

    // Show tool confirmation UI (live interaction on the copilot turn just restored).
    // Mirror the real run_in_terminal confirmation built by TerminalConfirmationHandler:
    // a primary "Allow Once" plus the session/global command-name, exact-command and
    // allow-all accept actions (rendered inside the split-button drop-down), and a
    // separate "Skip" dismiss button. Labels/scopes match production so the demo reflects
    // real behavior instead of a generic Allow/Deny pair.
    String cmdLabel = "'" + CONFIRM_COMMAND_NAME + "'";
    ConfirmationContent confirmation = new ConfirmationContent(
        CONFIRM_TITLE, CONFIRM_MESSAGE,
        List.of(
            ConfirmationAction.allowOnce(Messages.confirmation_action_allowOnce),
            demoAcceptAction(
                NLS.bind(Messages.confirmation_action_allowNamesSession, cmdLabel),
                ConfirmationActionScope.SESSION),
            demoAcceptAction(
                NLS.bind(Messages.confirmation_action_alwaysAllowNames, cmdLabel),
                ConfirmationActionScope.GLOBAL),
            demoAcceptAction(Messages.confirmation_action_allowExactSession,
                ConfirmationActionScope.SESSION),
            demoAcceptAction(Messages.confirmation_action_alwaysAllowExact,
                ConfirmationActionScope.GLOBAL),
            demoAcceptAction(Messages.confirmation_action_allowAllCommands,
                ConfirmationActionScope.SESSION),
            ConfirmationAction.skip(Messages.confirmation_action_skip)));
    Map<String, Object> confirmInput = Map.of(
        "command", CONFIRM_COMMAND,
        "explanation", CONFIRM_EXPLANATION);
    if (browser != null) {
      browser.requestToolConfirmation("turn-12", confirmation, confirmInput);
    }
    styledText.requestToolConfirmation("turn-12", confirmation, confirmInput);

    // --- Exchange 7 (turns 13-14): agent message with a non-quota error warning (400) ---
    restoreUser(browser, styledText, dataFactory, "turn-13", USER_MSG_ERROR);

    CopilotTurnData errorTurn = new CopilotTurnData();
    errorTurn.setTurnId("turn-14");
    ReplyData errorReply = errorTurn.getReply();
    errorReply.setText(ERROR_MSG);
    ErrorData errorData = new ErrorData();
    errorData.setMessage(ERROR_DETAIL);
    errorData.setCode(400);
    ErrorMessageData errorMessageData = new ErrorMessageData();
    errorMessageData.setError(errorData);
    errorReply.setErrorMessages(List.of(errorMessageData));
    errorReply.setModelName("GPT-5 mini");
    errorReply.setBillingMultiplier(1.0);
    restoreBoth(browser, styledText, errorTurn, dataFactory);

    // --- Exchange 8 (turns 15-16): quota-exceeded warning (402) with action buttons ---
    restoreUser(browser, styledText, dataFactory, "turn-15", USER_MSG_QUOTA);

    CopilotTurnData quotaTurn = new CopilotTurnData();
    quotaTurn.setTurnId("turn-16");
    ReplyData quotaReply = quotaTurn.getReply();
    quotaReply.setText(QUOTA_REPLY_TEXT);
    ErrorData quotaError = new ErrorData();
    quotaError.setMessage(QUOTA_ERROR_MSG);
    quotaError.setCode(402);
    ErrorMessageData quotaMessageData = new ErrorMessageData();
    quotaMessageData.setError(quotaError);
    quotaReply.setErrorMessages(List.of(quotaMessageData));
    quotaReply.setModelName("Claude Opus 4");
    quotaReply.setBillingMultiplier(2.5);
    quotaReply.setReasoningEffort("high");
    restoreBoth(browser, styledText, quotaTurn, dataFactory);
  }

  /** Restores a turn on both widgets; skips browser if null. */
  private static void restoreBoth(BrowserConversationWidget browser,
      StyledTextConversationWidget styledText, AbstractTurnData turn,
      ConversationDataFactory dataFactory) {
    if (browser != null) {
      browser.restoreTurn(turn, dataFactory);
    }
    styledText.restoreTurn(turn, dataFactory);
  }

  /** Restores a user turn on both widgets; skips browser if null. */
  private static void restoreUser(BrowserConversationWidget browser,
      StyledTextConversationWidget styledText, ConversationDataFactory dataFactory,
      String turnId, String message) {
    UserTurnData userTurn = new UserTurnData();
    userTurn.setTurnId(turnId);
    userTurn.setMessage(new MessageData(message));
    restoreBoth(browser, styledText, userTurn, dataFactory);
  }

  /** Creates a tool call with the given id, name, status and progress message. */
  private static ToolCallData toolCall(String id, String name, String status, String progress) {
    ToolCallData toolCall = new ToolCallData();
    toolCall.setId(id);
    toolCall.setName(name);
    toolCall.setStatus(status);
    toolCall.setProgressMessage(progress);
    return toolCall;
  }

  /**
   * Builds a non-primary accept action for the demo confirmation, mirroring the
   * session/global allow actions produced by {@code TerminalConfirmationHandler}.
   * Only the label, accept flag and (non-)primary flag affect rendering; the scope
   * is carried for realism.
   */
  private static ConfirmationAction demoAcceptAction(String label,
      ConfirmationActionScope scope) {
    return new ConfirmationAction(label, true, scope, null, false);
  }

  /**
   * Disposes the old StyledText widget and creates a fresh one with updated theme colors.
   * The StyledText-based widget reads theme colors at construction time and does not
   * dynamically refresh them, so recreation is necessary on theme change.
   */
  private static StyledTextConversationWidget recreateStyledTextWidget(
      Composite parent, ChatServiceManager serviceManager,
      StyledTextConversationWidget oldWidget) {
    if (oldWidget != null && !oldWidget.getControl().isDisposed()) {
      oldWidget.getControl().dispose();
    }
    StyledTextConversationWidget newWidget =
        new StyledTextConversationWidget(parent, serviceManager);
    parent.requestLayout();

    // Re-feed demo content to the new widget (browser-only stub as first arg)
    parent.getDisplay().asyncExec(() -> feedStyledTextOnly(newWidget));
    return newWidget;
  }

  /**
   * Feeds demo content to a single StyledText widget (used after theme-switch recreation).
   */
  private static void feedStyledTextOnly(StyledTextConversationWidget styledText) {
    feedWidgets(null, styledText);
  }

  /**
   * Switches the Eclipse platform CSS theme (affects StyledText colors).
   */
  private static void switchEclipseTheme(String themeId) {
    IThemeEngine themeEngine =
        (IThemeEngine) PlatformUI.getWorkbench().getService(IThemeEngine.class);
    if (themeEngine != null) {
      themeEngine.setTheme(themeId, true);
    }
  }
}

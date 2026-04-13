package com.microsoft.copilot.eclipse.ui.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.Bundle;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.git.GenerateCommitMessageParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.git.GenerateCommitMessageResult;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Handler for generating commit messages.
 */
public class GenerateCommitMessageHandler extends CopilotHandler {

  private static final int MAX_COMMITS = 5;

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    if (!checkRequiredBundles()) {
      return null;
    }

    IWorkbenchPart part = HandlerUtil.getActivePart(event);
    if (part instanceof StagingView stagingView) {
      Job job = new Job("Generating Commit Message with Copilot...") {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          GenerateCommitMessageHandler.this.generateCommitMessage(stagingView);
          return Status.OK_STATUS;
        }
      };
      job.setUser(true);
      job.schedule();
    }
    return null;
  }

  private void generateCommitMessage(StagingView stagingView) {
    Repository repository = stagingView.getCurrentRepository();
    if (repository == null) {
      SwtUtils.invokeOnDisplayThread(() -> {
        MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.generateCommitMessage_noRepo_title,
            Messages.generateCommitMessage_noRepo_message);
      });
      return;
    }

    try (Git git = new Git(repository)) {
      List<String> changes = getStagedChangesAsStrings(repository, git);
      if (changes.isEmpty()) {
        SwtUtils.invokeOnDisplayThread(() -> MessageDialog.openInformation(Display.getDefault().getActiveShell(),
            Messages.generateCommitMessage_noStagedFiles_title, Messages.generateCommitMessage_noStagedFiles_message));
        return;
      }

      CommitMessagesResult commitMessages = getRecentCommitMessages(repository, git, MAX_COMMITS);
      List<String> recentUserCommitMessages = commitMessages.getUserCommitMessages();
      List<String> recentRepoCommitMessages = commitMessages.getRepositoryCommitMessages();
      GenerateCommitMessageParams params = new GenerateCommitMessageParams(changes, recentUserCommitMessages,
          recentRepoCommitMessages);
      try {
        GenerateCommitMessageResult result = this.getLanguageServerConnection().generateCommitMessage(params).get();
        SwtUtils.invokeOnDisplayThread(() -> stagingView.setCommitText(result.getCommitMessage()));
      } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
        CopilotCore.LOGGER.error("Error generating commit message", e);
      }
    }
  }

  private List<String> getStagedChangesAsStrings(Repository repository, Git git) {
    List<String> changes = new ArrayList<>();

    try {
      List<DiffEntry> diffs = getStagedDiffs(git, repository);

      for (DiffEntry diff : diffs) {
        String diffText = getDiffText(repository, diff);
        changes.add(diffText);
      }

    } catch (GitAPIException | IOException e) {
      CopilotCore.LOGGER.error("Error getting staged changes", e);
    }

    return changes;
  }

  /**
   * Get diffs for staged changes (index vs HEAD).
   */
  private List<DiffEntry> getStagedDiffs(Git git, Repository repository) throws IOException, GitAPIException {
    ObjectId headCommit = repository.resolve(Constants.HEAD);
    if (headCommit == null) {
      // No HEAD commit (empty repository), compare with empty tree
      return git.diff().setOldTree(prepareTreeParser(repository, null)).setCached(true).call();
    }

    // Compare HEAD with index (staged changes)
    return git.diff().setOldTree(prepareTreeParser(repository, headCommit)).setCached(true).call();
  }

  /**
   * Prepare tree parser for the given commit.
   */
  private AbstractTreeIterator prepareTreeParser(Repository repository, ObjectId commitId) throws IOException {
    if (commitId == null) {
      return new CanonicalTreeParser();
    }

    try (RevWalk walk = new RevWalk(repository); ObjectReader reader = repository.newObjectReader()) {

      RevCommit commit = walk.parseCommit(commitId);
      RevTree tree = walk.parseTree(commit.getTree().getId());

      CanonicalTreeParser treeParser = new CanonicalTreeParser();
      treeParser.reset(reader, tree.getId());
      return treeParser;
    }
  }

  /**
   * Get the diff text for a single diff entry.
   */
  private String getDiffText(Repository repository, DiffEntry diff) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream(); DiffFormatter formatter = new DiffFormatter(out)) {

      formatter.setRepository(repository);
      formatter.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
      formatter.setDetectRenames(true);
      formatter.setContext(0);
      formatter.format(diff);

      return out.toString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      CopilotCore.LOGGER.error("Error getting diff for entry: " + diff.getNewPath(), e);
      return "";
    }
  }

  /**
   * Get recent commit messages for both the current user and the repository in a single traversal.
   *
   * @param repository The git repository
   * @param git The Git instance
   * @param maxCommits Maximum number of commits to retrieve for each category
   * @return CommitMessagesResult containing both user and repository commit messages
   */
  private CommitMessagesResult getRecentCommitMessages(Repository repository, Git git, int maxCommits) {
    List<String> userCommitMessages = new ArrayList<>();
    List<String> repositoryCommitMessages = new ArrayList<>();

    try {
      String currentUserName = repository.getConfig().getString("user", null, "name");
      String currentUserEmail = repository.getConfig().getString("user", null, "email");

      LogCommand logCommand = git.log();
      logCommand.setMaxCount(maxCommits * 4);
      Iterable<RevCommit> commits = logCommand.call();

      int userCommitCount = 0;
      int repoCommitCount = 0;

      for (RevCommit commit : commits) {
        // Stop if we've collected enough commits for both categories
        if (userCommitCount >= maxCommits && repoCommitCount >= maxCommits) {
          break;
        }

        String commitMessage = commit.getShortMessage();

        // Add to repository messages if we haven't reached the limit
        if (repoCommitCount < maxCommits) {
          repositoryCommitMessages.add(commitMessage);
          repoCommitCount++;
        }

        // Check if this is a user commit and add to user messages if we haven't reached the limit
        if (userCommitCount < maxCommits && (currentUserName != null || currentUserEmail != null)) {
          PersonIdent author = commit.getAuthorIdent();
          boolean isCurrentUser = (currentUserName != null && currentUserName.equals(author.getName()))
              || (currentUserEmail != null && currentUserEmail.equals(author.getEmailAddress()));

          if (isCurrentUser) {
            userCommitMessages.add(commitMessage);
            userCommitCount++;
          }
        }
      }
    } catch (Exception e) {
      CopilotCore.LOGGER.error(e);
    }

    return new CommitMessagesResult(userCommitMessages, repositoryCommitMessages);
  }

  private boolean checkRequiredBundles() {
    Bundle egitCoreBundle = Platform.getBundle("org.eclipse.egit.core");
    Bundle egitUiBundle = Platform.getBundle("org.eclipse.egit.ui");

    if (egitCoreBundle != null && egitUiBundle != null) {
      return true;
    }

    Shell shell = Display.getDefault().getActiveShell();
    MessageDialog.openWarning(shell, Messages.generateCommitMessage_requiredBundlesMissing_title,
        Messages.generateCommitMessage_requiredBundlesMissing_message);

    return false;
  }

  /**
   * Data structure to hold both user-specific and repository-wide commit messages.
   */
  private static class CommitMessagesResult {
    private final List<String> userCommitMessages;
    private final List<String> repositoryCommitMessages;

    public CommitMessagesResult(List<String> userCommitMessages, List<String> repositoryCommitMessages) {
      this.userCommitMessages = userCommitMessages;
      this.repositoryCommitMessages = repositoryCommitMessages;
    }

    public List<String> getUserCommitMessages() {
      return userCommitMessages;
    }

    public List<String> getRepositoryCommitMessages() {
      return repositoryCommitMessages;
    }
  }

}
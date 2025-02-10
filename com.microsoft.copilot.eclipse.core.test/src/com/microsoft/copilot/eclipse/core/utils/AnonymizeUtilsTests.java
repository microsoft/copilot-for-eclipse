package com.microsoft.copilot.eclipse.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class AnonymizeUtilsTests {
  @ParameterizedTest @CsvSource({
      // Emails
      "email, user@example.com, <REDACTED: Email>", "email, This is my email: user@example.com, <REDACTED: Email>",
      // Not API Keys
      "apiKey, AIza-not-a-key, AIza-not-a-key", "apiKey, AIzanotakey, AIzanotakey",
      // Slack Tokens
      "token, xoxb-1234567890, <REDACTED: Slack Token>",
      // Not GitHub Tokens
      "token, ghp_invalidtoken, ghp_invalidtoken", "token, github_pat_invalidtoken, github_pat_invalidtoken",
      // Jar file path
      "filePath,/home/user/.m2/repository/example.jar!/com/example/file.properties,<REDACTED: user-file-path>",
      "filePath,C:\\Users\\user\\.gradle\\cache\\example.jar!\\com\\example\\file.properties,<REDACTED: user-file-path>",
      // UNC Paths
      "filePath,file:///C:/Users/user/.gradle/cache/example.jar!/com/example/file.properties,<REDACTED: user-file-path>",
      "filePath, notebook:///home/user/file.txt, <REDACTED: user-file-path>",
      "filePath, notebook:////folder/subfolder/file, <REDACTED: user-file-path>",
      "filePath, file:///C:/Users/John/file.txt, <REDACTED: user-file-path>",
      "filePath, file:///C%3A/Users/John/file.txt, <REDACTED: user-file-path>",
      "filePath, file:////server/share/folder/file.txt, <REDACTED: user-file-path>",
      "filePath, file:///c%3a/devops/user/projects/consume-plugin-sdk/build.gradle.kts, <REDACTED: user-file-path>",
      "filePath, file:///c%3A/devops/user/projects/consume-plugin-sdk/build.gradle.kts, <REDACTED: user-file-path>",
      "filePath, file:///C:/devops/user/projects/consume-plugin-sdk/build.gradle.kts, <REDACTED: user-file-path>",
      "filePath, file://C:\\Users\\user\\.gradle\\e76c2167a35c394bd9\\some-package.jar!\\some-file.properties, <REDACTED: user-file-path>",
      "filePath, file:///C:\\Users\\user\\.gradle\\e76c2167a35c394bd9\\some-package.jar!\\some-file.properties, <REDACTED: user-file-path>",
      // SFTP, FTP
      "filePath,sftp://user@example.com/home/user/file.txt,<REDACTED: user-file-path>",
      "filePath,ftp://example.com/path/to/file.txt,<REDACTED: user-file-path>",
      // Remote files
      "filePath,\\\\server\\share\\folder\\file.txt,<REDACTED: user-file-path>",
      "filePath,\\\\server\\share\\folder\\subfolder\\file.txt,<REDACTED: user-file-path>",
      "filePath,\\\\192.168.1.1\\share\\file.txt,<REDACTED: user-file-path>",
      // WSL path
      "filePath,\\\\wsl$\\Ubuntu\\home\\user\\file.txt,<REDACTED: user-file-path>",
      "filePath,\\\\wsl$\\Ubuntu\\home\\user document\\file.txt,<REDACTED: user-file-path>",
      "filePath,\\\\wsl$\\Ubuntu\\home\\user\\file+name.txt,<REDACTED: user-file-path>",
      "filePath,\\\\wsl$\\Debian\\var\\www\\html\\index.html,<REDACTED: user-file-path>",
      "filePath,\\\\wsl$\\Ubuntu\\mnt\\c\\Users\\user\\file.txt,<REDACTED: user-file-path>",
      "filePath,\\\\wsl.localhost\\Ubuntu\\home\\user\\file.txt,<REDACTED: user-file-path>",
      "filePath,\\\\wsl.localhost\\Debian\\var\\www\\html\\index.html,<REDACTED: user-file-path>",
      "filePath,\\\\wsl.localhost\\Ubuntu\\mnt\\c\\Users\\user\\file.txt,<REDACTED: user-file-path>",
      // Unix paths
      "filePath,/home/user/file.txt,<REDACTED: user-file-path>", "filePath,/var/log/syslog,<REDACTED: user-file-path>",
      "filePath,/usr/local/bin/script.sh,<REDACTED: user-file-path>",
      "filePath,/mnt/data/file.txt,<REDACTED: user-file-path>",
      "filePath,/mnt/external-drive/file.txt,<REDACTED: user-file-path>",
      "filePath,/media/user/USB/file.txt,<REDACTED: user-file-path>",
      "filePath,/mnt/network-drive/file.txt,<REDACTED: user-file-path>",
      "filePath,/Users/user/Documents/file.txt,<REDACTED: user-file-path>",
      // Unix paths with special characters
      "filePath,/home/user/My Files/file.txt,<REDACTED: user-file-path>",
      "filePath,/home/user/file@name.txt,<REDACTED: user-file-path>",
      // macOS paths
      "filePath,/Applications/App.app/Contents/Resources/file.txt,<REDACTED: user-file-path>",
      "filePath,/Library/Application Support/App/file.txt,<REDACTED: user-file-path>",
      "filePath,/Volumes/External/file.txt,<REDACTED: user-file-path>",
      // Windows paths
      "filePath,C:\\Users\\user\\My Documents\\file.txt,<REDACTED: user-file-path>",
      "filePath,C:\\Users\\user\\Documents\\file.txt,<REDACTED: user-file-path>",
      "filePath,D:\\Projects\\project\\src\\main\\java\\com\\example\\Main.java,<REDACTED: user-file-path>",
      "filePath,C:\\Program Files\\Java\\jdk-17\\bin\\java.exe,<REDACTED: user-file-path>",
      "filePath,C:\\Users\\user\\AppData\\Local\\Temp\\file.tmp,<REDACTED: user-file-path>",
      "filePath, C:\\Users\\user\\Documents\\file.txt, <REDACTED: user-file-path>",
      "filePath, C%3A\\Users\\John\\file.txt, <REDACTED: user-file-path>",
      "filePath, C:\\Users\\user\\.gradle\\e76c2167a35c394bd9\\some-package.jar!\\some-file.properties, <REDACTED: user-file-path>",
      // Windows paths with special characters
      "filePath,C:\\Users\\user\\file+name.txt,<REDACTED: user-file-path>",
      "filePath,C:\\Users\\user\\file#1.txt,<REDACTED: user-file-path>",
      // Secrets
      "credentials, psexec -u user -p password, <REDACTED: CLI Credentials>",
      // MSFT Entra IDs
      "msftEntraId, eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9, <REDACTED: Microsoft Entra ID>",
      // Not MSFT Entra IDs
      "msftEntraId, abcdefg.hijklmn.opqrstuvwxyz, abcdefg.hijklmn.opqrstuvwxyz",
      // Normal: plain strings
      "empty, , ", "nullValue, null, null", "message, This is a safe message., This is a safe message.",
      "message, monthly_subscriber, monthly_subscriber",
      // Normal: UUID
      "uuid, cffd70fa-3ccd-4eb9-a3cc-80d7b8622281, cffd70fa-3ccd-4eb9-a3cc-80d7b8622281",
      "uuid-with-salt, cffd70fa-3ccd-4eb9-a3cc-80d7b86222811736924238824, cffd70fa-3ccd-4eb9-a3cc-80d7b86222811736924238824",
      // Normal: Obfuscated strings
      "obfuscated, d546f3a2599be6129c92a2bbd71fd96bdaff2878e0d5316be268ea0bd9aa8310, d546f3a2599be6129c92a2bbd71fd96bdaff2878e0d5316be268ea0bd9aa8310",
      // Normal: Version number
      "version, 0.23.2, 0.23.2", "version, 10.0.22631, 10.0.22631", "version, 1.96.3, 1.96.3",
      "version, 0.23.2-staging, 0.23.2-staging",
      // Normal: Arch
      "arch, x64, x64", "arch, arm64, arm64",
      // Normal: Plugin name
      "pluginName, copilot.jetbrains, copilot.jetbrains", "pluginName, copilot.eclipse, copilot.eclipse",
      "pluginName, copilot.xcode, copilot.xcode", "ideName, IntelliJ-IC, IntelliJ-IC",
      // Normal: prompt types
      "promptType, 'system:2437,user:349,user:2036', 'system:2437,user:349,user:2036'", "model, gpt-4o, gpt-4o",
      "diagnosticCode, 'ngtsc@2322:2,ngtsc@-995002:2,', 'ngtsc@2322:2,ngtsc@-995002:2,'",
      // Normal: feature flags
      "features, vscode.copilotchat.modelOverrides, vscode.copilotchat.modelOverrides",
      // Normal: timestamps
      "timestamp, 2025-01-15T10:00:00.0000000Z, 2025-01-15T10:00:00.0000000Z" })
  void removePii_shouldRedactCorrectly(String key, String value, String expected) {
    assertEquals(expected, AnonymizeUtils.removePii(value));
  }

  /**
   * AzDO will reject the code or fail the pipeline if it contains a Google API Key. Here, using the String concat to
   * bypass the policy and test the redaction. These are testing API Key.
   */
  @Test
  void removePii_shouldRedactGoogleApiKey() {
    assertEquals("<REDACTED: Google API Key>",
        AnonymizeUtils.removePii("AIza" + "SyD4-5G6H7I8J9K0L1M2N3O4P5Q6R7S8T9U"));
    assertEquals("<REDACTED: Google API Key>",
        AnonymizeUtils.removePii("AIza" + "AIza" + "SyD4-5G6H7I8J9K0L1M2N3O4P5Q6R7S8T9U1234"));
  }

  /**
   * AzDO will reject the code or fail the pipeline if it contains a Microsoft Entra ID. Here, using the String concat
   * to bypass the policy and test the redaction. These are testing Entra ID.
   */
  @Test
  void removePii_shouldRedactMsftEntraId() {
    assertEquals("<REDACTED: Microsoft Entra ID>",
        AnonymizeUtils.removePii("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" + "."
            + "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ" + "."
            + "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"));
    assertEquals("<REDACTED: Microsoft Entra ID>",
        AnonymizeUtils.removePii("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9" + "."
            + "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ" + "."
            + "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"));
    assertEquals("<REDACTED: Microsoft Entra ID>",
        AnonymizeUtils.removePii("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9" + "."
            + "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ" + "."
            + "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"));
    assertEquals("<REDACTED: Microsoft Entra ID>",
        AnonymizeUtils.removePii("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9" + "."
            + "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ" + "."
            + "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"));
  }

  /**
   * AzDO reject the code or fail the pipeline if it contains a GitHub PAT. Here, using the String concat to bypass the
   * policy and test the redaction. These are testing tokens.
   */
  @Test
  void removePii_shouldRedactGitHubPat() {
    assertEquals("<REDACTED: GitHub Token>",
        AnonymizeUtils.removePii("gh" + "p" + "_1234567890abcdefghijklmnopqrstuvwxyz"));
    assertEquals("<REDACTED: GitHub Token>",
        AnonymizeUtils.removePii("gh" + "p" + "_1234567890abcdefghijklmnopqrstuvwxyz1234"));
    assertEquals("<REDACTED: GitHub Token>", AnonymizeUtils.removePii("git" + "hub" + "_" + "pat"
        + "_1234567890abcdefghijkl_mnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz123456789"));
  }

  @RepeatedTest(10000)
  void removePii_shouldNotRedactUUID() {
    final String uuid = UUID.randomUUID().toString();
    assertEquals(uuid, AnonymizeUtils.removePii(uuid));
  }

  @Test
  void removePii_shouldNotRedactClsName() {
    final String clsName = "@github/copilot-language-server";
    assertEquals(clsName, AnonymizeUtils.removePii(clsName));
  }

  @Test
  void removePii_shouldNotRedactAbExpTags() {
    final String abExpTags = "vsliv368cf:30146710;vspor879:30202332;vspor708:30202333;vspor363:30204092;binariesv615:30325510;vsaa593:30376534;c4g48928:30535728;c771b703:30959264;aa_t_chat:31080850;pythonait:31006305;nativerepl1:31139838;pythonrstrctxt:31112756;nativeloc2:31192216;cf971741:31144450;c3e2j222:31144249;iacca1:31171482;notype1:31157159;b50ed353:31153171;5fd0e150:31155592;7d05f481:31203062;cg8ef616:31203065;stablechunks:31184530;85gh4286:31171602;adff0535:31171559;6074i472:31201624;";
    assertEquals(abExpTags, AnonymizeUtils.removePii(abExpTags));
  }

  @ParameterizedTest @CsvSource({ "multiLine, 'line1\nline2\nline3', 'line1\nline2\nline3'",
      "multiLine, 'line1\r\nline2\r\nline3', 'line1\nline2\nline3'",
      "multiLine, 'C:/Users/user\r\nline2\r\nline3', '<REDACTED: user-file-path>\nline2\nline3'",
      "multiLine, 'line1\r\nC:/Users/user\r\nline3', 'line1\n<REDACTED: user-file-path>\nline3'",
      "multiLine, 'line1\r\nline2\r\nC:/Users/user', 'line1\nline2\n<REDACTED: user-file-path>'",
      "multiLine, 'line1\r\nxoxb-1234567890\r\nC:/Users/user', 'line1\n<REDACTED: Slack Token>\n<REDACTED: user-file-path>'", })
  void removePii_shouldHandleMultiLineValues(String key, String value, String expected) {
    assertEquals(expected, AnonymizeUtils.removePii(value));
  }
}

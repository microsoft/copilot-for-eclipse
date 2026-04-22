// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * Utilities for anonymizing / redacting PII and sensitive tokens (file paths, credentials, etc.)
 * from telemetry payloads before they are emitted.
 */
public class AnonymizeUtils {

  private static final String PROTOCOL_REGEX = "(file|notebook|sftp|ftp)://{1,3}";
  private static final String USER_INFO_REGEX = "([^/]+@)?";
  private static final String HOST_REGEX = "([^/]+)?";
  private static final String DRIVE_LETTER_REGEX = "([a-zA-Z](%3A|%3a|:)(\\\\\\\\|\\\\|/)|(\\\\\\\\|\\\\|/))?";
  private static final String PATH_SEGMENT_REGEX = "([\\w-._!#@$+ ]+(\\\\\\\\|\\\\|/))+";
  private static final String FILE_NAME_REGEX = "[\\w-._!#@$+ ]*";
  private static final String FILE_PATH_REGEX = "(" + PROTOCOL_REGEX + USER_INFO_REGEX + HOST_REGEX + ")?"
      + DRIVE_LETTER_REGEX + PATH_SEGMENT_REGEX + FILE_NAME_REGEX;

  private static final Pattern FILE_PATH_PATTERN = Pattern.compile(FILE_PATH_REGEX);

  private static final Pattern GOOGLE_API_KEY_PATTERN = Pattern.compile("AIza[a-zA-Z0-9_\\-]{35}");

  private static final Pattern SLACK_TOKEN_PATTERN = Pattern.compile("xox[pabers]-[a-zA-Z0-9]",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern GITHUB_TOKEN_PATTERN = Pattern
      .compile("(gh[psuro]_[a-zA-Z0-9]{36}|github_pat_[a-zA-Z0-9]{22}_[a-zA-Z0-9]{59})");

  private static final Pattern GENERIC_SECRET_PATTERN = Pattern.compile(
      "(key|token|sig|secret|signature|password|passwd|pwd|android:value)[^a-zA-Z0-9]", Pattern.CASE_INSENSITIVE);

  private static final Pattern CLI_CREDENTIALS_PATTERN = Pattern.compile(
      "((login|psexec|(certutil|psexec)\\.exe).{1,50}(\\s-u(ser(name)?)?\\s+.{3,100})?\\s-(admin|user|vm|root)?"
          + "p(ass(word)?)?\\s+[\"']?[^$\\-/\\s]|(^|[\\s\\r\\n\\\\])net(\\.exe)?.{1,5}(user\\s+|share\\s+/user:|"
          + " user -? secrets ? set) \\s + [^ $\\s/])");

  private static final Pattern MSFT_ENTRA_ID_PATTERN = Pattern
      .compile("eyJ(?:0eXAiOiJKV1Qi|hbGci|[a-zA-Z0-9\\-_]+\\.[a-zA-Z0-9\\-_]+\\.[a-zA-Z0-9\\-_]*)");

  private static final Pattern EMAIL_PATTERN = Pattern.compile("(@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-]+)");

  /**
   * Refer to
   * https://github.com/microsoft/vscode/blob/5ba4503b8b5e9a4e92b4df90a55a6a7b91c31c15/src/vs/platform/telemetry/common/telemetryUtils.ts#L326.
   */
  private static final Map<Pattern, String> PATTERN_MAP = new HashMap<Pattern, String>() {
    {
      put(GOOGLE_API_KEY_PATTERN, "<REDACTED: Google API Key>");
      put(SLACK_TOKEN_PATTERN, "<REDACTED: Slack Token>");
      put(GITHUB_TOKEN_PATTERN, "<REDACTED: GitHub Token>");
      put(GENERIC_SECRET_PATTERN, "<REDACTED: Generic Secret>");
      put(CLI_CREDENTIALS_PATTERN, "<REDACTED: CLI Credentials>");
      put(MSFT_ENTRA_ID_PATTERN, "<REDACTED: Microsoft Entra ID>");
      put(EMAIL_PATTERN, "<REDACTED: Email>");
    }
  };

  private static final Pattern CLS_NAME = Pattern.compile("^@github/copilot-language-server$");
  private static final List<Pattern> SKIP_PATTERNS = List.of(CLS_NAME);

  /**
   * Remove PII data from the given properties.
   */
  public static String removePii(final String value) {
    if (StringUtils.isBlank(value)) {
      return value;
    }
    return Arrays.stream(value.split("\\r?\\n")).map(AnonymizeUtils::anonymizePiiData)
        .collect(Collectors.joining(StringUtils.LF));
  }

  private static String anonymizePiiData(final String input) {
    // Skip if matches any of the skip patterns
    for (Pattern skipPattern : SKIP_PATTERNS) {
      if (skipPattern.matcher(input).find()) {
        return input;
      }
    }

    final String result = FILE_PATH_PATTERN.matcher(input).replaceAll("<REDACTED: user-file-path>");

    for (final Pattern pattern : PATTERN_MAP.keySet()) {
      if (pattern.matcher(result).find()) {
        return PATTERN_MAP.get(pattern);
      }
    }
    return result;
  }
}

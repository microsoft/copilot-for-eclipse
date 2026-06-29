// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;

import com.microsoft.copilot.eclipse.core.utils.BundleUtils;
import com.microsoft.copilot.eclipse.ui.CopilotUi;

/**
 * Registry of inline SVG icons used by the browser-based chat renderer.
 *
 * <p>Each icon lives as a standalone {@code .svg} file under {@code resources/html/icons/}
 * containing only the icon geometry (path/line elements and a {@code viewBox}). At load time
 * this registry injects the presentation attributes the chat DOM requires (CSS {@code class},
 * sizing, {@code currentColor} theming, inline {@code style}) onto the root {@code <svg>}
 * element. Because those attributes are enforced by code rather than stored in the file, an
 * icon file can be replaced (e.g. with a fresh export) without risk of losing the CSS class or
 * theming the chat view depends on.
 *
 * <p>The resulting markup is inlined directly into the browser DOM (not referenced via an
 * {@code <img>} element), which is required for {@code currentColor} to adapt to the
 * light/dark theme.
 */
public final class SvgIcons {

  /** The available chat icons, each mapped to its file name and enforced root attributes. */
  public enum Icon {
    /** Lightbulb icon for sealed (completed) thinking blocks. */
    THINKING_BULB("thinking-bulb.svg", attrs(
        "class", "thinking-icon",
        "width", "14",
        "height", "14",
        "fill", "none",
        "stroke", "currentColor",
        "stroke-width", "1.3",
        "stroke-linecap", "round",
        "stroke-linejoin", "round")),
    /** Pull request icon for coding-agent message blocks. */
    PULL_REQUEST("pull-request.svg", attrs(
        "width", "14",
        "height", "14",
        "fill", "currentColor",
        "style", "vertical-align: middle; margin-right: 4px;")),
    /** Terminal/command icon for tool confirmation blocks. */
    TERMINAL("terminal.svg", attrs(
        "width", "14",
        "height", "14",
        "fill", "currentColor",
        "style", "vertical-align: middle; margin-right: 4px;")),
    /** Warning triangle icon for quota warning and error blocks. */
    WARNING("warning.svg", attrs(
        "width", "14",
        "height", "14",
        "fill", "currentColor",
        "style", "vertical-align: middle; margin-right: 4px; flex-shrink: 0;"));

    private final String fileName;
    private final Map<String, String> enforcedAttributes;

    Icon(String fileName, Map<String, String> enforcedAttributes) {
      this.fileName = fileName;
      this.enforcedAttributes = enforcedAttributes;
    }
  }

  private static final String ICON_RESOURCE_DIR = "resources/html/icons/";

  private static final Pattern ATTRIBUTE_PATTERN =
      Pattern.compile("([\\w:-]+)\\s*=\\s*\"([^\"]*)\"");

  private static volatile Map<Icon, String> cache;

  private SvgIcons() {
  }

  /**
   * Returns the ready-to-inline SVG markup for the given icon, with all required presentation
   * attributes applied. Icons are loaded and processed once, then cached.
   *
   * @param icon the icon to render
   * @return the inline SVG markup, or an empty string if the icon file cannot be read
   */
  public static String get(Icon icon) {
    Map<Icon, String> local = cache;
    if (local == null) {
      synchronized (SvgIcons.class) {
        local = cache;
        if (local == null) {
          local = loadAll(CopilotUi.getPlugin().getBundle());
          cache = local;
        }
      }
    }
    return local.getOrDefault(icon, "");
  }

  private static Map<Icon, String> loadAll(Bundle bundle) {
    Map<Icon, String> map = new EnumMap<>(Icon.class);
    for (Icon icon : Icon.values()) {
      String raw = BundleUtils.readResourceAsString(bundle, ICON_RESOURCE_DIR + icon.fileName);
      map.put(icon, raw == null ? "" : applyRootAttributes(raw, icon.enforcedAttributes));
    }
    return map;
  }

  /**
   * Applies (adds or overrides) the given attributes on the root {@code <svg>} element of the
   * supplied markup, dropping any {@code xmlns} declarations so the result is lean inline HTML.
   * Attributes present in the source but not listed are preserved (notably {@code viewBox}).
   *
   * <p>Attributes present in the source but not listed are preserved (notably {@code viewBox}). The
   * {@code style} attribute is treated specially: rather than replacing it wholesale, its CSS
   * sub-properties are merged so enforced declarations are added or overridden while unrelated
   * existing declarations are kept.
   *
   * <p>Visible for unit testing.
   *
   * @param svg the raw SVG markup
   * @param enforcedAttributes the attributes to enforce on the root element
   * @return the SVG markup with the enforced attributes applied, or the input unchanged if it
   *     does not contain a recognizable root {@code <svg>} element
   */
  public static String applyRootAttributes(String svg, Map<String, String> enforcedAttributes) {
    if (svg == null) {
      return "";
    }
    String trimmed = stripXmlProlog(svg.trim());
    int start = trimmed.indexOf("<svg");
    if (start < 0) {
      return trimmed;
    }
    int tagEnd = trimmed.indexOf('>', start);
    if (tagEnd < 0) {
      return trimmed;
    }
    String openTag = trimmed.substring(start, tagEnd); // "<svg ..." without the closing '>'

    Map<String, String> attributes = parseAttributes(openTag);
    attributes.keySet().removeIf(name -> name.equals("xmlns") || name.startsWith("xmlns:"));
    for (Map.Entry<String, String> enforced : enforcedAttributes.entrySet()) {
      String name = enforced.getKey();
      String value = enforced.getValue();
      if ("style".equals(name) && attributes.containsKey("style")) {
        value = mergeStyle(attributes.get("style"), value);
      }
      attributes.put(name, value);
    }

    StringBuilder rebuilt = new StringBuilder("<svg");
    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      rebuilt.append(' ').append(entry.getKey())
          .append("=\"").append(entry.getValue()).append('"');
    }
    String rest = trimmed.substring(tagEnd + 1); // inner content plus "</svg>"
    rebuilt.append('>').append(rest);
    return rebuilt.toString();
  }

  private static Map<String, String> parseAttributes(String openTag) {
    Map<String, String> attributes = new LinkedHashMap<>();
    Matcher matcher = ATTRIBUTE_PATTERN.matcher(openTag);
    while (matcher.find()) {
      attributes.put(matcher.group(1), matcher.group(2));
    }
    return attributes;
  }

  /**
   * Merges the {@code enforced} CSS declarations into the {@code existing} ones. Declarations from
   * {@code existing} are kept unless {@code enforced} overrides them by property name; declarations
   * present only in {@code enforced} are appended. This ensures enforcing a {@code style} attribute
   * never drops unrelated sub-properties already declared on the element.
   *
   * @param existing the current value of the {@code style} attribute
   * @param enforced the {@code style} declarations to add or override
   * @return the merged {@code style} value
   */
  private static String mergeStyle(String existing, String enforced) {
    Map<String, String> declarations = parseStyle(existing);
    declarations.putAll(parseStyle(enforced));
    StringBuilder merged = new StringBuilder();
    for (Map.Entry<String, String> entry : declarations.entrySet()) {
      if (merged.length() > 0) {
        merged.append(' ');
      }
      merged.append(entry.getKey()).append(": ").append(entry.getValue()).append(';');
    }
    return merged.toString();
  }

  private static Map<String, String> parseStyle(String style) {
    Map<String, String> declarations = new LinkedHashMap<>();
    if (style == null) {
      return declarations;
    }
    for (String declaration : style.split(";")) {
      int colon = declaration.indexOf(':');
      if (colon < 0) {
        continue;
      }
      String property = declaration.substring(0, colon).trim();
      String value = declaration.substring(colon + 1).trim();
      if (!property.isEmpty()) {
        declarations.put(property, value);
      }
    }
    return declarations;
  }

  private static String stripXmlProlog(String svg) {
    if (svg.startsWith("<?xml")) {
      int end = svg.indexOf("?>");
      if (end >= 0) {
        return svg.substring(end + 2).trim();
      }
    }
    return svg;
  }

  private static Map<String, String> attrs(String... pairs) {
    Map<String, String> map = new LinkedHashMap<>();
    for (int i = 0; i + 1 < pairs.length; i += 2) {
      map.put(pairs[i], pairs[i + 1]);
    }
    return map;
  }
}

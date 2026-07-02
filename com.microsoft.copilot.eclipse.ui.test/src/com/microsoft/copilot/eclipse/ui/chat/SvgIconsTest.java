// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class SvgIconsTest {

  private static Map<String, String> attrs(String... pairs) {
    Map<String, String> map = new LinkedHashMap<>();
    for (int i = 0; i + 1 < pairs.length; i += 2) {
      map.put(pairs[i], pairs[i + 1]);
    }
    return map;
  }

  @Test
  void applyRootAttributes_injectsMissingAttributes() {
    String svg = "<svg viewBox=\"0 0 16 16\"><path d=\"M0 0\"/></svg>";
    String result = SvgIcons.applyRootAttributes(svg,
        attrs("class", "thinking-icon", "width", "14", "height", "14", "fill", "none"));

    assertTrue(result.contains("class=\"thinking-icon\""), result);
    assertTrue(result.contains("width=\"14\""), result);
    assertTrue(result.contains("height=\"14\""), result);
    assertTrue(result.contains("fill=\"none\""), result);
  }

  @Test
  void applyRootAttributes_overridesExistingAttribute() {
    String svg = "<svg viewBox=\"0 0 16 16\" fill=\"#000000\"><path d=\"M0 0\"/></svg>";
    String result = SvgIcons.applyRootAttributes(svg, attrs("fill", "currentColor"));

    assertTrue(result.contains("fill=\"currentColor\""), result);
    assertFalse(result.contains("#000000"), result);
  }

  @Test
  void applyRootAttributes_stripsXmlnsDeclaration() {
    String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 16 16\">"
        + "<path d=\"M0 0\"/></svg>";
    String result = SvgIcons.applyRootAttributes(svg, attrs("width", "14"));

    assertFalse(result.contains("xmlns"), result);
  }

  @Test
  void applyRootAttributes_stripsXmlProlog() {
    String svg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<svg viewBox=\"0 0 16 16\">"
        + "<path d=\"M0 0\"/></svg>";
    String result = SvgIcons.applyRootAttributes(svg, attrs("width", "14"));

    assertTrue(result.startsWith("<svg"), result);
    assertFalse(result.contains("<?xml"), result);
  }

  @Test
  void applyRootAttributes_preservesUnlistedAttributesAndInnerContent() {
    String svg = "<svg viewBox=\"0 0 24 24\"><path d=\"M1 2 3 4\"/><line x1=\"0\"/></svg>";
    String result = SvgIcons.applyRootAttributes(svg, attrs("width", "14"));

    assertTrue(result.contains("viewBox=\"0 0 24 24\""), result);
    assertTrue(result.contains("<path d=\"M1 2 3 4\"/>"), result);
    assertTrue(result.contains("<line x1=\"0\"/>"), result);
    assertTrue(result.endsWith("</svg>"), result);
  }

  @Test
  void applyRootAttributes_nullInput_returnsEmptyString() {
    assertEquals("", SvgIcons.applyRootAttributes(null, attrs("width", "14")));
  }

  @Test
  void applyRootAttributes_nonSvgInput_returnedUnchanged() {
    String notSvg = "just some text";
    assertEquals(notSvg, SvgIcons.applyRootAttributes(notSvg, attrs("width", "14")));
  }

  @Test
  void applyRootAttributes_style_addedWhenMissing() {
    String svg = "<svg viewBox=\"0 0 16 16\"><path d=\"M0 0\"/></svg>";
    String result = SvgIcons.applyRootAttributes(svg,
        attrs("style", "vertical-align: middle; margin-right: 4px;"));

    assertTrue(result.contains("style=\"vertical-align: middle; margin-right: 4px;\""), result);
  }

  @Test
  void applyRootAttributes_style_addsMissingSubPropertiesKeepingExisting() {
    String svg = "<svg viewBox=\"0 0 16 16\" style=\"opacity: 0.5; display: block;\">"
        + "<path d=\"M0 0\"/></svg>";
    String result = SvgIcons.applyRootAttributes(svg,
        attrs("style", "vertical-align: middle; margin-right: 4px;"));

    String style = styleOf(result);
    assertEquals("0.5", declaration(style, "opacity"));
    assertEquals("block", declaration(style, "display"));
    assertEquals("middle", declaration(style, "vertical-align"));
    assertEquals("4px", declaration(style, "margin-right"));
  }

  @Test
  void applyRootAttributes_style_overridesExistingSubPropertiesKeepingOthers() {
    String svg = "<svg viewBox=\"0 0 16 16\" style=\"vertical-align: top; opacity: 0.5;\">"
        + "<path d=\"M0 0\"/></svg>";
    String result = SvgIcons.applyRootAttributes(svg,
        attrs("style", "vertical-align: middle; margin-right: 4px;"));

    String style = styleOf(result);
    assertEquals("middle", declaration(style, "vertical-align"));
    assertEquals("0.5", declaration(style, "opacity"));
    assertEquals("4px", declaration(style, "margin-right"));
  }

  private static String styleOf(String svg) {
    Matcher matcher = Pattern.compile("style=\"([^\"]*)\"").matcher(svg);
    assertTrue(matcher.find(), "no style attribute in: " + svg);
    return matcher.group(1);
  }

  private static String declaration(String style, String property) {
    for (String declaration : style.split(";")) {
      int colon = declaration.indexOf(':');
      if (colon >= 0 && declaration.substring(0, colon).trim().equals(property)) {
        return declaration.substring(colon + 1).trim();
      }
    }
    return null;
  }
}

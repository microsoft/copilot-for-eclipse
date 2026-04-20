// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatCompletionContentPart;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ImageContentPart;
import com.microsoft.copilot.eclipse.core.lsp.protocol.TextContentPart;

/**
 * Utility class for handling image files. Provides methods to check if a file is an image and to convert an image file
 * to a Base64 string.
 */
public class ChatMessageUtils {

  /**
   * Checks if the given file is an image file based on its extension.
   *
   * @param file the file to check
   * @return true if the file is an image, false otherwise
   */
  public static boolean isImageFile(IFile file) {
    if (file == null || file.getFileExtension() == null) {
      return false;
    }
    return Constants.ALLOWED_IMAGE_EXTENSIONS.contains(file.getFileExtension().toLowerCase());
  }

  /**
   * Converts an image file to a Base64 encoded string.
   *
   * @param file the image file to convert
   * @return a Base64 encoded string representation of the image, or null if the file is not an image or an error occurs
   */
  public static String convertImageToBase64(IFile file) {
    if (!isImageFile(file)) {
      return null;
    }
    String extName = file.getFileExtension();
    String mimeType = Constants.EXTENSION_TO_MIMETYPE.get(extName.toLowerCase());
    if (mimeType == null) {
      return null; // Unsupported image type
    }
    try (InputStream inputStream = file.getContents();) {
      byte[] bytes = inputStream.readAllBytes();
      String base64 = Base64.getEncoder().encodeToString(bytes);
      return "data:" + mimeType + ";base64," + base64;
    } catch (CoreException | IOException e) {
      CopilotCore.LOGGER.error("Error converting image to Base64: ", e);
      return null;
    }
  }

  /**
   * Creates a chat message that can contain both text and images.
   *
   * @param content the text content of the message
   * @param references the list of files to include in the message
   * @param modelSupportsVision indicates if the model supports vision capabilities
   * @return Either containing the text content or a list of ChatCompletionContentPart
   */
  public static Either<String, List<ChatCompletionContentPart>> createMessageWithImages(String content,
      List<IFile> references, boolean modelSupportsVision) {
    // Filter image files based on model support
    List<IFile> imageFiles = modelSupportsVision
        ? references.stream().filter(f -> isImageFile(f)).toList()
        : List.of();

    // If no images, return simple string content
    if (imageFiles.isEmpty()) {
      return Either.forLeft(content);
    }
    // If has images, create multi-modal content structure
    List<ChatCompletionContentPart> contentParts = new ArrayList<>();

    // Add initial text part
    contentParts.add(new TextContentPart(content));

    // Add all images
    for (IFile imageFile : imageFiles) {
      String base64 = convertImageToBase64(imageFile);
      if (StringUtils.isNotEmpty(base64)) {
        contentParts.add(new ImageContentPart(base64, ImageContentPart.ImageDetail.HIGH));
      }
    }
    return Either.forRight(contentParts);
  }

}

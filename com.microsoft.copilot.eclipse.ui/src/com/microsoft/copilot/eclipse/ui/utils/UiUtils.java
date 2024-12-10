package com.microsoft.copilot.eclipse.ui.utils;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Utilities for Eclipse UI.
 */
public class UiUtils {

  private UiUtils() {
    // prevent instantiation
  }

  /**
   * Gets the URI of the file opened in the given text editor.
   */
  @Nullable
  public static URI getUriFromTextEditor(ITextEditor editor) {
    IEditorInput input = editor.getEditorInput();
    if (input instanceof IFileEditorInput fileInput) {
      IFile file = fileInput.getFile();
      return file.getLocationURI();
    }

    return null;
  }

}

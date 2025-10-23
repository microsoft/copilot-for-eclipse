package com.microsoft.copilot.eclipse.ui.nes;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.ui.swt.CssConstants;

/**
 * Encapsulates the transient bottom notification pill that prompts the user to jump to an out-of-viewport Next Edit
 * Suggestion. Extracted from the controller to reduce its size / responsibilities.
 */
public class BottomBar {

  private final StyledText text;
  private final Runnable jumpAction;

  private Composite bar;
  private Label notificationLabel;

  private Font keyFont;

  /**
   * Constructor.
   */
  public BottomBar(StyledText text, Runnable jumpAction) {
    this.text = text;
    this.jumpAction = jumpAction;
  }

  /** Show (create if necessary) the bar. */
  public void show() {
    if (text == null || text.isDisposed()) {
      return;
    }
    if (bar == null || bar.isDisposed()) {
      create();
    }
    positionAtBottom();
    if (!bar.isVisible()) {
      bar.setVisible(true);
      bar.requestLayout();
    }
  }

  /** Hide the bar. */
  public void hide() {
    if (bar != null && !bar.isDisposed()) {
      bar.setVisible(false);
    }
  }

  /** Dispose resources. */
  public void dispose() {
    if (bar != null && !bar.isDisposed()) {
      bar.dispose();
    }
  }

  private void create() {
    Composite parent = text.getParent();
    if (parent == null || parent.isDisposed()) {
      return;
    }

    bar = new Composite(parent, SWT.DOUBLE_BUFFERED | SWT.NO_FOCUS | SWT.BORDER);

    bar.setData(CssConstants.CSS_ID_KEY, "nes-bottom-bar");
    
    GridLayout outer = new GridLayout(1, false);
    outer.marginWidth = 0;
    outer.marginHeight = 0;
    outer.horizontalSpacing = 0;
    outer.verticalSpacing = 0;
    bar.setLayout(outer);

    GridLayout contentLayout = new GridLayout(3, false);
    contentLayout.marginWidth = 10;
    contentLayout.marginHeight = 6;
    contentLayout.horizontalSpacing = 8;
    contentLayout.verticalSpacing = 0;
    Composite content = new Composite(bar, SWT.NO_FOCUS);
    content.setLayout(contentLayout);
    content.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    
    Label pressLabel = new Label(content, SWT.NONE);
    pressLabel.setText(Messages.bottomBar_press);
    pressLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    
    Label keyLabel = new Label(content, SWT.NONE);
    keyLabel.setText("Tab");
    
    FontData baseFd = keyLabel.getFont().getFontData()[0];
    baseFd.setStyle(SWT.BOLD);
    Display display = parent.getDisplay();
    keyFont = new Font(display, baseFd);
    keyLabel.setFont(keyFont);
    
    bar.addDisposeListener(e -> {
      if (keyFont != null && !keyFont.isDisposed()) {
        keyFont.dispose();
        keyFont = null;
      }
    });
    
    keyLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

    notificationLabel = new Label(content, SWT.NONE);
    notificationLabel.setText(Messages.bottomBar_jumpMessage);
    notificationLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    MouseAdapter clickListener = new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        if (jumpAction != null) {
          jumpAction.run();
        }
      }
    };
    bar.addMouseListener(clickListener);
    content.addMouseListener(clickListener);
    pressLabel.addMouseListener(clickListener);
    keyLabel.addMouseListener(clickListener);
    notificationLabel.addMouseListener(clickListener);

    positionAtBottom();
  }

  private void positionAtBottom() {
    if (bar == null || bar.isDisposed()) {
      return;
    }
    Composite parent = bar.getParent();
    Rectangle parentBounds = parent.getClientArea();
    Point preferred = bar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    int x = (parentBounds.width - preferred.x) / 2;
    int y = parentBounds.height - preferred.y - 20;
    bar.setBounds(x, y, preferred.x, preferred.y);
    bar.moveAbove(null);
  }
}

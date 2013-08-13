package Funk;

import java.awt.Component;
import java.util.*;

public class FocusHack implements Runnable {
  // requestFocus() doesn't work until component has been painted onscreen.

  private Component c;

  public FocusHack (Component c) {
    if (c != null) {
      this.c = c;
      new Thread(this).start();
    }
  }
  
  public void run () {
    try {
      Thread.sleep(500);
      // checking isShowing() or isVisible() doesn't seem to be enough!
      // They can return true even when the widget hasn't been painted
      // yet and so won't respond to requestFocus().
      c.requestFocus();
    } catch (Exception e) {
    }
  }
}

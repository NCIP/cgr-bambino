package Funk;

//
//  Change the cursor for a component's frame to WAIT_CURSOR,
//  until object being watched reports it's OK to change the cursor back.
//

import java.awt.*;
import java.util.*;

public class Curser implements Observer {
  Component comp;
  int old_cursor_type = Cursor.DEFAULT_CURSOR;
  // HACK

  public Curser (Component c) {
    //    f = Funk.Gr.getFrame(c);
    //    old_cursor = f.getCursorType();
    comp = c;
    old_cursor_type = c.getCursor().getType();
    if (old_cursor_type != Cursor.WAIT_CURSOR) {
      // if multiple instances...?
      c.setCursor(new Cursor(Cursor.WAIT_CURSOR));
      Funk.Gr.getFrame(c).getToolkit().sync();
    }
  }

  public void update (Observable o, Object arg) {
    comp.setCursor(new Cursor(old_cursor_type));
  }
}

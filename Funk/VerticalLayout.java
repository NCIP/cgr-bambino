package Funk;

import java.awt.*;

public class VerticalLayout {
  //
  // pack given Panels vertically with a series of nested BorderLayout panels
  //
  private Panel last;

  public VerticalLayout (Container c) {
    c.setLayout(new BorderLayout());
    last = new Panel();
    c.add("Center", last);
  }

  public void add_panel (Panel p) {
    last.setLayout(new BorderLayout());
    last.add("North", p);
    Panel next = new Panel();
    last.add("Center", next);
    last = next;
  }

  public void add_component (Component c) {
    Panel p = new Panel();
    p.add(c);
    last.setLayout(new BorderLayout());
    last.add("North", p);
    Panel next = new Panel();
    last.add("Center", next);
    last = next;
  }
}

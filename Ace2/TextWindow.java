package Ace2;

import java.awt.*;
import java.util.*;

public class TextWindow extends Funk.CloseFrame {
  public TextWindow (int cols, int rows, String title, String str) {
    TextArea ta = new TextArea(rows, cols);
    ta.setFont(new Font("Courier", Font.PLAIN, 12));
    ta.setText(str);
    ta.setEditable(false);

    setTitle(title);
    setLayout(new BorderLayout());
    add("Center", ta);
    Panel p = new Panel();
    p.setLayout(new BorderLayout());
    p.add("East", new Funk.CloseButton());
    add("North", p);
    pack();
    setVisible(true);
  }
}


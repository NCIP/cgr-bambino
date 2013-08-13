package Funk;

import java.awt.*;
import java.awt.event.*;

public class CloseButton extends Button implements ActionListener {

  public CloseButton () {
    setLabel("Close");
    addActionListener(this);
  }

  // begin ActionListener stubs 
  public void actionPerformed(ActionEvent e) {
    Frame f = Funk.Gr.getFrame(this);
    f.setVisible(false);
    f.dispose();
  }
  // end

  public static void main (String argv[]) {
    // debug
    Frame f = new Frame();
    f.add(new CloseButton());
    f.pack();
    f.setVisible(true);
    f.setSize(800,600);
  }
}

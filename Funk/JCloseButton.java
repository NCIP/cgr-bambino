package Funk;

import javax.swing.*;
import java.awt.event.*;

public class JCloseButton extends JButton implements ActionListener {

  public JCloseButton () {
    setText("Close");
    addActionListener(this);
  }

  // begin ActionListener stubs 
  public void actionPerformed(ActionEvent e) {
    JFrame f = Funk.Gr.getJFrame(this);
    f.setVisible(false);
    f.dispose();
  }
  // end

  public static void main (String argv[]) {
    // debug
    JFrame f = new JFrame();
    f.add(new JCloseButton());
    f.pack();
    f.setVisible(true);
    f.setSize(800,600);
  }
}

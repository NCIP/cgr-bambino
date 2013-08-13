package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class JButtonGenerator {
  private ActionListener listener;
  private Container container;

  public JButtonGenerator (ActionListener listener) {
    this.listener=listener;
  }

  public JButtonGenerator (ActionListener listener, Container container) {
    this.listener=listener;
    this.container=container;
  }

  public JButton generate_jbutton (String label) {
    JButton jb = new JButton(label);
    jb.addActionListener(listener);
    if (container != null) container.add(jb);
    return jb;
  }

}

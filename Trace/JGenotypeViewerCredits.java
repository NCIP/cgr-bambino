package Trace;

import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.awt.BorderLayout;

public class JGenotypeViewerCredits extends JFrame implements ActionListener {

  private JButton b_close;

  public JGenotypeViewerCredits() {
    setup();
  }
  
  private void setup () {
    setTitle("About");

    StringBuffer sb = new StringBuffer();

    sb.append("This software uses the Java implementation of the bzip2 compression library from Apache Ant:\n\n");

    sb.append("   This product includes software developed by the Apache Software Foundation (http://www.apache.org/).\n");
    sb.append("   Copyright (C) 2000-2003 The Apache Software Foundation. All rights reserved.");

    JTextArea ta = new JTextArea(sb.toString(), 4, 85);
    ta.setEditable(false);

    setLayout(new BorderLayout());
    ta.setSize(getMinimumSize());

    JPanel controls = new JPanel();
    controls.add(b_close = new JButton("Close"));
    b_close.addActionListener(this);
    
    add("Center", ta);
    add("South", controls);
    
    pack();
    setVisible(true);
  }

  // begin ActionListener stub
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source.equals(b_close)) {
      setVisible(false);
      dispose();
    }
  }
  // end ActionListener stub

  
  public static void main (String [] argv) {
    new JGenotypeViewerCredits();
  }
}

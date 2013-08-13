package Trace;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class GenotypeViewerCredits extends Funk.CloseFrame implements ActionListener {

  private Button b_close;

  public GenotypeViewerCredits() {
    setup();
  }
  
  private void setup () {
    setTitle("About");

    StringBuffer sb = new StringBuffer();

    sb.append("This software uses the Java implementation of the bzip2 compression library from Apache Ant:\n\n");

    sb.append("   This product includes software developed  by the Apache Software Foundation (http://www.apache.org/).\n");
    sb.append("   Copyright (C) 2000-2003 The Apache Software Foundation. All rights reserved.");

    // TextArea ta = new TextArea(sb.toString());
    TextArea ta = new TextArea(sb.toString(), 4, 85, TextArea.SCROLLBARS_NONE);
    // TextArea:
    // if we append() text, getMinimumSize() and getPreferredSize()
    // don't seem to return anything but [0,0].  Likewise getRows()
    // and getColumns() return 0.  You'd think these would be dynamically
    // calculated based on the contents of the current text, BUT NO.

    setLayout(new BorderLayout());
    ta.setSize(getMinimumSize());

    Panel controls = new Panel();
    controls.add(b_close = new Button("Close"));
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
    GenotypeViewerCredits gvc = new GenotypeViewerCredits();
  }
}

package TCGA;

import java.util.*;
import javax.swing.*;
import java.awt.event.*;

public class JCheckBoxMap implements ActionListener {
  //
  // map JCheckBox instances to a set of objects (checkboxes select/deselect set of objects)
  //
  // MNE 7/2011
  //
  HashSet<Object> selected;
  // ugh; any way to pass in type from caller??
  HashMap<JCheckBox,Object> cb2o;

  public JCheckBoxMap () {
    selected = new HashSet<Object>();
    cb2o = new HashMap<JCheckBox,Object>();
  }

  public void add_checkbox (JCheckBox jcb, Object o) {
    jcb.addActionListener(this);
    cb2o.put(jcb, o);
  }

  // begin ActionListener stub
  public void actionPerformed(ActionEvent e) {
    JCheckBox jcb = (JCheckBox) e.getSource();
    Object o = cb2o.get(jcb);
    if (jcb.isSelected()) {
      selected.add(o);
    } else {
      selected.remove(o);
    }
    //    System.err.println("click " + o + " " + jcb);  // debug
  }
  // end ActionListener stub

  public HashSet<Object> get_selected() {
    return selected;
  }
  
}
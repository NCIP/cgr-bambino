package TCGA;

import java.util.*;
import javax.swing.*;
import java.awt.event.*;

public class CheckBoxSet {
  private HashMap<String,JCheckBox> boxes;

  public CheckBoxSet (ArrayList<String> list, boolean default_value) {
    JCheckBox jcb;
    boxes = new HashMap<String,JCheckBox>();
    for (String item : list) {
      boxes.put(item, jcb = new JCheckBox(item, default_value));
    }
  }

  public JCheckBox get(String label) {
    return boxes.get(label);
  }

  public ArrayList<String> get_selected() {
    ArrayList<String> results = new ArrayList<String>();
    for (String item : boxes.keySet()) {
      JCheckBox cb = get(item);
      if (cb.isSelected()) results.add(item);
    }
    return results;
    
  }


}

package Funk;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class CheckboxMenuGroup implements ItemListener {
  // manage a set of CheckboxMenuItem objects, where only one may be
  // set at a time.
  
  private Vector list;
  private ItemListener il = null;
  private CheckboxMenuItem active = null;

  public CheckboxMenuGroup () {
    list = new Vector();
    il = null;
  }

  public CheckboxMenuGroup (ItemListener il) {
    list = new Vector();
    this.il = il;
  }

  public void add (CheckboxMenuItem item) {
    list.addElement(item);
    item.addItemListener(this);
  }

  public CheckboxMenuItem add_new (String name) {
    // create and add an item
    CheckboxMenuItem mi = new CheckboxMenuItem(name);
    add(mi);
    return mi;
  }

  public void set (CheckboxMenuItem selected) {
    Enumeration e = list.elements();
    CheckboxMenuItem item;
    active = selected;
    while (e.hasMoreElements()) {
      item = (CheckboxMenuItem) e.nextElement();
      item.setState(item.equals(selected));
    }
  }

  public void itemStateChanged (ItemEvent e) {
    set((CheckboxMenuItem) e.getSource());
    if (il != null) il.itemStateChanged(e);
    // tell external listener which item is now active
  }

  public boolean contains (CheckboxMenuItem item) {
    return list.contains(item);
  }

  public CheckboxMenuItem get_active () {
    // return currently active checkbox
    return active;
  }

}

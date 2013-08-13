package TCGA;
// manage a boolean-value option and associated AbstractButton
// (e.g. JCheckBoxMenuItem, etc.)

import javax.swing.*;
import java.awt.event.*;
import java.util.*;

public class BooleanOption extends Observable implements ActionListener {
  private AbstractButton button;
  private boolean value;
  // value is stored in this object rather than in the AbstractButton,
  // as the button may not be available initially
  private String label;
  private BooleanOptionGroup bog = null;

  public BooleanOption (boolean value) {
    button = null;
    this.value=value;
  }

  public BooleanOption (boolean value, String label) {
    button = null;
    this.value=value;
    this.label = label;
  }
  
  public boolean booleanValue() {
    return value;
  }

  public void setValue(boolean value) {
    this.value = value;
    if (button != null) button.setSelected(value);
  }

  public void set_bog (BooleanOptionGroup bog) {
    this.bog = bog;
  }

  public void setValueOnly(boolean value) {
    this.value = value;
  }

  public void set_button (AbstractButton button) {
    this.button = button;
    button.setSelected(value);
    button.addActionListener(this);
  }

  // begin ActionListener stub
  public void actionPerformed(ActionEvent e) {
    value = button.isSelected();
    //    System.err.println("BooleanOption toggle: value is now " + value + " for " + label);  // debug
    if (value && bog != null) bog.setSelected(this);
    setChanged();
    notifyObservers();
  }
  // end ActionListener stub
  
  public String get_label() {
    return label;
  }


}

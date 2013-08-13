package Ace2;
// manage a Number controlled by a JSpinner.

import javax.swing.*;
import javax.swing.event.*;

public class SpinnerNumber implements ChangeListener {
  private Number number;

  public SpinnerNumber (Number number) {
    this.number = number;
  }

  public Number get_number() {
    return number;
  }

  public int intValue() {
    return number.intValue();
  }

  public double doubleValue() {
    return number.doubleValue();
  }

  public float floatValue() {
    return number.floatValue();
  }

  // begin changeListener stub
  public void stateChanged(ChangeEvent e) {
    JSpinner sp = (JSpinner) e.getSource();
    SpinnerNumberModel snm = (SpinnerNumberModel) sp.getModel();
    number = snm.getNumber();
  }
  // end changeListener stub

}
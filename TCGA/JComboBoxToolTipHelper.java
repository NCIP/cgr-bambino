package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class JComboBoxToolTipHelper implements MouseListener {
  // set tooltip text for a JComboBox (for those w/very long entries,
  // whose display may be restricted with setPrototypeDisplayValue())
  private JComboBox jc;
  private boolean enabled = true;
  private String override_text = null;
  private Formatter fmt = null;

  public JComboBoxToolTipHelper (JComboBox jc) {
    this.jc = jc;
    jc.addMouseListener(this);
  }

  public JComboBoxToolTipHelper (JComboBox jc, Formatter fmt) {
    this.jc = jc;
    this.fmt = fmt;
    jc.addMouseListener(this);
  }

  public void setEnabled (boolean v) {
    enabled = v;
  }

  public void setToolTipText (String text) {
    // override with hardcoded text
    override_text = text;
  }

  // begin MouseListener stubs
  public void mousePressed(MouseEvent e) {}
  public void mouseClicked(MouseEvent e) {};
  public void mouseReleased(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {
    if (enabled) {
      String value = null;
      if (override_text != null) {
	// given specific text, use verbatim
	value = override_text;
      } else {
	value = jc.getSelectedItem().toString();
	if (fmt != null) value = fmt.format(value);
      }
      jc.setToolTipText(value);
    }
  };
  public void mouseExited(MouseEvent e) {};
  // end MouseListener stubs
  
}

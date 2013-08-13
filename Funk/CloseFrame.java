package Funk;

import java.awt.*;
import java.awt.event.*;

public class CloseFrame extends Frame implements WindowListener {

  public CloseFrame () {
    addWindowListener(this);
  }

  public void die () {
    setVisible(false);
    dispose();
  }

  public void windowActivated(WindowEvent we) {}
  public void windowClosed(WindowEvent we) {}
  public void windowDeactivated(WindowEvent we) {}
  public void windowDeiconified(WindowEvent we) {}
  public void windowIconified(WindowEvent we) {}
  public void windowOpened(WindowEvent we) {}
  public void windowClosing(WindowEvent we) {
    die();
  }

}

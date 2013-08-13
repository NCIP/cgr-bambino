package Funk;

import java.awt.*;
import java.awt.event.*;

public class DieFrame extends Frame implements WindowListener {

  public DieFrame () {
    addWindowListener(this);
  }

  public void die () {
    setVisible(false);
    dispose();
    System.exit(0);
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

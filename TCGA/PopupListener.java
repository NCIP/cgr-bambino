package TCGA;

import java.awt.Component;
import java.awt.event.*;
import javax.swing.JPopupMenu;
import java.util.*;

public class PopupListener extends Observable implements MouseListener {
  //
  // generic listener to display a JPopupMenu when appropriate
  //
  private JPopupMenu jpm;
  private MouseEvent me;

  public PopupListener (Component c, JPopupMenu jpm) {
    super();
    c.addMouseListener(this);
    this.jpm = jpm;
  }
  
  private void maybeShowPopup(MouseEvent e) {
    if (e.isPopupTrigger()) {
      setChanged();
      notifyObservers(e);
      // Q: will observers be notified before code below executes??

      me = e;
      jpm.show(e.getComponent(), e.getX(), e.getY());
    }
  }

  public MouseEvent getMouseEvent() {
    return me;
  }

  // begin MouseListener stubs
  public void mouseClicked(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};

  public void mousePressed(MouseEvent e) {
    maybeShowPopup(e);
  }

  public void mouseReleased(MouseEvent e) {
    maybeShowPopup(e);
  }
  // end MouseListener stubs

}

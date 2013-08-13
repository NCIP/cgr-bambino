package TCGA;

import java.awt.event.*;
import java.util.*;

public class ChromScaleClicker extends Observable implements MouseListener {
  private ChromScalePanel2 csp;

  public ChromScaleClicker (ChromScalePanel2 csp) {
    this.csp = csp;
    csp.addMouseListener(this);
  }

  // begin MouseListener stubs
  public void mousePressed(MouseEvent e) {}
  public void mouseClicked(MouseEvent e) {
    if (e.getClickCount() > 1) {
      // double-click
      setChanged();
      notifyObservers(csp.get_mouse_bin(e.getPoint()));
    }
  };
  public void mouseReleased(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};
  // end MouseListener stubs

}

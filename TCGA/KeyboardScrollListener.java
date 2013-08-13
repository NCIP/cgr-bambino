package TCGA;
// use the keyboard arrows and page up/down keys to send scrolling 
// events to a horizontal and vertical scrollbar

import java.awt.event.*;
import javax.swing.*;

public class KeyboardScrollListener implements KeyListener {
  private JScrollBar jsb_h, jsb_v;

  public KeyboardScrollListener(JScrollBar jsb_h, JScrollBar jsb_v) {
    this.jsb_h = jsb_h;
    this.jsb_v = jsb_v;
  }

  // begin KeyListener stubs 
  public void keyPressed(KeyEvent ke) {
    int direction = 0;
    int amount = 0;
    JScrollBar jsb = null;
    if (ke.getKeyCode() == KeyEvent.VK_LEFT) {
      jsb = jsb_h;
      direction = -1;
      amount = jsb.getUnitIncrement();
    } else if (ke.getKeyCode() == KeyEvent.VK_RIGHT) {
      jsb = jsb_h;
      direction = 1;
      amount = jsb.getUnitIncrement();
    } else if (ke.getKeyCode() == KeyEvent.VK_UP) {
      jsb = jsb_v;
      direction = -1;
      amount = jsb.getUnitIncrement();
    } else if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
      jsb = jsb_v;
      direction = 1;
      amount = jsb.getUnitIncrement();
    } else if (ke.getKeyCode() == KeyEvent.VK_PAGE_UP) {
      jsb = ke.isAltDown() ? jsb_h : jsb_v;
      direction = -1;
      amount = jsb.getBlockIncrement();
    } else if (ke.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
      jsb = ke.isAltDown() ? jsb_h : jsb_v;
      direction = 1;
      amount = jsb.getBlockIncrement();
    }

    if (jsb != null) {
      jsb.setValue(jsb.getValue() + (amount * direction));
    }

  }
  public void keyReleased(KeyEvent ke) {}
  public void keyTyped(KeyEvent ke) {}
  // end KeyListener stubs 

}
package Funk;

import java.awt.*;
import java.awt.event.*;

public class ScrollCanvas extends Canvas implements AdjustmentListener,KeyListener {
  private Scrollbar s_h = null;
  private Scrollbar s_v = null;
  
  public Scrollbar create_x_scrollbar () {
    s_h = new Scrollbar( Scrollbar.HORIZONTAL );
    s_h.addAdjustmentListener(this);
    return s_h;
  }

  public Scrollbar create_y_scrollbar () {
    s_v = new Scrollbar( Scrollbar.VERTICAL );
    s_v.addAdjustmentListener(this);
    return s_v;
  }

  // begin AdjustmentListener stub
  public void adjustmentValueChanged(AdjustmentEvent e) {
    repaint();
  }
  // end AdjustmentListener stub

  public Scrollbar get_horizontal_scrollbar () {
    return s_h;
  }

  public Scrollbar get_vertical_scrollbar () {
    return s_v;
  }

  public void x_scroll_setup (int visible, int max_value) {
    // set values given the maximum "virtual size" of the canvas
    scale_it(s_h, visible, max_value);
  }

  public void y_scroll_setup (int visible, int max_value) {
    // set values given the maximum "virtual size" of the canvas
    scale_it(s_v, visible, max_value);
  }

  public void set_y_scroll_max (int max_value) {
    Dimension d = getSize();
    scale_it(s_v, d.height, max_value);
  }

  private void scale_it (Scrollbar sb, int visible, int max_value) {
    sb.setValues(sb.getValue(),
		  visible,
		  0,
		  max_value);
    sb.setBlockIncrement(visible);
    int unit = visible / 10;
    if (unit < 1) unit = 1;
    sb.setUnitIncrement(unit);
  }

  public int get_scroll_x () {
    return s_h.getValue();
  }

  public int get_scroll_y () {
    return s_v.getValue();
  }

// begin KeyListener stubs 
public void keyPressed(KeyEvent ke) {
  int code = ke.getKeyCode();
  Scrollbar bar = null;
  int inc_type = 0;
  boolean block = false;
  if (s_v != null) {
    // vertical scrollbar present
    bar = s_v;
    if (code == KeyEvent.VK_PAGE_UP) {
      block = true;
      inc_type = -1;
    } else if (code == KeyEvent.VK_PAGE_DOWN) {
      block = true;
      inc_type = 1;
    } else if (code == KeyEvent.VK_UP) {
      inc_type = -1;
    } else if (code == KeyEvent.VK_DOWN) {
      inc_type = 1;
    }
  }

  if (inc_type != 0) {
    bar.setValue(bar.getValue() + ((block ? bar.getBlockIncrement() : bar.getUnitIncrement()) * inc_type));
    do_repaint();
  }
}

  public void do_repaint () {
    repaint();
  }

public void keyReleased(KeyEvent ke) {}
public void keyTyped(KeyEvent ke) {}
// end KeyListener stubs 


}

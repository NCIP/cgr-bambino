package Funk;
// generic scrollbar/canvas handler

import java.awt.*;
import java.awt.event.*;

public class ScrollThing implements AdjustmentListener {
  private Canvas c;
  private Scrollbar s_h, s_v;
  
  public ScrollThing (Canvas c, boolean horizontal, boolean vertical) {
    this.c = c;
    s_h = s_v = null;
    if (horizontal) {
      s_h = new Scrollbar( Scrollbar.HORIZONTAL );
      s_h.addAdjustmentListener(this);
    }
    if (vertical) {
      s_v = new Scrollbar( Scrollbar.VERTICAL );
      s_v.addAdjustmentListener(this);
    }
  }

  // begin AdjustmentListener stub
  public void adjustmentValueChanged(AdjustmentEvent e) {
    c.repaint();
  }
  // end AdjustmentListener stub

  public Scrollbar get_horizontal_scrollbar () {
    return s_h;
  }

  public Scrollbar get_vertical_scrollbar () {
    return s_v;
  }

  public void x_setup (int visible, int max_value) {
    // set values given the maximum "virtual size" of the canvas
    scale_it(s_h, visible, max_value);
  }

  public void y_setup (int visible, int max_value) {
    // set values given the maximum "virtual size" of the canvas
    scale_it(s_v, visible, max_value);
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

  public int get_x () {
    return s_h.getValue();
  }

  public int get_y () {
    return s_v.getValue();
  }

}

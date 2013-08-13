package TCGA;

import java.awt.*;
import java.awt.event.*;

public class RubberBandSelectionScaled extends RubberBandSelection {
  //
  //  convert selection to unscaled coordinates for ScalePanel2 components
  //  
  private ScalePanel2 sp;

  public RubberBandSelectionScaled (ScalePanel2 sp) {
    super(sp);
    this.sp = sp;
  }

  public void mousePressed(MouseEvent e) {
    if (is_usable_event(e)) {
      // start selection
      origin = sp.get_unscaled_point(e.getPoint());
      drag_point = null;
      sp.repaint();
    }
  }

  public void mouseDragged(MouseEvent e) {
    if (is_usable_event(e) &&
	e.getID() == MouseEvent.MOUSE_DRAGGED) {
      drag_point = sp.get_unscaled_point(e.getPoint());
      sp.repaint();
    }
  }

  
}

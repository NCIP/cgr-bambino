package TCGA;

import java.awt.*;
import java.awt.event.*;

public class RubberBandSelection implements MouseListener,MouseMotionListener {
  //
  //  track a "selection" made with the mouse on a Component.
  //
  protected Point origin, drag_point;
  protected Component c;

  private static int ACTIVE_BUTTON_MASK = MouseEvent.BUTTON1_MASK;
  private Point start,end;
  private String selected_label;

  public RubberBandSelection (Component c) {
    this.c = c;
    c.addMouseListener(this);
    c.addMouseMotionListener(this);
  }

  protected boolean is_usable_event(MouseEvent e) {
    return ((e.getModifiers() & ACTIVE_BUTTON_MASK) > 0);
  }

  // begin MouseListener stubs
  public void mousePressed(MouseEvent e) {
    if (is_usable_event(e)) {
      // start selection
      origin = e.getPoint();
      drag_point = null;
      c.repaint();
    }
  }

  public void mouseReleased(MouseEvent e) {};
  public void mouseClicked(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};
  // end MouseListener stubs

  // begin MouseMotionListener stubs
  public void mouseDragged(MouseEvent e) {
    if (is_usable_event(e) &&
	e.getID() == MouseEvent.MOUSE_DRAGGED) {
      drag_point = e.getPoint();
      c.repaint();
    }
  }
  public void mouseMoved(MouseEvent e) {};
  // begin MouseMotionListener stubs

  public boolean has_selection() {
    return origin != null && drag_point != null;
  }

  public void clear_selection() {
    origin = drag_point = null;
    selected_label = null;
  }

  private void sort_points() {
    start = new Point();
    end = new Point();

    if (drag_point.y < 0) drag_point.y = 0;

    // sort origin and final drag point:
    if (origin.x < drag_point.x) {
      start.x = origin.x;
      end.x = drag_point.x;
    } else {
      start.x = drag_point.x;
      end.x = origin.x;
    }

    if (origin.y < drag_point.y) {
      start.y = origin.y;
      end.y = drag_point.y;
    } else {
      start.y = drag_point.y;
      end.y = origin.y;
    }

    if (start.x < 0) start.x = 0;
    // hack: user can drag selection "offscreen"

  }

  public Point get_start_point() {
    sort_points();
    return start;
  }

  public Point get_end_point() {
    sort_points();
    return end;
  }

  public int get_start_x() {
    sort_points();
    return start.x;
  }

  public int get_end_x() {
    sort_points();
    return end.x;
  }

  public int get_start_y() {
    sort_points();
    return start.y;
  }

  public int get_end_y() {
    sort_points();
    return end.y;
  }

  public Rectangle get_selection() {
    Rectangle result = null;
    if (origin != null && drag_point != null) {
      sort_points();
      result = new Rectangle(start.x, start.y,
			     ((end.x - start.x) + 1),
			     ((end.y - start.y) + 1));
    }
    return result;
  }

  public void set_selection (Rectangle r) {
    origin = new Point(r.x, r.y);
    drag_point = new Point((r.x + r.width) - 1,
			   (r.y + r.height) - 1);
  }

  public void set_selected_label (String s) {
    selected_label = s;
  }

  public String get_selected_label() {
    return selected_label;
  }
  
}

package TCGA;

import javax.swing.JScrollBar;
import java.awt.*;
import java.awt.event.*;
import TCGA.Options;

public class MouseDragScroller implements MouseListener,MouseMotionListener,KeyListener {
  private JScrollBar sb_h, sb_v;
  private Component c;
  private Point origin = null;
  private Point sb_start = new Point(0,0);
  private boolean compensate_for_applied = false;
  private int ACTIVE_BUTTON_MASK = MouseEvent.BUTTON2_MASK;
  
  private static boolean EMULATE_WITH_KEYBOARD = true;
  private static int EMULATION_KEYCODE = KeyEvent.VK_CONTROL;
  // holding down control key emulates holding down the desired mouse button
  private boolean emulation_active = false;
  private MouseEvent last_mouse_event;

  private boolean REVERSE_MOUSE_DRAG_SCROLL = true;
  private Cursor previous_cursor = null;
  private Cursor drag_cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
  private int drag_scale_factor = 0;

  public MouseDragScroller(Component c, JScrollBar sb_h) {
    this.sb_h = sb_h;
    this.c = c;
    setup();
  }

  public MouseDragScroller(Component c, JScrollBar sb_h, JScrollBar sb_v) {
    this.c = c;
    this.sb_h = sb_h;
    this.sb_v = sb_v;
    setup();
  }

  public void set_drag_button_mask (int button_mask) {
    // MouseEvent.BUTTON1_MASK, MouseEvent,BUTTON2_MASK, etc.
    ACTIVE_BUTTON_MASK = button_mask;
  }
  
  private void setup() {
    drag_scale_factor = 0;
    c.addMouseMotionListener(this);
    c.addMouseListener(this);
    if (EMULATE_WITH_KEYBOARD) c.addKeyListener(this);
  }

  private boolean usable_event(MouseEvent e) {
    return ((e.getModifiers() & ACTIVE_BUTTON_MASK) > 0);
  }

  private void drag_it (int here, int origin_value, int sb_start, JScrollBar sb) {
    int total_diff = here - origin_value;

    int already_applied = sb.getValue() - sb_start;
    // compensate for drags already applied to and reflected by scrollbar
    if (compensate_for_applied) total_diff -= already_applied;

//     System.err.println("total_diff="+total_diff);  // debug
//     if (true) {
//       System.err.println("scaling");  // debug
//       total_diff /= 20;
//     }

    if (drag_scale_factor > 0) {
      //      System.err.println("drag scaling factor: " + drag_scale_factor);  // debug
      total_diff /= drag_scale_factor;
    }

    if (total_diff != 0) {
      int next_v;
      if (REVERSE_MOUSE_DRAG_SCROLL) {
	next_v = sb_start - total_diff;
      } else {
	next_v = sb_start + total_diff;
      }
      sb.setValue(next_v);
    }

  }


  public void set_compensate_for_applied (boolean c) {
    compensate_for_applied=c;
  }

  // begin MouseMotionListener stubs
  public void mouseDragged(MouseEvent e) {
    if (e.getID() == MouseEvent.MOUSE_DRAGGED && usable_event(e)) {
      do_drag(e);
    }
  }

  private void do_drag (MouseEvent e) {
    Point p = e.getPoint();
    drag_it(p.x, origin.x, sb_start.x, sb_h);
    if (sb_v != null) drag_it(p.y, origin.y, sb_start.y, sb_v);
  }

  public void mouseMoved(MouseEvent e) {
    if (EMULATE_WITH_KEYBOARD) {
      origin_setup(last_mouse_event = e, false);
      // paranoia: ensure drag origin is set
      if (emulation_active) do_drag(e);
    }
  };

  private void origin_setup(MouseEvent e, boolean force) {
    if (e != null && (force ? true : origin == null)) {
      origin = e.getPoint();
      if (sb_h != null) sb_start.x = sb_h.getValue();
      if (sb_v != null) sb_start.y = sb_v.getValue();
    }
  }


  // begin MouseListener stubs
  public void mousePressed(MouseEvent e) {
    // mouse pressed: record origin of drag and current scrollbar position
    if (EMULATE_WITH_KEYBOARD) c.requestFocusInWindow();
    if (usable_event(e)) {
      origin_setup(e, true);
      if (previous_cursor == null) {
	previous_cursor = c.getCursor();
	c.setCursor(drag_cursor);
      }
    }
  };

  public void mouseReleased(MouseEvent e) {
    if (previous_cursor != null && !previous_cursor.equals(drag_cursor)) {
      c.setCursor(previous_cursor);
      previous_cursor = null;
    }
  };
  public void mouseClicked(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};
  // end MouseListener stubs

  // begin KeyListener stubs 
  public void keyPressed(KeyEvent ke) {
    if (ke.getKeyCode() == EMULATION_KEYCODE) {
      origin = null;
      origin_setup(last_mouse_event, false);
      // maybe mouse event might not be available yet?
      emulation_active = true;
    }
  }
  public void keyReleased(KeyEvent ke) {
    if (ke.getKeyCode() == EMULATION_KEYCODE) emulation_active = false;
  }
  public void keyTyped(KeyEvent ke) {
  }
  // end KeyListener stubs 


  public void set_drag_scale_factor (int factor) {
    drag_scale_factor = factor;
  }

}


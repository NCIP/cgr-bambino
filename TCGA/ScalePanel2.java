package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public class ScalePanel2 extends JPanel implements AdjustmentListener,ComponentListener {
  public static float DEFAULT_X_SCALE_LEVEL = (float) 1.0;
  public static float DEFAULT_Y_SCALE_LEVEL = (float) 1.0;
  
  private Dimension raw_size = null;
  private float x_scale_level = DEFAULT_X_SCALE_LEVEL;
  private float y_scale_level = DEFAULT_Y_SCALE_LEVEL;
  private JScrollBar jsb_h,jsb_v;
  private boolean allow_scale = false;
  private float min_vertical_scale_level = 0;
  private float max_vertical_scale_level = 0;
  private float max_horizontal_scale_level = 0;

  public static int ZOOM_DIRECTION_IN = 1;
  public static int ZOOM_DIRECTION_OUT = 2;

  public static int ZOOM_AXIS_HORIZONTAL = 3;
  public static int ZOOM_AXIS_VERTICAL = 4;
  public static int ZOOM_AXIS_BOTH = 5;
  public static int ZOOM_AXIS_NONE = 6;
  
  public ScalePanel2 () {
    raw_size = new Dimension(0,0);
    setup();
  }

  public ScalePanel2 (Dimension raw_size, JScrollBar jsb_h, JScrollBar jsb_v) {
    this.raw_size = raw_size;
    this.jsb_h = jsb_h;
    this.jsb_v = jsb_v;
    setup();
  }
  
  private void setup () {
    //   setPreferredSize(get_scaled_size());
    if (jsb_h != null) set_horizontal_scrollbar(jsb_h);
    if (jsb_v != null) set_vertical_scrollbar(jsb_v);
    addComponentListener(this);
  }

  public void set_min_vertical_scale_level (float l) {
    min_vertical_scale_level = l;
  }

  public void set_max_vertical_scale_level (float l) {
    max_vertical_scale_level = l;
    //    System.err.println("max vscale " + l);  // debug
  }

  public void set_max_horizontal_scale_level (float l) {
    max_horizontal_scale_level = l;
  }

  public float get_max_horizontal_scale_level () {
    return max_horizontal_scale_level;
  }

  public float get_max_vertical_scale_level () {
    return max_vertical_scale_level;
  }
  
  public void set_allow_scale (boolean scale) {
    allow_scale = scale;
  }

  public boolean get_allow_scale () {
    return allow_scale;
  }

  public JScrollBar get_horizontal_scrollbar() {
    return jsb_h;
  }

  public JScrollBar get_vertical_scrollbar() {
    return jsb_v;
  }

  public void zoom_to_selection (int start_x, int width) {
    //
    // set horizontal view/zoom level to display specified range
    //
    Dimension d = getSize();
    float x_scale = (float) d.width / width;
    set_horizontal_scale_level(x_scale);
    jsb_h.setValue((int) (start_x * x_scale));
    repaint();
  }

  public void zoom_to_vertical_selection (int start_y, int height) {
    //
    // set horizontal view/zoom level to display specified range
    //
    Dimension d = getSize();
    float y_scale = (float) d.height / height;
    set_vertical_scale_level(y_scale);
    jsb_v.setValue((int) (start_y * y_scale));
    repaint();
  }

  public Dimension get_scaled_size(Dimension d) {
    return new Dimension((int) (d.width * x_scale_level),
			 (int) (d.height * y_scale_level));
  }

  public Dimension get_scaled_size() {
    return get_scaled_size(raw_size);
  }

  public void set_raw_size (Dimension d) {
    this.raw_size = d;
  }

  public int get_raw_height() {
    return raw_size.height;
  }

  public int get_raw_width() {
    return raw_size.width;
  }

  public int get_scaled_width() {
    return get_scaled_size().width;
  }

  public int get_scaled_height() {
    return get_scaled_size().height;
  }

  public void set_horizontal_scrollbar (JScrollBar jsb_h) {
    // hack and the hacktones
    this.jsb_h = jsb_h;
    jsb_h.addAdjustmentListener(this);
    set_extents_horizontal();
  }

  public void set_vertical_scrollbar (JScrollBar jsb_v) {
    // hack and the hacktones
    this.jsb_v = jsb_v;
    jsb_v.addAdjustmentListener(this);
    set_extents_vertical();
  }

  public void set_extents_horizontal() {
    if (jsb_h == null) {
      System.err.println("ERROR: null h scrollbar!");  // debug
    } else {
      BoundedRangeModel brm = jsb_h.getModel();
      brm.setMinimum(0);
      brm.setMaximum(get_scaled_width());
      Dimension d = getSize();
      int extent = d.width;

      //    System.err.println("size:" + d + " horiz extent:"+extent);  // debug

      int block = (int) (d.width * 0.9);
      int unit = (int) (d.width / 10);
      if (unit < 1) unit = 1;

      brm.setExtent(extent);
      jsb_h.setUnitIncrement(unit);
      jsb_h.setBlockIncrement(block);
    } 
  }

  public void set_extents_vertical() {
    BoundedRangeModel brm = jsb_v.getModel();
    brm.setMinimum(0);
    //    System.err.println("vscalelevel="+y_scale_level);  // debug

    if (false) {
      System.err.println("DEBUG extents vertical");  // debug
      brm.setMaximum(get_scaled_height());
    } else {
      //      brm.setMaximum(get_scaled_height() + (int) y_scale_level);
      brm.setMaximum(get_scaled_height() + (int) (y_scale_level * 0.49));
      // since painting starts at a rounded-down int, need to add a little
      // to ensure all rows are displayed.
      // FIX ME:
      //  1. this creates other problems by sometimes returning a too-large value.
      // FIX ME: columns too??
    }

    Dimension d = getSize();
    int extent = d.height;

    int block = (int) (d.height * 0.9);
    int unit = (int) (d.height / 10);
    if (unit < y_scale_level) unit = (int) y_scale_level;

    brm.setExtent(extent);
    jsb_v.setUnitIncrement(unit);
    jsb_v.setBlockIncrement(block);
  }

  public float get_horizontal_scale_level() {
    return x_scale_level;
  }

  public float get_vertical_scale_level() {
    return y_scale_level;
  }

  public void set_horizontal_scale_level (float f) {
    //    System.err.println("set h scale: " + f);  // debug
    x_scale_level = f;
    set_extents_horizontal();
    repaint();
  }

  public void set_vertical_scale_level (float f) {
    y_scale_level = f;
    set_extents_vertical();
    repaint();
  }

  public void set_scale_levels (float h, float v) {
    System.err.println("set scale levels:" +h);  // debug

    x_scale_level = h;
    y_scale_level = v;
    set_extents_horizontal();
    set_extents_vertical();
  }

  public void zoom_in () {
    zoom_in(Options.ZOOM_FACTOR);
  }

  public void center_on_x (int x) {
    // center on given coordinate
    // value is in RAW coordinate space (needs to be scaled)
    //    System.err.println("CENTER on " + x);  // debug
    int start = (int) (x * x_scale_level) - (int) (jsb_h.getModel().getExtent() / 2);
    if (start < 0) start = 0;
    jsb_h.setValue(start);
  }

  public void center_on_region (Rectangle r) {
    // center on given region
    // value is in RAW coordinate space (needs to be scaled)
    int end = r.x + r.width;
    center_on_x(r.x + (int) (r.width / 2));
  }

  public void zoom (int direction, int axis, float factor, Point mp) {
    if (axis == ZOOM_AXIS_NONE) {
      // disabled
    } else if (axis == ZOOM_AXIS_BOTH) {
      zoom(direction, ZOOM_AXIS_HORIZONTAL, factor, mp);
      zoom(direction, ZOOM_AXIS_VERTICAL, factor, mp);
    } else {
      if (direction == ZOOM_DIRECTION_OUT) factor = 1 / factor;
      JScrollBar jsb = null;
      if (axis == ZOOM_AXIS_HORIZONTAL) {
	float before = x_scale_level;
	x_scale_level *= factor;
	float min = get_max_zoom_out_level();
	//	System.err.println("before: " + before + " max=" + max_horizontal_scale_level);  // debug
	if (x_scale_level < min) x_scale_level = min;
	if (max_horizontal_scale_level > 0 &&
	    x_scale_level > max_horizontal_scale_level) {
	  x_scale_level = max_horizontal_scale_level;

	  if (x_scale_level < before) {
	    // grandfather in existing startup zoom level
	    //	    System.err.println("grandfather");  // debug
	    x_scale_level = before;
	  }
	  factor = x_scale_level / before;
	}
	//	System.err.println("zoom horiz, now " + x_scale_level);
	jsb = jsb_h;
      } else if (axis == ZOOM_AXIS_VERTICAL) {
	//	System.err.println("YSL start:"+y_scale_level + " factor:"+factor);  // debug
	float before = y_scale_level;
	y_scale_level *= factor;
	if (min_vertical_scale_level > 0 && y_scale_level < min_vertical_scale_level) {
	  y_scale_level = min_vertical_scale_level;
	  factor = min_vertical_scale_level / before;
	  //	  System.err.println("MIN VERT SCALE THRESHOLD");  // debug
	}
	if (max_vertical_scale_level > 0 && y_scale_level > max_vertical_scale_level) {
	  y_scale_level = max_vertical_scale_level;
	  // before * factor = max_vertical_scale_level
	  // factor = max_vertical_scale_level / before
	  factor = max_vertical_scale_level / before;
	  // we've passed maximum zoom level:
	  // adjust scale factor to reflect this or centering will break
	}
	jsb = jsb_v;
      } else {
	System.err.println("zoom(): format ERROR!");  // debug
      }
      float extent = (float) jsb.getModel().getExtent();
      float half_extent = ((float) jsb.getModel().getExtent() / 2);
      float center = jsb.getValue() + half_extent;
      // center of view (current scaled coordinates)
      if (Options.MOUSE_ZOOM_INFLUENCED_BY_CURSOR
	  && mp != null
	  && (Options.MOUSE_ZOOM_INFLUENCED_BY_CURSOR_IN_ONLY ? direction == ZOOM_DIRECTION_IN : true)
	  ) {
	//
	// when zooming with mouse wheel, zoom location is influenced by cursor position.
	//
	float frac;
	if (axis == ZOOM_AXIS_HORIZONTAL) {
	  frac = ((float) mp.x) / getSize().width;
	} else {
	  frac = ((float) mp.y) / getSize().height;
	}
	float mouse_val = jsb.getValue() + (extent * frac);

	//	System.err.println("center="+center + " mouse_raw=" + mp + " mouse=" + mouse_val + " x_scale="+x_scale_level + " y_scale="+y_scale_level);  // debug
	//	center = (mouse_val + center) / 2;
	//	center = (mouse_val + center + center) / 3;

	center = (mouse_val + (center * Options.MOUSE_ZOOM_INFLUENCE_DILUTION)) / (Options.MOUSE_ZOOM_INFLUENCE_DILUTION + 1);
	// dilution is how much to dilute mouse position with default/center position

	//	center = mouse_val;
	//	System.err.println("smoothed to " + center);  // debug
      }

      float scaled_center = center * factor;
      // center of view (target scaled coordinates)
      int start_new = (int) (scaled_center - half_extent);

      if (axis == ZOOM_AXIS_HORIZONTAL) {
	set_extents_horizontal();
      } else {
	set_extents_vertical();
      }
      jsb.setValue(start_new);
      repaint();
    }
  }

  public void zoom_in(float factor) {
    x_scale_level *= factor;
    //    System.err.println("x_scale now:"+ x_scale_level);  // debug
    int half_extent = (int) (jsb_h.getModel().getExtent() / 2);
    int center = jsb_h.getValue() + half_extent;
    // center of view (current scaled coordinates)
    int scaled_center = (int) (center * factor);
    // center of view (target scaled coordinates)
    int x_start_new = scaled_center - half_extent;
    set_extents_horizontal();
    jsb_h.setValue(x_start_new);
    repaint();
  }

  public void zoom_out () {
    zoom_out(Options.ZOOM_FACTOR);
  }

  public void zoom_out(float factor) {
    x_scale_level /= factor;
    float max = get_max_zoom_out_level();
    if (x_scale_level < max) {
      x_scale_level = max;
      System.err.println("gated:" + x_scale_level);  // debug
    }

    int half_extent = (int) (jsb_h.getModel().getExtent() / 2);
    int center = jsb_h.getValue() + half_extent;
    // center of view (current scaled coordinates)
    int scaled_center = (int) (center / factor);
    // center of view (target scaled coordinates)
    int start_new = scaled_center - half_extent;
    set_extents_horizontal();
    set_extents_vertical();
    jsb_h.setValue(start_new);
    repaint();
  }

  private float get_max_zoom_out_level() {
    int extent = jsb_h.getModel().getExtent();
    Dimension d = getSize();
    if (extent < d.width) {
      // source data very small, most "zoomed out" view may actually be zoomed in!
      extent = d.width;
    }
    return (float) extent / raw_size.width;
  }

  //
  //  set max zoom-out level for Y axis only:
  //
  public void zoom_out_max_y () {
    zoom_reset_y();
    y_scale_level = get_max_zoom_out_level_y();
    set_extents_vertical();
    repaint();
  }

  public void zoom_reset_y () {
    if (y_scale_level != (float) 1) {
      float factor = 1 / y_scale_level;
      zoom_in_y(factor);
    }
  }

  public void zoom_in_y (float factor) {
    y_scale_level *= factor;
    //    System.err.println("x_scale now:"+ x_scale_level);  // debug
    int half_extent = (int) (jsb_v.getModel().getExtent() / 2);
    int center = jsb_v.getValue() + half_extent;
    // center of view (current scaled coordinates)
    int scaled_center = (int) (center * factor);
    // center of view (target scaled coordinates)
    int y_start_new = scaled_center - half_extent;
    set_extents_vertical();
    jsb_v.setValue(y_start_new);
    repaint();
  }

  private float get_max_zoom_out_level_y () {
    int extent = jsb_v.getModel().getExtent();
    Dimension d = getSize();
    if (extent < d.height) {
      // source data very small, most "zoomed out" view may actually be zoomed in!
      extent = d.height;
    }
    return (float) extent / raw_size.height;
  }


  //
  //  set max zoom-out level for Y axis only
  //  END
  //



  public void zoom_out_max () {
    zoom_reset();
    x_scale_level = get_max_zoom_out_level();
    set_extents_horizontal();
    //    set_extents_vertical();
    repaint();
  }

  public void zoom_reset () {
    if (x_scale_level != (float) 1) {
      float factor = 1 / x_scale_level;
      zoom_in(factor);
    }
  }

  public void adjustmentValueChanged(AdjustmentEvent e) {
    //    JScrollBar src = (JScrollBar) e.getSource();
    repaint();
  }

  public int get_unscaled_x_start () {
    int v = jsb_h == null ? 0 : jsb_h.getValue();
    return (int) (v / x_scale_level);
  }

  public int get_unscaled_x_end () {
    int v = 0;
    if (jsb_h != null) {
      v = jsb_h.getValue() + jsb_h.getModel().getExtent();
    }
    return (int) (v / x_scale_level);
  }

  public int get_unscaled_y_start () {
    int v = jsb_v == null ? 0 : jsb_v.getValue();
    return (int) (v / y_scale_level);
  }

  public int get_unscaled_y_end () {
    int v = 0;
    if (jsb_v != null) {
      //      System.err.println("raw v=" + jsb_v.getValue() + " extent:" + jsb_v.getModel().getExtent());  // debug
      v = jsb_v.getValue() + jsb_v.getModel().getExtent();
    }
    return (int) (v / y_scale_level);
  }

  public Point get_unscaled_point (Point p) {
    // translate a relative point on the component (e.g. from a mouse click)
    // into unscaled component coordinate
    //    int x = (int) ((jsb_h.getValue() + p.x) / x_scale_level);
    // WRONG, since scaled image painting always starts at an int boundary
    int x = jsb_h == null ? p.x : get_unscaled_x_start() + (int) (p.x / x_scale_level);
    int y = jsb_v == null ? p.y : get_unscaled_y_start() + (int) (p.y / y_scale_level);
    return new Point(x,y);
  }

  // begin ComponentListener stubs
  public void componentHidden(ComponentEvent e) {}
  public void componentMoved(ComponentEvent e) {}

  public void componentResized(ComponentEvent e) {
    if (jsb_h != null) set_extents_horizontal();
    if (jsb_v != null) set_extents_vertical();
    if (Options.LOCK_ZOOM_OUT) {
      zoom_out_max();
    }
  }
  public void componentShown(ComponentEvent e) {
    set_extents_horizontal();
    set_extents_vertical();
  }
  // end ComponentListener stubs

}

package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.util.*;
import java.math.BigDecimal;

public class SummaryPanel extends ScalePanel2 {

  public ArrayList<Point> digest_array (byte[] array, byte max_value, float scale_by, boolean normalize) {
    //
    //  scan/translate array data for summary graphic
    //
    ArrayList<Point> results = new ArrayList<Point>();

    int start_x = get_unscaled_x_start();
    int end_x = get_unscaled_x_end();
    if (end_x >= array.length) end_x = array.length - 1;

    float x_scale = get_horizontal_scale_level();

    //    Timer t = new Timer("array gen");

    int bin_size = (int) (1 / x_scale);
    int i,j,y;
    float x;
    Point p = null;

    //    System.err.println("XScale="+x_scale);  // debug

    if (bin_size >= 2) {
      //
      // only one horizontal pixel available, but multiple bins
      // need to be painted: scan for largest
      //
      //      for (i=start_x, x=0; i < end_x; i += bin_size, x += (x_scale * bin_size)) {
      for (i=start_x, x=0; i <= end_x; i += bin_size, x += (x_scale * bin_size)) {
	y = 0;
	//	for(j=i; j < (i+bin_size) && j < end_x; j++) {
	for(j=i; j < (i+bin_size) && j <= end_x; j++) {
	  // scan region for highest/lowest values
	  if (array[j] > y) y = array[j];
	}
	p = new Point((int) x, y);
	if (p.y > 0) results.add(p);
      }
    } else {
      // 1:1
      for (i=start_x, x=0; i <= end_x; i++, x += x_scale) {
	p = new Point((int) x,array[i]);
	if (p.y > 0) results.add(p);
      }
    }

    if (scale_by != 1) {
      for (Point p3 : results) {
	p3.y *= scale_by;
      }
    }

    if (normalize) {
      float scale_factor = (float) 100 / (float) max_value;
      //      System.err.println("factor="+scale_factor);  // debug
      if (scale_factor != 1) {
	for (Point p2 : results) {
	  p2.y *= scale_factor;
	}
      }
    }

    //    t.finish();

    return results;
  }

  //  public ArrayList<Point> digest_arraylist (ArrayList<Number> array, float scale_by) {
  public ArrayList<Point> digest_arraylist (ArrayList<BigDecimal> array, float scale_by) {
    //
    //  scan/translate array data for summary graphic
    //
    ArrayList<Point> results = new ArrayList<Point>();

    int start_x = get_unscaled_x_start();
    int end_x = get_unscaled_x_end();
    if (end_x >= array.size()) end_x = array.size() - 1;

    float x_scale = get_horizontal_scale_level();

    //    Timer t = new Timer("array gen");

    int bin_size = (int) (1 / x_scale);
    int i,j;
    float x,y,f;
    Number n;

    //    System.err.println("XScale="+x_scale);  // debug
    int MIN_VALUE = 2;
    // tiny values are invisible; since in GISTIC mode data are relatively sparse,
    // set a minimum amplitude

    boolean present;

    if (bin_size >= 2) {
      //
      // only one horizontal pixel available, but multiple bins
      // need to be painted: scan for largest
      //
      //      for (i=start_x, x=0; i < end_x; i += bin_size, x += (x_scale * bin_size)) {
      for (i=start_x, x=0; i <= end_x; i += bin_size, x += (x_scale * bin_size)) {
	y = 0;
	present = false;
	//	for(j=i; j < (i+bin_size) && j < end_x; j++) {
	for(j=i; j < (i+bin_size) && j <= end_x; j++) {
	  // scan region for highest/lowest values
	  n = array.get(j);
	  if (n != null) {
	    present = true;
	    f = n.floatValue();
	    if (f > y) y = f;
	  }
	}
	y *= scale_by;
	if (present) results.add(new Point((int) x, floor(y, MIN_VALUE)));
      }
    } else {
      // 1:1
      for (i=start_x, x=0; i <= end_x; i++, x += x_scale) {
	n = array.get(i);
	if (n != null) {
	  y = n.floatValue() * scale_by;
	  //	  System.err.println("raw: " +y + " int:" + (int) y + " floor:" + floor(y,MIN_VALUE));  // debug
	  results.add(new Point((int) x, floor(y, MIN_VALUE)));
	}
      }
    }

    //    t.finish();

    return results;
  }

  private int floor (float f, int floor) {
    int v = (int) f;
    if (v < floor) v = floor;
    return v;
  }

  public void render_array(Graphics g, ArrayList<Point> points, int base_y, boolean add) {
    float x_scale = get_horizontal_scale_level();
    if (x_scale > 1) {
      //
      // zoomed in: each bin must be painted on > 1 pixel.
      // draw filled Rectangles rather than lines
      //
	int x,x2,width;
	int last_end=0;
	int last_start=0;
      for (Point p : points) {
	  x2 = p.x + (int) (x_scale + 1);
	  // may cause some interference with XOR drawing of neighboring rectangles

	  // x2 = p.x + (int) Math.floor(x_scale + 1);
	  // no help

	  width = x2 - p.x;

	  x = p.x;
	  //	  System.err.println("x:" + p.x + " x2:" +x2);
	  if (x < last_end) {
	      int diff = last_end - x;
	      //	      System.err.println("error, diff="+diff);
	      x += diff;
	      width -= diff;
	  } else {
	      //	      System.err.println("ok");
	  }

	  if (x == last_start && x2 == last_end) {
	      System.err.println("ERROR 2");
	  }

	  g.fillRect(x, add ? base_y : base_y - p.y,
		     width, p.y);
	  last_start = x;
	  last_end = x2;
      }
    } else {
      //
      // <= 1 bin per horizontal pixel: draw line
      //
      Point last_point = null;
      for (Point p : points) {
	if (p.equals(last_point)) continue;
	// HACK: skip drawing sequential identical lines, interferes with XOR mode
	g.drawLine(p.x, base_y,
		   p.x, base_y + (add ? p.y : -p.y));
	last_point = p;
      }
    }
  }

  public Font get_label_font() {
    return new Font("Times", Font.PLAIN, Options.SUMMARY_PANEL_FONT_HEIGHT);
  }

  public void draw_label (Graphics g, String text, Color c_background, Color c_text, int y) {
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
			java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
    Font f = get_label_font();
    FontMetrics fm = getFontMetrics(f);
    TextLayout textTl = new TextLayout(text, f, g2.getFontRenderContext());
    Shape shape = textTl.getOutline(new AffineTransform());

    // position:
    Rectangle r = shape.getBounds();
    AffineTransform at = new AffineTransform();
    at.translate(Options.SUMMARY_PANEL_INDENT_PIXELS, y);

    AffineTransform at_orig = g2.getTransform();
    g2.transform(at);

    Composite c_old = g2.getComposite();

    AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.80f);
    g2.setComposite(ac);
    // clear space for text by painting with background color,
    // preserving some existing painted color w/alpha blending

    g2.setPaint(c_background);
    g2.setStroke(new BasicStroke(3.5f));
    g2.draw(shape);
    g2.fill(shape);
    // draw and fill with thick stroke to clear space for text

    g2.setComposite(c_old);
    // restore previous blending state

    // draw "inner" text using normal stroke:
    g2.setPaint(c_text);
    g2.setStroke(new BasicStroke(1.0f));
    g2.fill(shape);

    g2.setTransform(at_orig);
  }


}

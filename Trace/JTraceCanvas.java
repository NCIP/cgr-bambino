package Trace;
// Swing version

//import java.awt.*;
import java.util.*;

import javax.swing.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Dimension;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.Graphics;
import java.awt.Graphics2D;


// TO DO:
// - ability to reload/reconfigure trace

public class JTraceCanvas extends JPanel implements Observer {
  private TraceFile trace;
  private JScrollBar scrollbar;
  private TraceDataView tdv = null;

  private static boolean SCROLL_BY_BASES = false;
  private static boolean BASE_SCROLL_FIXED_BLOCK_SIZE = false;
  private static int BASE_SCROLL_FIXED_BLOCK_SIZE_AMOUNT = 0;
  private boolean BASE_SCROLL_CENTERED_MODE = false;

  // static options as a convenience for multiple-trace-viewer code

  public static double DEFAULT_HEIGHT_TO_WIDTH_RATIO = 4;
  // 4 = 4:1 trace height:width aspect ratio
  public static boolean ENABLE_ANTIALIASING = true;
  // global hack

  private static double HEIGHT_TO_WIDTH_RATIO = DEFAULT_HEIGHT_TO_WIDTH_RATIO;

  private final static Color c_color = new Color(160,32,240);
  // purple color for "C" bases
  private final static boolean LOAD_PHD = false;
  private int centered_on = 0;
  private boolean center_visible;
  private boolean center_requested = false;

  private boolean center_base_requested = false;
  private int center_base_request = 0;

  private boolean resized = false;
  private int start_position = 0;
  private boolean loaded = false;
  private PhdFile phd = null;
  private int trace_bytes_loaded = 0;
  private String trace_label = null;

  private BasicStroke thin_stroke = new BasicStroke(0.33f);
  // lines thinner than 1.0 pixels are paler colors?

  private int start_center_base = 0;

  public JTraceCanvas (TraceFile t, int start_position, JScrollBar s) {
    // given pre-loaded TraceFile object
    this.scrollbar = s;
    if (start_position > 0) center_on(start_position);
    trace = t;
    loaded = true;
    if (trace.status() != TraceFile.LOADED)
      System.err.println("JTraceCanvas called w/non-loaded trace!");
  }

  public JTraceCanvas (TraceFile t, int start_position, JScrollBar s, String trace_label) {
    // given pre-loaded TraceFile object
    this.scrollbar = s;
    this.trace_label = trace_label;
    if (start_position > 0) center_on(start_position);
    trace = t;
    loaded = true;
    if (trace.status() != TraceFile.LOADED)
      System.err.println("JTraceCanvas called w/non-loaded trace!");
  }

  JTraceCanvas (String filename, int start_position, boolean rc,
	       JScrollBar s) {
    this.scrollbar = s;
    this.start_position = start_position;
    trace = new TraceFile(filename, rc, this);
    if (LOAD_PHD) {
      // try to get phred base-position info
      System.out.println("** FIX ME: load_phd on");  // debug
      phd = new PhdFile(filename, this);
    }
  }

  public static Color get_trace_color (int i) {
    switch (i) {
    case TraceFile.TRACE_A: case 'a': case 'A': return(Color.green); 
    case TraceFile.TRACE_C: case 'c': case 'C': return(c_color);
    case TraceFile.TRACE_G: case 'g': case 'G': return(Color.black);
    case TraceFile.TRACE_T: case 't': case 'T': return(Color.red);
    default: return(Color.black);
    }
  }

  public TraceFile get_trace () {
    // get the Trace object used by the viewer
    return trace;
  }

  public void setPhd (PhdFile p) {
    phd = p;
    repaint();
  }

  public void update (Observable o, Object arg) {
    if (o instanceof TraceFile) {
      // notification from trace file
      if (arg == null) {
	// finished loading
	loaded = true;
	if (start_position > 0) center_on(start_position);
      } else {
	// progress report
	trace_bytes_loaded = ((Integer) arg).intValue();
	//	System.out.println("trace reports:" + trace_bytes_loaded);  // debug
      }
      repaint();
    } else if (o instanceof PhdFile) {
      // trace position data has finished loading
      if (trace.reverse_complemented) {
	// have to reverse-complement calls
	while (trace.status() != TraceFile.LOADED) {
	  // trace must be loaded before we can proceed!
	  try {
	    Thread.sleep(100);
	  } catch (InterruptedException e) {}
	}
	phd.reverse_complement(trace.num_samples);
      }
      repaint();
    } else {
      System.out.println("unknown observable " + o); 
    }
  }
  
  public boolean loaded () {
    // done loading?
    return loaded;
  }

  public boolean error () {
    // error loading?
    return trace.error();
  }

  public void go_to(TraceFile tf, int offset) {
    trace = tf;
    center_on(offset);
  }

  public void center_on(int i) {
    // center the view on the specified point.
    //    System.out.println("before:" + i + " comp:" + trace.reverse_complemented + " loaded:" + trace.loaded);
    //    if (trace.reverse_complemented) {
    // automatically flip specified offsets if trace data has been
    // reverse-complemented.  This is a Feature; might make it optional.
    //      i = trace.num_samples - i;
    //    }
    //    System.out.println("after:" + i);

    centered_on = i;

    if (SCROLL_BY_BASES) {
      // find the nearest called base to use for a start point

      int diff, pos;
      int closest_i = 0;
      int closest_diff = -1;
      for (i=0; i < trace.num_bases; i++) {
	// set base index to the call closest to the sample index
	// we're centering on
	pos = trace.base_position[i];
	diff = Math.abs(trace.base_position[i] - centered_on);
	if (i == 0 || diff < closest_diff) {
	  closest_diff = diff;
	  closest_i = i;
	}
      }

      start_center_base = closest_i;

      BASE_SCROLL_CENTERED_MODE = true;
      // permanently on
    }

    center_requested = true;
    repaint();
  }

  public void center_on(int i, boolean auto_rc) {
    // center the view on the specified point.
    //    if (auto_rc && trace.loaded() && trace.reverse_complemented) {
    //    if (auto_rc && trace.reverse_complemented) {
    // "reverse_complemented" refers to initial request to flip, not internal state
    if (auto_rc && trace.flipped) {
      // automatically flip specified offsets if trace data has been
      // reverse-complemented.
      i = trace.num_samples - i;
    }
    center_on(i);
  }

  protected void paintComponent(Graphics gr) {
    //    super.paintComponent(gr);

    int status = trace.status();
    if (trace == null || status != TraceFile.LOADED) {
      // not loaded yet, or error
      String message;
      if (status == TraceFile.NO_DATA) {
 	message = "No trace available for " + trace.name + ".";
      } else if (status == TraceFile.UNKNOWN_FORMAT) {
 	message = "Can't decode trace data for " + trace.name + ".";
      } else {
 	message = "Loading " + trace.name + "...";
 	if (trace_bytes_loaded > 0) {
 	  message = message + " (" + trace_bytes_loaded / 1024 + "k read)";
 	  // why doesn't message.concat() do anything?!
 	}
      }
      //      System.err.println("message="+message);  // debug
      Funk.Gr.centerText(this, gr, message);
      return;
    }

    Graphics2D g = (Graphics2D) gr;
    Dimension d = getSize();
    int width = d.width;
    int height = d.height;

    if (ENABLE_ANTIALIASING) {
      g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
			 java.awt.RenderingHints.VALUE_ANTIALIAS_ON
			 );
    }

    int start_point;
    // first sample index drawn onscreen
    int start_base_index = 0;
    if (SCROLL_BY_BASES) {
      //
      //  scrollbar is in base number space, not sample space
      //
      if (BASE_SCROLL_CENTERED_MODE) {
	// 
	// scrollbar value is the base the view is centered on.
	//
        if (center_base_requested) {
          start_base_index = center_base_request;
          center_base_requested = false;
        } else {
          start_base_index = scrollbar.getValue();
        }
	start_point = 0;
	// assign below after geometry calculations
      } else {
	//
	//  scrollbar value is the start base to display.
	//
	start_base_index = scrollbar.getValue();
	start_point = trace.base_position[start_base_index];
	//      start_point -= trace.average_peak_spacing * 0.75;
	start_point -= trace.average_peak_spacing;
	// back up so we don't start right on top of the base peak
	if (start_point < 0) start_point = 0;
      }
    } else {
      //
      // scrollbar value is in sample space
      //
      start_point = scrollbar.getValue();
    }

    FontMetrics fm = getFontMetrics(getFont());
    int base_offset = (int) (fm.charWidth('C') / 2);
    // to center base letters above peaks/lines
    g.setColor(Color.white);
    g.fillRect(0,0,width,height);
    
    int font_height = (int) (fm.getHeight() * 1.1);

    //    int base_number_y_position = (int) (height * 0.07);
    int base_number_y_position = font_height;
    int phred_y_position = 0;
    int base_y_position, trace_top;
    if (phd != null && phd.loaded) {
      // we have phred base call information
      phred_y_position = font_height * 2;
      base_y_position = font_height * 3;
    } else {
      base_y_position = font_height * 2;
    }
    trace_top = base_y_position + fm.getMaxDescent() + 5;
    int trace_bottom = (int) (height * 0.98);
    int trace_space = trace_bottom - trace_top;
    // total vertical pixels available for drawing traces

    double y_mult = (double) trace_space / (double) trace.max_amplitude;
    // vertical normalizing factor

    double x_mult = (trace_space / trace.average_peak_spacing) / HEIGHT_TO_WIDTH_RATIO;

    //    System.err.println("ts:" + trace_space + " xm:" + x_mult + " ym:" + y_mult);  // debug

    int i;
    int fit = (int) (width / x_mult) + 2;
    // +2: 1 to round up, 1 to draw an additional point beyond
    // the border, to ensure we don't clip when zoomed in
    int end_point = start_point + fit;
    int chunk_size = end_point - start_point;

    if (center_requested || (resized && center_visible)) {
      // Recenter the specified trace point onscreen.
      // Either a recenter has been explicitly requested, or the 
      // viewer window has been resized when the centering point 
      // was visible.
      start_point = centered_on - (chunk_size / 2);
      if (start_point < 1) start_point = 1;
      end_point = start_point + chunk_size;
      center_requested = false;
      resized = false;
      start_base_index = start_center_base;
    } else if (SCROLL_BY_BASES && BASE_SCROLL_CENTERED_MODE) {
      // view is centered on a particular base number
      //      System.err.println("sbi:" + start_base_index + " max:" + scrollbar.getMaximum());  // debug

      start_point = trace.base_position[start_base_index] - (chunk_size / 2);
      end_point = start_point + chunk_size;
      if (start_point < 0) start_point = 0;
    }

    if (end_point > trace.num_samples) {
      // somehow the scrollbar is beyond the last page.
      // Happens in JDK 1.0.2 (on Alpha) if all of these:
      //  1. canvas is scrolled all the way to the right, and
      //  2. scroll control is covered by another window, and
      //  3. applet window is made visible again.
      start_point = trace.num_samples - chunk_size;
      end_point = trace.num_samples;
    }

    int [] x = new int[chunk_size];
    int [] y = new int[chunk_size];

    if (tdv == null) tdv = new TraceDataView(trace);
    tdv.set_buffer(y);
    tdv.set_start_point(start_point);
    tdv.analyze();

    //    System.err.println("chunk_size: " + chunk_size + " start point:" + start_point);  // debug

    //
    //  Draw traces
    //
    //    trace.dump_first_sample("before draw");
    for (i=0; i < 4; i++) {  // "4" = HACK
      // draw the trace for each base type
      g.setColor(get_trace_color(i));
      tdv.get_samples(i);

      int j;
      //      System.err.println("drawing trace base " + i + " => " + trace.trace_data[i]);  // debug

      for (j=0; j < chunk_size; j++) {
	x[j] = (int) (j * x_mult);

        //	y[j] = (int) (trace_bottom - (trace.trace_data[i][start_point + j] * y_mult));

        y[j] = trace_bottom - (int) (y[j] * y_mult);
        // - invert
        // - scale data to headroom

        //        System.err.println("  " + trace.trace_data[i][start_point + j] + " => " + y[j]);  // debug

      }

      g.drawPolyline(x, y, chunk_size);
    }

    //
    //  Draw internal bases / base positions
    //

    int base_number_start_x = 0;
    //
    // draw trace label, if present
    //
    if (trace_label != null) {
      g.setColor(Color.black);
      Font old_font = g.getFont();
      Font new_font = new Font(old_font.getName(),
                               //                               Font.BOLD | Font.ITALIC,
                               Font.ITALIC,
                               old_font.getSize());
      g.setFont(new_font);
      int buffer = 5;
      g.drawString(trace_label, buffer, base_number_y_position);
      base_number_start_x = fm.stringWidth(trace_label) + (buffer * 2);
      // when drawing base numbers, don't overwrite trace label
      g.setFont(old_font);
    }

    int pos, x_point;
    int base_x = 0;
    for (i=0; i < trace.num_bases; i++) {
      pos = trace.base_position[i];
      if (pos >= start_point) {
	if (pos > end_point) break;
	x_point = (int) ((pos - start_point) * x_mult);

        g.setColor(Color.black);

	if (i == 0 || ((i + 1) % 10 == 0)) {
          //
	  // draw base numbers
          //
	  String s = String.valueOf(i + 1);
          int sx = x_point - (int) (fm.stringWidth(s) / 2);
          if (sx > base_number_start_x) 
            g.drawString(s, sx, base_number_y_position);
	}

        int trace_index = TraceFile.TRACE_UNDEF;

	switch(trace.bases[i]) {
	case 'a': case 'A':
          trace_index = TraceFile.TRACE_A;
          break;
	case 'c': case 'C':
          trace_index = TraceFile.TRACE_C;
          break;
	case 'g': case 'G':
          trace_index = TraceFile.TRACE_G;
          break;
	case 't': case 'T':
          trace_index = TraceFile.TRACE_T;
          break;
	}
        
        if (trace_index == TraceFile.TRACE_UNDEF) {
          // if basecall is not A/C/G/T, choose the channel
          // with the strongest signal at position
          int max = -1;
          for (int b = 0; b < 4; b++) {
            if (trace.trace_data[b][pos] > max) {
              max = trace.trace_data[b][pos];
              trace_index = b;
            }
          }
        }

        g.setColor(get_trace_color(trace_index));
	
	g.drawString(String.valueOf(trace.bases[i]),
		     x_point - base_offset, base_y_position);
        // draw base

        //
        // draw vertical line to basecall position:
        //

        if (true) {
          g.setStroke(thin_stroke);
        } else {
          //
          // dashed line
          //
          Stroke dashed = new BasicStroke(1,
                                          BasicStroke.CAP_BUTT,
                                          BasicStroke.JOIN_ROUND,
                                          1.0f,
                                          //  new float[] {16.0f, 20.0f},
                                          new float[] {2.0f, 2.0f},
                                          //                                          new float[] {4.0f, 1.0f},
                                          //                                          new float[] {3.0f, 6.0f},
                                          0.0f
                                          );
          g.setStroke(dashed);
        }

        // draw basecall line from top to amplitude of called base type
        //        int basecall_y = trace.trace_data[trace_index][pos];
        int basecall_y = tdv.get_sample(trace_index, pos);
        int yy = (int) (trace_bottom - (basecall_y * y_mult)) - 3;
        // -3: don't quite touch the tip of the peak
        if (yy < trace_top) yy = trace_top;
        g.drawLine(x_point, trace_top, x_point, yy);
      }
    }

    if (phd != null && phd.loaded) {
      //
      // draw phred base calls
      //
      PhdData p;
      for (i=0; i < phd.data.size(); i++) {
	p = (PhdData) phd.data.elementAt(i);
	if (p.position >= start_point) {
	  if (p.position > end_point) break;
	  x_point = (int) ((p.position - start_point) * x_mult);
	  String base = String.valueOf(p.base);
	  if (base.equalsIgnoreCase("A")) {
	    g.setColor(Color.green);
	  } else if (base.equalsIgnoreCase("C")) {
	    g.setColor(c_color);
	  } else if (base.equalsIgnoreCase("T")) {
	    g.setColor(Color.red);
	  } else {
	    // G, or unknown
	    g.setColor(Color.black);
	  }
	  g.drawString(base, x_point - base_offset, phred_y_position);
	  g.drawLine(x_point, trace_top, x_point, trace_bottom);
	}
      }
    }

    /* ---------- check for a "marked" point in trace ---------- */
    if (centered_on > 0 &&
	start_point <= centered_on &&
	(start_point + chunk_size) >= centered_on) {
      // marked point is visible
      center_visible = true;
      g.setColor(Color.black);
      int [] tri_x = new int[3];
      int [] tri_y = new int[3];
      x_point = (int) ((centered_on - start_point) * x_mult);
      tri_x[0] = x_point - 5;
      tri_y[0] = base_number_y_position;
      tri_x[1] = x_point + 5;
      tri_y[1] = base_number_y_position;
      tri_x[2] = x_point;
      tri_y[2] = base_number_y_position + 5;
      //      g.drawLine(x_point, base_number_y_position, x_point, trace_bottom);
      g.fillPolygon(tri_x, tri_y, 3);
    } else {
      center_visible = false;
    }

    /* --------------- set scrollbar --------------- */
    if (SCROLL_BY_BASES) {
      // scroll in basecall space.
      int bases_per_screen = chunk_size / trace.average_peak_spacing;
      //      System.err.println("nb:" + trace.num_bases + " bps:" + bases_per_screen);  // debug

      // int max = trace.num_bases + bases_per_screen) - 1;

      int max = trace.num_bases +
        (BASE_SCROLL_CENTERED_MODE ? (bases_per_screen / 2) : bases_per_screen);
      max--;

      int chunk;
      if (BASE_SCROLL_FIXED_BLOCK_SIZE) {
	if (BASE_SCROLL_FIXED_BLOCK_SIZE_AMOUNT == 0) {
	  BASE_SCROLL_FIXED_BLOCK_SIZE_AMOUNT = bases_per_screen;
	  // HORRIBLE

	  //	  System.err.println("setting block amt to " + BASE_SCROLL_FIXED_BLOCK_SIZE_AMOUNT);  // debug

	}
	chunk = BASE_SCROLL_FIXED_BLOCK_SIZE_AMOUNT;
      } else {
	chunk = bases_per_screen;
      }

      scrollbar.setValues(start_base_index,
			  chunk,
			  0,
			  max
			  );
      scrollbar.setUnitIncrement(1);
      scrollbar.setBlockIncrement(chunk);
    } else {
      // set scrolling in sample space
      scrollbar.setValues(start_point, chunk_size,
			  0, trace.num_samples - chunk_size);
      scrollbar.setBlockIncrement(chunk_size - (trace.average_peak_spacing / 2));
      scrollbar.setUnitIncrement(trace.average_peak_spacing);
    }
  }

  public static void set_width_to_height_ratio (double r) {
    //    System.err.println("H2W ratio = " + r);  // debug
    HEIGHT_TO_WIDTH_RATIO = r;
  }

  public static void set_antialiasing (boolean aa) {
    ENABLE_ANTIALIASING = aa;
  }

  public static void set_scroll_by_bases (boolean sbb) {
    SCROLL_BY_BASES = sbb;
  }

  public static void set_fixed_bases_scroll (boolean fsc) {
    // bases scrolling mode: whether to use a static block scrolling amount
    // (useful for locking multiple instances)
    BASE_SCROLL_FIXED_BLOCK_SIZE = fsc;
  }

  public int get_bases_delta_since_start() {
    if (SCROLL_BY_BASES && BASE_SCROLL_CENTERED_MODE) {
      return scrollbar.getValue() - start_center_base;
    } else {
      return 0;
    }
  }

  public void apply_absolute_delta(int abs_delta) {
    //    System.err.println("AED: " + SCROLL_BY_BASES + " " + BASE_SCROLL_CENTERED_MODE);  // debug
    if (SCROLL_BY_BASES && BASE_SCROLL_CENTERED_MODE) {
      // oh, the humanity:
      // if the canvas hasn't been rendered at least once,
      //
      //   1. the scrollbar range values won't yet be set.
      //   2. there might be a pending centering action, which would
      //      override the effect of this method call.
      //
      // Code is gross because we can't complete centering until 
      // geometry is made available at rendering time.
      center_requested = resized = false;
      // disable any pending center request

      center_base_requested = true;
      center_base_request = start_center_base + abs_delta;
      if (center_base_request >= trace.num_bases)
        center_base_request = trace.num_bases - 1;
      if (center_base_request < 0) center_base_request = 0;
      //      System.err.println("AED: " + center_base_request + " " + trace.num_bases);  // debug

      repaint();

    }
  }

}

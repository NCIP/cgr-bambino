package TCGA;
// TO DO:
// possible GRADIENT of colors based on intensity?
// i.e. brightest red/blue at extremes?

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;
import java.math.BigDecimal;

public class SummaryPanelIncreaseDecrease extends SummaryPanelCNSI {
  
  private static Color COLOR_INCREASE = Color.red;
  private static Color COLOR_DECREASE = Color.blue;
  // HACK

  private static Color COLOR_BACKGROUND = Color.white;
  private static Color COLOR_TEXT = Color.black;
  private static Color COLOR_BASELINE = Color.black;

  private static final String LABEL_HELP = "Help: increase/decrease panel";

  private static final int MAX_GISTIC_MARKERS_PER_LINE = 5;
  private static final int MAX_GISTIC_MARKERS_TO_DISPLAY = 50;
  
  private int middle = 0;
  private ColorScheme color_scheme;
  private GISTIC gistic;
  private boolean gistic_mode;

  public SummaryPanelIncreaseDecrease (CopyNumberSummaryInfo2 cnsi, int raw_width, ColorScheme color_scheme, GISTIC gistic) {
    super(cnsi,raw_width);
    this.gistic = gistic;
    gistic_mode = gistic != null;

    setToolTipText("+/-");
    this.color_scheme = color_scheme;

    JPopupMenu jpm = new JPopupMenu();
    jpm.add(HelpLauncher.generate_jmenuitem(LABEL_HELP, HelpLauncher.ANCHOR_SUMMARY_INCREASE_DECREASE));
    new PopupListener(this, jpm);

  }

  protected void paintComponent(Graphics g) {
    Dimension d = getSize();
    middle = d.height / 2;
    int half = middle;

    int start_x = get_unscaled_x_start();
    int end_x = get_unscaled_x_end();
    float x_scale = get_horizontal_scale_level();

    g.setColor(COLOR_BACKGROUND);
    g.fillRect(0,0,d.width,d.height);

    //
    //  paint increase/decrease values:
    //
    ArrayList<Point> points;

    ColorSchemeModel csm = color_scheme.get_colorscheme_model();

    //    Timer t = new Timer("summary +/- paint");
    if (gistic_mode) {
      //
      // display GISTIC q-values
      //
      float scale_by = (float) (half - 1) / gistic.get_max_value().floatValue();

      String priority_subtype = gistic.get_paint_priority();
      //      System.err.println("priority="+priority_subtype);  // debug

      ArrayList<String> subs;
      if (priority_subtype == null) {
	subs = gistic.get_all_subtypes();
      } else {
	subs = new ArrayList<String>();	
	for (String subtype : gistic.get_all_subtypes()) {
	  if (!subtype.equals(priority_subtype)) subs.add(subtype);
	}
	subs.add(priority_subtype);
      }

      int iteration = 0;
      for (String subtype : gistic.get_all_subtypes()) {
	// each data subtype (e.g. broad, focal...)
	for (String data_type : gistic.get_data_types()) {
	  // each data type (amplification, deletion)...
	  points = digest_arraylist(gistic.get_display_data(subtype, data_type), scale_by);

	  //	      System.err.println("subtype="+subtype + " datatype:"+data_type);  // debug

	  Color c;
	  boolean add;
	  if (data_type.equals(GISTIC.TYPE_AMPLIFICATION)) {
	    c = csm.up_color;
	    add = false;
	  } else {
	    c = csm.down_color;
	    add = true;
	  }

	  if (priority_subtype == null) {
	    if (iteration > 0) c = fade_color(c);
	    // fade color for 2nd iteration onwards
	  } else if (!subtype.equals(priority_subtype)) {
	    c = fade_color(c);
	  }

	  g.setColor(c);
	  render_array(g, points, middle, add);
	}
	iteration++;
	if (priority_subtype == null) g.setXORMode(COLOR_BACKGROUND);
	// XOR mode will iteract when data are present for multiple subtypes
	// at the same position
      }
      g.setPaintMode();

      //      g.setXORMode();
      //      decreases = digest_array(cnsi.percent_showing_decrease, scale_by);
    } else {
      //
      // display percentages of samples showing increase/decrease
      //
      float scale_by = (float) half / 100;
      points = digest_array(cnsi.percent_showing_increase, cnsi.max_percent_showing_increase, scale_by, Options.NORMALIZE_SUMMARY_PANEL_PEAKS);
      g.setColor(csm.up_color);
      render_array(g, points, middle, false);

      points = digest_array(cnsi.percent_showing_decrease, cnsi.max_percent_showing_decrease, scale_by, Options.NORMALIZE_SUMMARY_PANEL_PEAKS);
      g.setColor(csm.down_color);
      render_array(g, points, middle, true);
    }
      
    //    t.finish();

    g.setColor(COLOR_BASELINE);
    g.drawLine(0,middle,d.width,middle);

    //
    //  label sections, if available
    //
    FontMetrics fm = getFontMetrics(get_label_font());

    String upper_label = Options.COMMENT_OPTIONS.get("high_label");
    String lower_label = Options.COMMENT_OPTIONS.get("low_label");
    //    System.err.println("ulen="+upper_label.length() + " llen="+lower_label.length());  // debug

    // if (upper_label != null) g2.drawString(upper_label, Options.SUMMARY_PANEL_INDENT_PIXELS, fm.getAscent());
    // if (lower_label != null) g2.drawString(lower_label, Options.SUMMARY_PANEL_INDENT_PIXELS, d.height - fm.getDescent());
    if (upper_label != null)
      draw_label(g, upper_label, COLOR_BACKGROUND, COLOR_TEXT, fm.getAscent());
    if (lower_label != null)
      draw_label(g, lower_label, COLOR_BACKGROUND, COLOR_TEXT, d.height - fm.getDescent());
    
    // FIX ME: draw arrows on scale? (over-expression UP, under-expression DOWN?)
  }

  public String getToolTipText(MouseEvent e) {
    Point mp = e.getPoint();
    String label = "";
    String upper_label = Options.COMMENT_OPTIONS.get("high_label");
    String lower_label = Options.COMMENT_OPTIONS.get("low_label");
    String result = "";

    byte[] array;
    if (mp.y <= middle) {
      label = upper_label == null ? "increase" : upper_label;
      array = cnsi.percent_showing_increase;
    } else {
      label = lower_label == null ? "decrease" : lower_label;
      array = cnsi.percent_showing_decrease;
    }
    label = label.toLowerCase();

    if (gistic_mode) {
      Point up = get_unscaled_point(mp);
      String amp_or_del = mp.y <= middle ? "amplification" : "deletion";
      SimpleHTMLGrid st = new SimpleHTMLGrid();
      st.set_th_headers(false);
      st.set_underline_headers(true);
      st.add_row("q-value");
      st.add_row("location");
      boolean has_data = false;
      if (up.x >= 0) {
	String bin_label;
	GISTICPeak focal_peak = gistic.get_peak("focal", mp.y <= middle ? "+" : "-", up.x);
	if (focal_peak == null) {
	  bin_label = cnsi.get_bin_label(up.x);
	  // heatmap's label for this bin (gene set)
	} else {
	  // if we have a focal peak, use its marker list for the label,
	  // as this may be much more specific than the genome-formatted heatmap.
	  // Broad peaks comprise entire chromosome arms so these aren't appropriate
	  // for this purpose
	  bin_label = Funk.Str.join(",", focal_peak.markers);
	}

	ArrayList<String> subtypes = new ArrayList<String>();
	subtypes.add("focal");
	subtypes.add("broad");
	// desired column order if both are present
	
	for (String subtype : subtypes) {
	  GISTICPeak peak = gistic.get_peak(subtype, mp.y <= middle ? "+" : "-", up.x);

	  //	  BigDecimal q = gistic.get_raw_value(subtype, mp.y <= middle ? "+" : "-", up.x);
	  if (peak != null) {
	    has_data = true;
	    String column_label = subtype + " peak";

	    st.set_cell_value(column_label, "location", peak.peak_pos.toString());

	    if (subtype.equals("focal")) {
	      st.add_row("markers");
	      StringBuffer sb = new StringBuffer();
	      int count = 0;
	      int size = peak.markers.size();

	      for (String marker : peak.markers) {
		if (count >= MAX_GISTIC_MARKERS_TO_DISPLAY) {
		  sb.append("...");
		  break;
		}
		if (count > 0) {
		  if (count < size) sb.append(", ");
		  if (count % MAX_GISTIC_MARKERS_PER_LINE == 0) sb.append("<br>\n");
		}
		sb.append(marker);
		count++;
	      }
	      st.set_cell_value(column_label, "markers", sb.toString());
	    }

	    st.add_column(column_label);
	    //	    if (result.length() == 0) result = "GISTIC " + label + " q-value for " + cnsi.get_bin_label(up.x) + ": ";
	    //	    if (result.length() == 0) result = "GISTIC " + label + " q-value for " + bin_label + ": ";
	    //	    if (result.length() == 0) result = "GISTIC " + label + " for " + bin_label + ": ";
	    st.set_cell_value(column_label, "q-value", peak.q.toString());

	    if (peak.z_score != null) {
	      st.add_row("z-score");
	      // add this row only if we actually have a z-score (i.e. broad peak)
	      st.set_cell_value(column_label, "z-score", peak.z_score.toString());
	    }
	  }
	}
      }

      if (has_data) {
	result = "<html><center><b>GISTIC " + amp_or_del + "</b></center> " + st.generate_html() + "</html>";
      } else {
	result = "GISTIC analysis data: move mouse over a colored region for details";
      }
    } else{
      result = generate_tooltip(mp, label, array);
    }

    return result.length() > 0 ? result : null;
  }

  public void set_gistic_mode (boolean v) {
    gistic_mode = v;
    repaint();
  }

  public void mouseClicked(MouseEvent e) {
    if (!gistic_mode) super.mouseClicked(e);
  }

  private Color fade_color (Color c) {
    float fade_fraction = 0.50f;

    int[] rgb = new int[3];
    rgb[0] = c.getRed();
    rgb[1] = c.getGreen();
    rgb[2] = c.getBlue();

    Color result;
    if (true) {
      for (int i = 0; i < 3; i++) {
	int left = 255 - rgb[i];
	int amount = (int) (left * fade_fraction);
	rgb[i] += amount;
      }
      result = new Color(rgb[0], rgb[1], rgb[2]);
    } else {
      result = new Color((int) (c.getRed() * fade_fraction),
			 (int) (c.getGreen() * fade_fraction),
			 (int) (c.getBlue() * fade_fraction)
			 );
    }
    return result;
  }

}

package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;
import java.text.NumberFormat;
import java.net.URLEncoder;

public class ChromDecoratorPanel extends ImageScalePanel2 implements MouseListener,ActionListener,Observer {

  private HeatmapConfiguration config;

  private GenomicSet gs;
  private GenomicMeasurement gm;
  private AnnotationFlatfile2 af;
  // HACKY: use config!

  private PearsonClusterControl pcc;
  private EuclideanClusterControl ecc;

  private String tooltip_field = null;

  private RubberBandSelectionScaled rbs;
  private PopupListener pul;
  private Point last_mouse_press_point = null;
  private SampleSubsets sample_subsets;

  private static final String LABEL_CGWB_LAUNCH = "Launch CGWB";
  private static final String LABEL_CGWB_LAUNCH_REGION = "Launch CGWB (region)";
  private static final String LABEL_VIEW_RAW_DATA = "View raw data";
  private static final String LABEL_ZOOM_TO_SELECTION = "Zoom to selection";
  private static final String LABEL_NEW_WINDOW_FOR_SELECTION = "New window for selection";

  private static final String LABEL_SORT_PREFIX = "Sort samples by data for region ";
  private static final String LABEL_SORT_SUFFIX = "...";
  
  private static final String LABEL_PATHWAYS_FOR_MARKER_PREFIX = "Pathways for region ";

  private static final String LABEL_PATHWAY_PREFIX = "Launch PID for pathway ";
  private static final String LABEL_EG_PREFIX = "Launch Entrez Gene for ";
  
  private static final String LABEL_HELP = "Help: heatmap panel";

  //  private static Color COLOR = Color.white;
  //  private static Color COLOR = new Color(143,188,143);
  // DarkSeaGreen

  //  private static Color COLOR = new Color(85,107,47);
  // DarkOliveGreen: nice but a bit too dark

  //  private static Color COLOR = new Color(0,100,0);
  // darkgreen: too dark

  private static Color COLOR = new Color(34,139,34);
  // forestgreen

  //  private static Color COLOR = Color.white;

  private boolean display_sample_ids = false;

  private Rectangle r_zoom_indicator;
  private boolean show_zoom_level = true;

  private JMenuItem jmi_sort,jmi_eg;
  private JMenu jm_sort_subset, jm_pathways;
  private int popup_bin_index;
  private HashSet<JMenuItem> pathway_mi = new HashSet<JMenuItem>();
  private Observable2 obs = new Observable2();

  public ChromDecoratorPanel (BufferedImage offscreen, JScrollBar jsb_h, JScrollBar jsb_v, HeatmapConfiguration cfg) {
    super(offscreen, jsb_h, jsb_v);
    this.config = cfg;
    gs = config.gs;
    gm = config.gm;
    af = config.af;
    setToolTipText("sample ID should be here");
    addMouseListener(this);
    rbs = new RubberBandSelectionScaled(this);
    sample_subsets = gm.get_sample_subsets();

    JPopupMenu jpm = new JPopupMenu();
    JMenuItem jmi;
    jpm.add(jmi = new JMenuItem(LABEL_CGWB_LAUNCH));
    jmi.addActionListener(this);
    //    jpm.add(jmi = new JMenuItem(LABEL_CGWB_LAUNCH_REGION));
    //    jmi.addActionListener(this);

    if (config.pathway != null) {
      jpm.add(jmi = new JMenuItem(LABEL_PATHWAY_PREFIX + config.pathway.name));
      jmi.addActionListener(this);
    }

    jpm.add(jmi_eg = new JMenuItem(LABEL_EG_PREFIX));
    jmi_eg.addActionListener(this);

    jpm.add(jmi = new JMenuItem(LABEL_ZOOM_TO_SELECTION));
    jmi.addActionListener(this);

    jpm.add(jmi = new JMenuItem(LABEL_NEW_WINDOW_FOR_SELECTION));
    jmi.addActionListener(this);

    jpm.add(new JSeparator());

    JRadioButtonMenuItem jrb;
    ButtonGroup bg = new ButtonGroup();

    if (sample_subsets.isEmpty()) {
      // no subsets: simple sort menu item
      jpm.add(jmi_sort = new JMenuItem("sort by whatever"));
      jmi_sort.addActionListener(this);
    } else {
      jpm.add(jm_sort_subset = new JMenu("sort by marker + subset"));
      jrb = new JRadioButtonMenuItem(SampleSortTools.CLUSTER_SUBSET_ALL, true);
      bg.add(jrb);
      jrb.addActionListener(this);
      jm_sort_subset.add(jrb);
    }

    jpm.add(jm_pathways = new JMenu("pathways for marker"));
    
    // TO DO:
    // add zoom, etc. options

    //    jpm.add(jmi = new JMenuItem(LABEL_VIEW_RAW_DATA));
    //    jmi.addActionListener(this);

    JMenu sub;

    if (!sample_subsets.isEmpty()) {
      for (String subset : sample_subsets.get_subsets_arraylist()) {
	// "sort by subset" submenu
	jrb = new JRadioButtonMenuItem(subset, false);
	jm_sort_subset.add(jrb);
	bg.add(jrb);
	jrb.addActionListener(this);
      }
    }

    if (Options.INCLUDE_DEVELOPMENT_CODE) {
      jpm.add(jmi = new JMenuItem("Combine datasets...[development]"));
      jmi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				  new SubsetCombinerControl(config);
				}
			      }
	);

      jpm.add(jmi = new JMenuItem("Report frequency...[development]"));
      jmi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    System.err.println("FIX ME!!!");  // debug
				    
				    //				    new FrequencyReporter(config, this);
				}
			      }
	);

    }

    jpm.add(new JSeparator());
    jpm.add(HelpLauncher.generate_jmenuitem(LABEL_HELP, HelpLauncher.ANCHOR_HEATMAP));

    pul = new PopupListener(this, jpm);
    pul.addObserver(this);
    
  }
  
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    if (gs.is_loaded()) {

      y_scale_check();
      
      ColorManager cm = gm.get_color_manager();

      int start_x = get_unscaled_x_start();
      int end_x = get_unscaled_x_end();
      // FIX ME:
      // write some kind of rectangle translation code in ScalePanel2
      // which takes Graphics as an arg.

      Dimension d = getSize();
      float x_scale = get_horizontal_scale_level();
      int x;

      Graphics2D g2 = (Graphics2D) g;
      Composite c_orig = g2.getComposite();

      g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
			  java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

      ArrayList<GenomicSample> samples = gm.get_visible_rows();
      float y_step = get_vertical_scale_level();

      if (display_sample_ids) {
	//
	//  test: display sample IDs with translucency?
	//

	if (y_step >= 10) {
	  //	  g2.setColor(Color.white);
	  g2.setColor(Color.gray);

	  //	AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
	  AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.80f);

	  g2.setComposite(ac);

	  //	System.err.println("YSF:"+get_vertical_scale_level());  // debug

	  int font_size = (int) (y_step - 2);
	  if (font_size > 15) font_size = 15;
	  Font f_old = g2.getFont();
	  Font f = new Font("Times", Font.BOLD, font_size);
	  g2.setFont(f);
	  FontMetrics fm = getFontMetrics(f);
	
	  //	  int y = (y_step / 2) + fm.getDescent();
	  //	  float y = y_step - fm.getDescent();
	  //	  float y = (y_step / 2) + (fm.getHeight() / 2);
	  //	  float y = (y_step / 2);
	  float y = (y_step / 2) + fm.getDescent() + 1;
	  //	  float y = (y_step / 2) + fm.getAscent() + 1;

	  x = fm.stringWidth("X") / 2;

	  //	  g2.setXORMode(Color.black);
	  for (int yi=get_unscaled_y_start();
	       y < d.height && yi < samples.size();
	       yi++) {
	    GenomicSample gs = samples.get(yi);
	    g2.drawString(gs.sample_id, x, (int) y);
	    //	  System.err.println(gs.sample_id);  // debug
	    y += y_step;
	  }

	  g2.setComposite(c_orig);
	  g2.setFont(f_old);
	  //	  g2.setPaintMode();
	}
      }

      Stroke orig_stroke = g2.getStroke();
      //
      // draw dividers:
      //
      DividerManager dm = gm.get_divider_manager();
      paint_dividers(g2, dm.get_patient_dividers());
      paint_dividers(g2, dm.get_cluster_dividers());
      g2.setStroke(orig_stroke);

      //
      //  draw chromosome border lines:
      //
      Stroke dashed = new BasicStroke(1,
				      BasicStroke.CAP_BUTT,
				      BasicStroke.JOIN_ROUND,
				      1.0f,
				      new float[] {2.0f, 2.0f},
				      0.0f
				      );
      g.setColor(COLOR);
      g2.setStroke(dashed);

      if (Options.are_bins_paintable(gs, this)) {
	for (GenomicBin gb : gs.get_bins()) {
	  //
	  //  draw border lines in box
	  // 
	  if (gb.end >= start_x && gb.end <= end_x) {
	    // mark end of chromosome if visible
	    x = (int) ((gb.end - start_x) * x_scale);
	    g2.drawLine(x,0,x,d.height);
	    // mark chr boundaries
	  }
	}
      }

      g2.setStroke(orig_stroke);

      //
      // draw current selection:
      //
      //      System.err.println("selection?:"+rbs.has_selection());  // debug

      if (rbs.has_selection()) {
	Rectangle selection = rbs.get_selection();
	
	float y_scale = get_vertical_scale_level();
	x = (int) ((selection.x - start_x) * x_scale);
	int y = (int) ((selection.y - get_unscaled_y_start()) * y_scale);

	//	Stroke dashed_heavy = new BasicStroke(1,
	Stroke dashed_heavy = new BasicStroke(2.0f,
					      BasicStroke.CAP_BUTT,
					      BasicStroke.JOIN_ROUND,
					      1.0f,
					      new float[] {4.0f, 1.0f},
					      0.0f
					      );

	//	g2.setStroke(dashed);
	g2.setStroke(dashed_heavy);

	g2.setColor(cm.get_selection_color());

	//
	// Clip selection region to component size.  If we don't then drawing the
	// dashed selection line can be VERY slow when zoomed in closely.
	// 
	if (x < 0) x = 0;
	if (y < 0) y = 0;
	int xw = (int) (selection.width * x_scale);
	int yw = (int) (selection.height * y_scale);
	if (xw > d.width) xw = d.width;
	if (yw > d.height) yw = d.height;
	
	g2.drawRect(x, y, xw, yw);
	g2.setStroke(orig_stroke);
      }

      //
      //  draw zoom level indicator:
      //
      if (show_zoom_level) {
	Font f = getFont();
	FontMetrics fm = getFontMetrics(f);

	int inset_border = 8;
	int text_border = 1;
	int string_border = 3;

	String msg = get_zoom_level_string() + "x";

	int w = fm.stringWidth(msg) + (string_border * 2);
	int x1 = d.width - w - inset_border;
	int y = inset_border;
	int h = fm.getHeight() + (text_border * 2);

	//	AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.80f);
	float tval = 0.70f;
	if (cm.is_white_mode()) tval = 1.0f - tval;
	AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, tval);

	g2.setComposite(ac);

	g2.setColor(cm.get_zoom_background_color());
	r_zoom_indicator = new Rectangle(x1, inset_border, w, h);
	if (true) {
	  // oval
	  g2.fillOval(r_zoom_indicator.x, r_zoom_indicator.y, r_zoom_indicator.width, r_zoom_indicator.height);
	} else {
	  // rectangle
	  g2.fillRect(r_zoom_indicator.x, r_zoom_indicator.y, r_zoom_indicator.width, r_zoom_indicator.height);
	}
	g2.setComposite(c_orig);


	g2.setColor(cm.get_zoom_foreground_color());
	g2.drawString(msg, x1 + string_border, y + fm.getAscent() + text_border);
      }
    }
  }

  private int get_bin_index(Point mp) {
    return get_unscaled_point(mp).x;
  }

  public String getToolTipText(MouseEvent e) {
    // TO DO:
    // add (selected) clinical data field(s)?
    // if clinical data are available, why not show a field or two here
    // as well??

    Point mp = e.getPoint();
    String tooltip = "";

    if (show_zoom_level &&
	r_zoom_indicator != null &&
	r_zoom_indicator.intersects(new Rectangle(mp.x, mp.y, 1, 1))) {
      String level = get_zoom_level_string();
      String direction = "in";
      if (level.charAt(0) == '-') {
	direction = "out";
	level = level.substring(1);
      }

      tooltip = "current horizontal magnification level ";
      if (level.equals("1.0")) {
	tooltip = tooltip.concat("(unchanged)");
      } else {
	tooltip = tooltip.concat("(zoomed " + direction + " " + level + " times)");
      }
    } else {
      int current_bin_index = get_bin_index(mp);

      Point p = get_unscaled_point(mp);

      int index = p.y;
      // good enough for samples

      ArrayList<GenomicSample> samples = gm.get_visible_rows();

      String[] headers = gm.get_headers();
      if (index < samples.size() && current_bin_index < headers.length) {
	GenomicSample sample = samples.get(index);

	if (config.parent_gm != null) {
	  //
	  //  this is a dynamically-generated combined dataset; refer to values
	  //  in parent dataset subsets
	  //
	  tooltip = "sample "+ sample.sample_id + " shows ";
	  int count=0;
	  for (String subset : config.parent_subsets) {
	    GenomicSample gs = config.parent_gm.get_sample_for_patient_subset(sample.patient_id, subset);
	    int amount = gs.copynum_data[current_bin_index];
	    if (count++ > 0) tooltip = tooltip.concat(", ");
	    tooltip = tooltip.concat(subset + " " + (amount > 0 ? "+" + amount : amount));
	  }

	  tooltip = tooltip.concat(" for region " + headers[current_bin_index]);
	} else {
	  //
	  //  normal
	  //
	  int amount = sample.copynum_data[current_bin_index];
	  String amount_label;
	  if (amount == GenomicSample.NULL_VALUE) {
	    amount_label = "has no data";
	  } else {
	    if (amount == 0) {
	      amount_label = "shows no change";
	    } else if (amount < 0) {
	      amount_label = "shows " + amount;
	    } else {
	      amount_label = "shows +" + amount;
	    }

	    boolean use_scale_info = true;

	    if (config != null &&
		config.gm_supplemental.size() > 0 &&
		Options.COMBINE_DATASETS_ADD_ROWS_MODE == false) {
	      // this dataset was dynamically built from multiple datasets,
	      // and in a mode that added new columns for different data subtypes.
	      // In this case
	      String data_type = config.gm.get_options().get("data_type").toLowerCase();
	      if (data_type.equals("copy number")) {
		data_type = Options.ABBREVIATION_COPY_NUMBER;
	      } else if (data_type.equals("gene expression")) {
		data_type = Options.ABBREVIATION_GENE_EXPRESSION;
	      }
	      if (headers[current_bin_index].indexOf(data_type) == -1) {
		use_scale_info = false;
	      }
	    } 
	    
	    if (use_scale_info) {
	      //	  if (Options.INCLUDE_DEVELOPMENT_CODE) {
	      DataScales ds = gm.get_options().get_data_scales();
	      if (ds != null) {
		amount_label += " (" + Options.DATA_TYPE + " is " + ds.translate_amount(amount) + ")";
	      }
	    }
	  }

	  tooltip = "sample "+ sample.sample_id + " " + 
	    amount_label + " for region " + headers[current_bin_index];

	  if (af != null && tooltip_field != null) {
	    ArrayList hits = af.find_annotations(sample);
	    Hashtable r = null;
	    if (hits != null) r = (Hashtable) hits.get(0);
	    if (r == null) {
	      tooltip = tooltip + " (no annotations)";
	    } else {
	      String value = (String) r.get(tooltip_field);
	      if (value == null || value.length() == 0) value = "[not specified]";
	      tooltip = tooltip + " " + tooltip_field + ":" + value;
	    }
	  }
	}

      }
    }
    return tooltip;
  }

  public void center_on_x (int x) {
    super.center_on_x(x);
    rbs.set_selection(new Rectangle(x,0,1,gm.get_row_count()));
  }

  public RubberBandSelection get_selection() {
    return rbs;
  }

  public void center_on_region (Rectangle r) {
    super.center_on_region(r);
    if (false) {
      r.y = 0;
      r.height = gm.get_row_count();
    }
    rbs.set_selection(r);
  }
  

  // begin MouseListener stubs
  public void mousePressed(MouseEvent e) {
    last_mouse_press_point = e.getPoint();
    if (e.getClickCount() > 1) {
      // double-click: zoom to this position
      Rectangle r = gm.generate_selection(get_bin_index(last_mouse_press_point));
      rbs.set_selection(r);
      zoom_to_selection();
    } 
  };
  public void mouseClicked(MouseEvent e) {};
  
  public void mouseReleased(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};
  // end MouseListener stubs

  public void set_tooltip_field (String f) {
    tooltip_field = f;
  }

  public String get_tooltip_field () {
    return tooltip_field;
  }

  // begin ActionListener stub
  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();
    if (src instanceof AbstractButton) {
      String label = ((AbstractButton) src).getText();
      if (label.equals(LABEL_CGWB_LAUNCH)) launch_CGWB(false);
      if (label.equals(LABEL_CGWB_LAUNCH_REGION)) launch_CGWB(true);
      if (label.equals(LABEL_VIEW_RAW_DATA)) view_raw_data();
      if (label.equals(LABEL_ZOOM_TO_SELECTION)) zoom_to_selection();
      if (label.equals(LABEL_NEW_WINDOW_FOR_SELECTION)) new_window_for_selection();
      if (label.indexOf(LABEL_PATHWAY_PREFIX) == 0) launch_pathway();
      if (label.indexOf(LABEL_EG_PREFIX) == 0) {
	String[] headers = gm.get_headers();
	if (popup_bin_index < headers.length) {
	  String marker = headers[popup_bin_index];
	  URLLauncher.launch_url(WebTools.entrez_gene_link(marker), "eg");
	}
      }

      if (src.equals(jmi_sort)) sort_by_bin(popup_bin_index, null);

      if (src instanceof JRadioButtonMenuItem) {
	JRadioButtonMenuItem jrb = (JRadioButtonMenuItem) src;
	if (jrb.isSelected()) {
	  sort_by_bin(popup_bin_index, jrb.getText());
	}
      }

      if (pathway_mi.contains(src)) {
	PathwayGenes pwg = new PathwayGenes();
	Pathway p = pwg.get_pathway(label);
	if (p != null) {
	  obs.setChanged();
	  obs.notifyObservers(p);
	}
      }

    }
  }
  // end ActionListener stub

  public void sort_by_bin(int bin, String subset) {
    float average = gm.sort_by_bin(bin, subset);
    JScrollBar jsb_v = get_vertical_scrollbar();
    if (jsb_v != null) {
      // attempt to set scrollbar to appropriate end --
      // start if average value is < 0, end if > 0
      jsb_v.setValue(average < 0 ? jsb_v.getMinimum() : jsb_v.getMaximum());
    }
  }

  public void launch_pathway() {
    if (config.pathway != null) {
      String url = "http://pid.nci.nih.gov/search/pathway_landing.shtml?pathway_id=" +
	config.pathway.pathway_id + ";what=graphic;gif=on";
      System.err.println("url="+url);  // debug
      if (url != null) URLLauncher.launch_url(url, "pathway");
    }
  }

  public void launch_CGWB(boolean force_region) {
    // 
    GenomicLocation gl = null;
    String bin_label = null;
    if (rbs.has_selection()) {
      int start = rbs.get_start_x();
      int end = rbs.get_end_x();
      if (start == end) {
	// selection is a single bin
	bin_label = rbs.get_selected_label();
	System.err.println("BL is:"+bin_label);  // debug
	if (bin_label == null) {
	  // if not already recorded in selection
	  // (i.e. disambiguated bin label from NavigationControl)
	  bin_label = get_bin_label(start);
	  BinLabel bl = new BinLabel(bin_label);
	  if (bl.is_genomic()) gl = bl.get_genomic_location();
	}
      } else {
	GenomicLocation gl1 = find_genomic_location(rbs.get_start_x());
	GenomicLocation gl2 = find_genomic_location(rbs.get_end_x());
	System.err.println("start="+rbs.get_start_x() + " end:"+rbs.get_end_x());  // debug

	if (gl1.chromosome.equals(gl2.chromosome)) {
	  gl = new GenomicLocation();
	  gl.chromosome = gl1.chromosome;
	  gl.start = gl1.start;
	  gl.end = gl2.end;
	} else {
	  System.err.println("ERROR: selection spans chromosomes!");  // debug
	  gl = gl1;
	}
      }
    } else {
      bin_label = get_bin_label(last_mouse_press_point);
      BinLabel bl = new BinLabel(bin_label);
      if (bl.is_genomic()) gl = bl.get_genomic_location();
      // for a single point
    }

    String spec = null;
    if (gl != null) {
      // genomic position
      //      spec = "/cgi-bin/hgTracks?position=" + gl.get_ucsc_location();
      if (false) {
	System.err.println("DEBUG CGWB launch");  // debug
	// spec = "/cgi-bin/hgGateway";
	// this starts quickly
	spec = "/cgi-bin/heatmap?param1=bogus;param2=bogus";
	// this starts quickly
      } else if (false) {
	try {
	  spec = "/cgi-bin/hgTracks?position=" + URLEncoder.encode(gl.get_ucsc_location(), "UTF-8");
	} catch (java.io.UnsupportedEncodingException unex) {
	  System.err.println("encoding error!:" + unex);  // debug
	  spec = "/cgi-bin/hgTracks?position=" + gl.get_ucsc_location();
	}
      } else {
	spec = "/cgi-bin/hgTracks?position=" + gl.get_ucsc_location();
      }
    } else {
      // ass-u-me gene-based lookup
      String gene;
      if (bin_label.indexOf(",") > 0) {
	String[] values = bin_label.split(",");
	gene =(String) JOptionPane.showInputDialog(
							 this,
							 "Choose a marker at this location:",
							 "Select marker",
							 JOptionPane.QUESTION_MESSAGE,
							 null,
							 values,
							 null);
      } else {
	gene = bin_label;
      }
      if (gene != null) {
	spec = "/cgi-bin/fwd?gene=" + gene + "&hint=tcga";
	CommentOptions options = gm.get_options();
	String pid = options.get("cgwb_project_id");
	if (pid != null) {
	  // CGWB project ID
	  spec = spec + "&project_id=" + pid;
	}
	System.err.println("CGWB link: " + spec);  // debug
      }      
    }

    if (spec != null) URLLauncher.launch_modified_url(spec, "cgwb");
  }

  public void zoom_to_selection() {
    GenomicLocation gl = null;
    String bin_label = null;
    if (rbs.has_selection()) {
      if (config.enable_horizontal_zoom.booleanValue()) {
	//
	// don't zoom in if horizontal zoom disabled (zoomed in to pathway, etc.)
	//
	int start = rbs.get_start_x();
	int end = rbs.get_end_x();
	int length = (end - start) + 1;
	int min_size = Options.MINIMUM_BINS_TO_DISPLAY_IN_SELECTION_ZOOM;
	// ensure at least this many bins are displayed onscreen
	// (prevent a small selection from taking up the entire width of the display)
	int needed_buffer = 0;
	if (length < min_size) needed_buffer = (min_size - length) / 2;
	zoom_to_selection(start - needed_buffer, length + (needed_buffer*2));
      }

      if (config.enable_vertical_zoom.booleanValue()) {
	int start = rbs.get_start_y();
	int end = rbs.get_end_y();
	int length = (end - start) + 1;
	int min_size = Options.MINIMUM_BINS_TO_DISPLAY_IN_SELECTION_ZOOM;
	// ensure at least this many bins are displayed onscreen
	// (prevent a small selection from taking up the entire width of the display)
	int needed_buffer = 0;
	if (length < min_size) needed_buffer = (min_size - length) / 2;
	zoom_to_vertical_selection(start - needed_buffer, length + (needed_buffer*2));
      }

    } else if (false) {
      // 10/2008: disable; menu accelerator ("s") interferes with
      // NavigationBar keyboard navigation of JComboBox.
      // ...any way to consume these events?
      JOptionPane.showMessageDialog(this,
				    "First select a region with the left mouse button.");
    }

  }

  public void new_window_for_selection() {
    GenomicLocation gl = null;
    String bin_label = null;
    if (rbs.has_selection()) {
      Rectangle sel = rbs.get_selection();
      obs.setChanged();
      obs.notifyObservers(sel);
    } else {
      JOptionPane.showMessageDialog(this,
				    "First select a region with the left mouse button.");
    }

  }


  private String get_bin_label (Point mp) {
    return get_bin_label(get_bin_index(mp));
  }

  private String get_bin_label (int index) {
    return gm.get_headers()[index];
  }

  private GenomicLocation find_genomic_location (int bin_index) {
    GenomicLocation result;
    String bin_label = get_bin_label(bin_index);
    BinLabel bl = new BinLabel(bin_label);
    System.err.println(bin_label + " genomic:"+bl.is_genomic());  // debug
    // if label contains chromosome formatting, use it, otherwise
    // use approximated coordinates
    // FIX ME: write BinLabel.java...

    if (bl.is_genomic()) {
      result = bl.get_genomic_location();
    } else {
      result = gs.get_genomic_location_for_bin(bin_index);
    }

    return result;
  }

  private void view_raw_data() {
    if (rbs.has_selection()) {
      Rectangle sel = rbs.get_selection();
      System.err.println("hey now:"+sel);

    } else {
      // FIX ME:
      // show ALL data??
      JOptionPane.showMessageDialog(this,
				    "First select a region with the left mouse button.");
    }
  }

  public void set_display_sample_ids (boolean status) {
    display_sample_ids = status;
  }

  private double get_zoom_level() {
    double level = (double) get_horizontal_scale_level();
    if (level < 1) level = -1 / level;
    return level;
  }

  private String get_zoom_level_string() {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(1);
    nf.setMinimumFractionDigits(1);
    return nf.format(get_zoom_level());
  }

  public void set_show_zoom (boolean show_zoom) {
    show_zoom_level = show_zoom;
    repaint();
  }

  public void update (Observable o, Object arg) {
    if (o instanceof PopupListener) {
      // pre-display hook for popup menu: update menu label to display current bin label
      MouseEvent me = (MouseEvent) arg;
      popup_bin_index = get_bin_index(me.getPoint());
      String[] headers = gm.get_headers();
      if (popup_bin_index < headers.length) {
	String marker = headers[popup_bin_index];
	if (jmi_sort != null) jmi_sort.setText(LABEL_SORT_PREFIX + marker);
	if (jm_sort_subset != null) jm_sort_subset.setText(LABEL_SORT_PREFIX + marker + LABEL_SORT_SUFFIX);
	pathway_popup_setup(marker);
      } else {
	System.err.println("header index error! " + popup_bin_index + " " + headers.length);  // debug
      }
    } else if (o instanceof ClusterTool) {
      ClusterTool ct = (ClusterTool) o;
      if (ct.wants_zoom()) {
	Rectangle r = gm.generate_selection(ct);
	center_on_region(r);
	zoom_to_selection();
      }

      repaint();
    } else {
      System.err.println("UNHANDLED UPDATE:"+o);  // debug
    }
  }

  private void pathway_popup_setup(String marker) {
    jm_pathways.setText(LABEL_PATHWAYS_FOR_MARKER_PREFIX + marker);
    jmi_eg.setText(LABEL_EG_PREFIX + marker);

    pathway_mi.clear();
    jm_pathways.removeAll();
    PathwayGenes pwg = new PathwayGenes();
    ArrayList<Pathway> pathways = pwg.find_pathways_for_gene(marker);
    
    if (pathways == null) {
      jm_pathways.add("(none annotated)");
    } else {
      ArrayList<String> pw_names = new ArrayList<String>();
      for (Pathway p : pathways) {
	pw_names.add(p.name);
      }
      Collections.sort(pw_names);

      for (String name : pw_names) {
	JMenuItem jmi = new JMenuItem(name);
	jm_pathways.add(jmi);
	jmi.addActionListener(this);
	pathway_mi.add(jmi);
      }
    }
  }

  public void addObserver (Observer o) {
    obs.addObserver(o);
  }

  private void paint_dividers (Graphics2D g2, DividerSet dividers) {
    if (dividers != null && dividers.size() > 0) {
      int start_x = get_unscaled_x_start();
      float y_step = get_vertical_scale_level();
      ArrayList<GenomicSample> samples = gm.get_visible_rows();

      Dimension d = getSize();
      float weight;

      g2.setColor(dividers.color);

      float range = dividers.max_weight - dividers.min_weight;
	
      if (y_step < dividers.full_weight_at_zoom) {
	weight = dividers.min_weight + (range * (y_step / dividers.full_weight_at_zoom));
      } else {
	weight = dividers.max_weight;
      }
      //      System.err.println("y_step:"+y_step + " weight:"+weight);  // debug

      if (false) {
	// dashed separator; meh
	Stroke dashed = new BasicStroke(weight,
					BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_ROUND,
					1.0f,
					new float[] {4.0f, 1.0f},
					0.0f
					);
	g2.setStroke(dashed);
      } else {
	g2.setStroke(new BasicStroke(weight));
      }

      float y = 0;
      int yi=get_unscaled_y_start();
      for (y = 0; y < d.height && yi < samples.size(); yi++, y += y_step) {
	if (dividers.contains(yi) && (int) y > 0) {
	  g2.drawLine(0, (int) y, d.width, (int) y);
	}
      }
    }
  }

  private void y_scale_check () {
    if (isVisible() && gm.is_loaded()) {
      int sample_count = config.gm.get_row_count();
      int available = getSize().height;
      float y_scale = ((float) available) / sample_count;
      // 5/2012: fix for dead space at bottom of panel if user resizes
      // the window larger.  compare with guess_y_scaling().
      //
      // If integer-only scaling is desired, this will need additional
      // work (i.e. setting a value that's one higher than will fit in
      // the available space, which could be jarring as a scrollbar 
      // would suddenly be required).
      if (y_scale < 1) y_scale = 1;
      // ensure at least 1 vertical pixel drawn per sample/row
      if (y_scale > Options.MAX_VERTICAL_SCALE_FACTOR) y_scale = Options.MAX_VERTICAL_SCALE_FACTOR;
      if (get_vertical_scale_level() != y_scale) {
	System.err.println("reset Y scale to " + y_scale);  // debug
	set_min_vertical_scale_level(y_scale);
	set_vertical_scale_level(y_scale);
	repaint();
      } else {
	//	System.err.println("Y scale reset not needed");  // debug
      }
    }
  }


}

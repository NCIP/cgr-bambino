package TCGA;
// "heat map" viewer
// Michael Edmonson <edmonson@nih.gov> <mnedmonson@gmail.com> 3/2008-
// version using ScalePanel2 

import javax.swing.*;
//import java.awt.*;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.awt.Toolkit;
//import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class Heatmap6 extends JFrame implements Observer,MouseListener,AdjustmentListener,ActionListener,MouseWheelListener,Runnable,ComponentListener {
  
  private JPanel main_panel;
  private CopyNumberVariationImage cnvi = null;
  private CopyNumberSummaryInfo2 cnsi;
  private ChromDecoratorPanel isp;
  //  private ImageScalePanel isp,isp_cnc_incr,isp_cnc_any;
  private SummaryPanelIncreaseDecrease sp_cnc_incr;
  private SummaryPanelAnyVariation sp_cnc_any;
  private NavigationControl nc;

  private ChromScalePanel2 csp;
  private JMenuItem mi_contrast,mi_view_annotations,mi_nav,mi_help_zoom;
  //  private JCheckBoxMenuItem mi_enable_vzoom,mi_enable_hzoom,mi_show_zoom;
  private JCheckBoxMenuItem mi_show_zoom;
  private ContrastControl cc = null;
  private AnnotationViewer av = null;
  private AnnotationGridPanel2 agp = null;
  private NavigationBar nb = null;

  private JPanel jp_heatmap = null;

  private JScrollBar jsb_h,jsb_v;
  private ArrayList<TrackPanel> tracks = null;

  private HeatmapConfiguration config;

  private static String LABEL_ZOOM_IN = "Zoom in";
  private static String LABEL_ZOOM_OUT = "Zoom out";
  private static String LABEL_ZOOM_OUT_MAX = "Zoom out max";
  private static String LABEL_ZOOM_RESET = "Zoom 1:1";
  private static String LABEL_ZOOM_TO_SELECTION = "Zoom to selection";
  private static String LABEL_SET_Y_SCALING = "Set vertical magnification...";
  private static String LABEL_SHOW_ZOOM_INDICATOR = "Display magnification level";
  private static String LABEL_SET_ANNOT_TOOLTIP = "Set annotation tooltip...";
  private static String LABEL_HELP = "Documentation";
  private static String LABEL_LINK_HEATMAPS = "Heatmap dataset index";
  private static String LABEL_LINK_CGWB = "Cancer Genome Workbench";

  private JScrollPane jsp_heatmap,jsp_cnc_incr,jsp_cnc_any,jsp_chrom,jsp_annot;
  private boolean laid_out = false;
  private boolean initial_gui_built = false;

  private JLabel loading_label;
  private int start_y_scaling;

  // FIX ME:
  // MOVE THESE TO HeatmapConfiguration! (startup):
  private boolean show_zoom_level = true;
  private boolean show_total_histogram = true;

  private LoadingTracker loading_tracker = null;

  public Heatmap6 (HeatmapConfiguration config) throws Exception {
    this.config = config;
    setup();
  }

  public void update (Observable o, Object arg) {
    // image built
    //    System.err.println("Heatmap6 update:"+o);  // debug

    if (o instanceof CopyNumberVariationImage) {
      layout_all();
      if (isp != null) {
	isp.set_image_size(((CopyNumberVariationImage) o).get_raw_size());
	// if we've changed the view to show only subsets of the data,
	// we're only populating a smaller section of the BufferedImage
	// created earlier (don't just create new BufferedImages 
	// due to Java heap size limitations).  Ensure heatmap panel
	// knows to use only the portion of the image in use.
	isp.repaint();

	// if bins re-ordered by clustering operation:
	csp.repaint();
	if (agp != null) agp.repaint();
      }
    } else if (o instanceof NavigationControl) {
      //      isp.set_horizontal_scale_level(20);
      // FIX ME: detect a scale level that will show some number of pixels
      // horizontally (depends on # of bins)
      NavigationControl nc = (NavigationControl) o;
      if (arg instanceof ArrayList) {
	// want to focus on specified sublist of bins
	launch_subwindow((ArrayList<Integer>) arg,
			 nc.get_selected_pathway(),
			 nc.get_selected_combinations()
			 );
      } else {
	Rectangle selection = (Rectangle) arg;
	RubberBandSelection rbs = isp.get_selection();

	Integer wants_sort_bin = nc.wants_sort_bin();
	// vile!
	if (wants_sort_bin != null) {
	  isp.sort_by_bin(wants_sort_bin, null);
	}

	rbs.set_selection(selection);
	rbs.set_selected_label(nc.get_selected_label());
	if (nc.wants_zoom()) {
	  // zoom in on selection
	  isp.zoom_to_selection();
	} else {
	  // center view on selection
	  isp.center_on_region(selection);
	}
	isp.repaint();
	setVisible(true);
      }
    } else if (o instanceof ChromScaleClicker) {
      Rectangle selection = config.gm.generate_selection((GenomicBin) arg);
      isp.get_selection().set_selection(selection);
      isp.zoom_to_selection();
    } else if (o instanceof GenomicMeasurement) {
      // progress report
      if (!laid_out && loading_label != null) {
	if (loading_tracker == null) loading_tracker = new LoadingTracker();
	loading_tracker.track((GenomicMeasurement) o);
	int file_count = loading_tracker.get_file_count();
	int cell_count = loading_tracker.get_cell_count();
	//	int columns = config.gm.get_headers().length;
	//	int lines_read = config.gm.get_lines_read();
	//	loading_label.setText("Loading...(" + lines_read + " rows, " + 
	//			      Funk.Str.comma_format((lines_read * columns)) + " cells)");
	loading_label.setText("Loading...(" + cell_count + " cells from " + 
			      file_count + " file" + (file_count == 1 ? "" : "s") + ")");
	pack();
      }
      layout_all();
    } else if (o instanceof AnnotationFlatfile2) {
      layout_all();
    } else if (o instanceof GenomicSet) {
      layout_all();
    } else if (o instanceof SelectionNotifier) {
      Rectangle selection = (Rectangle) arg;
      isp.get_selection().set_selection(selection);
      isp.zoom_to_selection();
    } else if (o instanceof Observable2) {
      // hacky
      if (arg instanceof Pathway) {
	// launch new window for pathway
	launch_pathway_subwindow((Pathway) arg);
      } else if (arg instanceof Rectangle) {
	// launch new subwindow from selection
	launch_selection_subwindow((Rectangle) arg);
      } else if (arg instanceof NavigationRequest) {
	NavigationRequest nr = (NavigationRequest) arg;
	if (nr.pathway != null) {
	  launch_pathway_subwindow(nr.pathway);
	} else if (nr.bin_index_list != null) {
	  launch_subwindow(nr.bin_index_list, null, null);
	} else {
	  Rectangle selection = null;
	  if (nr.bin_index != null) {
	    if (nr.wants_sort) isp.sort_by_bin(nr.bin_index, null);
	    selection = config.gm.generate_selection(nr.bin_index);
	  } else if (nr.selection != null) {
	    selection = nr.selection;
	  }

	  if (selection != null) {
	    RubberBandSelection rbs = isp.get_selection();
	    rbs.set_selection(selection);
	    rbs.set_selected_label(nr.marker);
	    // FIX ME

	    if (nr.wants_zoom) {
	      // zoom in on selection
	      isp.zoom_to_selection();
	    } else {
	      // center view on selection
	      isp.center_on_region(selection);
	    }
	    isp.repaint();
	    setVisible(true);
	  }
	}
      } else {
	System.err.println("WTF update from " + arg);  // debug
      }
    } else {
      System.err.println("ERROR: unhandled update from " + o);  // debug
    }
  }

  private boolean everything_loaded() {
    boolean cnvi_ok = (cnvi != null && cnvi.is_complete());
    boolean af_ok = config.af == null ? true : config.af.is_loaded();
    boolean vasari_ok = config.vasari == null ? true : config.vasari.is_loaded();
    boolean gm_ok = config.gm.is_loaded();
    boolean gs_ok = config.gs != null && config.gs.is_loaded();

    if (gm_ok) {
      //
      // main data file is loaded, create dependent objects
      //

      if (config.gm_supplemental.size() > 0) {
	// combining data from multiple heatmap datasets
	config.show_up_down_histogram = false;
	// disable: doesn't make sense
	// EXCEPT in new-column mode?
	config.display_sample_names = true;
	// enable sample label display
	Options.MAX_VERTICAL_SCALE_FACTOR = 16.0f;
	// allow higher zoom in level than default
	// HACK: quantify
	config.gm.combine_datasets(config.gm_supplemental, config.parent_ref);
      }

      config.gm.get_color_manager().set_config(config);
      // fugly: creates circular reference to gm...

      if (config.gs == null) {
	//
	// no bin layout file was specified; create a synthetic layout.
	//
	if (config.gm.is_genome_formatted()) {
	  config.gs = new GenomicSet(config.gm, GenomicSet.STYLE_GENOMIC, this);
	} else {
	  config.gs = new GenomicSet(config.gm, GenomicSet.STYLE_MARKER_LABEL, this);
	}
      }

      if (cnsi == null) cnsi = new CopyNumberSummaryInfo2(config.gm);

      if (cnvi == null) {
	cnvi = new CopyNumberVariationImage(config.gm);
	cnvi.addObserver(this);
	config.gm.addObserver(cnvi);
      }
    }

    if (false) {
      System.err.println(
			 "cnvi:"+cnvi_ok+
			 " af:"+af_ok+
			 " vasari:"+vasari_ok+
			 " gm:"+gm_ok+
			 " gs:"+gs_ok
			 );  // debug
    }

    return (cnvi_ok &&
	    af_ok &&
	    vasari_ok &&
	    gm_ok &&
	    gs_ok
	    );
  }

  private void create_final_gui() {
    //    System.err.println("create_final_gui(): everything?:" + everything_loaded()  + " laid_out:"+laid_out);  // debug

    if (everything_loaded() && 
	laid_out == false
	) {
      //
      //  data ready, build interface
      //

      //
      //  final pre-filtering which depends on all data being loaded
      //
      if (Options.STARTUP_RESTRICT_COLUMNS || Options.INTERACTIVE_RESTRICT_COLUMNS) {
	// columns have been filtered to a smaller set
	if (Options.RESTRICT_PRESERVE_BINS) {
	  System.err.println("fix me: bin layout preservation disabled");  // debug
	  //	  config.gs.collapse_to(Options.RESTRICT_USED_BINS);
	} else {
	  System.err.println("building new GenomicSet");  // debug
	  config.gs = new GenomicSet(config.gm, GenomicSet.STYLE_MARKER_LABEL, null);
	}
      }


      laid_out = true;

      if (Options.INCLUDE_DEVELOPMENT_CODE) {
	System.err.println("**** WARNING: build includes development features!");  // debug
      }

      if (config.af != null) {
	// annotations
	Exception e = config.af.get_parse_exception();
      
	if (e != null) {
	  // error loading data
	  JOptionPane.showMessageDialog(this,
					"Could not access protected sample annotation data.  Your session may have expired.  The viewer will display without annotations.",
					"Access denied",
					JOptionPane.ERROR_MESSAGE);	  
	  config.af = null;
	} else {
	  config.af.index_annotations(config.gm);
	}
      }

      if (config.vasari != null) config.vasari.index_annotations(config.gm);

      CommentOptions gm_options = config.gm.get_options();
      String option;
      if ((option = gm_options.get("bin_by_marker_name")) != null &&
	  option.equals("true")) {
	config.gs = new GenomicSet(config.gm, GenomicSet.STYLE_MARKER_LABEL, null);
      }

      int gm_columns = config.gm.get_headers().length;
      int bin_count = config.gs.get_bin_count();

      if (gm_columns != bin_count) {
	System.err.println("WARNING: bin count doesn't match data count! bins=" + bin_count + " cols=" + gm_columns);  // debug
      }

      if (gm_columns < Options.DISABLE_HORIZONTAL_ZOOM_COLUMNS) {
	if (config.gm_supplemental.size() > 0 && !Options.COMBINE_DATASETS_ADD_ROWS_MODE) {
	  // if combining heatmaps in column adding mode, don't
	  // disable as the column labels have been extended and are likely
	  // to need zooming-in
	} else {
	  config.enable_horizontal_zoom.setValue(false);
	}
      }

      //
      //  set component display preferences from input data options, if provided
      //
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

      if (config.title == null &&
	  (option = gm_options.get("title")) != null) {
	// if title not already specified in configuration (i.e. subwindow),
	// use title from data file if available
	config.title = option;
	set_title();
      }

      show_total_histogram = true;
      if (gm_options.option_equals_lc("total_histogram", "off")) {
	show_total_histogram = false;
      }
      if (gm_options.option_equals_lc("up_down_histogram", "off")) {
	config.show_up_down_histogram = false;
      }

      int img_width = cnvi.getWidth();
      jsb_h = new JScrollBar(JScrollBar.HORIZONTAL, 0, 0, 0, img_width);
      jsb_h.addAdjustmentListener(this);
      jsb_v = new JScrollBar(JScrollBar.VERTICAL, 0, 0, 0, cnvi.getHeight());
      jsb_v.addAdjustmentListener(this);

      BufferedImage cnvi_bi = cnvi.get_image();
      isp = new ChromDecoratorPanel(cnvi_bi, jsb_h, jsb_v, config);
      isp.addObserver(this);

      //      addKeyListener(this);
      isp.addKeyListener(new KeyboardScrollListener(jsb_h, jsb_v));

      if (config.af != null) {
	isp.set_tooltip_field("age of onset");
	// hacktacular
      }
      if (config.display_sample_names) isp.set_display_sample_ids(true);

      //
      //  user-specified supplemental/custom tracks:
      //
      tracks = new ArrayList<TrackPanel>();
      for (TrackInfo ti : gm_options.get_tracks()) {
	ti.set_width(gm_columns);
	TrackPanel tp = new TrackPanel(ti, jsb_h);
	tp.get_selection_notifier().addObserver(this);
	tracks.add(tp);
      }

      // isp_cnc_incr = new ImageScalePanel2(cnsi.get_increase_decrease_image());
      //      System.err.println("updown="+config.show_up_down_histogram);  // debug

      if (config.show_up_down_histogram) {
	GISTIC gistic = config.gm.get_options().get_gistic();

	if (gistic != null) gistic.bin_infill(config.gs);
	// hack: have to wait until now because GenomicSet isn't
	// accessible or loaded when GISTIC object is instantiated

	sp_cnc_incr = new SummaryPanelIncreaseDecrease(cnsi,
						       img_width,
						       config.gm.get_color_manager().get_all_color_schemes().get(0),
						       gistic
						       );
      }

      if (show_total_histogram) {
	sp_cnc_any = new SummaryPanelAnyVariation(cnsi, img_width);
	sp_cnc_any.set_horizontal_scrollbar(jsb_h);
      }

      csp = new ChromScalePanel2(config.gs, img_width);
      csp.get_clicker().addObserver(this);
 
      //
      //  set heatmap panel preferred size based on scaled size and
      //  available space
      //
      int y_used = 0;
      if (config.show_up_down_histogram) y_used += sp_cnc_incr.get_scaled_size().height;
      if (show_total_histogram) y_used += sp_cnc_any.get_scaled_size().height;
      y_used += csp.get_scaled_size().height;
      for (TrackPanel tp : tracks) {
	y_used += tp.get_scaled_size().height;
      }

      //      System.err.println("y_used="+y_used);  // debug
      start_y_scaling = guess_y_scaling(y_used);
      isp.set_min_vertical_scale_level(start_y_scaling);
      // minimum scale level to fit everything onscreen

      if (config.minimum_initial_vertical_scale_factor != null) {
	// set a minimum INITIAL vertical scale level, i.e. for zoomed-in subwindow
	int f = (int) (config.minimum_initial_vertical_scale_factor.floatValue());
	// feh
	if (start_y_scaling < f) start_y_scaling = f;
      }
      isp.set_vertical_scale_level(start_y_scaling);

      //      isp.set_min_vertical_scale_level(Options.MIN_VERTICAL_SCALE_FACTOR);

      float max_y_scale = get_max_vertical_scale_factor();

      if (start_y_scaling > max_y_scale) max_y_scale = start_y_scaling;
      // if zoomed-in very far to start
      isp.set_max_vertical_scale_level(max_y_scale);
      isp.set_max_horizontal_scale_level(150);
      // hacktacular: USE A CONSTANT!

      int max = (int) (screen.height * Options.MAX_Y_WINDOW_SIZE_FRACTION) - y_used;

      Dimension visible_image_size = cnvi.get_raw_size();
      isp.set_image_size(visible_image_size);
      // the size of the currently visible image (some subsets might be turned off initially)

      int py = isp.get_scaled_size().height;
      // scaled size of the currently visible image
      // ensure scaling/scrollbars are appropriately set if only some subsets are initially visible

      if (py > max) py = max;

      //      System.err.println("PY="+py + " used:" + y_used + " total:"+screen.height + " max:"+max);  // debug

      //      new MouseDragScroller(isp, jsp_heatmap.getHorizontalScrollBar());

      //
      //  rebuild layout
      //

      main_panel.removeAll();
      BoxLayout bl = new BoxLayout(main_panel, BoxLayout.Y_AXIS);
      main_panel.setLayout(bl);
      // FIX ME: better choice??

      isp.set_horizontal_scrollbar(jsb_h);
      if (config.show_up_down_histogram) sp_cnc_incr.set_horizontal_scrollbar(jsb_h);
      csp.set_horizontal_scrollbar(jsb_h);

      isp.addMouseWheelListener(this);

      //      new MouseDragScroller(isp, jsb_h, jsb_v);
      new MouseDragScroller(isp, jsb_h, jsb_v);

      //
      //  add components to layout
      //

      if (true) {
	//	System.err.println("FIX ME: set ISP pref size *first*???");  // debug
	jp_heatmap = new JPanel();
	jp_heatmap.setLayout(new BorderLayout());
	jp_heatmap.add("Center", isp);
	jp_heatmap.add("East", jsb_v);
	main_panel.add(jp_heatmap);
      } else {
	main_panel.add(isp);
      }

      for (TrackPanel tp : tracks) {
	main_panel.add(tp);
      }

      if (config.show_up_down_histogram) main_panel.add(sp_cnc_incr);
      if (show_total_histogram) main_panel.add(sp_cnc_any);
      main_panel.add(csp);
      main_panel.add(jsb_h);

      getContentPane().setLayout(new BorderLayout());

      JSplitPane sp = null;
      if (config.af == null && config.vasari == null) {
	//
	//  no annotation panel, simple layout
	//
	getContentPane().add("Center", main_panel);
      } else {
	//
	//  use JSplitPane to hold heatmap and annotation panels
	//
	agp = new AnnotationGridPanel2(config.af, config.vasari, config.gm, isp);
	//	System.err.println("agp:"+ agp.getPreferredSize());  // debug

	agp.set_grid_height(isp.getPreferredSize().height);
	jsb_v.addAdjustmentListener(agp);

	jsp_annot = new JScrollPane(agp);

	//	dump_sizes();

	//	System.err.println("mp pref #0:"+main_panel.getPreferredSize());  // debug
	sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
			    main_panel, jsp_annot);

	getContentPane().add("Center", sp);
      }

      nb = new NavigationBar(config);
      if (true) {
	JScrollPane jsp_nb = new JScrollPane(nb,
					     JScrollPane.VERTICAL_SCROLLBAR_NEVER,
					     //JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
					     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
					     );
	// problem enabling horizontal scrollbar: dead space appears below components.
	// However this is still an improvement as it keeps all components on one line
	// even if parent window doesn't have enough space to display everything.
	getContentPane().add("North", jsp_nb);
      } else {
	// problematic: if resize window very small, components can be laid out offscreen
	getContentPane().add("North", nb);
      }
      nb.addObserver(this);

      int DEFAULT_PANEL_WIDTH = (int) (screen.width * 0.99);
      //      if (gm_columns < screen.width) {
      if (gm_columns < screen.width) {
	// all columns can fit onscreen at 1 pixel each

	if (gm_columns < Options.MIN_COLUMNS_TO_SET_FIXED_WIDTH) {
	  // we're displaying a limited set of columns;
	  // reserve a limited amount of space based on column labels.
	  FontMetrics fm = isp.getFontMetrics(isp.getFont());
	  String[] headers = config.gm.get_headers();
	  int max_header_width = 0;
	  int w;
	  for (int i=0; i < headers.length; i++) {
	    w = fm.stringWidth(headers[i]);
	    if (w > max_header_width) max_header_width = w;
	  }
	  DEFAULT_PANEL_WIDTH = (int) ((max_header_width * headers.length) * 1.10);
	  //	  System.err.println("max HW="+max_header_width);  // debug
	  //	  System.err.println("DPW = " + DEFAULT_PANEL_WIDTH);  // debug
	} else if (gm_columns < (DEFAULT_PANEL_WIDTH / 2)) {
	  int scale = DEFAULT_PANEL_WIDTH / gm_columns;
	  DEFAULT_PANEL_WIDTH = gm_columns * scale;
	  //	  System.err.println("gcols="+gm_columns + " scale=" + scale + " DPW="+DEFAULT_PANEL_WIDTH);  // debug
	} else {
	  DEFAULT_PANEL_WIDTH = gm_columns;
	}

	if (DEFAULT_PANEL_WIDTH < nb.getPreferredSize().width) {
	  // request at least as much space as taken up by navigation bar.
	  // Avoid case where heatmap + annotation panel don't request all 
	  // available space; leads to "dead space" to right of annotation
	  // panel (e.g. dataset #189 Gene_Mutation_TCGA_GBM.txt_binary.gz
	  // on 1280x800 display)
	  DEFAULT_PANEL_WIDTH = nb.getPreferredSize().width;
	}

	//	System.err.println("screen_width=" + screen.width + " DWP_raw="+ DEFAULT_PANEL_WIDTH);  // debug
	//	System.err.println("nav bar width: " + nb.getPreferredSize());  // debug
      }

      if (config.af != null || config.vasari != null) {
	//	int anno_width = agp.getPreferredSize().width;
	int anno_width = agp.getPreferredSize().width + 20;
	//int anno_width = agp.getPreferredSize().width;
	// HACK: add a little padding to account for JScrollPane controls.
	int max_anno_width = screen.width / 3;
	//	System.err.println("screen_w="+ screen.width + " max anno w="+max_anno_width);  // debug

	if (anno_width > max_anno_width) {
	  //	  System.err.println("annotation width exceeds max");  // debug
	  anno_width = max_anno_width;
	}
	//	DEFAULT_PANEL_WIDTH -= anno_width;
	// only subtract if the two panels together take up too much space!

	int overflow = (DEFAULT_PANEL_WIDTH + anno_width) - screen.width;
	//	System.err.println("overflow="+overflow);  // debug

	if (overflow > 0) DEFAULT_PANEL_WIDTH -= overflow;

	//	DEFAULT_PANEL_WIDTH -= anno_width;

	//	System.err.println("anno_w=" + anno_width + " final DPW="+DEFAULT_PANEL_WIDTH);  // debug

	sp.setDividerLocation(DEFAULT_PANEL_WIDTH);
      }

      //
      //  summary histogram panels:
      //

      if (show_total_histogram) 
	sp_cnc_any.setPreferredSize(new Dimension(DEFAULT_PANEL_WIDTH, sp_cnc_any.get_scaled_size().height));
      for (TrackPanel tp : tracks) {
	tp.setPreferredSize(new Dimension(DEFAULT_PANEL_WIDTH, tp.get_scaled_size().height));
      }

      if (config.show_up_down_histogram) sp_cnc_incr.setPreferredSize(new Dimension(DEFAULT_PANEL_WIDTH, sp_cnc_incr.get_scaled_size().height));
      csp.setPreferredSize(new Dimension(DEFAULT_PANEL_WIDTH, csp.get_scaled_size().height));
      isp.setPreferredSize(new Dimension(DEFAULT_PANEL_WIDTH, py));

      if (jp_heatmap != null) jp_heatmap.revalidate();
      // since we delayed setting preferred sizes

      //      dump_sizes();
      //      System.err.println("mp preferred:"+main_panel.getPreferredSize());  // debug

      if (false) {
	System.err.println("disable main_panel pref size");  // debug
      } else {
	main_panel.setPreferredSize(new Dimension(DEFAULT_PANEL_WIDTH,
						  main_panel.getPreferredSize().height));
      }

      Dimension pref = getPreferredSize();
      int x_max = (int) (screen.width * 0.99);
      if (pref.width > x_max) {
	try {
	  setPreferredSize(new Dimension(x_max, pref.height));
	  // dies trying to compile with -target jsr14 (this method new in 1.5)
	} catch (NoSuchMethodError e) {
	  System.err.println("can't set pref JFrame size on this JVM");  // debug
	}
      }

      //      if (pref.height > screen.height) setPreferredSize(new Dimension(pref.width, screen.height));

      
      //
      //  start menu building
      //
      JMenuBar mb = new JMenuBar();
      JMenu m;
      mb.add(m = new JMenu("Tools")); 
      m.setMnemonic(KeyEvent.VK_T);

      mi_contrast = add_menuitem(m, "Color and contrast...", KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));

      if (config.af != null) {
	//
	// annotations available, create annotation-specific menu items
	//

	mi_view_annotations = add_menuitem(m, "Annotations...", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.ALT_MASK));
	
	add_menuitem(m, LABEL_SET_ANNOT_TOOLTIP, KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK));
      }
      mi_contrast.addActionListener(this);


      mi_nav = add_menuitem(m, "Navigation...", KeyEvent.VK_V, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK));
      mi_nav.addActionListener(this);

      JMenu sub = new JMenu("Cluster...");
      m.add(sub);
      JMenuItem jmi;
      sub.add(jmi = new JMenuItem("by Pearson's r..."));
      jmi.addActionListener(new SetVisible(new PearsonClusterControl(config.gm, isp)));

      sub.add(jmi = new JMenuItem("by Euclidean distance..."));
      jmi.addActionListener(new SetVisible(new EuclideanClusterControl(config.gm, isp)));

      sub.add(jmi = new JMenuItem("by variation frequency..."));
      jmi.addActionListener(new SetVisible(new ClusterControl(config.gm, config.gs)));

      //      mi_nav.addActionListener(this);

      if (Options.INCLUDE_DEVELOPMENT_CODE) {
	m.add(jmi = new JMenuItem("Report frequency...[development]"));
	jmi.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      FrequencyReporter fr = new FrequencyReporter(config, isp);
	    }
	  }
	  );
      }
      
      //
      //  View menu:
      //
      mb.add(m = new JMenu("View"));
      m.setMnemonic(KeyEvent.VK_V);
      
      SampleSubsets ss = config.gm.get_sample_subsets();
      if (!ss.isEmpty()) {
	m.add(sub = new JMenu("Display samples for..."));
	//	sub.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.ALT_MASK));
	sub.setMnemonic(KeyEvent.VK_D);

	for (String subset : ss.get_subsets_arraylist()) {
	  sub.add(ss.get_checkbox_menuitem(subset));
	  // "show samples for..." submenu
	}
      
	sub.add(new JSeparator());
	sub.add(jmi = new JMenuItem("all"));
	jmi.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      for (JCheckBoxMenuItem cb : config.gm.get_sample_subsets().get_checkboxes()) {
		if (!cb.isSelected()) cb.doClick();
		// hacky (slow!)
	      }
	    }
	  });
	sub.add(jmi = new JMenuItem("none"));
	jmi.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      for (JCheckBoxMenuItem cb : config.gm.get_sample_subsets().get_checkboxes()) {
		if (cb.isSelected()) cb.doClick();
	      }
	    }
	  });
      }

      m.add(jmi = new JMenuItem("Custom sort order..."));
      jmi.addActionListener(
			    new ActionListener() {
			      public void actionPerformed(ActionEvent e) {
				new CustomSortControl(config.gm);
				// FIX ME: could use SetVisible instance
				// if we detect the window has been made visible and
				// refresh the ordering list...
			      }
			    }
			    );
      
      if (config.gm.get_options().has_gistic() && config.show_up_down_histogram) {
	ButtonGroup bg = new ButtonGroup();

	String upper_label = config.gm.get_options().get("high_label");
	if (upper_label == null) upper_label = "Increase";
	String lower_label = config.gm.get_options().get("low_label");
	if (lower_label == null) lower_label = "Decrease";
	String label = upper_label + "/" + lower_label + " display...";
	m.add(sub = new JMenu(label));

	JCheckBoxMenuItem jcb_changes = new JCheckBoxMenuItem("Percentage change", false);
	JCheckBoxMenuItem jcb_gistic = new JCheckBoxMenuItem("GISTIC q-values", true);
	bg.add(jcb_changes);
	bg.add(jcb_gistic);
	sub.add(jcb_changes);
	sub.add(jcb_gistic);

	jcb_changes.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      sp_cnc_incr.set_gistic_mode(false);
	    }
	  });
	jcb_gistic.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      sp_cnc_incr.set_gistic_mode(true);
	    }
	  });

      }

      //
      //  Zoom menu:
      //
      m = new JMenu("Zoom");
      m.setMnemonic(KeyEvent.VK_Z);
      add_menuitem(m, LABEL_ZOOM_IN, KeyEvent.VK_I, KeyStroke.getKeyStroke('+'));
      add_menuitem(m, LABEL_ZOOM_OUT, KeyEvent.VK_O, KeyStroke.getKeyStroke('-'));
      add_menuitem(m, LABEL_ZOOM_OUT_MAX, KeyEvent.VK_X, KeyStroke.getKeyStroke('<'));
      add_menuitem(m, LABEL_ZOOM_RESET, KeyEvent.VK_1, KeyStroke.getKeyStroke('='));
      add_menuitem(m, LABEL_ZOOM_TO_SELECTION, KeyEvent.VK_S, KeyStroke.getKeyStroke('s'));
      m.add(new JSeparator());
      add_menuitem(m, LABEL_SET_Y_SCALING, KeyEvent.VK_L, KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.ALT_MASK));

      m.add(generate_checkbox("Enable vertical zooming", config.enable_vertical_zoom));
      m.add(generate_checkbox("Enable horizontal zooming", config.enable_horizontal_zoom));

      m.add(new JSeparator());
      m.add(mi_show_zoom = new JCheckBoxMenuItem(LABEL_SHOW_ZOOM_INDICATOR, show_zoom_level));
      mi_show_zoom.addActionListener(this);

      m.add(new JSeparator());
      m.add(mi_help_zoom = new JMenuItem("Help: zooming and scrolling"));
      mi_help_zoom.addActionListener(this);
      
      //      m.add(mi_contrast = new MenuItem("Contrast..."));
      //      m.add(mi_view_annotations = new MenuItem("Annotations..."));
      //      mi_contrast.addActionListener(this);
      //      mi_view_annotations.addActionListener(this);
      mb.add(m);

      m = new JMenu("Links");
      m.setMnemonic(KeyEvent.VK_L);
      add_menuitem(m, LABEL_LINK_HEATMAPS, KeyEvent.VK_H, null);
      add_menuitem(m, LABEL_LINK_CGWB, KeyEvent.VK_C, null);
      mb.add(m);

      if (true) {
	m = new JMenu("Help");
	m.setMnemonic(KeyEvent.VK_H);
	//      add_menuitem(m, LABEL_HELP, KeyEvent.VK_H, KeyStroke.getKeyStroke('?'));
	
	add_menuitem(m, LABEL_HELP, KeyEvent.VK_H, KeyStroke.getKeyStroke(KeyEvent.VK_F1,0));
	// hacky, but VK_HELP returns "Help" (what key is that??)
	mb.add(m);
      }

      setJMenuBar(mb);
      // 
      //  end menu
      //

      pack();
      setVisible(true);

      isp.requestFocusInWindow();
      // for MouseDragScroller keyboard emulation

      //      if (Options.LOCK_ZOOM_OUT) isp.zoom_out_max();
      // contributes to initial size problem??

      if (config.start_bin != null) {
	GenomicBin wanted = null;
	for (GenomicBin gb : config.gs.get_bins()) {
	  if (gb.bin_name.equals(config.start_bin)) {
	    wanted = gb;
	    break;
	  }
	}

	if (wanted == null) {
	  System.err.println("ERROR: no such start bin name " + config.start_bin);  // debug
	} else {
	  Rectangle selection;
	  if (config.start_pos != null && config.end_pos != null) {
	    // positions within bin specified
	    selection = config.gs.get_selection_for_chr_location(wanted.bin_name,
								 config.start_pos,
								 config.end_pos);
	  } else {
	    // just bin specified
	    selection = config.gm.generate_selection(wanted);
	  }	    
	  SelectionNotifier sn = new SelectionNotifier();
	  sn.addObserver(this);
	  sn.set_pending_selection(selection);
	  javax.swing.SwingUtilities.invokeLater(sn);
	  // update selection after all pending GUI events have been flushed
	}
      } else if (config.start_marker != null) {
	BinIndex bi = new BinIndex(config.gm);
	int wanted = bi.find(config.start_marker);
	if (wanted == -1) {
	  System.err.println("ERROR: can't find marker " + config.start_marker);  // debug
	} else {
	  Rectangle selection = config.gm.generate_selection(wanted);
	  SelectionNotifier sn = new SelectionNotifier();
	  sn.addObserver(this);
	  sn.set_pending_selection(selection);
	  javax.swing.SwingUtilities.invokeLater(sn);
	  // update selection after all pending GUI events have been flushed
	}

      }


    }
  }
  
  private void set_title () {
    String t = "Heatmap";
    if (config.title != null) t = t.concat(": " + config.title);
    if (config.pathway != null) t = t.concat(": " + config.pathway.name);
    setTitle(t);
  }

  private void setup() {
    new PathwayGenes();
    // start static load in background
    initial_gui_built = false;
    //    addComponentListener(this);
    javax.swing.SwingUtilities.invokeLater(this);
    // ensure all GUI-building code executed from the event-dispatching thread
    //    System.err.println("new thread");  // debug
    //    new Thread(this).start();
  }

  public void layout_all() {
    if (laid_out == false) javax.swing.SwingUtilities.invokeLater(this);
    // ensure all GUI-building code executed from the event-dispatching thread
  }

  public void run() {
    // ensure all GUI-building code executed from the event-dispatching thread
    //    (new Exception("run invoked")).printStackTrace();
    if (initial_gui_built) {
      create_final_gui();
    } else {
      create_initial_gui();
    }
  }
  
  private void create_initial_gui() {
    main_panel = new JPanel();
    set_title();

    if (config.gs != null) config.gs.addObserver(this);
    // data layout may not be built until GenomicMeasurement is finished loading

    Funk.LookAndFeeler.set_native_lookandfeel();
    config.gm.addObserver(this);

    for (GenomicMeasurement gm : config.gm_supplemental) {
      gm.addObserver(this);
    }

    if (config.af != null) config.af.addObserver(this);
    if (config.vasari != null) config.vasari.addObserver(this);

    if (Options.IS_APPLET == false && 
	config.exit_on_close) setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    // call exit() only when primary window closed, not subwindows

    if (everything_loaded()) {
      layout_all();
    } else {
      main_panel.setLayout(new BorderLayout());
      main_panel.add(loading_label = new JLabel("Loading..."));
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

      //    setPreferredSize(new Dimension((int) (screen.width * 0.99), (int) (screen.height * 0.85)));

      getContentPane().setLayout(new BorderLayout());
      getContentPane().add((config.af == null && config.vasari == null) ? "Center" : "West", main_panel);
      pack();
    }
    setVisible(true);
    initial_gui_built = true;
  }

  public static void main (String [] argv) {
    try {
      String gm_file = null;
      String annotation_file = null;
      String genomic_set_file = null;
      String vasari_file = null;

      String url = null;
      String auth = null;
      boolean has_bins = false;
      boolean has_annotations = false;
      boolean has_vasari = false;
      String start_dir = null;

      String url_gm = null;
      String url_annot = null;
      String url_set = null;

      HeatmapConfiguration conf = new HeatmapConfiguration();

      ArrayList<String> dataset_ids = new ArrayList<String>();

      for (int i=0; i < argv.length; i++) {
	if (argv[i].equals("-gm")) {
	  if (gm_file == null) {
	    gm_file = argv[++i];
	  } else {
	    boolean async = true;
	    //	    System.err.println("loading supplemental file, async=" + async);
	    conf.gm_supplemental.add(new GenomicMeasurement(argv[++i], async));
	  }
	} else if (argv[i].equals("-help") ||
		   argv[i].equals("/?") 
		   ) {
	  show_help();
	  System.exit(1);
	} else if (argv[i].equals("-default")) {
	  gm_file = "Broad_SNP6_GenomicMeasurement.txt";
	  annotation_file = "intgen.org_GBM.biotab.1.0.0.tab";
	  genomic_set_file = "broad_snp6_genomicset.txt";
	} else if (argv[i].equals("-demo")) {
	  gm_file = "CopyNumbers.tab";
	  annotation_file = "annotations.tab";
	  genomic_set_file = "GenomicSet.tab";
	} else if (argv[i].equals("-annot")) {
	  annotation_file = argv[++i];
	} else if (argv[i].equals("-vasari")) {
	  vasari_file = argv[++i];
	} else if (argv[i].equals("-set")) {
	  genomic_set_file = argv[++i];
	} else if (argv[i].equals("-enable-acceleration")) {
	  System.err.println("enabling acceleration (look out below!)");  // debug
	  Options.DISABLE_ACCELERATION = false;
	} else if (argv[i].equals("-bright-percent")) {
	  Options.DEFAULT_MIN_COLOR_INTENSITY_PERCENT = Integer.parseInt(argv[++i]);
	} else if (argv[i].equals("-no-optimize")) {
	  Options.AUTO_OPTIMIZE_GRADIENTS = false;
	} else if (argv[i].equals("-columns")) {
	  // on startup, restrict view to the specified comma-delimited list of columns
	  String[] cols = argv[++i].split(",");
	  Options.STARTUP_RESTRICT_COLUMNS = true;
	  Options.RESTRICT_COLUMNS = new HashSet<String>();
	  for (int j=0; j < cols.length; j++) {
	    Options.RESTRICT_COLUMNS.add(new String(cols[j]));
	  }
	} else if (argv[i].equals("-combine-add-columns")) {
	  // when combining heatmaps, add new columns instead of new rows
	  Options.COMBINE_DATASETS_ADD_ROWS_MODE = false;
	} else if (argv[i].equals("-combine-add-rows")) {
	  // when combining heatmaps, add new rows
	  Options.COMBINE_DATASETS_ADD_ROWS_MODE = true;
	} else if (argv[i].equals("-gradients")) {
	  String[] grads = argv[++i].split(",");
	  int[] gradients = new int[grads.length];
	  for (int j=0; j < grads.length; j++) {
	    gradients[j] = Integer.parseInt(grads[j]);
	  }
	  Options.DEFAULT_COPYNUMBER_GRADIENTS = gradients;
	} else if (argv[i].equals("-quiet")) {
	  Options.VERBOSE_ERRORS = false;
	} else if (argv[i].equals("-dataset")) {
	  dataset_ids.add(new String(argv[++i]));
	} else if (argv[i].equals("-auth")) {
	  auth = argv[++i];
	} else if (argv[i].equals("-title")) {
	  conf.title = argv[++i];
	} else if (argv[i].equals("-url")) {
	  url = argv[++i];
	  URLLauncher.set_url(url);
	} else if (argv[i].equals("-url-gz")) {
	  Options.GZIP_URL_STREAMS = binary_option(argv[++i]);
	} else if (argv[i].equals("-binary")) {
	  Options.DEFAULT_USE_BINARY_SAMPLE_FILE = binary_option(argv[++i]);
	} else if (argv[i].equals("-url-gm")) {
	  url_gm = argv[++i];
	} else if (argv[i].equals("-url-annot")) {
	  url_annot = argv[++i];
	} else if (argv[i].equals("-url-set")) {
	  url_set = argv[++i];
	} else if (argv[i].equals("-dir")) {
	  start_dir = argv[++i];
	} else if (argv[i].equals("-has-bins")) {
	  has_bins = true;
	} else if (argv[i].equals("-has-annotations")) {
	  has_annotations = true;
	} else if (argv[i].equals("-has-vasari")) {
	  has_vasari = true;
	} else if (argv[i].equals("-start-bin")) {
	  conf.start_bin = argv[++i];
	} else if (argv[i].equals("-start-marker")) {
	  conf.start_marker = argv[++i];
	} else if (argv[i].equals("-start-pos")) {
	  conf.start_pos = new Integer(argv[++i]);
	} else if (argv[i].equals("-end-pos")) {
	  conf.end_pos = new Integer(argv[++i]);
	} else {
	  System.err.println("ERROR: unknown arg " + argv[i]);  // debug
	  show_help();
	  System.exit(1);
	}
      }

      if (gm_file != null) {
	//
	//  using local flatfiles
	//
	conf.af = (annotation_file == null) ? null : new AnnotationFlatfile2(annotation_file);
	conf.vasari = (vasari_file == null) ? null : new AnnotationFlatfile2(vasari_file);
	conf.gm = new GenomicMeasurement(gm_file, true);
	//	conf.gs = (genomic_set_file == null) ? new GenomicSet(conf.gm, GenomicSet.STYLE_GENOMIC) : new GenomicSet(genomic_set_file);

	if (genomic_set_file != null) conf.gs = new GenomicSet(genomic_set_file);
      } else if (dataset_ids.size() > 0) {
	//
	//  load a dataset from web server using "project" / authentication model
	//
	String dataset_id = dataset_ids.get(0);
	// primary dataset
	boolean use_binary = Options.DEFAULT_USE_BINARY_SAMPLE_FILE;
	Hashtable p = new Hashtable();

	if (dataset_ids.size() > 1) {
	  // supplemental datasets to be combined with primary
	  for (int idx = 1; idx < dataset_ids.size(); idx++) {
	    String dsid = dataset_ids.get(idx);
	    System.err.println("loading supplemental dataset ID " + dsid);  // debug
	    p.clear();
	    p.put("ds", dsid);
	    p.put("serve", "data");
	    if (auth != null) p.put("auth", auth);
	    if (use_binary) p.put("binary", "1");
	    conf.gm_supplemental.add(new GenomicMeasurement(new GZIPInputStream(open_url(url, p)), true, use_binary));
	  }
	}

	p.clear();
	p.put("ds", dataset_id);
	p.put("serve", "data");
	if (auth != null) p.put("auth", auth);
	if (use_binary) p.put("binary", "1");
	conf.gm = new GenomicMeasurement(new GZIPInputStream(open_url(url, p)), true, use_binary);

	if (has_bins) {
	  p.clear();
	  p.put("ds", dataset_id);
	  p.put("serve", "bin");
	  if (auth != null) p.put("auth", auth);

	  conf.gs = new GenomicSet(new GZIPInputStream(open_url(url, p)));
	  //	System.err.println("bins:" + config.gs.get_bins().size());  // debug
	}
      
	if (has_annotations) {
	  p.clear();
	  p.put("ds", dataset_id);
	  p.put("serve", "annotations");
	  if (auth != null) p.put("auth", auth);
	  conf.af = new AnnotationFlatfile2(new GZIPInputStream(open_url(url, p)));
	}

	if (has_vasari) {
	  p.clear();
	  p.put("ds", dataset_id);
	  p.put("serve", "vasari");
	  conf.vasari = new AnnotationFlatfile2(new GZIPInputStream(open_url(url, p)));
	}
      } else if (url_gm != null) {
	conf.gm = new GenomicMeasurement(open_simple_url(url_gm), true, Options.DEFAULT_USE_BINARY_SAMPLE_FILE);
	conf.af = (url_annot == null) ? null : new AnnotationFlatfile2(open_simple_url(url_annot));
	conf.gs = (url_set == null) ? null : new GenomicSet(open_simple_url(url_set));
      }

      if (conf.gm != null) {
	Heatmap6 hm = new Heatmap6(conf);
	if (conf.gs != null) conf.gs.addObserver(hm);
      } else {
	//
	//  not enough information specified to start viewer
	//
	show_help();
	Funk.LookAndFeeler.set_native_lookandfeel();
	HeatmapLocalFileLauncher fl = new HeatmapLocalFileLauncher();
	if (start_dir != null) fl.get_chooser().setCurrentDirectory(new File(start_dir)); 
      }

    } catch (Throwable t) {
      show_help();
      System.out.println("ERROR:" + t);  // debug
      t.printStackTrace();
      new ErrorReporter(t);
    }
  }

  private static void show_help () {
    System.out.println("Heatmap viewer command-line parameters:");  // debug
    System.err.println("");  // debug
    System.err.println("Input options:");  // debug

    System.out.println("     -gm [file]: genomic measurement file (copy number data) [REQUIRED]");
    System.out.println("  -annot [file]: annotations file (optional)");
    System.out.println("    -set [file]: genomic set/bin file (optional)");

    System.err.println("");  // debug
    System.err.println("Display options:");  // debug
    System.out.println("    -gradients [comma-delimited list]: specify copy-number thresholds for brightness gradients (e.g. 1,2,3)");
    System.out.println("    -bright-percent [value]: specify starting color brightness percent (1-100)");
  }

  // begin MouseListener stubs
  public void mousePressed(MouseEvent e) {}
  public void mouseClicked(MouseEvent e) {
    //    System.err.println("click " + e);  // debug
    //    Point p = isp.get_raw_view_position();

    //    isp.center_on_x(e.getPoint().x);
  };
  public void mouseReleased(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};
  // end MouseListener stubs

  // begin AdjustmentListener stub
  public void adjustmentValueChanged(AdjustmentEvent e) {
    if (true) {
      // FIX ME: have the clients listen/change directly???
      if (isp != null && !config.initial_max_x_zoom_checked) {
	float hscale = isp.get_horizontal_scale_level();
	if (hscale > 1) {
	  if (hscale > isp.get_max_horizontal_scale_level()) {
	    // grandfather in an initial zoom level that's greater than default maximum
	    // (e.g. displaying very few columns)
	    isp.set_max_horizontal_scale_level(hscale);
	  }
	  config.initial_max_x_zoom_checked = true;
	}
      }

      sync_horizontal_zoom(isp, sp_cnc_incr);
      for (TrackPanel tp : tracks) {
	sync_horizontal_zoom(isp, tp);
      }
      if (show_total_histogram) sync_horizontal_zoom(isp, sp_cnc_any);
      sync_horizontal_zoom(isp, csp);
      // sync horizontal scale factor between heatmap and other components
    }
    sync_annotation_zoom();
  }
  // end AdjustmentListener stub

  private void sync_horizontal_zoom (ScalePanel2 from, ScalePanel2 to) {
    if (from != null) {
      float hscale = from.get_horizontal_scale_level();
      if (to != null && to.get_horizontal_scale_level() != hscale) {
	//	System.err.println("from:"+from + "   to:"+to);  // debug
	to.set_horizontal_scale_level(hscale);
      }
    }
  }

  private void sync_viewports (JScrollPane from, JScrollPane to) {
    Point p_from = from.getViewport().getViewPosition();
    JViewport jv_to = to.getViewport();
    jv_to.setViewPosition(new Point(p_from.x, jv_to.getViewPosition().y));
  }

  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();

    if (src.equals(mi_contrast)) {
      // launch contrast control
      if (cc == null) {
	cc = new ContrastControl(config);
      } else {
	cc.setVisible(true);
	cc.setState(JFrame.NORMAL);
      }
    } else if (src.equals(mi_help_zoom)) {
      HelpLauncher hl = new HelpLauncher(HelpLauncher.ANCHOR_ZOOM);
      hl.launch_url();
    } else if (src.equals(mi_nav)) {
      if (nc != null) {
	nc.setState(JFrame.NORMAL);
	nc.setVisible(true);
      } else {
	nc = new NavigationControl(config.gm, config.gs);
	nc.addObserver(this);
      }
    } else if (src.equals(mi_view_annotations)) {
      av = new AnnotationViewer(config.af, config.gm, isp);
    } else if (src.equals(mi_show_zoom)) {
      show_zoom_level = mi_show_zoom.getState();
      isp.set_show_zoom(show_zoom_level);
    } else if (src instanceof AbstractButton) {
      String label = ((AbstractButton) src).getText();
      if (label.equals(LABEL_ZOOM_IN)) {
	isp.zoom(isp.ZOOM_DIRECTION_IN, get_zoom_type(), Options.ZOOM_FACTOR, null);
      } else if (label.equals(LABEL_ZOOM_OUT)) {
	isp.zoom(isp.ZOOM_DIRECTION_OUT, get_zoom_type(), Options.ZOOM_FACTOR, null);
      } else if (label.equals(LABEL_ZOOM_TO_SELECTION)) {
	isp.zoom_to_selection();
      } else if (label.equals(LABEL_ZOOM_RESET)) {
	//	isp.set_scale_levels(1,1);
	if (horizontal_zoom_enabled()) isp.zoom_reset();
	if (vertical_zoom_enabled()) isp.set_vertical_scale_level(start_y_scaling);
      } else if (label.equals(LABEL_ZOOM_OUT_MAX)) {
	isp.zoom_out_max();
	// isp.set_vertical_scale_level(start_y_scaling);
	if (vertical_zoom_enabled()) isp.set_vertical_scale_level(start_y_scaling);
      } else if (label.equals(LABEL_SET_Y_SCALING)) {
	//	int count = (((int) Options.MAX_VERTICAL_SCALE_FACTOR) - start_y_scaling) + 1;
	// problems if set to higher value in subwindow

	int max = (int) get_max_vertical_scale_factor();
	int here = (int) Math.round(isp.get_vertical_scale_level());
	if (here > max) max = here;

	Integer[] values = new Integer[max];

	Integer current = null;
	int j = 0;
	for (int i = 1; i <= max; i++, j++) {
	  values[j] = new Integer(i);
	  if (i == here) current = values[j];
	}
	if (current == null) current = values[0];

	Integer result = (Integer) JOptionPane.showInputDialog(
							       this,
							       "Sample height in pixels:",
							       "Set vertical magnification",
							       JOptionPane.QUESTION_MESSAGE,
							       null,
							       values,
							       current);
	if (result != null) {
	  float value = (float) result.intValue();
	  isp.set_vertical_scale_level(value);
	  if (agp != null) agp.set_vertical_scale_level(value);
	  if (Options.PRESERVE_VERTICAL_SCALING_SETTING) {
	    // start_y_scaling = (int) value;
	    config.enable_vertical_zoom.setValue(false);
	  }
	}

      } else if (label.equals(LABEL_SET_ANNOT_TOOLTIP)) {
	ArrayList columns = config.af.get_sorted_keys();
	String[] cols = new String[columns.size()];
	for (int i=0; i < cols.length; i++) {
	  cols[i] = (String) columns.get(i);
	}
	// fix me: toArray()??

 	String result = (String) JOptionPane.showInputDialog(
 							     this,
 							     "Annotation field to show in tooltip:",
 							     "Tooltip annotation",
 							     JOptionPane.QUESTION_MESSAGE,
 							     null,
 							     cols,
 							     isp.get_tooltip_field());
	if (result != null) {
	  isp.set_tooltip_field(result);
	}
      } else if (label.equals(LABEL_LINK_HEATMAPS)) {
	URLLauncher.launch_modified_url("/cgi-bin/heatmap", "heatmaps");
      } else if (label.equals(LABEL_LINK_CGWB)) {
	String url = "https://cgwb.nci.nih.gov/";
	URLLauncher.launch_url(url, "cgwb");
      } else if (label.equals(LABEL_HELP)) {
	//
	// posted HTML documentation
	//
	URLLauncher.launch_modified_url("/goldenPath/heatmap/documentation/index.html", "heatmap_doc");
      } else {
	System.err.println("UNHANDLED label:" + label);  // debug
      }
    } else {
      System.err.println("UNHANDLED action event!");
    }
  }

  // begin MouseWheelListener stub
  public void mouseWheelMoved(MouseWheelEvent e) {
    if ((e.getModifiers() & MouseEvent.MOUSE_DRAGGED) == 0) {
      // ignore mouse wheel events if a drag is in effect, because
      // button 2 (wheel) is also used for drag-scrolling.
      // if we zoom during a drag scroll the view will change/jump unexpectedly.
      //      System.err.println("e="+e);  // debug
      isp.zoom(
	       (e.getWheelRotation() < 0 ? isp.ZOOM_DIRECTION_IN : isp.ZOOM_DIRECTION_OUT),
	       get_zoom_type(),
	       Options.ZOOM_FACTOR,
	       e.getPoint());
      sync_annotation_zoom();
    }
  }
  // end MouseWheelListener stub



  private JMenuItem add_menuitem(JMenu m, String label, int mnemonic) {
    return add_menuitem(m,label,mnemonic,null);
  }

  private JMenuItem add_menuitem(JMenu m, String label, int mnemonic, KeyStroke accelerator) {
    JMenuItem mi = new JMenuItem(label, mnemonic);
    if (accelerator != null) mi.setAccelerator(accelerator);
    m.add(mi);
    mi.addActionListener(this);
    return(mi);
  }

  private int guess_y_scaling (int y_used) {
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    int sample_count = config.gm.get_row_count();
    
    //    int available = (int) (screen.height * 0.95) - y_used;
    int available = (int) (screen.height * Options.MAX_Y_WINDOW_SIZE_FRACTION) - y_used;
    //    int available = (int) (screen.height * Options.DEFAULT_HEATMAP_VERTICAL_SIZE_FRACTION);
    // HACKY
    int minimum_y_scale = 1;
    int y;

    if ((sample_count * Options.DEFAULT_VERTICAL_PIXELS_PER_SAMPLE) < (available / 3)) {
      // not many samples: even at preferred pixel height, won't fill much of available space
      y = available / sample_count;
    } else {
      for (y=Options.DEFAULT_VERTICAL_PIXELS_PER_SAMPLE; y > minimum_y_scale; y--) {
	int required = sample_count * y;
	//      System.err.println("req for " + y + "=" +required);  // debug
	if (required <= available) break;
      }
    }

    if (y > Options.MAX_VERTICAL_SCALE_FACTOR) y = (int) Options.MAX_VERTICAL_SCALE_FACTOR;

    return y;
  }

  private boolean vertical_zoom_enabled() {
    return config.enable_vertical_zoom.booleanValue();
  }

  private boolean horizontal_zoom_enabled() {
    return config.enable_horizontal_zoom.booleanValue();
  }

  private int get_zoom_type() {
    boolean v = vertical_zoom_enabled();
    boolean h = horizontal_zoom_enabled();
    //    System.err.println("zoom type: h="+h + " v="+v);  // debug

    if (v && h) {
      return isp.ZOOM_AXIS_BOTH;
    } else if (v) {
      return isp.ZOOM_AXIS_VERTICAL;
    } else if (h) {
      return isp.ZOOM_AXIS_HORIZONTAL;
    } else {
      return isp.ZOOM_AXIS_NONE;
    }
  }

  public static InputStream open_url (String u, Hashtable params) throws java.io.IOException {
    // POST-style open
    // thanks to http://www.javaworld.com/javaworld/javatips/jw-javatip34.html
    URL url = new URL(u);

    URLConnection urlConn = url.openConnection();
    // URL connection channel.

    urlConn.setDoInput (true);
    // Let the run-time system (RTS) know that we want input.

    urlConn.setDoOutput (true);
    // Let the RTS know that we want to do output.

    urlConn.setUseCaches (false);
    // No caching, we want the real thing.

    urlConn.setRequestProperty
      ("Content-Type", "application/x-www-form-urlencoded");
    // Specify the content type.

    int count=0;
    StringBuffer sb = new StringBuffer();
    Enumeration e = params.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      String value = (String) params.get(key);
      if (count++ > 0) sb.append("&");
      // parameter separator
      sb.append(key + "=" + URLEncoder.encode(value, "UTF-8"));
    }
    
    System.err.println("POST: " + u + "?" + sb.toString());  // debug

    DataOutputStream printout = new DataOutputStream (urlConn.getOutputStream ());
    // Send POST output.
    printout.writeBytes (sb.toString());
    printout.flush();
    printout.close();

    // Get response data.
    //    DataInputStream result = new DataInputStream (new BufferedInputStream(urlConn.getInputStream()));
    // return result;

    return urlConn.getInputStream();
  }

  private void sync_annotation_zoom() {
    if (isp != null && agp != null) {
      float vscale = isp.get_vertical_scale_level();
      if (agp.get_vertical_scale_level() != vscale) agp.set_vertical_scale_level(vscale);
      JScrollBar jsb_v = jsp_annot.getVerticalScrollBar();
      if (jsb_v != null && jsb_v.getValue() != jsb_v.getMinimum()) {
	// ensure the vertical scrollbar in the annotations panel is 
	// scrolled to the top, so that annotations will line up with
	// the just-scrolled heatmap panel
	jsb_v.setValue(jsb_v.getMinimum());
      }
    }
  }

  
  private void launch_subwindow (ArrayList<Integer> bin_list,
				 Pathway pathway,
				 ArrayList<CombineInfo> combines
				 ) {
    //    GenomicMeasurement gm2 = (GenomicMeasurement) config.gm.generate_subset(bin_list);
    GenomicMeasurement gm2 = (GenomicMeasurement) config.gm.clone();
    gm2.generate_subset(bin_list);
    
    try {
      HeatmapConfiguration hc = new HeatmapConfiguration(config.af,
							 gm2,
							 new GenomicSet(gm2, GenomicSet.STYLE_MARKER_LABEL, null));
      hc.vasari = config.vasari;
      hc.title = config.title;
      hc.exit_on_close = false;
      hc.display_sample_names = true;
      hc.maximum_vertical_scale_factor = new Float(30.0f);
      hc.enable_horizontal_zoom.setValue(false);
      hc.pathway = pathway;
      if (combines != null) {
	//
	//  also combine additional datasets
	//
	Options.INTERACTIVE_RESTRICT_COLUMNS = true;
	for (CombineInfo ci : combines) {
	  String url = ci.get_url();
	  if (url == null) {
	    System.err.println("ERROR: can't combine, no URL");  // debug
	  } else {
	    // OK
	    System.err.println("HEY NOW " + url);  // debug
	    //	    hc.gm_supplemental.add(new GenomicMeasurement(open_simple_url(url), true, Options.DEFAULT_USE_BINARY_SAMPLE_FILE));
	    boolean binary = Options.DEFAULT_USE_BINARY_SAMPLE_FILE;
	    if (true) {
	      System.err.println("FIX ME: forcing binary off");  // debug
	      binary = false;
	    }
	    hc.gm_supplemental.add(new GenomicMeasurement(open_simple_url(url), true, binary));
	  }
	}
      }

      //      this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      // delayed

//       final Heatmap6 myself = this;
//       javax.swing.SwingUtilities.invokeLater(new Runnable() {
// 	  public void run() {
// 	    myself.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
// 	  }
// 	});
      // also delayed
      hc.parent_ref = this;
      Heatmap6 sub_map = new Heatmap6(hc);
    } catch (Exception e) {
      new ErrorReporter(e);
    }
  }

  private float get_max_vertical_scale_factor() {
    if (config.maximum_vertical_scale_factor != null) {
      return config.maximum_vertical_scale_factor.floatValue();
    } else {
      return Options.MAX_VERTICAL_SCALE_FACTOR;
    }
  }

  private void dump_sizes() {
    System.err.println("pref size dump:");  // debug
    System.err.println("  jp_heatmap:"+jp_heatmap.getPreferredSize());  // debug
    System.err.println("  sp_cnc_incr:"+sp_cnc_incr.getPreferredSize());
    if (sp_cnc_any != null) System.err.println("  sp_cnc_any:"+sp_cnc_any.getPreferredSize());
    System.err.println("  csp:"+csp.getPreferredSize());
    System.err.println("  jsb_h:"+jsb_h.getPreferredSize());
    System.err.println("  isp:" + isp.getPreferredSize());  // debug
  }

  private JCheckBoxMenuItem generate_checkbox (String label, BooleanOption state) {
    JCheckBoxMenuItem cb = new JCheckBoxMenuItem(label, state.booleanValue());
    state.set_button(cb);
    return cb;
  }

  private void launch_pathway_subwindow (Pathway p) {
    ArrayList<Integer> wanted = new ArrayList<Integer>();
    ArrayList<String> missing = new ArrayList<String>();
    BinIndex bi = new BinIndex(config.gm);
    for (String marker : p.genes) {
      int index = bi.find(marker);
      if (index == -1) {
	missing.add(marker);
      } else {
	wanted.add(new Integer(index));
      }
    }

    if (wanted.size() > 0) launch_subwindow(wanted, p, null);
  }

  private void launch_selection_subwindow (Rectangle sel) {
    GenomicMeasurement gm2 = (GenomicMeasurement) config.gm.clone();
    gm2.generate_subset(sel);
    
    try {
      HeatmapConfiguration hc = new HeatmapConfiguration(config.af,
							 gm2,
							 new GenomicSet(gm2, GenomicSet.STYLE_MARKER_LABEL, null));
      hc.title = config.title;
      hc.exit_on_close = false;
      Heatmap6 subwin = new Heatmap6(hc);
    } catch (Exception e) {
      new ErrorReporter(e);
    }
  }

  private static InputStream open_simple_url (String u) throws Exception {
    URL url = new URL(u);
    InputStream is = url.openStream();
    if (Options.GZIP_URL_STREAMS) is = new GZIPInputStream(is);
    return is;
  }

  private static boolean binary_option (String s) {
    Integer option = Integer.parseInt(s);
    return option > 0;
  }

  public NavigationControl get_nc() {
    return nc;
  }

  // begin ComponentListener stubs
  public void componentHidden(ComponentEvent e) {}
  public void componentMoved(ComponentEvent e) {}
  public void componentResized(ComponentEvent e) {
    if (laid_out && isp.isVisible()) {
      int sample_count = config.gm.get_row_count();
      int available = isp.getSize().height;
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
      if (isp.get_vertical_scale_level() != y_scale) {
	System.err.println("reset Y scale to " + y_scale);  // debug
	isp.set_min_vertical_scale_level(y_scale);
	isp.set_vertical_scale_level(y_scale);
	repaint();
      } else {
	System.err.println("Y scale reset not needed");  // debug
      }
    }
  }
  public void componentShown(ComponentEvent e) {}
  // end ComponentListener stubs


  
}

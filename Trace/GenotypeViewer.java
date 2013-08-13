package Trace;
// view traces for SNP genotypes
// mne 1/06

// ESSENTIALLY OBSOLETE, UPDATE JGENOTYPEVIEWER INSTEAD!

import java.awt.*;
import java.awt.event.*;

import java.applet.Applet;

import javax.swing.BoxLayout;
import javax.swing.JProgressBar;

import java.util.*;

import java.io.IOException;
import java.net.URL;

import Funk.MultiScrollPanel;
import Funk.Counter;
import Funk.CheckboxMenuGroup;

import java.text.NumberFormat;

public class GenotypeViewer extends Funk.CloseFrame implements ItemListener,ActionListener,MouseWheelListener,AdjustmentListener,Observer {
  
  private static boolean LOCAL_MODE = false;
  private static boolean SINGLE_LOAD = false;

  private static boolean DEFAULT_DYNAMIC_NORMALIZATION = true;
  private static boolean DEFAULT_LOCK_SCROLLING = true;

  private static int PREFERRED_WIDTH_PERCENT = 50;

  private HashMap<String,TracePanel> trace2panel;
  private GenotypeParser gp;
  private MultiScrollPanel msp;
  private Counter progress;
  private TraceZoomer tz = new TraceZoomer();
  private HashSet<String> unique_trace_list;

  private final static String GENOTYPE_ANY = "Any";
  private final static String GENOTYPE_HETEROZYGOUS = "Heterozygous";

  // widgets:
  private Choice snp_chooser;
  private Choice genotype_chooser;

  private MenuItem mi_recenter, mi_quit, mi_about, mi_snap, mi_zoom_in, mi_zoom_out, mi_zoom_reset, mi_zoom_max, mi_zoom_min, mi_download;
  private CheckboxMenuItem cmi_aa,cmi_ls;
  private CheckboxMenuItem cmi_dn_nolimit;

  private Panel controls;
  private Applet applet;
  private Label loading_message;
  private JProgressBar loading_progress;

  private CheckboxMenuGroup cmg_dn;

  public GenotypeViewer(GenotypeParser gp) throws IOException {
    this.gp = gp;
    setup();
  }

  public GenotypeViewer(GenotypeParser gp, Applet a) throws IOException {
    this.gp = gp;
    applet = a;
    setup();
  }
  
  public static void set_window_width_percent (int w) {
    PREFERRED_WIDTH_PERCENT = w;
  }

  public static void main (String [] argv) {
    StreamDelegator.guess_compression();
    StreamDelegator.set_local(true);

    String file = null;

    for (int i = 0; i < argv.length; i++) {
      if (argv[i].equals("-local")) {
        System.err.println("enabling local mode");  // debug
        LOCAL_MODE = true;
        SINGLE_LOAD = true;
        TraceLoader.LOCAL_MODE = true;
      } else if (argv[i].equals("-old")) {
        System.err.println("enabling old genotype.dat format");  // debug
        GenotypeParser.OLD_MODE = true;
      } else if (argv[i].equals("-file")) {
	file = argv[++i];
      }
    }

    if (file == null) {
      if (LOCAL_MODE) {
	// traces stored locally
	// String file = "genotype.dat";
	// file = "genotype_small.dat";
        if (GenotypeParser.OLD_MODE) {
          file = "genotype_fixed.dat";
        } else {
          file = "genotype_small_labeled.dat";
        }
      } else {
	// load traces from trace server
	//        file = "genotype_small_ti.dat";
	//file = "genotype_tiny.dat";
	file = "crash.dat";
	//	file = "crash2.dat";
      }
    }


    GenotypeParser gp = null;
    try {
      System.err.println("data file: " + file);  // debug
      gp = new GenotypeParser(file);
      new GenotypeViewer(gp);
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }

  void setup () throws IOException {
    if (true) {
      TraceCanvas.set_scroll_by_bases(true);
      TraceCanvas.set_fixed_bases_scroll(true);
    }
    
    msp = new MultiScrollPanel();

    trace2panel = new HashMap<String,TracePanel>();

    MenuBar mb = new MenuBar();
    setMenuBar(mb);
    Menu m = new Menu("File");
    m.add(mi_download = new MenuItem("Download traces"));
    m.add(mi_quit = new MenuItem("Exit"));
    mb.add(m);

    m = new Menu("View");
    m.add(mi_recenter = new MenuItem("Recenter"));
    m.add(mi_snap = new MenuItem("Snap"));
    m.add(new MenuItem("-"));

    Menu m2 = new Menu("Zoom");
    m2.add(mi_zoom_in = new MenuItem("In"));
    m2.add(mi_zoom_out = new MenuItem("Out"));
    m2.add(new MenuItem("-"));
    m2.add(mi_zoom_max = new MenuItem("Max in"));
    m2.add(mi_zoom_min = new MenuItem("Max out"));
    m2.add(new MenuItem("-"));
    m2.add(mi_zoom_reset = new MenuItem("Reset"));
    m.add(m2);
    mb.add(m);

    m = new Menu("Options");
    Menu m3 = new Menu("Dynamic normalization");
    m3.add(new MenuItem("Maximum zoom:"));

    cmg_dn = new CheckboxMenuGroup();

    for (int i = 1; i <= 10; i++) {
      String label = "  " + i + "x";
      if (i == 1) label = label.concat("  (disable)");
      CheckboxMenuItem cmi = new CheckboxMenuItem(label, i == 5);
       cmg_dn.add(cmi);
       m3.add(cmi);
       cmi.addItemListener(this);
     }

    m3.add(cmi_dn_nolimit = new CheckboxMenuItem("  unlimited", false));
    cmg_dn.add(cmi_dn_nolimit);
    cmi_dn_nolimit.addItemListener(this);

    m.add(m3);
    
    m.add(cmi_ls = new CheckboxMenuItem("Synchronize trace scrolling", DEFAULT_LOCK_SCROLLING));
    
    m.add(cmi_aa = new CheckboxMenuItem("Antialiasing", true));
    TraceDataView.set_static_auto_normalization(DEFAULT_DYNAMIC_NORMALIZATION);
    mb.add(m);

    m = new Menu("Help");
    m.add(mi_about = new MenuItem("About"));

    mb.add(m);

    mi_recenter.addActionListener(this);
    mi_quit.addActionListener(this);
    mi_about.addActionListener(this);
    mi_snap.addActionListener(this);
    mi_zoom_in.addActionListener(this);
    mi_zoom_out.addActionListener(this);
    mi_zoom_reset.addActionListener(this);
    mi_zoom_max.addActionListener(this);
    mi_zoom_min.addActionListener(this);
    mi_download.addActionListener(this);
    cmi_aa.addItemListener(this);
    //    cmi_dn.addItemListener(this);

    setLayout(new BorderLayout());
    add("Center", msp);

    controls = new Panel();
    controls.addMouseWheelListener(this);

    FlowLayout cpl = new FlowLayout();
    //    cpl.setHgap(0);

    Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
    //    System.err.println("screen size="+ss);  // debug
    int set_width = (int) ((ss.width * PREFERRED_WIDTH_PERCENT) / 100);
    //    System.err.println("set width:" + set_width);  // debug

    if (true) {
      loading_progress = new JProgressBar();
      loading_progress.setString("Loading...");
      loading_progress.setStringPainted(true);
      loading_progress.setMinimum(0);
      loading_progress.setMaximum(100);
      Dimension pref = loading_progress.getPreferredSize();
      pref.width = (int) (set_width * 0.90);
      loading_progress.setPreferredSize(pref);
      controls.add(loading_progress);
    } else {
      loading_message = new Label("Loading...");
      controls.add(loading_message);
    }
    controls.setLayout(cpl);

    snp_chooser = new Choice();
    snp_chooser.addItemListener(this);
    Vector<String> snps = gp.get_snp_ids();
    for (String s : snps) {
      snp_chooser.add(s);
    }

    genotype_chooser = new Choice();
    genotype_chooser.addItemListener(this);

    //    genotype_chooser.add("any");
    //    b_recenter = new Button("Recenter");
    //    b_recenter.addActionListener(this);
    //    controls.add(b_recenter);

    add("North", controls);

    change_snp_view();
    pack();

    setSize(set_width, getSize().height);

    //    setSize(800,600);
    setVisible(true);
    toFront();

    unique_trace_list = gp.get_unique_traces();
    //    System.err.println("unique traces: " + unique_trace_list.size());  // debug

    progress = new Counter(unique_trace_list);

    if (SINGLE_LOAD) {
      // fetch traces one at a time
      TraceLoader tl = new TraceLoader(unique_trace_list, this);
    } else {
      // bulk fetch w/zipfile
      //      System.err.println("loading from zip!");  // debug
      TraceServerClient tsc = new TraceServerClient();
      try {
	tsc.get_traces(unique_trace_list, this);
      } catch (java.io.FileNotFoundException fnf) {
	String error_message = "Error: " + fnf.getMessage();
	new Funk.Message(error_message);
	if (loading_progress != null) 
	  loading_progress.setString(error_message);
      }
    }
  }

  public void change_snp_view () {
    //
    // change view to current SNP.
    //
    String snp_id = snp_chooser.getSelectedItem();

    //
    // reset genotype chooser to alleles observed in this SNP's data:
    //
    GenotypeData gd = gp.get_genotypes_for_snp(snp_id);
    genotype_chooser.removeAll();
    genotype_chooser.add(GENOTYPE_ANY);
    HashSet alleles = gd.get_alleles();
    for (Iterator i = alleles.iterator(); i.hasNext(); ) {
      String allele = (String) i.next();
      genotype_chooser.add(allele);
    }
    genotype_chooser.add(GENOTYPE_HETEROZYGOUS);

    
    //    for (String allele : gd.get_alleles()) {
    //genotype_chooser.add(allele);
    //    }

    set_view();
  }
  
  public void set_view () {
    // determine set of visible traces based on current restrictions
    // (selected SNP and allele)
    String snp_id = snp_chooser.getSelectedItem();
    GenotypeData gd = gp.get_genotypes_for_snp(snp_id);

    recenter_all();

    //
    // reset trace panel:
    //

    msp.removeAll();
    // gross, but useful in case traces are loaded out of genotype.dat order
    // (will be restored below)

    String current_genotype = genotype_chooser.getSelectedItem();

    TracePanel tp;
    //    for (GenotypeTrace gt : gd.traces) {
    for (Iterator i = gd.traces.iterator(); i.hasNext(); ) {
      GenotypeTrace gt = (GenotypeTrace) i.next();
      // preserve order from genotype.dat
      if ((tp = trace2panel.get(gt.trace_id)) != null) {
        if (current_genotype.equals(GENOTYPE_HETEROZYGOUS) ?
            gt.is_heterozygous() :
            (current_genotype.equals(GENOTYPE_ANY) ||
             gt.contains_allele(current_genotype))) {
	  // visibility check
	  msp.add(tp);
	}
      }
    }
    msp.validate();
  }

  public void recenter_all () {
    String snp_id = snp_chooser.getSelectedItem();
    GenotypeData gd = gp.get_genotypes_for_snp(snp_id);
    TracePanel tp;
    //    for (GenotypeTrace gt : gd.traces) {
    for (Iterator i = gd.traces.iterator(); i.hasNext(); ) {
      GenotypeTrace gt = (GenotypeTrace) i.next();
      // center each trace for SNP on appropriate position
      if ((tp = trace2panel.get(gt.trace_id)) != null) {
	tp.center_on(gt.trace_offset, true);
      }
    }    
  }

  // begin ItemListener stub
  public void itemStateChanged(ItemEvent e) {
    Object source = e.getSource();
    if (source.equals(snp_chooser)) {
      // new SNP selected
      change_snp_view();
    } else if (source.equals(genotype_chooser)) {
      // genotype restricted within current SNP
      set_view();
    } else if (source.equals(cmi_aa)) {
      TraceCanvas.set_antialiasing(cmi_aa.getState());
      msp.validate();
      //    } else if (source.equals(cmi_dn)) {
      //      TraceDataView.set_static_auto_normalization(cmi_dn.getState());
      //      msp.validate();
    } else if (source.equals(cmi_dn_nolimit)) {
      TraceDataView.set_static_auto_normalization(true);
      TraceDataView.set_static_auto_normalization_limit(0);
      msp.validate();
    } else if (source instanceof CheckboxMenuItem &&
               cmg_dn.contains((CheckboxMenuItem) source)) {
      CheckboxMenuItem selected = (CheckboxMenuItem) source;
      String label = selected.getLabel();
      String amt = Funk.Str.trim_whitespace(label.substring(0, label.indexOf("x")));
      
      Double factor = Double.parseDouble(amt);
      if (factor == 1.0) {
	TraceDataView.set_static_auto_normalization(false);
      } else {
	TraceDataView.set_static_auto_normalization(true);
	TraceDataView.set_static_auto_normalization_limit(factor);
      }
      msp.validate();
    } else {
      System.err.println("unhandled: " + e);  // debug
    }
  }
  // end ItemListener stub

  // begin ActionListener stub
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source.equals(mi_recenter)) {
      // recenter traces in current view
      recenter_all();
    } else if (source.equals(mi_quit)) {
      setVisible(false);
      dispose();
    } else if (source.equals(mi_about)) {
      new GenotypeViewerCredits();
    } else if (source.equals(mi_snap)) {
      msp.snap(false);
    } else if (source.equals(mi_zoom_in)) {
      tz.zoom(-2);
      msp.validate();
    } else if (source.equals(mi_zoom_out)) {
      tz.zoom(2);
      msp.validate();
    } else if (source.equals(mi_zoom_max)) {
      tz.zoom_to_maximum();
      msp.validate();
    } else if (source.equals(mi_zoom_min)) {
      tz.zoom_to_minimum();
      msp.validate();
    } else if (source.equals(mi_zoom_reset)) {
      tz.reset();
      msp.validate();
    } else if (source.equals(mi_download)) {
      download_traces();
    } else {
      System.err.println("unhandled: " + e);  // debug
    }
  }
  // end ActionListener stub

  public void update (Observable o, Object arg) {
    TraceFile t = null;
    //    System.err.println("o=" +o);  // debug

    if (o instanceof TraceLoader) {
      TraceLoader tl = (TraceLoader) o;
      t = (TraceFile) arg;
    } else if (o instanceof TraceServerClient) {
      t = (TraceFile) arg;
    } else {
      System.err.println("unhandled update!");  // debug
    }

    if (t != null) {
      //      System.err.println("loaded: " + t.name);  // debug
      progress.next();
      stash_panel(t);
      int complete = progress.get_percent_complete();
      String msg = "Loading (" + complete + "%)...";
      if (progress.complete()) {
	// FIX ME: what if never finished due to crash?
        controls.removeAll();
        controls.add(new Label("SNP:"));
        controls.add(snp_chooser);
        controls.add(new Label("allele:"));
        controls.add(genotype_chooser);
	controls.validate();
	set_view();
	msp.snap(true);
        toFront();
      } else if (loading_message != null) {
        System.err.println(msg);  // debug
        loading_message.setText(msg);
        controls.validate();
      } else if (loading_progress != null) {
        loading_progress.setValue(complete);
        loading_progress.setString(msg);
        controls.validate();
      }
    }
  }
  
  private void stash_panel (TraceFile t) {
    //    System.err.println("stash start");  // debug
    String orientation = gp.get_orientation_for(t.name);
    //    System.err.println("orientation for " + t.name + "=" + orientation);  // debug

    Panel info = new Panel();
    FlowLayout bl = new FlowLayout();
    bl.setAlignment(FlowLayout.LEFT);
    info.setLayout(bl);

    GenotypeTrace gt = gp.get_genotypetrace_for(t.name);
    //      info.add(new Label(t.name + " (" + orientation + ")"));
    String label = gt.trace_label;
    if (label == null) {
      label = gt.trace_id + " (" + gt.orientation + ")";
    }
    info.add(new Label(label));
    if (orientation.equals("-")) t.reverse_complement();

    TracePanel tp;
    if (true) {
      tp = new TracePanel(t, label);
    } else {
      tp = new TracePanel(t, info);
    }

    Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
    int preferred_width = ss.width;
    // HACK

    //int preferred_panel_height = (int) (ss.height * 0.23);
    int preferred_panel_height = (int) (ss.height * 0.22);

    tp.setPreferredSize(new Dimension(preferred_width, preferred_panel_height));
    tp.setMinimumSize(new Dimension(preferred_width, preferred_panel_height));
    tp.setMaximumSize(new Dimension(ss.width, preferred_panel_height));

    Scrollbar sb = tp.get_scrollbar();
    sb.addAdjustmentListener(this);

    trace2panel.put(t.name, tp);
  }

  // begin MouseWheelListener stub
  public void mouseWheelMoved(MouseWheelEvent e) {
    int rotation = e.getWheelRotation();
    tz.zoom(rotation);
    repaint_all();
  }
  // end MouseWheelListener stub

  private void repaint_all() {
    for (TracePanel tp : trace2panel.values()) {
      tp.repaint();
    }
    msp.validate();
    // force redraw
  }

  private void download_traces() {
    TraceServerClient tsc = new TraceServerClient();
    URL url = gp.get_url();
    URL dl = tsc.get_download_url(unique_trace_list,
				  url == null ? "" : url.toString());
    System.err.println(dl.toString());  // debug
    if (applet == null) {
      new Funk.Message("can't launch download URL: no applet", "OK");
    } else {
      applet.getAppletContext().showDocument(dl, "Download");
    }
  }

  // begin AdjustmentListener stubs
  public void adjustmentValueChanged(AdjustmentEvent e) {
    if (cmi_ls.getState()) {

      //      System.err.println(e);  // debug
      Scrollbar source_sb = (Scrollbar) e.getSource();
      Scrollbar sb;
      int abs_delta = 0;

      for (TracePanel tp : trace2panel.values()) {
	sb = tp.get_scrollbar();
	if (sb.equals(source_sb)) {
	  abs_delta = tp.get_bases_delta_since_start();
	  break;
	}
      }

      for (TracePanel tp : trace2panel.values()) {
	sb = tp.get_scrollbar();
	if (!sb.equals(source_sb)) {
	  //	  tp.mimic_AdjustmentEvent(e, delta);
	  // nifty, but:
	  //  - breaks in edge cases (if a trace edge is encountered
	  //    scrolling queue will be broken)
	  //  - thread synchronization/race condition problems?

	  //	System.err.println("passing scrolling to " + tp);  // debug
	  tp.apply_absolute_delta(abs_delta);
	  // better: track absolute difference in nt from start
	  // and current positions
	}
      }
    }
  }
  // end AdjustmentListener stubs

}


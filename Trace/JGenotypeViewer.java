package Trace;
// view traces for SNP genotypes
// mne 1/06

//import java.awt.*;
import java.awt.event.*;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Toolkit;

import java.applet.Applet;

import javax.swing.*;

import java.util.*;

import java.io.IOException;
import java.net.URL;

import Funk.JMultiScrollPanel;
import Funk.Counter;
import Funk.MenuBuilder;
import Funk.LookAndFeeler;

import java.text.NumberFormat;

public class JGenotypeViewer extends JFrame implements ItemListener,ActionListener,MouseWheelListener,AdjustmentListener,Observer,MouseListener {
  
  private static boolean LOCAL_MODE = false;
  private static boolean SINGLE_LOAD = false;

  private static boolean DEFAULT_DYNAMIC_NORMALIZATION = true;
  private static boolean DEFAULT_LOCK_SCROLLING = true;

  private static int PREFERRED_WIDTH_PERCENT = 50;

  private static final boolean HIDE_COMPONENTS_DURING_LOAD = true;

  private HashMap<String,JTracePanel> trace2panel;
  private GenotypeParser gp;
  private JMultiScrollPanel msp;
  private Counter progress;
  private TraceZoomer tz = new TraceZoomer();
  private HashSet<String> unique_trace_list;

  private final static String GENOTYPE_ANY = "Any";
  private final static String GENOTYPE_HETEROZYGOUS = "Heterozygous";

  // widgets:
  private JComboBox snp_chooser, genotype_chooser;
  private JCheckBoxMenuItem cmi_ls;

  private JPanel controls;
  private Applet applet;
  private JLabel loading_message;
  private JProgressBar loading_progress;

  private ButtonGroup bg_normalization;

  private JScrollBar active_scrollbar = null;

  public JGenotypeViewer(GenotypeParser gp) throws IOException {
    this.gp = gp;
    setup();
  }

  public JGenotypeViewer(GenotypeParser gp, Applet a) throws IOException {
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
      } else if (argv[i].equals("-hp")) {
        // height percent
        Funk.JMultiScrollPanel.set_window_height_percent(Integer.parseInt(argv[++i]));
      } else if (argv[i].equals("-wp")) {
        // width percent
        JGenotypeViewer.set_window_width_percent(Integer.parseInt(argv[++i]));
      } else {
        System.err.println("unknown option " + argv[i]);  // debug
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
      }
    }


    GenotypeParser gp = null;
    try {
      System.err.println("data file: " + file);  // debug
      gp = new GenotypeParser(file);
      new JGenotypeViewer(gp);
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }

  private void setup () throws IOException {
    setTitle("Genotype viewer");
    Funk.LookAndFeeler.set_native_lookandfeel();

    if (applet == null) setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    // illegal in applet mode

    if (true) {
      JTraceCanvas.set_scroll_by_bases(true);
      JTraceCanvas.set_fixed_bases_scroll(true);
    }
    
    msp = new JMultiScrollPanel();

    trace2panel = new HashMap<String,JTracePanel>();
    
    MenuBuilder mb = new MenuBuilder(this, this);
    mb.start_menu("File", KeyEvent.VK_F);
    mb.add("Download traces", KeyEvent.VK_D);
    mb.add("Exit", KeyEvent.VK_X);

    mb.start_menu("View", KeyEvent.VK_V);
    mb.add("Recenter", KeyEvent.VK_C);
    mb.add("Snap", KeyEvent.VK_N);
    mb.add(new JSeparator());

    mb.start_submenu("Zoom", KeyEvent.VK_Z);
    mb.add("In");
    mb.add("Out");
    mb.add(new JSeparator());
    mb.add("Max in");
    mb.add("Max out");
    mb.add(new JSeparator());
    mb.add("Reset");
    mb.end_submenu();

    mb.start_menu("Options", KeyEvent.VK_O);
    mb.start_submenu("Dynamic normalization", KeyEvent.VK_D);
    mb.add("Maximum zoom:");

    bg_normalization = new ButtonGroup();

    char zero = '0';

    for (int i = 1; i <= 10; i++) {
      //      String label = "  " + i + "x";
      String label = i + "x";
      if (i == 1) label = label.concat("  (disable)");
      JRadioButtonMenuItem rbm = new JRadioButtonMenuItem(label, i == 5);
      bg_normalization.add(rbm);
      if (i < 10) {
        mb.add(rbm, (int) '0' + i);
        // javadocs:
        // VK_0 thru VK_9 are the same as ASCII '0' thru '9' (0x30 - 0x39)
      } else {
        mb.add(rbm);
      }
    }

    JMenuItem jmi_ul = new JRadioButtonMenuItem("unlimited", false);
    mb.add(jmi_ul);
    bg_normalization.add(jmi_ul);

//     m3.add(cmi_dn_nolimit = new JCheckBoxMenuItem("  unlimited", false));

    mb.end_submenu();
    
    cmi_ls = new JCheckBoxMenuItem("Synchronize trace scrolling", DEFAULT_LOCK_SCROLLING);
    cmi_ls.setMnemonic(KeyEvent.VK_S);
    mb.add(cmi_ls);

    JCheckBoxMenuItem cmi = new JCheckBoxMenuItem("Antialiasing", true);
    cmi.setMnemonic(KeyEvent.VK_A);
    mb.add(cmi);

    TraceDataView.set_static_auto_normalization(DEFAULT_DYNAMIC_NORMALIZATION);

    new LookAndFeeler(this, mb);


    mb.start_menu("Help", KeyEvent.VK_H);
    mb.add("About", KeyEvent.VK_A);

    setLayout(new BorderLayout());
    add("Center", msp);

    controls = new JPanel();
    controls.addMouseWheelListener(this);

    FlowLayout cpl = new FlowLayout();
    //    cpl.setHgap(0);

    msp.set_preferred_width_percent(PREFERRED_WIDTH_PERCENT);

    loading_progress = new JProgressBar();
    loading_progress.setString("Loading...");
    loading_progress.setStringPainted(true);
    loading_progress.setMinimum(0);
    loading_progress.setMaximum(100);
    Dimension pref = loading_progress.getPreferredSize();
    pref.width = (int) (get_preferred_width() * 0.90);
    loading_progress.setPreferredSize(pref);
    controls.add(loading_progress);

    controls.setLayout(cpl);

    Vector<String> snps = gp.get_snp_ids();
    snp_chooser = new JComboBox(snps);
    snp_chooser.addItemListener(this);

    genotype_chooser = new JComboBox();
    genotype_chooser.addItemListener(this);

    //    genotype_chooser.add("any");
    //    b_recenter = new Button("Recenter");
    //    b_recenter.addActionListener(this);
    //    controls.add(b_recenter);

    add("North", controls);

    change_snp_view();

    if (HIDE_COMPONENTS_DURING_LOAD) {
      //
      // hide all components except progress bar until loading complete
      //
      getJMenuBar().setVisible(false);
      msp.setVisible(false);
    }

    pack();
    set_width_hack();

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
    String snp_id = (String) snp_chooser.getSelectedItem();

    //
    // reset genotype chooser to alleles observed in this SNP's data:
    //
    GenotypeData gd = gp.get_genotypes_for_snp(snp_id);
    genotype_chooser.removeAllItems();
    genotype_chooser.addItem(GENOTYPE_ANY);
    HashSet alleles = gd.get_alleles();
    for (Iterator i = alleles.iterator(); i.hasNext(); ) {
      String allele = (String) i.next();
      genotype_chooser.addItem(allele);
    }
    genotype_chooser.addItem(GENOTYPE_HETEROZYGOUS);
    
    //    for (String allele : gd.get_alleles()) {
    //genotype_chooser.add(allele);
    //    }

    set_view();
  }
  
  public void set_view () {
    // determine set of visible traces based on current restrictions
    // (selected SNP and allele)
    String snp_id = (String) snp_chooser.getSelectedItem();
    GenotypeData gd = gp.get_genotypes_for_snp(snp_id);

    recenter_all();

    //
    // reset trace panel:
    //

    msp.removeAll();
    // gross, but useful in case traces are loaded out of genotype.dat order
    // (will be restored below)

    String current_genotype = (String) genotype_chooser.getSelectedItem();

    JTracePanel tp;
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
    String snp_id = (String) snp_chooser.getSelectedItem();
    GenotypeData gd = gp.get_genotypes_for_snp(snp_id);
    JTracePanel tp;
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
    } else {
      System.err.println("unhandled: " + e);  // debug
    }
  }
  // end ItemListener stub

  // begin ActionListener stub
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    Object source = e.getSource();
    // basing commands off of strings is ugly, but at least we don't
    // have to define variables for each component
    //    System.err.println(cmd);
    boolean need_refresh = false;
    if (cmd.equals("Download traces")) {
      download_traces();
    } else if (cmd.equals("Exit")) {
      setVisible(false);
      dispose();
    } else if (cmd.equals("Recenter")) {
      recenter_all();
    } else if (cmd.equals("Snap")) {
      msp.snap(false);
    } else if (cmd.equals("Antialiasing")) {
      JTraceCanvas.set_antialiasing(get_jcheckbox_status(source));
      need_refresh = true;
    } else if (cmd.equals("In")) {
      tz.zoom(-2);
      need_refresh = true;
    } else if (cmd.equals("Out")) {
      tz.zoom(2);
      need_refresh = true;
    } else if (cmd.equals("Max in")) {
      tz.zoom_to_maximum();
      need_refresh = true;
    } else if (cmd.equals("Max out")) {
      tz.zoom_to_minimum();
      need_refresh = true;
    } else if (cmd.equals("Reset")) {
      tz.reset();
      need_refresh = true;
    } else if (cmd.equals("About")) {
      new JGenotypeViewerCredits();
    } else if (Funk.Misc.contains(bg_normalization, source)) {
      //
      // item in normalization button group
      //
      if (cmd.equals("unlimited")) {
        TraceDataView.set_static_auto_normalization(true);
        TraceDataView.set_static_auto_normalization_limit(0);
      } else {
        String amt = Funk.Str.trim_whitespace(cmd.substring(0, cmd.indexOf("x")));
        Double factor = Double.parseDouble(amt);
        if (factor == 1.0) {
          TraceDataView.set_static_auto_normalization(false);
        } else {
          TraceDataView.set_static_auto_normalization(true);
          TraceDataView.set_static_auto_normalization_limit(factor);
        }
      }
      need_refresh = true;
    } else {
      //      System.err.println("unhandled event: " + cmd);  // debug
    }
    
    if (need_refresh) {
      msp.invalidate();
      repaint();
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
        controls.add(new JLabel("SNP:"));
        controls.add(snp_chooser);
        controls.add(new JLabel("allele:"));
        controls.add(genotype_chooser);
	//	controls.validate();
	controls.invalidate();

	set_view();

        if (HIDE_COMPONENTS_DURING_LOAD) {
          //
          // restore visibility of components hidden during loading process
          //
          getJMenuBar().setVisible(true);

          msp.setVisible(true);
          msp.snap(true);
          //          pack();

          set_width_hack();
          // UGH: have to re-set the width because 

        } else {
          msp.snap(true);
        }

        toFront();
	repaint();
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

    JPanel info = new JPanel();
    FlowLayout bl = new FlowLayout();
    bl.setAlignment(FlowLayout.LEFT);
    info.setLayout(bl);

    GenotypeTrace gt = gp.get_genotypetrace_for(t.name);
    //      info.add(new JLabel(t.name + " (" + orientation + ")"));
    String label = gt.trace_label;
    if (label == null) {
      label = gt.trace_id + " (" + gt.orientation + ")";
    }
    info.add(new JLabel(label));
    if (orientation.equals("-")) t.reverse_complement();

    JTracePanel tp;
    if (true) {
      tp = new JTracePanel(t, label);
    } else {
      tp = new JTracePanel(t, info);
    }

    Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
    int preferred_width = ss.width;
    // HACK

    //int preferred_panel_height = (int) (ss.height * 0.23);
    int preferred_panel_height = (int) (ss.height * 0.22);

    tp.setPreferredSize(new Dimension(preferred_width, preferred_panel_height));
    tp.setMinimumSize(new Dimension(preferred_width, preferred_panel_height));
    tp.setMaximumSize(new Dimension(ss.width, preferred_panel_height));

    JScrollBar sb = tp.get_scrollbar();

    //    System.err.println("FIX ME: scroll events not monitored");  // debug
    sb.addAdjustmentListener(this);
    sb.addMouseListener(this);

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
    for (JTracePanel tp : trace2panel.values()) {
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
    JScrollBar source_sb = (JScrollBar) e.getSource();
    //    System.err.println("event:" + e.getID());  // debug

    if (cmi_ls.getState() &&
        //
        //  synchronized scrolling enabled
        //
        active_scrollbar != null &&
        source_sb.equals(active_scrollbar)
        // if this AdjustmentEvent came from the scrollbar the user
        // last interacted with, propagate that change to the other
        // panels.  If we don't restrict processing to the active scrollbar,
        // an endless loop of AdjustmentEvents will be created
        // (each propagation will trigger another call to this method).
        //
        // Fugly.  Can we tell somehow from the AdjustmentEvent that 
        // it came from a user action, rather than a code-based adjustment?

        ) {

      JScrollBar sb;
      int abs_delta = 0;

      for (JTracePanel tp : trace2panel.values()) {
	sb = tp.get_scrollbar();
	if (sb.equals(source_sb)) {
	  abs_delta = tp.get_bases_delta_since_start();
	  break;
	}
      }

      for (JTracePanel tp : trace2panel.values()) {
        sb = tp.get_scrollbar();
        if (!sb.equals(source_sb)) {
          //
          // don't apply delta to origin panel
          //

          //	  tp.mimic_AdjustmentEvent(e, delta);
          // nifty, but:
          //  - breaks in edge cases (if a trace edge is encountered
          //    scrolling queue will be broken)
          //  - thread synchronization/race condition problems?

          tp.apply_absolute_delta(abs_delta);
          // better: track absolute difference in nt from start
          // and current positions
        }
      }

    }
  }
  // end AdjustmentListener stubs

  private boolean get_jcheckbox_status (Object o) {
    return ((JCheckBoxMenuItem) o).getState();
  }

  // begin MouseListener stubs
  public void mousePressed(MouseEvent e) {}
  public void mouseClicked(MouseEvent e) {};
  public void mouseReleased(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {
    active_scrollbar = (JScrollBar) e.getSource();
  };
  public void mouseExited(MouseEvent e) {
    active_scrollbar = null;
  };
  // end MouseListener stubs


  private int get_preferred_width () {
    Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
    return (int) ((ss.width * PREFERRED_WIDTH_PERCENT) / 100);
  }
  
  private void set_width_hack () {
    Dimension pref = new Dimension(get_preferred_width(),
                                   getSize().height);
    setSize(pref);

    // Q: does a preferred value of zero indicate deference to
    //    other components specify a value greater than zero?
    // A: no!
    //    pref.width = 0;
    //    setPreferredSize(pref);


  }

}


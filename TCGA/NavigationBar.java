package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class NavigationBar extends JPanel implements Formatter {
  private static final String LABEL_OK = "Go";

  private JComboBox jc_type, jc_what;
  private JCheckBox cb_sort,cb_zoom;

  private BinIndex bin_index;

  private GenomicMeasurement gm;
  private GenomicSet gs;

  private PathwayGenes pathway_genes;
  private Observable2 obs;

  private Pathway last_pathway;
  private HashSet<String> markers,pathways,samples;
  private HeatmapConfiguration config;

  private static String TYPE_MARKER = "Marker";
  private static String TYPE_PATHWAY = "Pathway";
  private static String TYPE_SAMPLE = "Sample";
  private static String TYPE_GENOMIC_REGION = "Genomic region";
  private static String TYPE_ALL = "All";
  private static String TYPE_MARKER_SET = "Marker list";

  private static String TYPE_SEPARATOR = "- - - - - - - - -";
  // HACK: use a JSeparator, but this apparently requires a custom cell renderer, etc.

  private JComboBoxToolTipHelper tth;

  public NavigationBar (HeatmapConfiguration config) {
    this.config = config;
    this.gm = config.gm;
    this.gs = config.gs;
    setup();
  }
  
  private void setup() {
    obs = new Observable2();
    pathway_genes = new PathwayGenes();
    pathway_genes.sleep_until_ready();
    // hack and the hacktones

    bin_index = new BinIndex(gm);

    markers = new HashSet<String>(bin_index.get_unique_list());
    pathways = new HashSet<String>(get_pathway_labels());
    samples = new HashSet<String>(gm.get_visible_sample_ids());

    //
    //  single bin location combo box:
    //
    
    //    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    JLabel label;

    JButton jb;

    add(jb = new JButton("Background"));
    jb.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  toggle_background_colors();
	}
      });


    add(new JToolBar.Separator());
    add(new JToolBar.Separator());
    add(new JToolBar.Separator());
    add(new JToolBar.Separator());

    add(label = new JLabel("Search for:", JLabel.TRAILING));
    Vector types = new Vector();
    types.add(TYPE_ALL);
    types.add(TYPE_MARKER);
    types.add(TYPE_PATHWAY);
    types.add(TYPE_SAMPLE);
    //    types.add(new JSeparator());
    types.add(TYPE_SEPARATOR);

    if (gm.is_genome_formatted()) {
      types.add(TYPE_GENOMIC_REGION);
    }

    types.add(TYPE_MARKER_SET);
    add(jc_type = new JComboBox(new Vector(types)));

    jc_type.addActionListener(
			      new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				  search_box_setup();
				}
			      }
			      );

    add(jc_what = new JComboBox());
    jc_what.setPrototypeDisplayValue("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
    // hack: use to set size of combo box so it doesn't change if we change its elements
    tth = new JComboBoxToolTipHelper(jc_what, this);

    jc_what.addKeyListener(
			   new KeyListener() {
			     public void keyPressed(KeyEvent ke) {
			       if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
				 handle_result();
			       }
			     }
			     public void keyReleased(KeyEvent ke) {};
			     public void keyTyped(KeyEvent ke) {};
			   }
			   );

    jc_what.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {

	  String wanted_result = (String) jc_what.getSelectedItem();
	  if (pathways.contains(wanted_result)) {
	    // if the selected item is a pathway, save gene list
	    last_pathway = pathway_genes.get_pathway(wanted_result);
	  }

	  String wanted_type = (String) jc_type.getSelectedItem();
	  if (wanted_type.equals(TYPE_GENOMIC_REGION) ||
	      wanted_type.equals(TYPE_MARKER_SET)) {
	    // only respond to action events if editing a genomic region.
	    // Otherwise result will fire simply by choosing an item from the combo box.
	    handle_result();
	  }
	}
      });

    add(cb_zoom = new JCheckBox("Zoom", Options.NAVIGATION_DEFAULT_ZOOM));
    cb_zoom.setToolTipText("Zoom the viewer to the selected item when navigating to a marker, sample, or region.  Pathways will open in a new window.");

    add(cb_sort = new JCheckBox("Sort", false));
    cb_sort.setToolTipText("When navigating to a marker, sort the view by the data at the marker position.");

    jb = new JButton(LABEL_OK);
    add(jb);
    jb.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  handle_result();
	}
      });

    search_box_setup();

  }

  private Vector get_pathway_labels() {
    Vector pathway_labels = new Vector();
    for (String s : pathway_genes.get_pathway_names()) {
      pathway_labels.add(s);
    }
    return pathway_labels;
  }
  
  public void addObserver (Observer o) {
    obs.addObserver(o);
  }
  
  private void search_box_setup() {
    // populate search combo box
    Vector combo_data = null;
    String wanted_type = (String) jc_type.getSelectedItem();

    Vector marker_labels = new Vector(bin_index.get_unique_list());
    Vector pathway_labels = get_pathway_labels();
    Vector sample_labels = new Vector(gm.get_visible_sample_ids());
    
    boolean zoom_enabled = true;
    boolean sort_enabled = true;

    if (wanted_type.equals(TYPE_MARKER)) {
      combo_data = marker_labels;
    } else if (wanted_type.equals(TYPE_PATHWAY)) {
      combo_data = pathway_labels;
      zoom_enabled = sort_enabled = false;
    } else if (wanted_type.equals(TYPE_SAMPLE)) {
      combo_data = sample_labels;
      zoom_enabled = sort_enabled = false;
    } else if (wanted_type.equals(TYPE_GENOMIC_REGION)) {
      combo_data = new Vector();
      combo_data.add("chr7:3000000-10000000");
      zoom_enabled = true;
      sort_enabled = false;
    } else if (wanted_type.equals(TYPE_MARKER_SET)) {
      combo_data = new Vector();
      if (last_pathway != null) {
	combo_data.add(Funk.Str.join(" ", last_pathway.genes.iterator()));
      }
      combo_data.add("");
      zoom_enabled = false;
      sort_enabled = false;
    } else if (wanted_type.equals(TYPE_ALL)) {
      // everything
      combo_data = new Vector();
      combo_data.addAll(marker_labels);
      combo_data.addAll(pathway_labels);
      combo_data.addAll(sample_labels);
    } else {
      System.err.println("ERROR, unknown filter subtype");  // debug
    }

    if (wanted_type.equals(TYPE_GENOMIC_REGION)) {
      tth.setToolTipText("enter genomic region in format chrX:start-end, e.g. \"chr7:3000000-100000000\"");
      jc_what.setEditable(true);
    } else if (wanted_type.equals(TYPE_MARKER_SET)) {
      tth.setToolTipText("Enter a list of marker names, separated by spaces.  Results will open in a new window.");
      jc_what.setEditable(true);
    } else {
      tth.setToolTipText(null);
      jc_what.setEditable(false);
    }

    cb_zoom.setEnabled(zoom_enabled);
    cb_sort.setEnabled(sort_enabled);

    if (combo_data != null) {
      DefaultComboBoxModel dcbm = new DefaultComboBoxModel(combo_data);
      jc_what.setModel(dcbm);
      Dimension d = jc_what.getMinimumSize();

      d.width = 50;
      jc_what.setMinimumSize(d);
      // doesn't seem to help
    }
  }

  private void handle_result() {
    String wanted_type = (String) jc_type.getSelectedItem();
    String wanted_result = (String) jc_what.getSelectedItem();
    
    NavigationRequest request = new NavigationRequest();
    request.wants_zoom = cb_zoom.isSelected();
    request.wants_sort = cb_sort.isSelected();
    
    if (wanted_type.equals(TYPE_GENOMIC_REGION)) {
      // UCSC format
      // chr7:127,471,196-127,495,720
      String[] f = wanted_result.split(":");
      boolean parse_error = true;
      String error_msg = "Please specify genomic search coordinates in the format chrX:start-end, e.g. \"chr7:1-30000000\"";

      int[] positions = {-1, -1};

      int ok = 0;
      if (f.length == 2) {
	String[] pos = f[1].split("-");
	if (pos.length == 2) {
	  for(int i=0; i < 2; i++) {
	    try {
	      positions[i] = Integer.parseInt(pos[i]);
	      ok++;
	    } catch (NumberFormatException e) {}
	  }

	  if (gs == null) {
	    System.err.println("error: no GenomicSet");  // debug
	  } else {
	    int chr = gs.chr2int(f[0]);

	    if (ok == 2 && chr > 0) {
	      // start and end positions and chromosome parsed successfully
	      int start_bin = gs.get_bin_for(chr, positions[0]);
	      int end_bin = gs.get_bin_for(chr, positions[1]);
	      if (start_bin == -1 || end_bin == -1) {
		error_msg = "Can't find genomic positions; is this not a genomically-formatted dataset?";
	      } else {
		request.selection = gm.generate_selection(start_bin,
							  (end_bin - start_bin) + 1);
		parse_error = false;
	      }
	    }
	  } 
	}
      }

      if (parse_error) {
	JOptionPane.showMessageDialog(this,
				      error_msg,
				      "Formatting error",
				      JOptionPane.ERROR_MESSAGE);	  
      }
    } else if (wanted_type.equals(TYPE_MARKER_SET)) {
      String[] markers = wanted_result.split("\\s+");
      ArrayList<Integer> wanted = new ArrayList<Integer>();
      ArrayList<String> missing = new ArrayList<String>();
      for(int i=0; i < markers.length; i++) {
	String marker = markers[i];
	int index = bin_index.find(markers[i]);
	if (index == -1) {
	  missing.add(marker);
	} else {
	  wanted.add(new Integer(index));
	}
      }
      if (missing.size() > 0) {
	System.err.println("MISSING:"+missing.toString());  // debug
	int missing_count = missing.size();

	JOptionPane.showMessageDialog(this,
				      "Can't find " + missing_count +
				      " marker" + (missing_count == 1 ? "" : "s") + 
				      " " + missing.toString(),
				      "Warning",
				      JOptionPane.WARNING_MESSAGE
				      );
      }
	
      if (wanted.size() > 0) {
	request.bin_index_list = wanted;
      }

    } else {
      if (markers.contains(wanted_result)) {
	request.marker = wanted_result;
	request.bin_index = new Integer(bin_index.find(wanted_result));
	//      request.selection = gm.generate_selection(bin_index.find(wanted_result));
      } else if (pathways.contains(wanted_result)) {
	request.pathway = pathway_genes.get_pathway(wanted_result);
      } else if (samples.contains(wanted_result)) {
	request.sample = wanted_result;
	request.selection = gm.generate_selection(wanted_result);
      } else {
	System.err.println("handle_result(): ERROR, unhandled result");  // debug
      }
    }
    
    if (request != null) {
      obs.setChanged();
      obs.notifyObservers(request);
    }
  }

  public static void main (String [] argv) {
    Funk.LookAndFeeler.set_native_lookandfeel();
    try {
      //      GenomicMeasurement gm = new GenomicMeasurement("snp6_genomicmeasurement_broad_updated_cn.txt", false);
      //      GenomicMeasurement gm = new GenomicMeasurement("test.txt", false);
      GenomicMeasurement gm = new GenomicMeasurement("carl.txt", false);
      System.err.println("fix me");  // debug

      //      NavigationBar nb = new NavigationBar(gm, null);
      JFrame jf = new JFrame();
      jf.setLayout(new BorderLayout());
      //      jf.add("Center", nb);
      jf.pack();
      jf.setVisible(true);
    } catch (Exception e) {
      System.err.println(e);  // debug
    }
  }


  public String format (String v) {
    String label = null;
    if (markers.contains(v)) {
      label = "marker";
    } else if (pathways.contains(v)) {
      label = "pathway";
    } else if (samples.contains(v)) {
      label = "sample";
    }

    return label == null ? v : v + " (" + label + ")";
  }

  private void toggle_background_colors() {
    config.white_mode = !config.white_mode;
    ColorManager cm = gm.get_color_manager();
    ArrayList<ColorScheme> schemes = cm.get_all_color_schemes();
    for (ColorScheme cs : schemes) {
      ColorSchemeModel csm = cs.get_colorscheme_model();
      csm.white_mode = config.white_mode;
      cs.set_colorscheme_model(csm, false);
    }
    schemes.get(0).notify_observers();
    // hack: notify only once so we rebuild the image only once
  }

}


package TCGA;

// rpf 
// import javax.swing.*;
// import java.awt.*;
// import java.awt.event.*;
// import java.util.*;
//
//
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import layout.SpringUtilities;

public class NavigationControl extends Observable implements ActionListener,KeyListener {
  private static final String LABEL_OK = "OK";
  private static String LABEL_CANCEL = "Cancel";
  private static String LABEL_ZOOM = "Zoom";
  private static String LABEL_NEW_WINDOW = "New window";
  private static String LABEL_HELP = "Help";

  private JFrame jf;
  private JPanel panel;
  private JButton jb_ok_bin, jb_ok_loc, jb_cancel, jb_zoom_bin, jb_zoom_loc, jb_markers, jb_pathway, jb_help, jb_ok_sample, jb_zoom_sample;
  private JComboBox jc, jc_pathways, jc_samples;
  private JCheckBox cb_sort;
  private JTextField tf;
  private JTextArea jta;
  private JScrollPane jsp_genes;
  private boolean wants_zoom;
  private BinIndex bin_index;

  private GenomicMeasurement gm;
  private GenomicSet gs;
  private String selected_label;
  private Pathway selected_pathway;

  private PathwayGenes pathway_genes;

  private Integer wants_sort_bin = null;
  private JCheckBoxMap jcbm = null;

  public NavigationControl (GenomicMeasurement gm, GenomicSet gs) {
    this.gm = gm;
    this.gs = gs;
    setup();
  }
  
  private JPanel create_panel (String label, Component c, Component b) {
    JPanel p = new JPanel();
    p.setLayout(new BorderLayout());
    JLabel jl = new JLabel(label);
    p.add("West", jl);
    p.add("Center", c);
    p.add("East", b);

    p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    
    return p;
  }

  private JPanel panel_wrap (Component c) {
    JPanel p = new JPanel();
    p.add(c);
    return p;
  }

  private void setup() {
    pathway_genes = new PathwayGenes();

    pathway_genes.sleep_until_ready();
    // hack and the hacktones

    bin_index = new BinIndex(gm);

    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

    JPanel sub_p = get_buffer_titled_panel(panel, "Move to a location, region, or sample");
    sub_p.setLayout(new SpringLayout());

    int rows = 0;

    //
    //  single bin location combo box:
    //
    sub_p.add(new JLabel("Select a location:", JLabel.TRAILING));
    sub_p.add(jc = new JComboBox(new Vector(bin_index.get_unique_list())));
    sub_p.add(jb_ok_bin = new JButton(LABEL_OK));
    sub_p.add(jb_zoom_bin = new JButton(LABEL_ZOOM));
    sub_p.add(cb_sort = new JCheckBox("Sort", false));
    rows++;

    if (gm.is_genome_formatted()) {
      //
      //  genomic region (for genomic-bin formatted heatmaps only)
      // 
      sub_p.add(new JLabel("Genomic region:", JLabel.TRAILING));
      sub_p.add(tf = new JTextField(20));
      sub_p.add(jb_ok_loc = new JButton(LABEL_OK));
      sub_p.add(jb_zoom_loc = new JButton(LABEL_ZOOM));
      tf.setText("chr7:3000000-100000000");
      sub_p.add(new JLabel());
      rows++;
    }

    //
    //  sample ID
    //
    Vector labels = new Vector(gm.get_visible_sample_ids());
    jc_samples = new JComboBox(labels);
    sub_p.add(new JLabel("Sample ID:", JLabel.TRAILING));
    sub_p.add(jc_samples);
    sub_p.add(jb_ok_sample = new JButton("OK"));
    sub_p.add(jb_zoom_sample = new JButton(LABEL_ZOOM));
    sub_p.add(new JLabel());
    rows++;

    SpringUtilities.makeCompactGrid(sub_p,
				    rows, 5,
				    // rows, columns
				    
				    6,6,
				    6,6);

    panel.add(sub_p);

    //
    //  drill-down section
    //
    sub_p = get_buffer_titled_panel(panel, "Drill down to a set of markers");
    sub_p.setLayout(new SpringLayout());

    //
    // pathway markers:
    //
    sub_p.add(new JLabel("Pathway:", JLabel.TRAILING));

    labels = new Vector();
    for (String s : pathway_genes.get_pathway_names()) {
      labels.add(s);
    }
    jc_pathways = new JComboBox(labels);
    jc_pathways.setPrototypeDisplayValue("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
    new JComboBoxToolTipHelper(jc_pathways);
    // limit size of combo box

    sub_p.add(jc_pathways);

    boolean BUTTON_MESS = false;

    if (BUTTON_MESS) {
      sub_p.add(jb_pathway = new JButton(LABEL_NEW_WINDOW));
    } else {
      sub_p.add(panel_wrap(jb_pathway = new JButton(LABEL_NEW_WINDOW)));
    }

    sub_p.add(new JLabel(""));

    sub_p.add(new JLabel("-or-   ", JLabel.TRAILING));
    sub_p.add(new JLabel(""));
    sub_p.add(new JLabel(""));
    sub_p.add(new JLabel(""));

    //
    //  user-entered markers
    //
    jta = new JTextArea(4,25);
    //    jta.setText("VSTM2A\nSEC61G\nEGFR\nLANCL2\nGBAS");
    jta.setText("(example: VSTM2A SEC61G EGFR LANCL2 GBAS)");

    jsp_genes = new JScrollPane(jta,
				      JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				      //				      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
				      JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
				      );

    sub_p.add(new JLabel("Enter marker list:", JLabel.TRAILING));
    sub_p.add(jsp_genes);
    if (BUTTON_MESS) {
      // (too-)large button
      sub_p.add(jb_markers = new JButton(LABEL_NEW_WINDOW));
    } else {
      // smaller button, but not equally sized
      sub_p.add(panel_wrap(jb_markers = new JButton(LABEL_NEW_WINDOW)));
    }
    sub_p.add(new JLabel(""));
    rows = 3;

    CommentOptions co = gm.get_options();
    if (co.has_combines()) {
      //
      // heatmap has others it may be pooled with
      //
      ArrayList<CombineInfo> combines = co.get_combines();
      sub_p.add(new JLabel("Import data from related heatmaps:", JLabel.TRAILING));

      JPanel pool_p = new JPanel();
      pool_p.setLayout(new BoxLayout(pool_p, BoxLayout.PAGE_AXIS));
      jcbm = new JCheckBoxMap();
      
      for (CombineInfo ci : combines) {
	JCheckBox jcb = new JCheckBox(ci.get_label(), false);
	jcbm.add_checkbox(jcb, ci);
	pool_p.add(jcb);
      }

      sub_p.add(pool_p);
      sub_p.add(new JLabel(""));
      sub_p.add(new JLabel(""));
      rows++;
    }

    SpringUtilities.makeCompactGrid(sub_p,
				    rows, 4,
				    // rows, columns
				    6,6,
				    6,6);

    jb_cancel = new JButton(LABEL_CANCEL);

    sub_p = new JPanel();
    sub_p.add(jb_help = new JButton(LABEL_HELP));
    sub_p.add(new JLabel("    "));
    // HACK
    sub_p.add(jb_cancel);
    panel.add(sub_p);

    jb_ok_bin.addActionListener(this);
    jb_ok_sample.addActionListener(this);
    jb_zoom_sample.addActionListener(this);
    jb_zoom_bin.addActionListener(this);
    jb_markers.addActionListener(this);
    jb_pathway.addActionListener(this);
    jb_cancel.addActionListener(this);
    jb_help.addActionListener(this);
    jc.addActionListener(this);
    jc_pathways.addActionListener(this);

    if (gm.is_genome_formatted()) {
      jb_ok_loc.addActionListener(this);
      jb_zoom_loc.addActionListener(this);
      tf.addActionListener(this);
    }

    jc.addKeyListener(this);
    jc_pathways.addKeyListener(this);

    jf = new JFrame("Navigation");
    jf.getContentPane().add(panel);
    jf.pack();
    jf.setVisible(true);
  }

  public void setVisible (boolean v) {
    jf.setVisible(v);
  }
  
  public void setState (int state) {
    jf.setState(state);
  }
  
  private void jump_to (String target) {
    // UCSC format
    // chr7:127,471,196-127,495,720
    fire_clear();

    String[] f = target.split(":");
    boolean parse_error = true;

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

	int chr = gs.chr2int(f[0]);

	if (ok == 2 && chr > 0) {
	  // start and end positions and chromosome parsed successfully
	  int start_bin = gs.get_bin_for(chr, positions[0]);
	  int end_bin = gs.get_bin_for(chr, positions[1]);
	  Rectangle selection = gm.generate_selection(start_bin,
						      (end_bin - start_bin) + 1);
	  setChanged();
	  notifyObservers(selection);
	  parse_error = false;
	}
      }
    }

    if (parse_error) {
      JOptionPane.showMessageDialog(jf,
				    "please specify coordinates in format chrX:start-end, e.g. \"chr7:1-30000000\"",
				    "Formatting error",
				    JOptionPane.ERROR_MESSAGE);	  
    }
  }
  
  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();
    if (src instanceof JTextField) {
      jump_to(tf.getText());
    } else if (src.equals(jc_pathways)) {
      String pname = (String) jc_pathways.getSelectedItem();
      Pathway p = pathway_genes.get_pathway(pname);
      StringBuffer sb = new StringBuffer();
      for (String gene : p.genes) {
	sb.append(gene + "\n");
      }
      jta.setText(sb.toString());

      javax.swing.SwingUtilities.invokeLater(new Runnable() {
	  public void run() {
	    // thread safety: reset the vertical scrollbar to the top
	    // after we're sure widget has been repopulated
	    JScrollBar v = jsp_genes.getVerticalScrollBar();
	    v.setValue(v.getMinimum());
	  }
	});
    } else if (src instanceof JButton) {
      JButton jb = (JButton) src;
      String label = jb.getText();
      if (jb.equals(jb_markers)) {
	String[] markers = jta.getText().split("\\s+");
	//	System.err.println("markers:"+markers.length);  // debug
	ArrayList<String> m2 = new ArrayList<String>();
	for (int i=0; i < markers.length; i++) {
	  // feh
	  m2.add(markers[i]);
	}
	launch_markers(m2);
      } else if (jb.equals(jb_help)) {
	HelpLauncher hl = new HelpLauncher(HelpLauncher.ANCHOR_NAVIGATION);
	hl.launch_url();
      } else if (jb.equals(jb_pathway)) {
	fire_pathway();
      } else if (label.equals(LABEL_CANCEL)) {
	jf.setVisible(false);
      } else {
	fire_single(jb);
      }
    }
  }

  private void fire_clear() {
    selected_label = null;
    selected_pathway = null;
    wants_sort_bin = null;
  }

  private void fire_single (JButton jb) {
    fire_clear();
    wants_zoom = jb.equals(jb_zoom_bin) || jb.equals(jb_zoom_loc) || jb.equals(jb_zoom_sample);
    if (jb.equals(jb_ok_bin) || jb.equals(jb_zoom_bin)) {
      String selected = (String) jc.getSelectedItem();
      //	  System.err.println("selected:"+selected);  // debug
      int index = bin_index.find(selected);
      Rectangle selection = gm.generate_selection(index);
      //      if (cb_sort.isSelected()) gm.sort_by_bin(index);
      if (cb_sort.isSelected()) wants_sort_bin = new Integer(index);
      selected_label = selected;
      setChanged();
      notifyObservers(selection);
      //	jf.setVisible(false);
    } else if (jb.equals(jb_ok_loc) || jb.equals(jb_zoom_loc)) {
      jump_to(tf.getText());
    } else if (jb.equals(jb_ok_sample) || jb.equals(jb_zoom_sample)) {
      Rectangle selection = gm.generate_selection((String) jc_samples.getSelectedItem());
      setChanged();
      notifyObservers(selection);
    }
  }

  public Pathway get_selected_pathway () { 
    return selected_pathway;
  }

  public String get_selected_label () { 
    return selected_label;
  }
  
  private void fire_pathway() {
    fire_clear();
    String pname = (String) jc_pathways.getSelectedItem();
    selected_pathway = pathway_genes.get_pathway(pname);
    launch_markers(selected_pathway.genes);
  }


  public static void main (String [] argv) {
    Funk.LookAndFeeler.set_native_lookandfeel();
    try {
      //      GenomicMeasurement gm = new GenomicMeasurement("snp6_genomicmeasurement_broad_updated_cn.txt", false);
      //      GenomicMeasurement gm = new GenomicMeasurement("test.txt", false);
      //      GenomicMeasurement gm = new GenomicMeasurement("carl.txt", false);
      //      GenomicMeasurement gm = new GenomicMeasurement("genome_copynumber_gbm_broad_paired_affymetrix.txt.gz", false);
      GenomicMeasurement gm = new GenomicMeasurement("combine\\Gene_CopyNumber_OV_Broad_Paired_Affymetrix.txt", false);
      GenomicSet gs = new GenomicSet(gm, GenomicSet.STYLE_GENOMIC, null);
      NavigationControl nc = new NavigationControl(gm, gs);
    } catch (Exception e) {
      System.err.println(e);  // debug
    }
  }

  public boolean wants_zoom() {
    return wants_zoom;
  }

  public Integer wants_sort_bin() {
    return wants_sort_bin;
  }


  private void launch_markers (ArrayList<String> markers) {
    ArrayList<Integer> wanted = new ArrayList<Integer>();
    ArrayList<String> missing = new ArrayList<String>();
    for(int i=0; i < markers.size(); i++) {
      String marker = markers.get(i);
      int index = bin_index.find(marker);
      if (index == -1) {
	missing.add(marker);
      } else {
	wanted.add(new Integer(index));
      }
    }
    if (missing.size() > 0) {
      System.err.println("MISSING:"+missing.toString());  // debug
      JOptionPane.showMessageDialog(jf,
				    "Can't find markers " + missing.toString(),
				    "Warning",
				    JOptionPane.WARNING_MESSAGE
				    );
    }
	
    if (wanted.size() > 0) {
      setChanged();
      notifyObservers(wanted);
    }
  }

  private JPanel get_buffer_titled_panel (JPanel panel, String title) {
    // create a new line-bordered JPanel and add it to the given panel
    JPanel jp_buffer = new JPanel();
    jp_buffer.setLayout(new BorderLayout());
    jp_buffer.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

    JPanel jp_titled = new JPanel();
    jp_titled.setLayout(new BoxLayout(jp_titled, BoxLayout.PAGE_AXIS));
    jp_titled.setBorder(BorderFactory.createTitledBorder(title));

    jp_buffer.add("Center", jp_titled);
    panel.add(jp_buffer);

    return jp_titled;
  }

  // begin KeyListener stubs 
  public void keyPressed(KeyEvent ke) {
    Object src = ke.getSource();
    if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
      if (src.equals(jc_pathways)) {
	fire_pathway();
      } else if (src.equals(jc)) {
	fire_single(jb_zoom_bin);
      }
    }
  }
  public void keyReleased(KeyEvent ke) {}
  public void keyTyped(KeyEvent ke) {}
  // end KeyListener stubs 

  public ArrayList<CombineInfo> get_selected_combinations() {
    // return list of combined 
    ArrayList<CombineInfo> results = new ArrayList<CombineInfo>();
    if (jcbm != null) {
      for (Object o : jcbm.get_selected()) {
	// fix me: generics?
	results.add((CombineInfo) o);
      }
    }
    return results;
  }

  public JFrame get_jframe() {
    return jf;
  }


}


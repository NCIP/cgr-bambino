package Ace2;
// controls for AssemblyCanvas.

import TCGA.MouseDragScroller;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;

import java.util.*;
import java.net.URL;
import java.io.*;

import net.sf.samtools.*;

public class AcePanel extends JPanel implements Observer,ActionListener,ItemListener,KeyListener,FocusListener,MouseListener,ChangeListener,Runnable {
  AssemblyCanvas canvas;
  // FIX ME: remove, use AceViewerConfig reference
  Ace ace;
  String filename;
  
  private boolean use_postgres = false;

  private static boolean snp_info = true;

  private String start_contig = "Contig1";

  private int start_offset = -1;
  private SNPList start_snps = null;
  private Hashtable all_snps = null;
  private Hashtable heterozygotes = null;

  private JButton b_best_snp, b_exit, b_help, b_find;
  private JTextField nav_field;
  private JComboBox choice_contigs,choice_exons;
  private JSpinner sp_exons, sp_snps;

  private AceViewerConfig config;
  private boolean is_built = false;

  private JPanel cp;
  private Observer deep_sigh;

  public AcePanel (AceViewerConfig config) {
    this.config = config;
    deep_sigh = this;
    setup();
    new Thread(this).start();
  }

  public void set_font_size (int size) {
    if (canvas != null) canvas.set_font_size(size);
  }

  public void find () {
    SearchWidget2 sw = new SearchWidget2(Funk.Gr.getJFrame(this));
    if (sw.wants_find()) {
      String value = sw.get_value();
      System.err.println("find " + value);
      canvas.set_consensus_highlight(0, 0);
      if (sw.is_id_search()) {
	find_ids(value);
      } else {
	consensus_search(value);
      }
    }
  }

  public void update (Observable o, Object arg) {
    //    System.err.println("update: " + o);  // debug
    if (o instanceof SNPControl) {
      // SNP params changed, proceed
      System.err.println("SNPControl!");  // debug
      find_snps();
    } else if (o instanceof AnnotationLoader) {
      // database annotations loaded, init reference sequence interface
      init_exon_navigation();
      canvas.repaint();
    } else if (o instanceof Ace) {
      //
      //  notification from Ace file
      //
      Ace a = (Ace) o;
      if (arg instanceof Integer) {
	// progress report in bytes
	//	System.out.println("loaded " + arg);  // debug
	if (canvas != null) canvas.repaint();
      } else {
	//
	// acefile has finished loading
	//
	boolean ok = ((Boolean) arg).booleanValue();
	if (ok && !a.is_empty()) {
	  // finish building UI
	  post_load();
	} else {
	  // problem
	  canvas.repaint();
	}
      }
    }
  }

  public void run() {
    // VILE, FIX ME:
    // much better if this was an Observer to Assembly build process...

    boolean contig_setup = false;

    while (true) {

      if (!contig_setup) {
	if (config.assembly.is_loaded()) {
	  if (config.assembly.supports_contigs()) {
	    // select initial contig
	    start_contig = config.ACE_AUTOCONTIG ?
	      config.assembly.get_biggest_contig_id() : config.assembly.get_contig_id_list().get(0);
	    config.assembly.set_contig(start_contig);
	    // FIX ME: move this to Ace itself so this step is not required.
	    // Without this code viewer will hang waiting for assembly to finish.
	    // (.ace version needs contig selection to finish building assembly)
	  }
	  contig_setup = true;
	}
      }

      //      System.err.println("loaded?:" + assembly.is_loaded() + " built?: " + assembly.alignment_is_built());

      if (config.assembly.alignment_is_built()) {
	post_load();
	break;
      } else {
	try {
	  //	  System.err.println("panel spin");  // debug
	  Thread.sleep(25);
	} catch (InterruptedException e) {}
      }
    }
  }

  public void setup() {
    //
    //  set up GUI, wait for assembly
    //

    //    Scrollbar hs = new Scrollbar( Scrollbar.HORIZONTAL, 1, 1, 1, 1 );
    //    Scrollbar hv = new Scrollbar( Scrollbar.VERTICAL, 1, 1, 1, 1);
    JScrollBar hs = new JScrollBar(JScrollBar.HORIZONTAL );
    JScrollBar hv = new JScrollBar(JScrollBar.VERTICAL );
    addMouseListener(this);

    //    canvas = new AssemblyCanvas(this, assembly, hs, hv, config);
    canvas = new AssemblyCanvas(this, config, hs, hv);
    //    Funk.Gr.getFrame(this).addKeyListener(this);
    addKeyListener(this);
    canvas.addKeyListener(this);
    MouseDragScroller mdc = new MouseDragScroller(canvas, hs, hv);
    mdc.set_drag_button_mask(config.DRAG_BUTTON_MASK);
    mdc.set_drag_scale_factor(config.DRAG_SCALE_FACTOR);

    // control panel --
    // this could be a separate class...probably overkill...
    cp = new JPanel();
    cp.setLayout(new BorderLayout());
    JPanel left_p = new JPanel();

    choice_contigs = new JComboBox();
    choice_contigs.addItem("Contig1");
    // hack so the chooser is packed properly; actual items added later
    choice_contigs.addItemListener(this);
    if (config.assembly.supports_contigs()) left_p.add(choice_contigs);

    if (snp_info) {
      left_p.add(b_best_snp = new JButton("SNPs"));
      b_best_snp.setToolTipText("click to detect SNPs and indels");
      b_best_snp.addActionListener(this);
    }
    //    left_p.add(new JLabel(" "));

    ArrayList<String> dummy = new ArrayList<String>();
    dummy.add("                   ");
    SpinnerListModel slm = new SpinnerListModel(dummy);
    sp_snps = new JSpinner(slm);
    sp_snps.setToolTipText("SNP/indel information");
    sp_snps.setVisible(false);

    JComponent editor = sp_snps.getEditor();
    if (editor instanceof JSpinner.ListEditor) {
      JFormattedTextField jtf = ((JSpinner.ListEditor) editor).getTextField();
      jtf.setHorizontalAlignment(JTextField.LEFT);
      jtf.setEditable(false);
    }

    //    JTextField tf_snp = new JTextField(10);
    //    sp_snps.setEditor(tf_snp);


    //    sp_snps.setEditable(false);
    //    left_p.add(tf_snp);
    left_p.add(sp_snps);
    sp_snps.setEnabled(false);

    sp_snps.addChangeListener(new ChangeListener() {
	// begin changeListener stub
	public void stateChanged(ChangeEvent e) {
	  SpinnerListModel slm = (SpinnerListModel) sp_snps.getModel();
	  String entry = (String) slm.getValue();
	  sp_snps.setToolTipText(entry);
	  
	  int index = 0;
	  int idx = entry.indexOf(":");
	  if (idx > -1) {
	    String number = entry.substring(1, idx);
	    index = Integer.parseInt(number) - 1;
	  }
	  
	  SNPList snps = config.assembly.get_snps();
	  if (snps != null) {
	    snps.set_index(index);
	    SNP snp = (SNP) snps.elementAt(index);
	    int cpos = snp.position;
	    config.clamp_cons_start = canvas.get_start_for_center(cpos);
	    canvas.center_on(cpos);
	    int loc = snps.current_location_id();
	    if (loc > 0 && heterozygotes != null) {
	      canvas.set_heterozygotes((Hashtable) heterozygotes.get(Integer.toString(loc)));
	    }
	  }

	}
	// end changeListener stub
      });

    //    if (config.enable_exon_navigation && config.refgenes != null) {
    if (config.enable_exon_navigation) {
      choice_exons = new JComboBox();
      choice_exons.addItem("NM_000000");
      // pack hack

      //      SpinnerNumberModel snm = new SpinnerNumberModel(10,1,10,1);
      //      SpinnerNumberModel snm = new SpinnerNumberModel(1,1,10,1);
      // sp_exons = new JSpinner(snm);
      sp_exons = new JSpinner(new SpinnerListModel());
      // empty for now

      left_p.add(new JLabel("    RefSeq:", JLabel.RIGHT));
      left_p.add(choice_exons);
      //      left_p.add(new JLabel(" exon:", JLabel.RIGHT));
      left_p.add(sp_exons);
      sp_exons.setToolTipText("exon number");

      choice_exons.setEnabled(false);
      sp_exons.setEnabled(false);
    }

    if (config.LOCAL_FILE_MODE) {
      left_p.add(new JLabel("    "));
      JLabel jl;
      left_p.add(jl = new JLabel("Jump:", JLabel.RIGHT));

      String help = "move to new location: enter gene symbol or position in format chrX:start-end";
      jl.setToolTipText(help);
      left_p.add(nav_field = new JTextField(10));
      nav_field.setToolTipText(help);

      nav_field.addActionListener(new ActionListener() {
	  // begin ActionListener stub
	  public void actionPerformed(ActionEvent e) {
	    switch_assembly(nav_field.getText()); 
	  }
	  // end ActionListener stub
	});
    }



    cp.add("West", left_p);

    JPanel right_p = new JPanel();

    right_p.add(b_find = new JButton("Find"));
    b_find.addActionListener(this);

    //    right_p.add(b_primers = new JButton("Primers"));
    
    if (false) {
      right_p.add(b_help = new JButton("?"));
      b_help.addActionListener(this);
      right_p.add(b_exit = new JButton("Exit"));
      b_exit.addActionListener(this);
    }

    cp.add("East", right_p);

    setLayout(new BorderLayout());
    add("North", cp);
    add("Center", canvas);
    add("East", hv);
    add("South", hs);

    addFocusListener(this);
    canvas.addFocusListener(this);
    cp.addFocusListener(this);

  }

  public void finalize () {
    System.out.println("ap finalize");  // debug
    canvas.finalize();
  }

  public void dispose() {
    System.out.println("ap dispose");  // debug
  }

  void snp_move (int direction) {
    // move the assembly canvas to a specified position in the list
    // of SNP locations.  Update text detail.
    SNPList snps = config.assembly.get_snps();
    if (snps != null) {
      if (direction > 0) snps.move(direction);
      int cpos = snps.current_position();
      config.clamp_cons_start = canvas.get_start_for_center(cpos);
      canvas.center_on(cpos);
      int loc = snps.current_location_id();
      if (loc > 0 && heterozygotes != null) {
	canvas.set_heterozygotes((Hashtable) heterozygotes.get(Integer.toString(loc)));
      }
    }
  }

  public static void set_snp_info (boolean b) {
    snp_info = b;
  }

  private String get_accession () {
    // extract Genbank accession number from filename.
    // my kingdom for $string =~ /^[A-Z]+\d+$/
    StringTokenizer st = new StringTokenizer(filename, "/");
    String result = null;
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (Funk.Str.is_genbank_accession(token)) {
	result = token;
	break;
      }
    }
    return result;
  }

  private void show_url (String url, String title) {
    System.err.println("FIX ME: launch URL here: " + url);  // debug
  }

  public void find_ids (String search) {
    search = search.toLowerCase();
    Hashtable matches = new Hashtable();
    if (search.length() > 0) {
      int start_offset = 0;
      boolean found = false;
      Integer dummy = new Integer(0);

      for (AssemblySequence as : config.assembly.get_sequences()) {
	String id = as.get_name();
	if (id.toLowerCase().indexOf(search) > -1) {
	  start_offset = as.get_asm_start();
	  matches.put(id, dummy);
	  found=true;
	}
      }
      if (found) {
	// move to site of one matching ID
	canvas.center_on(start_offset);
      } else {
	// no matches
	new Funk.Message("No sequence IDs whose names match \"" + search + "\".");
      }
    }

    canvas.set_highlights(matches);
    canvas.repaint();
  }

  private void consensus_search (String search) {
    PaddedNucleotideSearch pns =
      new PaddedNucleotideSearch(new String(config.assembly.get_consensus_sequence()));
    // hack, FIX ME
    Enumeration e = pns.find(search);

    if (e.hasMoreElements()) {
      // hack; just the first hit
      int start = ((Integer) e.nextElement()).intValue();
      canvas.set_consensus_highlight(start, pns.get_end(start));
      canvas.center_on(start);
    } else {
      new Funk.Message("Sorry, not found.");
    }
  }

  public void set_postgres (boolean status) {
    use_postgres = status;
  }

  private void find_snps() {
    config.snp_config.REPORT_RESULTS = false;
    config.snp_config.CHECK_GENOME_VERSION = false;
    // in case running via Java Web Start (permissions)

    //    System.err.println("MQ="+snp_config.MIN_QUALITY);  // debug

    Cursor previous_cursor = canvas.getCursor();
    canvas.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    // FIX ME: SNP detection probably needs to be run in a separate
    // thread for this to take effect 

    try {
      Funk.Timer tm = new Funk.Timer("snp find");
      SAMStreamingSNPFinder sf = new SAMStreamingSNPFinder(config.snp_config);
      
      for (SAMResource sr : config.sams) {
	//	System.err.println("sfr="+sr.getSAMFileReader());  // debug

	//	sr.close();
	// skip close(): speeds up SNP finding w/internal iterators?
      }
      
      sf.extent_setup(config);
      sf.set_dbsnp(false);
      // disable dbSNP lookups for interactive use (don't see final report)

      sf.find_snps();

      ArrayList<SNP2> results = sf.get_results();

      tm.finish();
	      
      SNPList sl = new SNPList();
      PadMap pm = config.assembly.get_padmap();
      for (SNP2 snp2 : results) {
	//	System.err.println("pos:" + snp2.base_number + " type:" + snp2.type + " size:" + snp2.size + " ref:" + snp2.reference_allele + " alt:" + snp2.alternative_allele);  // debug

	SNP snp = new SNP();
	snp.filename = "n/a";
	snp.contig = snp2.reference_sequence_name;
	//	System.err.println("ruler start="+config.ruler_start);  // debug
	snp.position = pm.get_unpadded_to_padded(snp2.base_number - config.ruler_start);
	if (snp2.type.equals("insertion")) {
	  // insertions use the first base number after the event.
	  // adjust position to last base in indel so option to display
	  // only non-reference sequences will work.
	  snp.position--;
	}
	snp.location_id = -1;
	snp.score = snp2.p_value;
	snp.snp2 = snp2;
	sl.add(snp);
      }

      if (sl == null || sl.size() == 0) {
	JOptionPane.showMessageDialog(this,
				      "No SNPs were found.",
				      "Message",
				      JOptionPane.INFORMATION_MESSAGE);
      } else {
	Dimension initial_pref_size = sp_snps.getPreferredSize();
	//	System.err.println("sp_snps #1 size=" + sp_snps.getSize() + " pref_size=" + sp_snps.getPreferredSize());  // debug

	sl.sort();
	config.assembly.set_snps(sl);
	sp_snps.setEnabled(true);
	SpinnerListModel slm = (SpinnerListModel) sp_snps.getModel();
	System.err.println("SNP count: " + sl.size());  // debug

	ArrayList<String> descs = new ArrayList<String>();
	int snp_num = 0;
	for (Object o : sl) {
	  SNP2 snp2 = ((SNP) o).snp2;

	  String label = new String("#" + ++snp_num + ": ");
	  if (snp2.type.equals("SNP")) {
	    label = label.concat(snp2.reference_allele + ">" + snp2.alternative_allele);
	  } else {
	    // indel
	    label = label.concat(snp2.type.substring(0,3) + " " + snp2.size);
	  }
	  descs.add(label);
	}
	slm.setList(descs);

	if (sp_snps.isVisible() == false) {
	  //	  System.err.println("sp_snps size=" + sp_snps.getSize() + " pref_size=" + sp_snps.getPreferredSize());  // debug

	  sp_snps.setPreferredSize(initial_pref_size);
	  // FIX ME: how to have preferred size set appropriately??
	  // if we make the component visible initially, a dummy initial value (spaces) works.
	  // Hiding the component initially seems to break this however.
	  // Use 

	  sp_snps.setVisible(true);
	  sp_snps.invalidate();
	}
	
      }

    } catch (Exception e) {
      new ErrorReporter(e);
    }

    canvas.setCursor(previous_cursor);
  }

  // begin ActionListener stub 
  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();

    if (src.equals(b_best_snp)) {
      SNPList snps = config.assembly.get_snps();
      //      if (snps == null || ((e.getModifiers() & ActionEvent.CTRL_MASK) > 0)) {
	// no SNPs, or holding down CTRL when clicking "Find"/"Best"
      if (true) {
	if (config.assembly.has_quality()) {
	  // FIX ME: this should detect whether underlying assembly is SAM or .ace based...
	  //	  SNPControl sc = new SNPControl(config.snp_config, config);
	  new SNPControl(Funk.Gr.getJFrame(this), this, config.snp_config, config);
	  //	  sc.addObserver(this);
	  // now modal, so this wouldn't be executed until dialog closed
	} else {
	  System.err.println("can't call SNPs without quality data.");  // debug
	}
      }
      snp_move(SNPList.BEST);
    } else if (src.equals(b_find)) {
      find();
    } else if (src.equals(b_help)) {
      show_url("http://lpgws.nci.nih.gov/html-cgap/gai_assembly_help.html", "Help");
    } else if (src.equals(b_exit)) {
      // close
      ((AceViewer) Funk.Gr.getFrame(this)).die();
    }
  }
  // end ActionListener stub

  // begin ItemListener stub
  public void itemStateChanged(ItemEvent e) {
    // a new contig may have been selected
    //    String new_contig = (String) e.arg;
    if (e.getSource().equals(choice_contigs)) {
      String new_contig = (String) choice_contigs.getSelectedItem();
      if (! config.assembly.get_contig_id().equals(new_contig)) {
	config.assembly.set_contig(new_contig);
	canvas.reset();
	if (all_snps != null &&
	    all_snps.containsKey(new_contig)) {
	  config.assembly.set_snps((SNPList) all_snps.get(new_contig));
	  snp_move(0);
	}
	canvas.repaint();
      }
    } else if (e.getSource().equals(choice_exons)) {
      //
      //  exon accession number (refGene) has changed
      //
      set_current_refgene();
    } else {
      System.err.println("unhandled event");  // debug
    }
  }
  // end ItemListener stub

      
  private void set_current_refgene() {
    RefGene rg = get_current_exon_refgene();
    if (rg != null) {

      ArrayList<Exon> exons = rg.get_exons();
      ArrayList<Integer> usable_exons = new ArrayList<Integer>();
      for (Exon exon : exons) {
	if (exon.get_first_start() > 0) {
	  // exon has (at least some) mapping available
	  int exno = Integer.parseInt(exon.id);
	  usable_exons.add(exno);
	}
      }
      //      System.err.println("first mapped exon:" + first_mapped_exon + " last:"+last_mapped_exon);  // debug

      //      int ec = rg.get_exon_count();  // debug
      //      SpinnerNumberModel snm = (SpinnerNumberModel) sp_exons.getModel();
      //      snm.setMaximum(Integer.valueOf(ec));
      //      snm.setMaximum(ec);
      //      snm.setMinimum(first_mapped_exon);
      //      snm.setMaximum(last_mapped_exon);

      if (usable_exons.size() > 0) {
	SpinnerListModel slm = (SpinnerListModel) sp_exons.getModel();

	slm.setList(usable_exons);

	//      snm.setValue(Integer.valueOf(1));

	//      slm.setValue(0);
	// hack: if all refseqs have only one exon, the value 
	// will only ever be 1, so state change trigger won't
	// be fired here.  Since JSpinner.fireStateChanged() is 
	// protected (why?), resort to this nonsense (setting
	// a bogus value first).

	//      snm.setValue(first_mapped_exon);
	slm.setValue(usable_exons.get(0));
	// rebuild exon spinner to count of exons
	// FIX ME: if current value is not out of range, leave it alone...
      }

    }
  }


  // begin KeyListener stubs 
  public void keyPressed(KeyEvent ke) {
    int code = ke.getKeyCode();

    if (code == KeyEvent.VK_X) {
      // reset snps
      config.assembly.set_snps(null);
      canvas.repaint();
    } else if (code == KeyEvent.VK_Q) {
      System.exit(0);
    }
  }
  
  public void keyReleased(KeyEvent ke) {};

  public void keyTyped(KeyEvent ke) {};
  // end KeyListener stubs 

  // begin FocusListener stubs
  public void focusGained(FocusEvent e) {}
  public void focusLost(FocusEvent e) {}
  // end FocusListener stubs


  // begin MouseListener stubs
  public void mouseDragged(MouseEvent e) {};
  public void mousePressed(MouseEvent e) {};
  public void mouseMoved(MouseEvent e) {};
  public void mouseClicked(MouseEvent e) {};
  public void mouseReleased(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {
    requestFocus();
  };
  public void mouseExited(MouseEvent e) {};
  // end MouseListener stubs

  public void init_exon_navigation () {
    //
    // markups have been loaded: populate exon navigation controls
    //
    choice_exons.removeAllItems();
    if (config.refgenes == null) {
      //      System.err.println("refgenes null");  // debug
      return;
    }

    for (RefGene rg : config.refgenes) {
      if (rg.is_broken()) {
	System.err.println("skipping broken refgene " + rg.get_accession());  // debug
      } else {
	//	System.err.println("adding refgene " + rg.get_accession());  // debug
	choice_exons.addItem(rg.get_accession());
      }
    }

    set_current_refgene();
    //    SpinnerNumberModel snm = (SpinnerNumberModel) sp_exons.getModel();
    //    snm.setValue(1);
    // hack

    sp_exons.invalidate();
    choice_exons.invalidate();
    cp.validate();
    // choice_exons values have changed, need to layout again

    choice_exons.setEnabled(true);
    sp_exons.setEnabled(true);

    sp_exons.addChangeListener(this);
    choice_exons.addItemListener(this);
    // add listeners only after widgets are fully populated to prevent premature firing

  }

  // begin changeListener stub
  public void stateChanged(ChangeEvent e) {
    Object src = e.getSource();
    if (src.equals(sp_exons)) {
      RefGene rg = get_current_exon_refgene();
      if (rg != null) {
	int exno = (Integer) sp_exons.getValue();
	Exon exon = rg.get_exon_id(Integer.toString(exno));
	ArrayList<Codon> codons = exon.get_codons();
	if (codons.size() == 0) {
	  System.err.println("no codon mapping for exon " + exno);  // debug
	} else {
	  for (Codon c : codons) {
	    if (!c.is_unmappable()) {
	      int pos = codons.get(0).c_last_offset();
	      // focus on the last nucleotide in the codon, as this will always
	      // be in the exon in question (first 2 might be in previous exon!)
	      //      canvas.center_on(pos);
	      //	  canvas.move_to(pos - 5);
	      canvas.center_on(pos);
	      break;
	    }
	  }
	}
      }
    }
  }
  // end changeListener stub

  private RefGene get_current_exon_refgene() {
    String accession = (String) choice_exons.getSelectedItem();
    RefGene result = null;
    if (config.refgenes != null) {
      for (RefGene rg : config.refgenes) {
	if (rg.get_accession().equals(accession)) {
	  result = rg;
	  break;
	}
      }
    }
    return result;
  }

  public AssemblyCanvas get_canvas() {
    return canvas;
  }

  public boolean is_built() {
    return is_built;
  }

  public Assembly get_assembly () {
    return config.assembly;
  }

  public void post_load() {
    if (is_built) {
      System.err.println("UI already built");  // debug
    } else if (choice_contigs == null || canvas == null) {
      System.err.println("UI not ready yet");  // debug
    } else {
      //      System.err.println("OK to build!");  // debug

      //      System.err.println("FINISHING UI");  // debug

      canvas.requestFocus();

      if (config.assembly.supports_contigs()) {
	for (String item : config.assembly.get_contig_id_list()) {
	  if (! item.equals("Contig1")) choice_contigs.addItem(item);
	  // already added Contig1 when when Choice was created
	}
	choice_contigs.setSelectedItem(start_contig);
	//	if (config.ACE_AUTOCONTIG) {
	//	  start_contig = assembly.get_biggest_contig_id();
	//	}
	//
	//	assembly.set_contig(start_contig);
      }

      if (start_snps != null) {
	System.err.println("setting start SNPs");  // debug
	config.assembly.set_snps(start_snps);
	sp_snps.setEnabled(true);
      }

      config.assembly.build_alignment();
      // FIX ME: this method really needs to be changed or renamed.
      // separate methods for main assembly building and building of summary info?
      // only call after raw .ace data loaded / contig selected

      if (config.refgenes != null) {
	//
	//  raw refgene data has been loaded synchronously (i.e. from markup file).
	//  - initialize refgenes now
	//  - must be called after assembly.build_alignment()
	//
	PadMap pm = config.assembly.get_padmap();
	for (RefGene rg : config.refgenes) {
	  rg.consensus_setup(pm);
	}
	init_exon_navigation();
      } else if (config.ENABLE_JDBC && config.region != null && config.region.isValid()) {
	//
	//  need to load database annotations for desired region.
	//
	AnnotationLoader al = new AnnotationLoader(config, true);
	al.addObserver(this);
      }

      init_hetero_summary();

      canvas.repaint();

      //	  System.err.println("CSP="+config.start_padded_offset);  // debug

      if (config.start_padded_offset != 0 || config.start_unpadded_offset != 0) {
	// only reliable if ruler start position is not in markup file,
	// otherwise potential race condition / translation problem
	int clen = config.assembly.get_consensus_sequence().length;
	if (config.start_unpadded_offset < clen) {
	  // don't proceed if out of range -- i.e. not-yet-translated 
	  // site using a different ruler start position
	  int cpos;
	  if (config.start_padded_offset != 0) {
	    cpos = config.start_padded_offset;
	  } else {
	    // unpadded
	    cpos = config.assembly.get_padmap().get_unpadded_to_padded(config.start_unpadded_offset);
	  }
	  SNPList sl = new SNPList();
	  sl.addElement(new SNP(cpos, 0.0));
	  config.assembly.set_snps(sl);
	  canvas.center_on(config.start_padded_offset);
	}
      } else if (start_offset != -1) {
	canvas.center_on(start_offset);
      } else if (start_snps != null) {
	snp_move(0);
      }

      is_built=true;
    }
  }

  public void switch_assembly (String text) {
    //
    //  to do: RUN IN SEPARATE THREAD!
    //
    SAMRegion region = new SAMRegion();
    setCursor(new Cursor(Cursor.WAIT_CURSOR));
    repaint();
    // FIX ME: this doesn't seem to work well; put entire process
    // in a new thread?

    String DELIMITER = ":";

    //
    //  see if user entered a reference sequence name:
    //
    try {
      String[] stuff = text.split(DELIMITER);
      String rn = stuff[0];
      // split in case user entered a position as well
      for (SAMResource sr : config.sams) {
	ChromosomeDisambiguator cd = new ChromosomeDisambiguator(sr.getSAMFileReader());
	region.tname = cd.find(rn);
	if (region.tname != null) break;
      }
    } catch (Exception e) {
      System.err.println("ERROR getting SAMFileReaders");  // debug
    }

    if (region.tname == null) {
      //
      //  user didn't specify a reference sequence name;
      //  see if it's a gene name
      //
      JDBCCache ucsc = config.get_ucsc_genome_client();
      if (false) {
	System.err.println("debug: resetting cache");  // debug
	ucsc.flush_cache();
      }

      ArrayList<HashMap<String,String>> results = null;

      try {
	results = ucsc.query("select * from refGene where name2=\"" + text + "\"");
	// Internet connection required
      } catch (Exception e) {
	System.err.println("ERROR: " + e);  // debug
	e.printStackTrace();
      }

      if (results != null && results.size() > 0) {
	//
	// SQL hit for gene name
	//
	GeneInfo gi = new GeneInfo();
	gi.import_ucsc(results);
	region.tname = gi.chr.toString();
	region.range.start = gi.start;
	region.range.end = gi.end;
	region.gene_name = text;
	//     } else {
	//       // try built-in gene position list
	//       GeneList genes = new GeneList();
	//       genes.sleep_until_ready();

	//       GeneInfo gi = genes.get_gene(text);
	//       if (gi != null) {
	// 	//
	// 	// navigate to a gene symbol
	// 	//
	// 	region.tname = gi.chr.toString();
	// 	region.range.start = gi.start;
	// 	region.range.end = gi.end;
	// 	region.gene_name = text;
	//       }
      }
    }

    if (region.gene_name == null) {
      //
      //  see if user specified a target location
      //
      text = Funk.Str.trim_whitespace(text);
      String[] things = text.split(DELIMITER);
      
      if (things.length >= 1 && things.length <= 2) {
	if (things.length == 1) {
	  //
	  // only the reference name specified
	  //
	  config.region = region;
	  try {
	    config.region_default_setup();
	    // detect location of first reads mapped to specified reference
	  } catch (Exception e) {
	    System.err.println("ERROR setting startup range for ref seq");  // debug
	    System.err.println("setting range to ref start");  // debug
	    region.range.start = 1;
	    region.range.end = config.DEFAULT_INITIAL_VIEW_NT;
	  }
	  System.err.println("result="+region.range.start);  // debug
	} else {
	  //
	  // reference name plus start and/or end position
	  //
	  String[] pos = things[1].split("-");
	  try {
	    if (pos.length == 1) {
	      if (true) {
		// center on specified site
		int center = Integer.parseInt(pos[0]);
		int flank = config.DEFAULT_INITIAL_VIEW_NT / 2;
		int start = center - flank;
		int end = center + flank;
		if (start < 1) start = 1;
		//		config.start_unpadded_offset = center;
		region.range.start = start;
		region.range.end = end;
		// FIX ME: unfinished; centering doesn't work...
	      } else {
		// start at specified site
		region.range.start = Integer.parseInt(pos[0]);
		region.range.end = region.range.start + config.DEFAULT_INITIAL_VIEW_NT;
	      }
	    } else if (pos.length == 2) {
	      region.range.start = Integer.parseInt(pos[0]);
	      region.range.end = Integer.parseInt(pos[1]);
	    }
	  } catch (NumberFormatException e) {}
	}
      }
    }

    try {
      if (region.isValid()) {
	repaint();

	config.region = region;
	SAMUtils.sam_config_setup(config, region);
	//	config.assembly = new SAMAssembly(config.sams, config.target_sequence, config.ruler_start, false);

	//	config.assembly = new SAMAssembly(config.sams, config.target_sequence, config.ruler_start, true);

	if (true) {
	  SpinnerListModel slm = (SpinnerListModel) sp_snps.getModel();
	  ArrayList<String> dummy = new ArrayList<String>();
	  String dvalue = "                         ";
	  dummy.add(dvalue);
	  slm.setList(dummy);
	  sp_snps.setValue(dvalue);
	  //	sp_snps.setEnabled(false);
	}

	//	System.err.println("NEW ASM");  // debug
	//	config.assembly = new SAMAssembly(config.sams, config.target_sequence, config.ruler_start, true);
	config.assembly = new SAMAssembly(config, true);
	canvas.set_config(config);
	JFrame jf = Funk.Gr.getJFrame(this);

	Runnable later = new Runnable() {
	    public void run() {
	      while (true) {
		if (config.assembly.alignment_is_built()) {
		  setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

		  AnnotationLoader al = new AnnotationLoader(config, true);
		  al.addObserver(deep_sigh);

// 		  PadMap pm = config.assembly.get_padmap();
// 		  for (RefGene rg : config.refgenes) {
// 		    rg.consensus_setup(pm);
// 		  }
// 		  init_exon_navigation();

		  init_hetero_summary();

		  break;
		} else if (config.assembly.has_error()) {
		  setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		  repaint();
		  break;
		} else {
 		  try {
		    //		    System.err.println("waiting for asm...");
 		    Thread.sleep(100);
 		  } catch (InterruptedException e) {}
		}
	      }
	    }
	  };
	new Thread(later).start();
	//	javax.swing.SwingUtilities.invokeLater(later);

	if (jf != null) {
	  jf.setTitle(config.title);
	  jf.repaint();
	}

	canvas.repaint();

      } else {
	JOptionPane.showMessageDialog(this,
				      "Specify a gene symbol or a location in the format chrX, chrX:start, or chrX:start-end",
				      "Error",
				      JOptionPane.ERROR_MESSAGE);
	setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }

  public void init_hetero_summary() {
    config.hetero_summary = new HeteroSummary(config, true);
    canvas.set_hetero_summary(config.hetero_summary);
  }


}


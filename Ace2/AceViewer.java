// .ace-format alignment viewer
// Michael Edmonson 1997-

package Ace2;

import javax.swing.*;
import java.util.zip.*;
import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Font;
import java.awt.Color;

import java.awt.event.*;
import java.util.*;
import java.net.*;
import java.io.*;
import java.util.prefs.*;

import java.lang.reflect.Field;

import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;
//import Ace2.security.RemoteSecurityHandler;
//import Ace2.security.SecurityToken;

import TCGA.URLLauncher;

public class AceViewer extends JFrame implements WindowListener,ActionListener,KeyListener {
  public boolean standalone = false;
  private AcePanel ap;

  private JMenuItem m_find, m_primers, m_blast, m_tn;
  
  private ButtonGroup view_by = new ButtonGroup();
  private ButtonGroup font_size = new ButtonGroup();
  private AceViewerConfig config = new AceViewerConfig();
  // unless overridden

  public AceViewer (AceViewerConfig config) {
    this.config = config;
    if (config.LOCAL_FILE_MODE && config.EXIT_ON_CLOSE) setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    ap = new AcePanel(config);
    //    new MemoryMonitor();
    setup();
  }

  void setup () {
    //    addFocusListener(ap);
    Funk.LookAndFeeler.set_native_lookandfeel();
    if (config.title != null) {
      setTitle(config.title);
    } else if (ap.filename == null) {
      setTitle("alignment viewer");
    } else {
      setTitle(Funk.Str.basename(ap.filename));
      // don't show full pathname [public web]
    }
    addWindowListener(this);
    addKeyListener(this);

    JMenuBar mb = new JMenuBar();
    setJMenuBar(mb);
    JMenu m = new JMenu("Tools");
    m.add(m_find = new JMenuItem("Find..."));
    m_find.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));

    m_find.addActionListener(this);

    JMenu jm = new JMenu("Show sequence IDs...");
    m.add(jm);
    
    JMenuItem jmi = new JMenuItem("Aligned to visible region");
    jmi.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  show_sequence_ids(false);
	}
      });
    jm.add(jmi);

    jmi = new JMenuItem("Visible only");
    jmi.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  show_sequence_ids(true);
	}
      });
    jm.add(jmi);

    m_tn = new JMenuItem("Set tumor/normal status...");
    m_tn.addActionListener(this);
    m.add(m_tn);

    if (false) {
      m.add(m_primers = new JMenuItem("Design primers..."));
      m.add(m_blast = new JMenuItem("BLAST SNP..."));
      m_primers.addActionListener(this);
      m_blast.addActionListener(this);
    }

    mb.add(m);

    m = new JMenu("View");
    JCheckBoxMenuItem j = null;
    
    JMenu sm = new JMenu("Display sequences...");
    m.add(sm);

    //    String[] labels = {"by alignment position", "by name"};
    String[] labels = {"sort by name", "sort by alignment position"};

    ItemListener il_view_by = new ItemListener () {
	// begin ItemListener stub
	public void itemStateChanged(ItemEvent e) {
	  JCheckBoxMenuItem i = (JCheckBoxMenuItem) e.getItem();
	  
	  config.assembly.set_display_by_position(i.getText().equals("sort by alignment position"));
	  //	  ap.sequence_display_by_position(i.getText().equals("by alignment position"));
	  repaint();

	}
	// end ItemListener stub
      };

    for (int i=0; i < labels.length; i++) {
      sm.add(j = new JCheckBoxMenuItem(labels[i]));
      j.addItemListener(il_view_by);
      view_by.add(j);
    }
    j.setSelected(true);

    sm.add(new JSeparator());

    jmi = new JCheckBoxMenuItem("Hide optical/PCR duplicates");
    jmi.addItemListener(new ItemListener() {
	public void itemStateChanged(ItemEvent e) {
	  JCheckBoxMenuItem i = (JCheckBoxMenuItem) e.getItem();
	  config.HIDE_OPTICAL_PCR_DUPLICATES = i.isSelected();
	  ap.get_canvas().repaint();
	}
      });
    jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK));
    jmi.setSelected(true);
    sm.add(jmi);

    jmi = new JCheckBoxMenuItem("Hide skip-only alignments");
    jmi.addItemListener(new ItemListener() {
	public void itemStateChanged(ItemEvent e) {
	  JCheckBoxMenuItem i = (JCheckBoxMenuItem) e.getItem();
	  config.HIDE_SKIP_ONLY_ALIGNMENTS = i.isSelected();
	  ap.get_canvas().repaint();
	}
      });
    jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
    //    jmi.setSelected(true);
    sm.add(jmi);


    jmi = new JCheckBoxMenuItem("Shade sequence IDs by mapping quality");
    jmi.addItemListener(new ItemListener() {
	public void itemStateChanged(ItemEvent e) {
	  JCheckBoxMenuItem i = (JCheckBoxMenuItem) e.getItem();
	  config.SHADE_SEQUENCE_IDENTIFIERS_BY_MAPQ = i.isSelected();
	  ap.get_canvas().repaint();
	}
      });
    //    jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK));
    jmi.setSelected(config.SHADE_SEQUENCE_IDENTIFIERS_BY_MAPQ);
    sm.add(jmi);

    JMenu sm2 = new JMenu("SNP/indel browsing...");
    sm.add(sm2);
    
    jmi = new JCheckBoxMenuItem("only display reads aligned at SNP site");
    jmi.addItemListener(new ItemListener() {
	public void itemStateChanged(ItemEvent e) {
	  JCheckBoxMenuItem i = (JCheckBoxMenuItem) e.getItem();
	  config.CLAMP_SNP_VIEW = i.isSelected();
	  ap.get_canvas().repaint();
	}
      });
    jmi.setSelected(config.CLAMP_SNP_VIEW);
    jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.ALT_MASK));
    sm2.add(jmi);

    jmi = new JCheckBoxMenuItem("only display non-reference reads aligned at SNP site");
    jmi.addItemListener(new ItemListener() {
	public void itemStateChanged(ItemEvent e) {
	  JCheckBoxMenuItem i = (JCheckBoxMenuItem) e.getItem();
	  config.CLAMP_SNP_VIEW_NONREFERENCE = i.isSelected();
	  ap.get_canvas().repaint();
	}
      });
    jmi.setSelected(config.CLAMP_SNP_VIEW_NONREFERENCE);
    jmi.setMnemonic(KeyEvent.VK_R);
    jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));

    sm2.add(jmi);

    jmi = new JMenuItem("Set minimum mapping quality for display...");
    jmi.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  //	  JCheckBoxMenuItem i = (JCheckBoxMenuItem) e.getItem();
	  //	  config.HIDE_OPTICAL_PCR_DUPLICATES = i.isSelected();
	  String value = JOptionPane.showInputDialog("Minimum mapping quality required for read display:", config.MINIMUM_MAPQ_FOR_DISPLAY);
	  try {
	    config.MINIMUM_MAPQ_FOR_DISPLAY = Integer.parseInt(Funk.Str.trim_whitespace(value));
	  } catch (Exception ex) {};

	  ap.get_canvas().repaint();
	}
      });
    //    jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK));
    //    jmi.setSelected(true);
    sm.add(jmi);

    

    sm = new JMenu("Font size...");

    ItemListener il_font = new ItemListener () {
	// begin ItemListener stub
	public void itemStateChanged(ItemEvent e) {
	  JCheckBoxMenuItem i = (JCheckBoxMenuItem) e.getItem();
	  ap.set_font_size(Integer.parseInt(i.getText()));
	}
	// end ItemListener stub
      };

    for (int i=8; i <= 14; i += 2) {
      j = new JCheckBoxMenuItem(Integer.toString(i));
      sm.add(j);
      font_size.add(j);
      j.addItemListener(il_font);
    }
    j.setSelected(true);
    m.add(sm);
    mb.add(m);

    m = new JMenu("Help");

    jmi = add_menuitem(m, "Documentation", KeyEvent.VK_H, KeyStroke.getKeyStroke(KeyEvent.VK_F1,0));
    jmi.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  URLLauncher.launch_modified_url("/goldenPath/bamview/documentation/index.html", "bamview_docs");
	}
      });

    m.add(jmi = new JMenuItem("Short read alignment viewer index"));
    jmi.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  URLLauncher.launch_modified_url("/cgi-bin/bamview", "bamview");
	}
      });

    m.add(jmi = new JMenuItem("Cancer Genome Workbench"));
    jmi.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  URLLauncher.launch_modified_url("https://cgwb.nci.nih.gov/", "cgwb");
	}
      });

    m.add(new JSeparator());

    m.add(jmi = new JMenuItem("About"));
    jmi.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
 	  JOptionPane.showMessageDialog(null,
 					get_version_string(),
 					"About",
 					JOptionPane.INFORMATION_MESSAGE);
	}
      });


    mb.add(m);


    setLayout(new BorderLayout());
    add("Center", ap);
    pack();
    //    Funk.Gr.respectful_resize(this, 0.6, 0.5);
    Funk.Gr.respectful_resize(this, 0.98, 0.9);
    //    System.err.println("setting visible");  // debug

    setVisible(true);

    if (config.start_unpadded_offset != 0) {
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
	  public void run() {
	    while (true) {
	      AcePanel ap = get_acepanel();
	      Assembly asm = ap.get_assembly();
	      PadMap pm = null;
	      if (asm.is_loaded()) {
		if (asm.alignment_is_built()) pm = asm.get_padmap();
		if (asm.has_error()) {
		  System.err.println("error, breaking. msg=" + asm.get_error_message());  // debug
		  ap.get_canvas().repaint();
		  break;
		}
	      }

	      if (pm == null) {
		System.err.println("waiting for PadMap...");  // debug
		try {
		  Thread.sleep(200);
		} catch (InterruptedException e) {}
	      } else {
		int upo = (config.start_unpadded_offset - config.ruler_start);
		int po = pm.get_unpadded_to_padded(upo);
		SNPList sl = new SNPList();
		sl.addElement(new SNP(po, 0.0));
		config.assembly.set_snps(sl);
		ap.get_canvas().center_on(po);
		ap.get_canvas().repaint();
		//		System.err.println("start center on " + config.start_unpadded_offset);  // debug
		break;
	      } 

	    }
	  }
	});
    }


    ap.get_canvas().requestFocusInWindow();
    // for keyboard commands

    Funk.Gr.anchor(this, "nw");
  }

  public static void main (String [] argv) {
    // when run as a standalone app
    // FIX ME: flags for local I/O, etc
    try {
      Funk.LookAndFeeler.set_native_lookandfeel();
      SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);

      AceViewer av = null;
      MarkupReader mr = null;

      AceViewerConfig avc = new AceViewerConfig();

      String quality_url = null;
      String markup_url = null;
      boolean compress_mode = false;
      avc.region = new SAMRegion();

      URLLauncher.set_url("https://cgwb.nci.nih.gov/cgi-bin/bamview");
      // default root (public server)

      String contig = null;
      SNPList sl = null;
      String ace_url = null;

      for (int i=0; i < argv.length; i++) {
	if (argv[i].substring(0,1).equals("-")) {
	  if (argv[i].equals("-contig")) {
	    contig = "Contig" + argv[++i];
	  } else if (argv[i].equals("-compress")) {
	    // must be specified before any URLs (HACK)
	    compress_mode = true;
	  } else if (argv[i].equals("-generate-consensus")) {
	    avc.GENERATE_CONSENSUS = true;
	  } else if (argv[i].equals("-poffset")) {
	    // start viewer on specified padded offset
	    avc.start_padded_offset = Integer.parseInt(argv[++i]);
	  } else if (argv[i].equals("-uoffset")) {
	    // start viewer on specified unpadded offset
	    avc.start_unpadded_offset = Integer.parseInt(argv[++i]);
	  } else if (argv[i].equals("-offset")) {
	    // just one SNP, obsolete
	    sl = new SNPList();
	    sl.addElement(new SNP(Integer.parseInt(argv[++i]), 1.0));
	  } else if (argv[i].equals("-offsets")) {
	    // list of SNPs
	    sl = new SNPList();
	    StringTokenizer st = new StringTokenizer(argv[++i], ",");
	    while (st.hasMoreTokens()) {
	      sl.addElement(new SNP(Integer.parseInt(st.nextToken()), 1.0));
	    }
	  } else if (argv[i].equals("-compress-introns")) {
	    avc.COMPRESS_INTRONS = true;
	  } else if (argv[i].equals("-no-quality-paint")) {
	    avc.ENABLE_QUALITY_PAINT = false;
	  } else if (argv[i].equals("-disable-mismap-filter")) {
	    avc.snp_config.ENABLE_MISMAP_FILTER = false;
	  } else if (argv[i].equals("-require-tags")) {
	    if (avc.sam_tag_filter != null) {
	      System.err.println("ERROR: only one of -require-tags and -optional-tags may be specified");  // debug
	      System.exit(1);
	    }
	    avc.sam_tag_filter = new SAMTagFilter(true);
	    avc.sam_tag_filter.parse(argv[++i]);
	  } else if (argv[i].equals("-optional-tags")) {
	    if (avc.sam_tag_filter != null) {
	      System.err.println("ERROR: only one of -require-tags and -optional-tags may be specified");  // debug
	      System.exit(1);
	    }
	    avc.sam_tag_filter = new SAMTagFilter(false);
	    avc.sam_tag_filter.parse(argv[++i]);
	  } else if (argv[i].equals("-convention")) {
	    SampleNamingConvention.import_convention(argv[++i]);
	  } else if (argv[i].equals("-min-display-mapq") ||
		     argv[i].equals("-min-mapq")
		     ) {
	    avc.MINIMUM_MAPQ_FOR_DISPLAY = Integer.parseInt(argv[++i]);
	  } else if (argv[i].equals("-mapq-id-shade")) {
	    avc.SHADE_SEQUENCE_IDENTIFIERS_BY_MAPQ = true;
	  } else if (argv[i].equals("-mapq-id-min-intensity")) {
	    Float intensity = Float.parseFloat(argv[++i]);
	    if (intensity >= 0 && intensity <= 1) {
	      ColorIntensifier.MINIMUM_INTENSITY_PERCENT = intensity;
	    } else {
	      System.err.println("-mapq-id-min-intensity must be a fractional value between 0 and 1");  // debug
	      System.exit(1);
	    }

	  } else if (argv[i].equals("-no-db")) {
	    avc.ENABLE_JDBC = false;
	  } else if (argv[i].equals("-db-server")) {
	    JDBCCache.UCSC_DB_SERVER = argv[++i];
	  } else if (argv[i].equals("-db-database")) {
	    JDBCCache.UCSC_DB_DATABASE = argv[++i];
	  } else if (argv[i].equals("-db-user")) {
	    JDBCCache.UCSC_DB_USERNAME = argv[++i];
	  } else if (argv[i].equals("-db-password")) {
	    JDBCCache.UCSC_DB_PASSWORD = argv[++i];
	  } else if (argv[i].equals("-db-cache-flush")) {
	    System.err.println("flushing JDBC cache");  // debug
	    JDBCCache cache = new JDBCCache();
	    cache.flush_cache();
	  } else if (argv[i].equals("-no")) {
	    //	    AcePanel.AUTO_OVERVIEW = false;
	  } else if (argv[i].equals("-consensus-label")) {
	    avc.CONSENSUS_TAG = argv[++i];
	  } else if (argv[i].equals("-drag-scale")) {
	    avc.DRAG_SCALE_FACTOR = Integer.parseInt(argv[++i]);
	  } else if (argv[i].equals("-drag-button")) {
	    int button = Integer.parseInt(argv[++i]);
	    if (button == 1) {
	      avc.DRAG_BUTTON_MASK = MouseEvent.BUTTON1_MASK;
	    } else if (button == 2) {
	      avc.DRAG_BUTTON_MASK = MouseEvent.BUTTON2_MASK;
	    } else if (button == 3) {
	      avc.DRAG_BUTTON_MASK = MouseEvent.BUTTON3_MASK;
	    } else {
	      System.err.println("invalid button");  // debug
	    }
	  } else if (argv[i].equals("-title")) {
	    avc.title = argv[++i];
	  } else if (argv[i].equals("-ruler-start")) {
	    avc.ruler_start = Integer.parseInt(argv[++i]);
	  } else if (argv[i].equals("-no-snp")) {
	    AcePanel.set_snp_info(false);
	  } else if (argv[i].equals("-homepage")) {
	    URLLauncher.set_url(argv[++i]);
	  } else if (argv[i].equals("-url")) {
	    ace_url = argv[++i];
	  } else if (argv[i].equals("-qurl")) {
	    quality_url = argv[++i];
	  } else if (argv[i].equals("-murl")) {
	    markup_url = argv[++i];
	  } else if (argv[i].equals("-tv")) {
	    avc.ENABLE_TRACE_VIEWER = true;
	  } else if (argv[i].equals("-no-duplicates")) {
	    avc.LOAD_OPTICAL_PCR_DUPLICATES = false;
	  } else if (argv[i].equals("-ace")) {
	    ace_url = localfile_to_url(argv[++i]);
	  } else if (argv[i].equals("-qual")) {
	    quality_url = localfile_to_url(argv[++i]);
	  } else if (argv[i].equals("-markup")) {
	    markup_url = localfile_to_url(argv[++i]);
	  } else if (argv[i].equals("-fixed-font")) {
	    avc.FIXED_FONT_TYPEFACE = argv[++i];
	  } else if (argv[i].equals("-var-font")) {
	    avc.VARIABLE_FONT_TYPEFACE = argv[++i];
	  } else if (argv[i].equals("-fixed-font")) {
	    avc.ENABLE_VARIABLE_WIDTH_FONTS = false;
	  } else if (argv[i].equals("-restrict")) {
	    SAMResource.add_restrict_string(argv[++i]);
	  } else if (argv[i].equals("-restrict-no")) {
	    SAMResource.add_negative_restrict_string(argv[++i]);
	  } else if (argv[i].equals("-overlap")) {
	    SAMResource.set_restrict_overlap(Integer.parseInt(argv[++i]));
	  } else if (argv[i].equals("-restrict-f")) {
	    SAMResource.set_restrict_fr(true);
	  } else if (argv[i].equals("-restrict-r")) {
	    SAMResource.set_restrict_fr(false);
	  } else if (argv[i].equals("-name")) {
	    // start region name
	    avc.region.tname = new String(argv[++i]);
	  } else if (argv[i].equals("-start")) {
	    // start region start
	    avc.region.range.start = Integer.parseInt(argv[++i]);
	  } else if (argv[i].equals("-nib")) {
	    NIB.DEFAULT_NIB_DIR = new String(argv[++i]);
	    avc.reference_sequence = new NIB();
	  } else if (argv[i].equals("-trace-dir")) {
	    avc.CHROMAT_DIR = new String(argv[++i]);
	    File dir = new File(avc.CHROMAT_DIR);
	    if (dir.exists() && dir.isDirectory()) {
	      //	      System.err.println("OK");  // debug
	    } else {
	      System.err.println("ERROR: " + avc.CHROMAT_DIR + " is not a directory");  // debug
	      System.exit(1);
	    }
	  } else if (argv[i].equals("-fasta")) {
	    String thing = argv[++i];
	    File f = new File(thing);
	    if (f.isFile()) {
	      // .fai-indexed FASTA file
	      avc.reference_sequence = new FASTAIndexedFAI(thing);
	    } else if (f.isDirectory()) {
	      avc.reference_sequence = new FASTADirectory(thing);
	    } else {
	      System.err.println("ERROR: not a file/directory: " + thing);  // debug
	    }
	    //	    System.err.println("refseq="+avc.reference_sequence);  // debug
	  } else if (argv[i].equals("-2bit")) {
	    avc.reference_sequence = new TwoBitFile(argv[++i]);
	  } else if (argv[i].equals("-jz")) {
	    avc.snp_config.MIN_QUALITY = 15;
	    avc.snp_config.MIN_ALT_ALLELE_COUNT = 2;
	    avc.snp_config.MIN_MINOR_ALLELE_FREQUENCY = 0f;
	  } else if (argv[i].equals("-center")) {
	    int center = Integer.parseInt(argv[++i]);
	    int FLANK = avc.DEFAULT_INITIAL_VIEW_NT / 2;
	    int start = center - FLANK;
	    int end = center + FLANK;
	    if (start < 1) start = 1;
	    avc.start_unpadded_offset = center;
	    avc.region.range.start = start;
	    avc.region.range.end = end;
	  } else if (argv[i].equals("-end")) {
	    // start region end
	    avc.region.range.end = Integer.parseInt(argv[++i]);
	  } else if (argv[i].equals("-sam") || argv[i].equals("-bam")) {
	    SAMResource sr = new SAMResource();
	    String thing = argv[++i];
	    URL u = generate_url(thing);
	    if (u != null) {
	      sr.url = u;
	    } else {
	      File f = new File(thing);
	      if (f.exists()) {
		if (SAMUtils.is_bam_indexed(f)) {
		  sr.file = f;
		  avc.LOCAL_FILE_MODE = true;
		} else {
		  System.err.println("ERROR: BAM file " + f + " is not indexed; use \"samtools index file.bam\" to generate.");  // debug
		  System.exit(1);
		}
	      } else {
		System.err.println("ERROR: file not found: " + thing);  // debug
		System.exit(1);
	      }
	    }
	    sr.detect_sample_id();
	    avc.sams.add(sr);
	  } else if (argv[i].equals("-color")) {
	    // specify custom color for reads in previously-specified "-bam"
	    String cname = new String(argv[++i]);
	    ColorParser cp = new ColorParser(cname);
	    Color c = cp.get_color();
	    System.err.println("color name="+cname+ " result="+c);  // debug
	    int size = avc.sams.size();
	    if (size > 0) {
	      SAMResource sr = avc.sams.get(size - 1);
	      sr.custom_color = c;
	    } else {
	      System.err.println("ERROR: -color must be specified after -bam");  // debug
	      System.exit(1);
	    }
	  } else if (argv[i].equals("-sample")) {
	    if (avc.sams.size() > 0) {
	      avc.sams.get(avc.sams.size() - 1).import_data(SAMResourceTags.SAM_SAMPLE, argv[++i]);
	    } else {
	      System.err.println("ERROR: must specify -sample after -bam");  // debug
	      System.exit(1);
	    }
	  } else if (argv[i].equals("-tn")) {
	    // specifies tumor or normal for preceding SAMResource
	    if (avc.sams.size() > 0) {
	      SAMResource sr = avc.sams.get(avc.sams.size() - 1);
	      sr.set_tumor_normal(argv[++i]);
	    } else {
	      System.err.println("ERROR: -tn must be specified after -sam");  // debug
	    }
	  } else if (argv[i].equals("-verbose-refgene")) {
	    RefGene.VERBOSE = true;
	  } else if (argv[i].equals("-version")) {
	    print_version();
	    System.exit(0);
	  } else if (argv[i].equals("-uc-reference")) {
	    avc.UPPERCASE_REFERENCE_SEQUENCE = true;
	  } else if (argv[i].equals("-dump-fonts")) {
	    dump_fonts();
	    System.exit(0);
	  } else if (argv[i].equals("-color-background") ||
		     argv[i].equals("-color-sequence") ||
		     argv[i].equals("-color-softclip") ||
		     argv[i].equals("-color-border") ||
		     argv[i].equals("-color-label") ||
		     argv[i].equals("-color-summary-background") ||
		     argv[i].equals("-color-reference") ||
		     argv[i].equals("-color-ruler") 
		     ) {
	    color_setup(argv[i], argv[++i]);
	  } else if (argv[i].equals("-blat-db")) {
	    avc.BLAT_GENOME = new String(argv[++i]);
	  } else if (argv[i].equals("-indels-only")) {
	    // show indel-only reads
	    avc.DEFAULT_VIEW_INDELS_ONLY = true;
	  } else {
	    System.out.println("ERROR: unknown command-line argument " + argv[i]);  // debug
	    System.exit(1);
	  }
	}
      }

      if (markup_url != null) {
	try {
	  //	  mr = new MarkupReader(url_to_br(markup_url, compress_mode), av, false);
	  mr = new MarkupReader(url_to_br(markup_url, compress_mode), avc, false);
	  avc.ENABLE_JDBC = false;
	  // if annotations are loaded from a markup file, disable automated db lookups
	  // (since everything has already been specified)
	} catch (Exception e) {
	  System.err.println("URL error: " + e);  // debug
	}
      }

      if (mr == null && ace_url == null && avc.sams.size() == 0) {
	new Launcher(avc);
      } else {
	if (ace_url != null) {
	  try {
	    URL u = new URL(ace_url);
	    avc.title = u.getPath();
	    DataInputStream dis = url_to_dis(ace_url, compress_mode);
	    Ace ace = new Ace(dis);
	    avc.assembly = new Alignment(ace, avc);
	  } catch (Exception e) {
	    e.printStackTrace();
	    System.err.println("ace URL error: " + e);  // debug
	  }
	} else if (avc.sams.size() > 0) {

	  if (avc.reference_sequence == null && avc.target_sequence == null) {
	    System.err.println("Note: no reference sequence specified, generating consensus sequence.");  // debug
	    avc.GENERATE_CONSENSUS = true;
	  }

	  if (!avc.LOAD_OPTICAL_PCR_DUPLICATES) {
	    for (SAMResource sr : avc.sams) {
	      sr.set_load_duplicates(false);
	    }
	  }

	  if (mr == null) avc.region_default_setup();
	  // detect start region if one isn't specified
	  // DON'T do this in markup mode!

// 	  if (!avc.region.isValid()) {
// 	    //
// 	    //  no valid startup region specified
// 	    //

// 	    boolean set_to_first_read = true;

// 	    if (avc.region.tname != null) {
// 	      //
// 	      // user has provided a target reference sequence name.
// 	      // if valid, start at the first aligned reads for that sequence.
// 	      // if invalid, default to first read position below.
// 	      //
// 	      SAMResource sres = avc.sams.get(0);
// 	      sres.set_region(avc.region);
// 	      int end = avc.region.range.end;
// 	      // may be altered by call below
// 	      //	      System.err.println("end="+end);  // debug

// 	      CloseableIterator <SAMRecord> iterator = sres.get_iterator();
// 	      while (iterator.hasNext()) {
// 		SAMRecord sr = iterator.next();
// 		if (sr.getReadUnmappedFlag()) continue;
// 		avc.region.tname = sr.getReferenceName();
// 		avc.region.range.start = sr.getAlignmentStart();

// 		//		System.err.println("start="+avc.region.range.start + " spec end="+end);  // debug

// 		if (end > avc.region.range.start) {
// 		  //		  System.err.println("end view OK!");  // debug
// 		  //		  System.err.println("end="+end);  // debug
// 		  avc.region.range.end = end;
// 		} else {
// 		  avc.region.range.end = avc.region.range.start + avc.DEFAULT_INITIAL_VIEW_NT;
// 		}
// 		set_to_first_read = false;
// 		break;
// 	      }
// 	      sres.close();
// 	    }
	    
// 	    if (set_to_first_read) {
// 	      //
// 	      //  set start position to location of first aligned reads in 1st file.
// 	      //  HACK: would be "nicer" to pool all files and go to first reads there.
// 	      //
// 	      System.err.println("setting to first read");  // debug
// 	      SAMFileReader sfr = avc.sams.get(0).getSAMFileReader();
// 	      for (SAMRecord sr : sfr) {
// 		if (sr.getReadUnmappedFlag()) continue;
// 		avc.region.tname = sr.getReferenceName();
// 		avc.region.range.start = sr.getAlignmentStart();
// 		avc.region.range.end = avc.region.range.start + avc.DEFAULT_INITIAL_VIEW_NT;
// 		break;
// 	      }
// 	    }
// 	  }

	  SAMUtils.sam_config_setup(avc, avc.region);

	  if (avc.target_sequence == null) {
	    System.err.println("ERROR: no reference sequence specified");  // debug
	    System.exit(1);
	  }
	  
	  try {
	    //	    avc.assembly = new SAMAssembly(avc.sams, avc.target_sequence, avc.ruler_start, true);
	    avc.assembly = new SAMAssembly(avc, true);
	  } catch (Throwable t) {
	    new Funk.ErrorReporter(t);
	  }
	} else {
	  System.err.println("No alignment specified!");  // debug
	  System.exit(1);
	}

	av = new AceViewer(avc);

	if (av == null) {
	  System.out.println("Usage: AceViewer acefile [contig] [offset]");
	  System.exit(1);
	} else {
	  if (quality_url != null) {
	    try {
	      avc.fq = new FASTAQualityReader(url_to_br(quality_url, compress_mode), av, true);
	    } catch (Exception e) {
	      System.err.println("URL error: " + e);  // debug
	    }
	  }

	  av.standalone = true;
	}
      }
    } catch (Throwable t) {
      System.out.println("ERROR:" + t);  // debug
      t.printStackTrace();
      new ErrorReporter(t);
    }

  }

  public void die () {
    setVisible(false);
    dispose();
  }

  public void set_postgres (boolean status) {
    ap.set_postgres(status);
  }

  protected void finalize () {
    System.out.println("finalize!");  // debug
    if (standalone) {
      // hack, kills all instances...
      System.exit(1);
    } else {
      //      ap.finalize();
      // finalize() should NOT be called manually!
      die();
    }
  }

  // begin WindowListener stubs 
  public void windowActivated(WindowEvent we) {}
  public void windowClosed(WindowEvent we) {}
  public void windowClosing(WindowEvent we) {
    die();
  }
  public void windowDeactivated(WindowEvent we) {}
  public void windowDeiconified(WindowEvent we) {}
  public void windowIconified(WindowEvent we) {}
  public void windowOpened(WindowEvent we) {}
  // end WindowListener stubs 

  // begin ActionListener stubs 
  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();
    if (src.equals(m_find)) {
      ap.find();
    } else if (src.equals(m_tn)) {
      new TumorNormalStatusDialog((JFrame) Funk.Gr.getFrame(this), config.sams);
      repaint();
    }
  }
  // end ActionListener stubs 

  public AcePanel get_acepanel () {
    return ap;
  }

  private static DataInputStream url_to_dis (String url_string, boolean compressed) throws IOException,MalformedURLException {
    URL url = new URL(url_string);
    if (url_string.endsWith(".gz")) compressed = true;
    if (false) {
      System.err.println("DEBUG");  // debug
      System.exit(1);
    }
    System.err.println("url="+url + " compression="+compressed);  // debug
    InputStream is = url.openStream();
    if (compressed) is = new GZIPInputStream(is);
    return new DataInputStream(new BufferedInputStream(is));
  }

  private static BufferedReader url_to_br (String url_string, boolean compressed) throws IOException,MalformedURLException {
    URL url = new URL(url_string);
    if (url_string.endsWith(".gz")) compressed = true;
    System.err.println("url="+url + " compression="+compressed);  // debug
    InputStream is = url.openStream();
    if (compressed) is = new GZIPInputStream(is);
    return new BufferedReader(new InputStreamReader(is));
  }

  public AceViewerConfig get_config() {
    return config;
  }

  private static String localfile_to_url (String s) {
    // hack: assumes in local directory
    String result = null;
    try {
      File f = new File(s);
      result = "file://localhost/" + f.getCanonicalFile();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

// begin KeyListener stubs 
public void keyPressed(KeyEvent ke) {
  System.err.println("PRESS:"+ke);  // debug

}
public void keyReleased(KeyEvent ke) {}
public void keyTyped(KeyEvent ke) {}
// end KeyListener stubs 

  public static URL generate_url_OLD (String s) {
    URL u = null;
    try {
      u = new URL(s);
    } catch (MalformedURLException e) {
      try {
	u = new URL(localfile_to_url(s));
      } catch (Exception e2) {
	e2.printStackTrace();
	System.exit(1);
      }
    }
    return u;
  }

  public static URL generate_url (String s) {
    URL u = null;
    try {
      u = new URL(s);
    } catch (MalformedURLException e) {}
    return u;
  }

  public void show_sequence_ids (boolean visible_only) {
    AssemblyCanvas canvas = ap.get_canvas();
    ArrayList<AssemblySequence> seqs = visible_only ? canvas.get_visible_sequences() : canvas.get_aligned_sequences();
	  
    StringBuffer sb = new StringBuffer();
    for (AssemblySequence as : seqs) {
      sb.append(as.get_name() + "\n");
    }

    JFrame jf = new JFrame();
    jf.setTitle("Sequence IDs (note: suffix is generated)");
    jf.add("Center", new JScrollPane(new JTextArea(sb.toString())));
    jf.pack();
    jf.setVisible(true);
  }

  private JMenuItem add_menuitem(JMenu m, String label, int mnemonic, KeyStroke accelerator) {
    JMenuItem mi = new JMenuItem(label, mnemonic);
    if (accelerator != null) mi.setAccelerator(accelerator);
    m.add(mi);
    mi.addActionListener(this);
    return(mi);
  }

  private static void dump_fonts() {
    GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
    ArrayList<String> names = new ArrayList<String>();
    for(String fname : e.getAvailableFontFamilyNames()) {
      names.add(fname);
    }
    Collections.sort(names);
    for(String fname : names) {
      Font f = new Font(fname, Font.PLAIN, 12);
      System.out.println(fname);
    }
  }

  public static void print_version() {
    System.err.println(get_version_string());
  }

  public static String get_version_string() {
    BambinoProperties bp = new BambinoProperties();
    return "Bambino version " + AceViewerConfig.VERSION + "." + bp.get_build_number() + " (" + bp.get_build_date() + ")";
  }

  private static void color_setup (String arg, String cspec) {
    ColorParser cp = new ColorParser(cspec);
    Color c = cp.get_color();

    if (c == null) {
      System.err.println("ERROR: unknown color " + cspec);  // debug
      System.exit(1);
    }

    if (c != null) {
      // color spec is OK
      if (arg.equals("-color-background")) {
	AssemblyCanvas.BACKGROUND_COLOR = c;
      } else if (arg.equals("-color-sequence")) {
	AssemblyCanvas.SEQUENCE_COLOR = c;
      } else if (arg.equals("-color-softclip")) {
	AssemblyCanvas.TRIMMED_COLOR = c;
      } else if (arg.equals("-color-border")) {
	AssemblyCanvas.BORDER_COLOR = c;
      } else if (arg.equals("-color-label")) {
	AssemblyCanvas.SEQID_COLOR = c;
      } else if (arg.equals("-color-summary-background")) {
	AssemblyCanvas.SUMMARY_PANEL_BACKGROUND_COLOR = c;
      } else if (arg.equals("-color-reference")) {
	AssemblyCanvas.CONSENSUS_COLOR = c;
      } else if (arg.equals("-color-ruler")) {
	AssemblyCanvas.RULER_COLOR = c;
      } else {
	System.err.println("ERROR: unhandled color type " + cspec);  // debug
      }
    }
  }

}


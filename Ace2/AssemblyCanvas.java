package Ace2;
import java.util.*;
//import java.awt.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;

import java.io.*;
import Funk.DoWhatISayObservable;
import javax.swing.*;
import java.util.Random;

import TCGA.URLLauncher;
import TCGA.WebTools;
import TCGA.PopupListener;

import Trace.*;

import net.sf.samtools.*;
import net.sf.samtools.SAMRecord.*;

import static Ace2.Constants.ALIGNMENT_SKIPPED_CHAR_F;
import static Ace2.Constants.ALIGNMENT_SKIPPED_CHAR_R;
import static Ace2.Constants.ALIGNMENT_GAP_CHAR;
import static Ace2.Constants.ALIGNMENT_DELETION_CHAR;

public class AssemblyCanvas extends JPanel implements MouseListener,MouseMotionListener,MouseWheelListener,KeyListener,ActionListener,Observer {

  private boolean ENABLE_TRACE_VIEWER = false;

  private JScrollBar hscroll, vscroll;
  private ArrowPainter arrow_painter;
  private int line_height, draw_line, leftbuffer,
    contig_start_offset,
    contig_end_view,
    contig_end_offset,
    contig_chunk_size,
    font_width, font_ascent, font_descent;
  private int contig_start_view = 1;
  private Hashtable drawn_y = new Hashtable();
  private int font_size = 14;

  //  private Image offscreen = null;

  private long down_click_time;
  private DoWhatISayObservable notifier;
  private AcePanel ap;

  private Hashtable highlights = null;
  private Hashtable heterozygotes = null;
  // IDs to highlight

  private int cons_highlight_start, cons_highlight_end;

  //  private Font OUR_FONT = new Font(config.FIXED_FONT_TYPEFACE, Font.PLAIN, 14);
  private Font OUR_FONT = null;
  // this MUST be a fixed width font!

  private Font VAR_FONT = null;

  //static Color COVERAGE_COLOR = new Color(72,61,139);
  // darkSlateBlue
  //  static Color COVERAGE_COLOR = new Color(66,66,111);
  // Corn Flower Blue
  //  static Color COVERAGE_COLOR = new Color(35,35,142);
  static Color COVERAGE_COLOR = new Color(54,100,139);
  // SteelBlue4

  static Color BACKGROUND_COLOR = Color.black;
  //  static Color BACKGROUND_COLOR = Color.white;
  static Color SEQUENCE_COLOR = new Color(152,245,255); // CadetBlue1
  static Color SNP_COLOR = Color.red;
  static Color NONREF_FREQUENCY_COLOR = Color.yellow;

  static Color SUMMARY_PANEL_BACKGROUND_COLOR = new Color(25,25,25);
  // near-black to separate these panels from background
  
  // when painting quality values:
  static Color SEQUENCE_COLOR_QUAL = Color.black;
  static int QUALITY_THRESHOLD_LOWERCASE = 20;

  // FIX ME: move these to AceViewerConfig??:
  static Color SEQID_COLOR = Color.yellow;
  static Color HIGHLIT_COLOR = Color.white;
  static Color HETEROZYGOTE_COLOR = Color.green;
  static Color BORDER_COLOR = new Color(160,32,240);  // purple
  static Color CONSENSUS_COLOR = Color.yellow;
  static Color RULER_COLOR = Color.cyan;
  static Color TRIMMED_COLOR = Color.gray;
  static Color HIGHLIGHT_COLOR = new Color(0, 100, 0); // dark green
  static Color SELECTED_COLOR = new Color(0,0,139); // dark blue
  //  static Color NORMAL_COLOR = Color.green;
  //  static Color NORMAL_COLOR = Color.blue;
  //  static Color NORMAL_COLOR = new Color(106,90,205);  // slate blue
  //  static Color NORMAL_COLOR = new Color(123,104,238);  // medium slate blue
  //  static Color NORMAL_COLOR = new Color(132,112,255);  // light slate blue
  //  static Color NORMAL_COLOR = new Color(0,0,139); // dark blue
  static Color NORMAL_COLOR = new Color(0,0,205); // medium blue
  //  static Color NORMAL_COLOR = new Color(173,216,230); // light blue
  static Color TUMOR_COLOR = Color.red;
  static Color TUMOR_RECURRENT_COLOR = new Color(255,0,255);  // fuschia

  static Color INTRON_SPLICE_COLOR = new Color(255,70,0);

  private final static int MAX_INTENSITY_QUALITY = 40;

  final static private int TYPE_BLANK = 0;
  final static private int TYPE_TRIM = 1;
  final static private int TYPE_SEQ_AGREE = 2;
  final static private int TYPE_SEQ_DISAGREE = 3;

  private AceViewerConfig config;

  private static Color[] quality_colors;
  private int wheel_motion_start_center=0;

  private static final String LABEL_TRACE_VIEWER = "View chromatogram";
  private static final String LABEL_DEBUG_PEAK_AMP_RATIO = "Show alternate peak amplitude ratio";
  private static final String LABEL_PROTEIN_CHANGE = "Check for protein change";
  private static final String LABEL_SHOW_ONLY_ALIGNED = "Show only reads aligned at this site";
  private static final String LABEL_SHOW_ONLY_ALIGNED_NO_SKIPS = "Show only reads aligned at this site (excluding skips)";
  private static final String LABEL_SHOW_ONLY_ALIGNED_NO_SKIPS_BUT_CONTAINING_SKIP = "Show only reads aligned at this site (excluding skips, but containing skip)";
  private static final String LABEL_SHOW_ONLY_ALIGNED_NO_SKIPS_BUT_CONTAINING_SKIP2 = "Show only reads aligned at this site (excluding skips, but containing skip and target end)";
  private static final String LABEL_SHOW_ONLY_NONREFERENCE = "Show only non-reference sequences at this site";
  private static final String LABEL_SHOW_ONLY_INDEL = "Show only reads containing indels (anywhere in alignment)";

  private static final String LABEL_NONREF_FREQUENCY = "non-reference frequency";
  //  private static final String LABEL_ASSEMBLY_COVERAGE = "assembly coverage";
  private static final String LABEL_ASSEMBLY_COVERAGE = "overview";
  private static final String LABEL_DBSNP = "dbSNP";

  private JMenuItem jmi_blat, jmi_blat_mate;
  //  private String BLAT_BADGER_LABEL_UID = "medmonso";
  private String BLAT_BADGER_LABEL_UID = "mparker";
  // "that would be the badger"

  private PopupListener pul;

  private static final String KEY_NONREF_FREQUENCY = "__nonref_freq";
  private static final String KEY_OVERVIEW = "__overview";
  private static final String KEY_RULER = "__ruler";
  private static final String KEY_REFSEQ = "__refseq";


  private HashSet<String> refseq_ids = new HashSet<String>();
  private ProgressInfo info;

  private int sequences_drawn;
  private ArrayList<AssemblySequence> aligned_seqs, visible_seqs;

  private JCheckBoxMenuItem cb_show_only_aligned, cb_show_only_aligned_no_skips, cb_show_only_nonreference, cb_show_only_aligned_no_skips_but_containing_skip, cb_show_only_indel, cb_show_only_aligned_no_skips_but_containing_skip2;

  private boolean first_paint;

  private SequenceViewLock svl;
  private HeteroSummary hs = null;

  private FontWidthTracker fwt = null;

  private TraceViewerCache tvc = new TraceViewerCache();

  public AssemblyCanvas (AcePanel ap, AceViewerConfig config, JScrollBar h, JScrollBar v) {
    this.ap = ap;
    addMouseListener(this);
    addMouseMotionListener(this);
    addKeyListener(this);
    set_config(config);

    FontWidthTracker.add_tracked_string(LABEL_NONREF_FREQUENCY);
    FontWidthTracker.add_tracked_string(LABEL_ASSEMBLY_COVERAGE);

    svl = new SequenceViewLock(h, v);
    new Filfre(this);

    if (false) {
      // for manuscript screenshots
      System.err.println("manuscript mode");  // debug
      SELECTED_COLOR = new Color(230,220,210);
      HIGHLIGHT_COLOR = new Color(46,139,87);
      CONSENSUS_COLOR = Color.green;
      SEQID_COLOR = Color.black;
      BACKGROUND_COLOR = new Color(250,240,230);
      SEQUENCE_COLOR = Color.blue;
      SNP_COLOR = Color.red;
      BORDER_COLOR = new Color(160,32,240);  // purple
      RULER_COLOR = Color.black;
      TRIMMED_COLOR = Color.gray;
    }

    this.hscroll = h;
    this.vscroll = v;

    AdjustmentListener al = new AdjustmentListener() {
	public void adjustmentValueChanged(AdjustmentEvent e) {
	  scroll_paint();
	}
      };
    h.addAdjustmentListener(al);
    v.addAdjustmentListener(al);

    set_font_size(14);
    // calculate one-time font-based values

    notifier = new DoWhatISayObservable();
    
    addMouseWheelListener(this);
    init_quality_colors();

    JPopupMenu jpm = new JPopupMenu();
    JMenuItem jmi;

    if (config.ENABLE_TRACE_VIEWER) {
      jpm.add(jmi = new JMenuItem(LABEL_TRACE_VIEWER));
      jmi.addActionListener(this);

      jpm.add(jmi = new JMenuItem(LABEL_DEBUG_PEAK_AMP_RATIO));
      jmi.addActionListener(this);
    }

    jpm.add(jmi = new JMenuItem(LABEL_PROTEIN_CHANGE));
    jmi.addActionListener(this);

    jpm.add(jmi = new JMenuItem("Display sequence info"));
    jmi.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  String id = get_mouse_sequence_id(pul.getMouseEvent());
	  display_sam_info(id);
	}
      });

    if (config.LOCAL_FILE_MODE) {
      jpm.add(jmi = new JMenuItem("Copy sequence ID to clipboard"));
      jmi.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    String id = get_mouse_sequence_id(pul.getMouseEvent());
	    ClipboardSetter cs = new ClipboardSetter();
	    cs.setClipboardContents(id);
	  }
	});

      jpm.add(jmi = new JMenuItem("Copy reference base # to clipboard"));
      jmi.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    ClipboardSetter cs = new ClipboardSetter();
	    int consensus_pos = get_consensus_pos(pul.getMouseEvent());
	    int ref_base = get_reference_base_number(consensus_pos);
	    cs.setClipboardContents(ref_base);
	  }
	});

      
    }

    jpm.add(jmi = new JMenuItem("NCBI query for this sequence ID"));
    jmi.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    String id = get_mouse_sequence_id(pul.getMouseEvent());
	    String link = "http://www.ncbi.nlm.nih.gov/gquery/?term=" + id;
	    new URLLauncher(link, "ncbi");
	  }
	});


    jpm.add(new JSeparator());

    jpm.add(cb_show_only_aligned = new JCheckBoxMenuItem(LABEL_SHOW_ONLY_ALIGNED, false));
    cb_show_only_aligned.addActionListener(this);

    jpm.add(cb_show_only_aligned_no_skips = new JCheckBoxMenuItem(LABEL_SHOW_ONLY_ALIGNED_NO_SKIPS, false));
    cb_show_only_aligned_no_skips.addActionListener(this);

    jpm.add(cb_show_only_aligned_no_skips_but_containing_skip = new JCheckBoxMenuItem(LABEL_SHOW_ONLY_ALIGNED_NO_SKIPS_BUT_CONTAINING_SKIP, false));
    cb_show_only_aligned_no_skips_but_containing_skip.addActionListener(this);

    jpm.add(cb_show_only_aligned_no_skips_but_containing_skip2 = new JCheckBoxMenuItem(LABEL_SHOW_ONLY_ALIGNED_NO_SKIPS_BUT_CONTAINING_SKIP2, false));
    cb_show_only_aligned_no_skips_but_containing_skip2.addActionListener(this);

    jpm.add(cb_show_only_nonreference = new JCheckBoxMenuItem(LABEL_SHOW_ONLY_NONREFERENCE, false));
    cb_show_only_nonreference.addActionListener(this);

    jpm.add(cb_show_only_indel = new JCheckBoxMenuItem(LABEL_SHOW_ONLY_INDEL, config.DEFAULT_VIEW_INDELS_ONLY));
    cb_show_only_indel.addActionListener(this);


    jpm.add(new JSeparator());
    String uid = null;
    try {
      uid = System.getProperty("user.name");
    } catch (java.security.AccessControlException feh) {}

    Random r = new Random();
    String blat_base_label = (uid != null && uid.equals(BLAT_BADGER_LABEL_UID) && r.nextDouble() * 10 > 9 ? "BADGER" : "BLAT");
    jmi_blat = new JMenuItem(blat_base_label + " this read");
    jmi_blat.addActionListener(this);
    jpm.add(jmi_blat);

    jmi_blat_mate = new JMenuItem(blat_base_label + " this read's mate");
    jmi_blat_mate.addActionListener(this);
    jpm.add(jmi_blat_mate);

    pul = new PopupListener(this, jpm);
  }

  public void display_sam_info (String id) {
    for (AssemblySequence as : config.assembly.get_sequences()) {
      if (as.get_name().equals(id)) {
	SAMRecord sr = as.get_samrecord();
	if (sr != null) {
	  JFrame jf = new JFrame();
	  jf.setTitle("SAM info for " + id);
	  StringBuffer sb = new StringBuffer();
	  sb.append(id + "\n");
	  sb.append("               sequence: " + new String(sr.getReadBases()) + "\n");
	  sb.append("                  CIGAR: " + SAMUtils.cigar_to_string(sr.getCigar()) + "\n");
	  sb.append("         alignmentStart: " + sr.getAlignmentStart() + "\n");
	  sb.append("unclippedAlignmentStart: " + sr.getUnclippedStart() + "\n");
	  sb.append("           alignmentEnd: " + sr.getAlignmentEnd() + "\n");
	  sb.append("        mapping quality: " + sr.getMappingQuality() + "\n");
	  
	  int flags = sr.getFlags();
	  sb.append("                  flags: " + flags + "\n");
	  if (sr.getReadPairedFlag()) {
	    sb.append("                         - read is paired\n");
	    if (sr.getProperPairFlag()) sb.append("                         - read is mapped in a proper pair\n");
	    sb.append("                         - first read of pair?: " + (sr.getFirstOfPairFlag() ? "yes" : "no") + "\n");
	    sb.append("                         - second read of pair?: " + (sr.getSecondOfPairFlag() ? "yes" : "no") + "\n");
	    if (sr.getMateUnmappedFlag()) {
	      sb.append("                         - mate is unmapped\n");
	    } else {
	      sb.append("                         - mate strand: " + (sr.getMateNegativeStrandFlag() ? "-" : "+") + "\n");
	      sb.append("                         - mate align start: " + sr.getMateAlignmentStart() + "\n");
	    }
	  } else {
	    sb.append("                         - read is not paired\n");
	  }
	  if (sr.getReadUnmappedFlag()) sb.append("                         - read is unmapped\n");
	  // not likely to see this interactively  :P
	  sb.append("                         - strand: " + (sr.getReadNegativeStrandFlag() ? "-" : "+") + "\n");
	  sb.append("                         - primary alignment?: " + (sr.getNotPrimaryAlignmentFlag() ? "no" : "yes") + "\n");

	  sb.append("                         - fails vendor checks: " + (sr.getReadFailsVendorQualityCheckFlag() ? "yes" : "no") + "\n");
	  sb.append("                         - PCR/optical duplicate?: " + (sr.getDuplicateReadFlag() ? "yes" : "no") + "\n");

	  boolean has_tags = false;
	  try {
	    for (SAMTagAndValue tav : sr.getAttributes()) {
	      if (!has_tags) {
		sb.append("Tags:\n");
		has_tags = true;
	      }
	      sb.append("  " + tav.tag + ": " + tav.value + "\n");
	    }
	  } catch (NullPointerException e) {
	    System.err.println("feh: null ptr for getAttributes");  // debug
	  }
	  
	  jf.add("Center", new JScrollPane(new JTextArea(sb.toString())));
	  jf.pack();
	  jf.setVisible(true);
	}
      }
    }
  }

  public void set_font_size (int font_size) {
    int wanted=font_size;
    if (font_size < 1) font_size = 1;
    if (font_size > 30) font_size = 30;
    OUR_FONT = new Font(config.FIXED_FONT_TYPEFACE, Font.PLAIN, font_size);
    //    System.err.println("wanted="+wanted + " got:" + OUR_FONT.getSize());  // debug
    this.font_size = font_size;

    if (config.ENABLE_VARIABLE_WIDTH_FONTS) {
      VAR_FONT = new Font(config.VARIABLE_FONT_TYPEFACE, Font.PLAIN, font_size);
      fwt = null;
    }

    FontMetrics fm = getFontMetrics(OUR_FONT);
    line_height = fm.getHeight();
    //    font_width = fm.stringWidth("X");
    font_width = fm.stringWidth("W");
    // fixed width, any letter will do?
    // 8/2009: maybe not!
    //    System.err.println("font_height="+fm.getHeight());  // debug

    font_ascent = fm.getMaxAscent();
    font_descent = fm.getMaxDescent();
    //      System.out.println("Using " + gr.getFont().getName() + " font");
    arrow_painter = new ArrowPainter(font_width, line_height,
				     font_ascent, font_descent, BORDER_COLOR);
    leftbuffer = (10 * font_width) + arrow_painter.total_width;
    // temporary until alignment built
    repaint();
  }

  public void update (Observable o, Object arg) {
    if (o instanceof SAMResource) {
      info = (ProgressInfo) arg;
    }
    repaint();
  }

  public void reset () {
    // contig change, etc; reset private variables
    cons_highlight_start = cons_highlight_end = 0;
    highlights = heterozygotes = null;
  }

  public void addObserver (Observer o) {
    notifier.addObserver(o);
  }

  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    String title = config.assembly.get_title();
    String filename = title == null ? "assembly file" : Funk.Str.basename(title);

    if (config.assembly.is_loaded()) {
      if (config.assembly.has_error()) {
	//	Funk.Gr.centerText(g, "Error loading " + filename);
	Funk.Gr.centerText(this, g, config.assembly.get_error_message());
      } else if (config.assembly.alignment_is_built()) {
	if (config.assembly.is_empty()) {
	  //	  Funk.Gr.centerText(this, g, "Sorry, there are no sequences present in this alignment.");
	  Funk.Gr.centerText(this, g, "No sequences available in this region.");
	} else {
	  if (first_paint) {
	    // feh
	    //	    System.err.println("first paint " + contig_start_view);  // debug
	    if (contig_start_view == 1) contig_start_view = config.assembly.get_leftmost();
	    // set initial start view to leftmost aligned sequence
	    // (important if config.LOCK_VIEW_TO_ALIGNED_SEQUENCES is false)
	    //
	    // only set if not already overridden, e.g. by AceViewer startup position
	    //

	    //	    System.err.println("setting initial position to leftmost at " + contig_start_view);  // debug
	    first_paint = false;
	  }
	  
	  buffer_paint(g);
	}
      } else {
	//	Funk.Gr.centerText(g, "Building alignment...");
	String msg;
	if (info == null) {
	  msg = "Building alignment...";
	} else {
	  msg = "Building gapped alignment: " + info.total_processed + " reads (" + info.get_percent() + "%)";
	}
	Funk.Gr.centerText(this, g, msg);
	repaint();
      }
    } else {
      Funk.Gr.centerText(this, g, "Loading...");
    }
  }

  void scroll_paint () {
    // invoked when scrollbar value has changed
    contig_start_view = hscroll.getValue();
    repaint();
  }

  public void move_to (int i) {
    //
    // change view to start at given consensus offset
    //
    while (isValid() == false) {
      // need to wait for component to be laid out onscreen
      try {
	System.err.println("AssemblyCanvas spin...");  // debug
	Thread.sleep(100);
      } catch (InterruptedException e) {}
    }
    contig_start_view = i;
    //    if (contig_start_view < 1) contig_start_view = 1;
    if (contig_start_view < config.assembly.get_leftmost()) contig_start_view = config.assembly.get_leftmost();
    // i.e. if trying to center on an SNP early in the contig
    repaint();
    
    //    trace_spawner.move_all_to(i);
    // move all visible traces to this point
  }

  public int get_start_for_center (int pos) {
    return (pos - (contig_chunk_size() / 2));
  }

  public void center_on (int i) {
    // center view on this consensus offset.
    move_to(i - (contig_chunk_size() / 2));
  }

  int contig_chunk_size() {
    // return size of visible chunk of consensus, in bases.
    return(((getSize().width - leftbuffer) / font_width) + 1);
  }

  int contig_start_view () {
    return contig_start_view;
  }

  int contig_end_view () {
    return contig_end_view;
  }

  public void buffer_paint (Graphics gr) {
    // paint into (usually offscreen) buffer.
    Graphics2D g = (Graphics2D) gr;
    //    g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

    if (config.ENABLE_VARIABLE_WIDTH_FONTS) {
      FontWidthTracker.add_tracked_string(config.CONSENSUS_TAG);
      if (fwt == null) fwt = new FontWidthTracker(getFontMetrics(VAR_FONT), config.assembly);
      //    arrow_painter.left_margin = config.assembly.get_max_id_length() * font_width;
      // calculated here because alignment may not be built when constructor called
    }
    set_leftbuffer();

    Dimension d = getSize();

    draw_line = 1;
    drawn_y.clear();

    char[] cons = config.assembly.get_consensus_sequence();
    //    System.err.println("cev=" + contig_end_view + " ceo = " + contig_end_offset + " clen=" + cons.length);  // debug

    int leftmost = config.assembly.get_leftmost();
    // normally viewer's left bound is restricted by first aligned sequence,
    // however this can block display of exon translations, etc.
    //    System.err.println("LM="+leftmost);  // debug
    int left_border, right_border;
    if (config.LOCK_VIEW_TO_ALIGNED_SEQUENCES) {
      left_border = config.assembly.get_leftmost();
      right_border = config.assembly.get_rightmost();
    } else {
      left_border = 0;
      right_border = cons.length;
    }

    if (contig_start_view < left_border) {
      // we can be at an "illegal" offset if we flip a contig
      // while in negative consensus space
      contig_start_view = left_border;
    }

    contig_start_offset = contig_start_view - leftmost;
    // contig_start_view is actual consensus base number (numbered starting at 1)
    // contig_start_offset is start array offset
    contig_chunk_size = contig_chunk_size();
    // hack, again for grid geometry
    contig_end_view = contig_start_view + contig_chunk_size;
    contig_end_offset = contig_start_offset + contig_chunk_size;

    int too_far = contig_end_view - right_border;

    if (too_far > 0) {
      // Window has been resized at end of consensus, or contig switched.
      // Start offset + chunk size is past end; back off to visible area.
      //      System.err.println("TOO FAR");  // debug
      int csv2 = contig_start_view - too_far;
      if (csv2 > leftmost) {
	// if shift won't move out of range to left
	contig_start_view -= too_far;
	contig_end_view -= too_far;
	contig_start_offset -= too_far;
	contig_end_offset -= too_far;
      }
    }
    
    // determine which/how many sequences are visible at this point

    // FIX ME: cache this!
    // (much potential filtering, etc.)

      //    Vector aligned_seqs = config.assembly.get_visible(contig_start_view, contig_end_view, sequence_display_by_position);
    aligned_seqs = config.assembly.get_visible_sequences(contig_start_view, contig_end_view);

    if (config.HIDE_OPTICAL_PCR_DUPLICATES) {
      // BAM 
      ArrayList<AssemblySequence> filtered = new ArrayList<AssemblySequence>();
      SAMRecord sr;
      for (AssemblySequence as : aligned_seqs) {
	sr = as.get_samrecord();
	if (sr == null) {
	  filtered.add(as);
	} else {
	  if (sr.getDuplicateReadFlag() == false) filtered.add(as);
	}
      }
      aligned_seqs = filtered;
    }

    if (config.MINIMUM_MAPQ_FOR_DISPLAY > 0) {
      ArrayList<AssemblySequence> filtered = new ArrayList<AssemblySequence>();
      SAMRecord sr;
      for (AssemblySequence as : aligned_seqs) {
	sr = as.get_samrecord();
	if (sr == null) {
	  filtered.add(as);
	} else {
	  if (sr.getMappingQuality() >= config.MINIMUM_MAPQ_FOR_DISPLAY) filtered.add(as);
	}
      }
      aligned_seqs = filtered;
    }

    if (config.sam_tag_filter != null) {
      ArrayList<AssemblySequence> filtered = new ArrayList<AssemblySequence>();
      SAMRecord sr;
      for (AssemblySequence as : aligned_seqs) {
	sr = as.get_samrecord();
	if (sr == null) {
	  filtered.add(as);
	} else {
	  if (config.sam_tag_filter.check(sr, null)) filtered.add(as);
	}
      }
      aligned_seqs = filtered;
    }

    if (cb_show_only_indel.isSelected()) {
      //
      //  show only reads containing indels
      //  simple version (anywhere in read)
      //
      ArrayList<AssemblySequence> filtered = new ArrayList<AssemblySequence>();
      SAMRecord sr;
      for (AssemblySequence as : aligned_seqs) {
	sr = as.get_samrecord();
	if (sr == null) {
	  filtered.add(as);
	} else {
	  boolean usable = false;
	  Cigar c = sr.getCigar();
	  for (CigarElement ce : c.getCigarElements()) {
	    CigarOperator co = ce.getOperator();
	    if (
		co.equals(CigarOperator.INSERTION) ||
		co.equals(CigarOperator.DELETION) 
		) {
	      usable = true;
	      break;
	    }
	  }
	  if (usable) filtered.add(as);
	}
      }
      aligned_seqs = filtered;

    }

    SNPList snps = config.assembly.get_snps();

    if ((config.CLAMP_SNP_VIEW || config.CLAMP_SNP_VIEW_NONREFERENCE) &&
	snps != null &&
	contig_start_view == config.clamp_cons_start) {
      int current_snp_pos = snps.current_position();
      if (current_snp_pos >= contig_start_view &&
	  current_snp_pos <= contig_end_view) {
	// clamping enabled and currently-selected SNP is visible onscreen:
	// restrict view to sequences aligned at SNP site
	ArrayList<AssemblySequence> filtered = new ArrayList<AssemblySequence>();
	String str = get_consensus_for(current_snp_pos, 1).toUpperCase();
	char cbase = str.charAt(0);
	for (AssemblySequence as : aligned_seqs) {
	  if (!(as.get_asm_start() > current_snp_pos ||
		as.get_asm_end() < current_snp_pos)) {
	    // read is aligned at site
	    if (config.CLAMP_SNP_VIEW_NONREFERENCE) {
	      char base = Character.toUpperCase(as.get_base(current_snp_pos));
	      //	      System.err.println("hey now " + base + " " + cbase);  // debug
	      if (base != cbase) {
		filtered.add(as);
	      }
	    } else {
	      filtered.add(as);
	    }
	  }
	}
	aligned_seqs = filtered;
      }
    }

    if (config.CLAMP_MANUAL_VIEW &&
	cb_show_only_aligned.isSelected() && 
	contig_start_view == config.manual_clamp_cons_start &&
	config.manual_clamp_cons_pos >= contig_start_view && 
	config.manual_clamp_cons_pos <= contig_end_view
	) {
      //
      //  show only sequences aligned at manually-chosen site
      //
	ArrayList<AssemblySequence> filtered = new ArrayList<AssemblySequence>();
	for (AssemblySequence as : aligned_seqs) {
	  if (!(as.get_asm_start() > config.manual_clamp_cons_pos ||
		as.get_asm_end() < config.manual_clamp_cons_pos)) filtered.add(as);
	}
	aligned_seqs = filtered;
    } else {
      cb_show_only_aligned.setSelected(false);
    }

    if (config.CLAMP_MANUAL_NONREFERENCE &&
	cb_show_only_nonreference.isSelected() && 
	contig_start_view == config.clamp_nonref_cons_start &&
	config.clamp_nonref_cons_pos >= contig_start_view && 
	config.clamp_nonref_cons_pos <= contig_end_view
	) {
      //
      //  show only non-reference sequences at manually-chosen site
      //
	ArrayList<AssemblySequence> filtered = new ArrayList<AssemblySequence>();
	String str = get_consensus_for(config.clamp_nonref_cons_pos, 1).toUpperCase();
	char cbase = str.charAt(0);
	//	System.err.println("cpos=" + (config.clamp_nonref_cons_pos + config.ruler_start) + " cbase=" + cbase);  // debug
	//	System.err.println("cbase-1=" + get_consensus_for(config.clamp_nonref_cons_pos - 1, 1));  // debug
	//	System.err.println("cbase+1=" + get_consensus_for(config.clamp_nonref_cons_pos + 1, 1));  // debug
	
	for (AssemblySequence as : aligned_seqs) {
	  if (!(as.get_asm_start() > config.clamp_nonref_cons_pos ||
		as.get_asm_end() < config.clamp_nonref_cons_pos)) {
	    // in range
	    char base = Character.toUpperCase(as.get_base(config.clamp_nonref_cons_pos));
	    //	    System.err.println("  base=" + base);  // debug

	    if (base != cbase) {
	      //	      System.err.println("   adding");  // debug
	      filtered.add(as);
	    }
	  }
	}
	aligned_seqs = filtered;
    } else {
      cb_show_only_nonreference.setSelected(false);
    }

    if ((
	 cb_show_only_aligned_no_skips.isSelected() ||
	 cb_show_only_aligned_no_skips_but_containing_skip.isSelected() ||
	 cb_show_only_aligned_no_skips_but_containing_skip2.isSelected()
	 ) &&
	contig_start_view == config.manual_clamp_cons_start &&
	config.manual_clamp_cons_pos >= contig_start_view && 
	config.manual_clamp_cons_pos <= contig_end_view
	) {
      //
      //  show only sequences aligned at site,
      //  excluding reads showing skips
      //
      ArrayList<AssemblySequence> filtered = new ArrayList<AssemblySequence>();

      for (AssemblySequence as : aligned_seqs) {
	if (!(as.get_asm_start() > config.manual_clamp_cons_pos ||
	      as.get_asm_end() < config.manual_clamp_cons_pos)) {
	  // in range
	  char base = Character.toUpperCase(as.get_base(config.manual_clamp_cons_pos));
	  //	  System.err.println("  base=" + base);  // debug

	  if (!(base == ' ' ||
		base == ALIGNMENT_SKIPPED_CHAR_F ||
		base == ALIGNMENT_SKIPPED_CHAR_R)) {
	    boolean usable;
	    if (
		cb_show_only_aligned_no_skips_but_containing_skip.isSelected() ||
		cb_show_only_aligned_no_skips_but_containing_skip2.isSelected()
		) {
	      usable = false;
	      SAMRecord sr = as.get_samrecord();
	      if (sr != null) {
		Cigar c = sr.getCigar();
		for (CigarElement ce : c.getCigarElements()) {
		  if (ce.getOperator().equals(CigarOperator.SKIPPED_REGION)) {

		    if (cb_show_only_aligned_no_skips_but_containing_skip2.isSelected()) {
		      int last_base_before_skip = get_reference_base_number(config.manual_clamp_cons_pos);
		      //		      System.err.println("end base="+config.SKIP_END_BASENUM + " cpos=" + last_base_before_skip);

		      int skip_len = config.SKIP_END_BASENUM - last_base_before_skip - 1;
		      System.err.println("desired skip="+skip_len);  // debug

		      //		      System.err.println("FIX ME! skip_len="+skip_len + " event="+ce.getLength());  // debug
		      if (ce.getLength() == skip_len) usable = true;
		    } else {
		      usable = true;
		    }
		    break;
		  }
		}
	      } else {
		System.err.println("need SAMRecord");  // debug
	      }
	    } else {
	      usable = true;
	    }

	    if (usable) filtered.add(as);
	  }
	}
      }
      aligned_seqs = filtered;
    } else {
      cb_show_only_aligned_no_skips.setSelected(false);
      cb_show_only_aligned_no_skips_but_containing_skip.setSelected(false);
    }


    if (config.HIDE_SKIP_ONLY_ALIGNMENTS) {
      char[] buf = new char[contig_chunk_size];
      ArrayList<AssemblySequence> filtered = new ArrayList<AssemblySequence>();
      for (AssemblySequence as : aligned_seqs) {
	as.get_visible_sequence(buf, contig_start_view);
	boolean usable = false;
	for (int i = 0; i < buf.length; i++) {
	  if (!(buf[i] == ' ' ||
		buf[i] == ALIGNMENT_SKIPPED_CHAR_F ||
		buf[i] == ALIGNMENT_SKIPPED_CHAR_R)) {
	    usable = true;
	    break;
	  }
	}
	if (usable) filtered.add(as);
      }
      aligned_seqs = filtered;
    }

    svl.auto_lock(aligned_seqs);

    g.setFont(OUR_FONT);
    // it seems we have to do this every time...
    g.setColor(BACKGROUND_COLOR);
    g.fillRect(0,0,d.width,d.height);

    
    int lines_needed = aligned_seqs.size() + 4;
    // + 2 for ruler and consensus, 1 for (potential) separator, 1 for buffer
    int lines_available = d.height / line_height;
    // hack: need gridded geometry!
    //    System.out.println("la:" + lines_available + " ln:" + lines_needed);

    // highlight offsets of any identified SNPs
    highlight_snps(g, lines_needed);

    PadMap pm = config.assembly.get_padmap();

    if (config.ENABLE_OVERVIEW && hs != null && hs.is_loaded()) {
      draw_overview(g);
      for (int i = 0; i < config.OVERVIEW_LINES; i++, draw_line++, lines_needed++) {
	drawn_y.put(KEY_OVERVIEW + "_" + draw_line, Integer.valueOf(draw_line * line_height));
      }
    }

    //
    // draw consensus base numbers ("ruler"):
    //
    g.setColor(RULER_COLOR);
    int y = draw_line * line_height;
    drawn_y.put(KEY_RULER, y);

    if (config.ENABLE_VARIABLE_WIDTH_FONTS && config.ENABLE_VARIABLE_WIDTH_RULER) {
      //      System.err.println("csv="+contig_start_view + " rs:"+ config.ruler_start);  // debug
      draw_variable_ruler(g, draw_line);
    } else {
      Ruler ruler = new Ruler(config);
      String ruler_chunk = ruler.get_ruler_for(contig_start_view, contig_chunk_size, pm);
      g.drawString(ruler_chunk, leftbuffer, y);
    }
    draw_multiline_indicator(g, config.CONSENSUS_TAG, draw_line, 2);
    draw_line++;

    //
    // draw consensus:
    //
    int reference_line = draw_line;
    y = draw_line++ * line_height;
    if (cons_highlight_start > 0 || cons_highlight_end > 0) {
      // mark highlighted region of consensus
      Rectangle r1 = new Rectangle(contig_start_view, 1,
				   contig_end_view - contig_start_view, 1);
      Rectangle r2 = new Rectangle(cons_highlight_start + 1, 1,
				   cons_highlight_end - cons_highlight_start, 1);
      // +1: translate from string space into consensus space
      if (r1.intersects(r2)) {
	// overlap between
	Rectangle intersect = r1.intersection(r2);
	int o1 = intersect.x - contig_start_view;
	int x1 = leftbuffer + (o1 * font_width);
	int draw_width = (intersect.width + 1) * font_width;
	g.setColor(Color.green);
	g.fillRect(x1, y - font_ascent, draw_width, font_ascent + font_descent);
      }
    }

    // draw consensus itself
    g.setColor(SEQID_COLOR);
    draw_fonted_string(g, config.CONSENSUS_TAG, 0, y);
    // reference sequence name
    drawn_y.put(config.CONSENSUS_TAG, y);
    
    //    System.err.println("cons font="+g.getFont());  // debug
    g.setColor(CONSENSUS_COLOR);
    g.drawString(
		 //		 new String(s.sequence, contig_start_offset, contig_chunk_size),
		 get_consensus_for(contig_start_view, contig_chunk_size),
		 leftbuffer, y);

    if (config.ENABLE_HETERO_SUMMARY && hs != null && hs.is_loaded()) {
      //
      //  draw heterozygosity summary
      //
      draw_hetero_summary(g);
      
      for (int i = 0; i < config.HETERO_SUMMARY_LINES; i++, draw_line++, lines_needed++) {
	drawn_y.put(KEY_NONREF_FREQUENCY + "_" + draw_line, Integer.valueOf(draw_line * line_height));
      }
    }

    if (config.dbsnp != null) {
      //
      //  draw dbSNP sites
      //
      boolean track_painted = false;

      int csv_unpadded = pm.get_padded_to_unpadded(contig_start_view);
      int cev_unpadded = pm.get_padded_to_unpadded(contig_end_view);

      for (dbSNP snp : config.dbsnp) {

	if (snp.start >= csv_unpadded && snp.end <= cev_unpadded) {
	  // SNP visible
	  if (!track_painted) {
	    y = draw_line++ * line_height;
	    drawn_y.put(LABEL_DBSNP, new Integer(y));
	    g.setColor(CONSENSUS_COLOR);
	    draw_fonted_string(g, LABEL_DBSNP, 0, y);
	    track_painted=true;
	    lines_needed++;
	  }

	  int snp_padded_x = pm.get_unpadded_to_padded(snp.start);

	  //	  System.err.println("drawing " + snp.name + " cpos=" + snp_padded_x + " strand=" + snp.strand + " obs=" + snp.observed_bases + " norm:" + snp.get_normalized_observed_bases());  // debug

	  g.drawString("$",
		       (leftbuffer + ((snp_padded_x - contig_start_view) * font_width)),
		       y);

	}
      }
    }

    if (config.refgenes != null) {
      //
      //  draw reference genes
      //
      for (RefGene rg : config.refgenes) {
	if (!rg.is_initialized()) {
	  //	  System.err.println("refgene not initialized");  // debug
	  continue;
	  // if markups loaded synchronously, need to init here (feh)
	  //	  rg.consensus_setup(pm);
	}

	if (rg.is_broken()) {
	  //	  System.err.println("refgene broken");  // debug
	  continue;
	}
	//	System.err.println("refgene OK: " + rg.get_accession());  // debug

	//	ArrayList<Exon> exons = rg.get_visible_exons(contig_start_view, contig_end_view);
	//	System.err.println("visible exons: " + exons.size());  // debug
	ArrayList<Exon> exons = rg.get_exons();
	// 4/2012: simple visibility check doesn't always work:
	// sometimes a codon starts in an offscreen exon but ends
	// in a different exon that is visible

	if (exons.size() > 0) {
	  // exon contains codons which MIGHT fall into current view
	  // (sometimes a codon will start in one exon and continue in another,
	  // leaving a large gap inbetween)

	  boolean accession_painted=false;

	  //	  System.err.println("vex1=" + exons.get(0).consensus_start + " " + exons.get(0).consensus_end);  // debug

	  for (Exon exon : exons) {
	    //	    System.err.println("codon count:"+exon.get_codons().size());  // debug

	    for (Codon codon : exon.get_codons()) {
	      int center = codon.c_center_offset();
	      //	      System.err.println("codon:"+codon + " center:"+center);  // debug


	      //	      if (!codon.is_unmappable() && center >= contig_start_view && center <= contig_end_view) {
	      if (center >= contig_start_view && center <= contig_end_view) {
		// center of codon is visible, draw protein symbol

		if (accession_painted == false) {
		  // 
		  String accession = rg.get_accession();
		  y = draw_line++ * line_height;
		  refseq_ids.add(accession);
		  g.setColor(CONSENSUS_COLOR);
		  //		  String label = accession + " / " + rg.get_strand() + " / " + rg.get_symbol();
		  String label;
		  if (config.REFSEQ_LABEL_GENE_SYMBOL_FIRST) {
		    label = rg.get_symbol() + " / " + accession;
		  } else {
		    label = accession + " / " + rg.get_symbol();
		  }

		  //		  drawn_y.put(accession, new Integer(y));
		  drawn_y.put(KEY_REFSEQ + "," + label, new Integer(y));

		  draw_fonted_string(g, label, 0, y);
		  accession_painted=true;
		  lines_needed++;

		  arrow_painter.draw(g, rg.is_rc(), y, false);
		}

		//		System.err.println("unmappable?:"+codon.is_unmappable());  // debug
		String label;
		if (codon.is_unmappable()) {
		  if (codon.get_valid_base_count() > 0) {
		    label = "?";
		  } else {
		    label = " ";
		    // 4/2012: not sure how to deal with/separate these yet
		  }
		} else {
		  label = Character.toString(codon.to_code());
		}

		g.drawString(label,
			     (leftbuffer + ((center - contig_start_view) * font_width)),
			     y);
	      }
	    }
	  }
	}
      }
    }

    int sequence_start_line = draw_line;
    // 1st line where reads drawn
    
    // draw sequences
    //    System.err.println("drawing seqs, start="+ vscroll.getValue());  // debug

    //    System.err.println("seqs font="+g.getFont());  // debug

    boolean drew_separator = draw_seqs(g, aligned_seqs, vscroll.getValue(), lines_available);
    if (!drew_separator) lines_needed--;

    intron_splice_check(g,
			//			sequence_start_line * line_height
			reference_line * line_height
			);
    
    //
    // set horizontal scrollbar:
    //
    hscroll.setValues(contig_start_view,
		      contig_chunk_size,
		      left_border,
		      right_border
		      );

    hscroll.setBlockIncrement(contig_chunk_size);
    hscroll.setUnitIncrement(1);

    //
    // set vertical scrollbar:
    //
    int aligned_count = aligned_seqs.size();

    //    int max_value = aligned_count + 6;
    int max_value = lines_available >= lines_needed ? aligned_count : lines_needed;
    // if everything will fit onscreen, disable scrolling

    //    int max_value = aligned_count;
    // max_value is count of aligned sequences at this position.
    // +1 = padding

    int vv = vscroll.getValue();
    int max_allowable = max_value - lines_available;
    if (max_allowable < 0) max_allowable = 0;

    //    System.err.println("max=" + max_value + " max_allowable=" + max_allowable);

    if (vv > max_allowable) vv = max_allowable;
    // if horizontal position has changed, make sure existing value is within bounds
    
    //    System.err.println("set: " + vv + "," + sequences_drawn + "," + 0 + ","+max_value);  // debug

    vscroll.setValues(vv,
		      //		      sequences_drawn,
		      lines_available,
		      0,
		      max_value);

    //    System.err.println("needed="+lines_needed + " avail="+lines_available  + " setting_max_to:"+max_value + " visible_seqs:"+ visible_seqs.size() + " max_current:"+max_current);  // debug

//     if (sequences_drawn >= 3) {
//       //      vscroll.setBlockIncrement(sequences_drawn - 2);
//       //vscroll.setBlockIncrement(lines_available - 2);
//     } else {
//       vscroll.setBlockIncrement(1);
//     }
    vscroll.setUnitIncrement(1);

    notifier.setChanged();
    notifier.notifyObservers(this);
  }

  void highlight_snps (Graphics g, int lines_needed) {
    // highlight slices of the contig known to contain SNPs.
    // TO DO: this draws one too many rows if there is no fwd/rev separator line.
    SNPList snps = config.assembly.get_snps();
    if (snps != null) {
      int loc;
      for (int i=0; i < snps.size(); i++) {
	if (snps.is_current(i)) {
	  g.setColor(SELECTED_COLOR);
	} else {
	  g.setColor(HIGHLIGHT_COLOR);
	}
	loc = snps.position_of(i);
	if (loc >= contig_start_view && loc <= contig_end_view) {
	  // SNP is visible
	  int x = leftbuffer + ((loc - contig_start_view) * font_width);
	  g.fillRect(x,
		     ((line_height * 2) - font_ascent),
		     font_width,
		     ((lines_needed - 1) * line_height));
	}
      }
    }
  }

  boolean draw_seqs(Graphics gr, ArrayList<AssemblySequence> v, int start_index, int lines_available) {
    Graphics2D g = (Graphics2D) gr;
    //
    // paint sequences
    //
    int start, end, i, x, y;
    boolean exception;
    int drawn_forward = 0;
    int drawn_reverse = 0;
    int index = start_index;
    boolean drew_separator = false;
    sequences_drawn = 0;

    char[] newbuf = new char[contig_chunk_size];

    lines_available++;
    // draw an extra sequence; hack, we really need gridded geometry!

    char[] cseq = config.assembly.get_consensus_sequence();

    boolean paint_quality = config.ENABLE_QUALITY_PAINT;
    // FIX ME: disable locally via this variable if quality data not available?

    int end_index = v.size();
    AssemblySequence as;
    visible_seqs = new ArrayList<AssemblySequence>();

    ColorIntensifier ci = new ColorIntensifier();
    ci.set_max_intensity_value(99);
    // MAPQ

    while (index < end_index && draw_line <= lines_available) {
      as = v.get(index++);

      if (as.get_asm_start() >= contig_end_view ||
	  as.get_asm_end() < contig_start_view) {
	// sequence starts after view ends, or ends before view starts
	System.err.println("WTF: " + as.get_name() + " out of visible range");
	// should be unpossible
      } else {
	// sequence is visible

	if (as.is_complemented()) {
	  if (drawn_reverse++ == 0 && drawn_forward > 0) {
	    // first reverse read, draw forward/reverse separator line
	    g.setColor(BORDER_COLOR);
	    y = (draw_line * line_height) - font_ascent + ((font_ascent + font_descent) / 2);
	    g.drawLine(leftbuffer, y, getSize().width, y);
	    draw_line++;
	    drew_separator = true;
	  }
	} else {
	  drawn_forward++;
	}
	
	y = draw_line * line_height;
	String id = as.get_name();
	drawn_y.put(id, new Integer(y));
	// record the Y position onscreen where sequence was drawn

	sequences_drawn++;
	visible_seqs.add(as);

	
	//	System.err.println("painting " + id);  // debug

	//
	// draw the sequence id:
	//
	Sample sample = config.assembly.get_sample_for(id);
	boolean shadeable = false;
	if (heterozygotes != null && heterozygotes.containsKey(id)) {
	  // sequence should be highlighted
	  g.setColor(HETEROZYGOTE_COLOR);
	} else if (highlights != null && highlights.containsKey(id)) {
	  g.setColor(HIGHLIT_COLOR);
	} else if (svl.contains(id)) {
	  g.setColor(HIGHLIT_COLOR);
	} else if (sample != null) {
	  //	  System.err.println("sample N="+sample.is_normal() + " T=" + sample.is_tumor());
	  shadeable = true;
	  if (sample.is_tumor()) {
	    g.setColor(sample.is_recurrent() ? TUMOR_RECURRENT_COLOR : TUMOR_COLOR);
	  } else if (sample.is_normal()) {
	    g.setColor(NORMAL_COLOR);
	  } else {
	    g.setColor(SEQID_COLOR);
	  }
	} else {
	  g.setColor(SEQID_COLOR);
	  shadeable = true;
	}

	if (config.assembly instanceof SAMAssembly) {
	  SAMResource sr = ((SAMAssembly) config.assembly).get_sr_for(id);
	  // command-level argument: overrides any Sample annotation
	  if (sr.custom_color != null) g.setColor(sr.custom_color);
	}

	if (shadeable && config.SHADE_SEQUENCE_IDENTIFIERS_BY_MAPQ) {
	  SAMRecord sr = as.get_samrecord();
	  g.setColor(ci.get_shaded_color(g.getColor(), sr.getMappingQuality()));
	}

	draw_fonted_string(g, id, 0, y);

	//	System.err.println("id="+id + " len=" + id.length() + " max=" + config.assembly.get_max_id_length());
	// draw the orientation "arrow" for this sequence
	SAMRecord sr = as.get_samrecord();
	boolean cancel_mode = sr != null && sr.getDuplicateReadFlag();
	arrow_painter.draw(g, as.is_complemented(), y, cancel_mode);

	//
	// draw the sequence
	//
	as.get_visible_sequence(newbuf, contig_start_view);
	int cs = as.get_clip_start();
	int ce = as.get_clip_end();
	// consensus offsets for clipping

	//	System.err.println("debug: " + SAMUtils.get_printable_read_name(sr) + " cs=" + (config.ruler_start + cs) + " ce="+ (config.ruler_start + ce));  // debug


	int co = contig_start_view;
	int last_type = -1;
	int this_type;
	int last_index = 0;
	int last_qual = -1;
	char b, cb;
	String outbuf;
	for (i=0; i < contig_chunk_size; i++, co++) {
	  if (newbuf[i] == ' ') {
	    // no alignment here
	    this_type = TYPE_BLANK;
	  } else if (co < cs || co > ce) {
	    // trimmed area
	    this_type = TYPE_TRIM;
	  } else if (co < 1 || co > cseq.length) {
	    // out of consensus range; never happens?
	    this_type = TYPE_SEQ_AGREE;
	  } else {
	    // in consensus-aligned, untrimmed sequence
	    cb = cseq[co - 1];
	    b = newbuf[i];

	    if (cb == b) {
	      this_type = TYPE_SEQ_AGREE;
	    } else if (b == ALIGNMENT_SKIPPED_CHAR_F || b == ALIGNMENT_SKIPPED_CHAR_R) {
	      this_type = TYPE_TRIM;
	    } else {
	      exception = true;
	      switch (b) {
	      case 'a': case 'A':
		if (cb == 'a' || cb == 'A') exception = false;
		break;
	      case 'c': case 'C':
		if (cb == 'c' || cb == 'C') exception = false;
		break;
	      case 'g': case 'G':
		if (cb == 'g' || cb == 'G') exception = false;
		break;
	      case 't': case 'T':
		if (cb == 't' || cb == 'T') exception = false;
		break;
	      }
	      this_type = exception ? TYPE_SEQ_DISAGREE : TYPE_SEQ_AGREE;
	    }
	  }

	  if (paint_quality) {
	    // HACK: since painting quality, process one base at a time
	    // ...this will probably be dog slow
	    int qual = as.get_quality(co);
	    //	    System.err.println("co " + co + " = " + qual);  // debug

	    flush_it(g, newbuf, last_index, last_type, i, y, paint_quality, last_qual);
	    last_index = i;
	    last_type = this_type;
	    last_qual = qual;
	  } else if (i == 0) {
	    // init
	    last_type = this_type;
	  } else if (this_type != last_type) {
	    // flush buffer
	    flush_it(g, newbuf, last_index, last_type, i, y, false, 0);
	    last_index = i;
	    last_type = this_type;
	  }
	}
	flush_it(g, newbuf, last_index, last_type, i, y, paint_quality, 0);

	draw_line++;
      }
    }
    //    System.out.println("drew:" + drawn_forward + " " + drawn_reverse);
    return (drew_separator);
  }
  
//   public boolean handleEvent(Event e) {
//     switch (e.id) {
//       //    case Event.KEY_RELEASE:
//       //      System.out.println("yow! " + e.key);  // debug
//       //      return true;
//     case Event.MOUSE_DOWN:
//       // mouse click in the sequence window
//       down_click_time = System.currentTimeMillis();
//       return true;
//     case Event.MOUSE_UP:
//       // mouse click in the sequence window
//       mouse_up(e);
//       return true;
//     default:
//       return false;
//     }
//   }

  private void mouse_up(MouseEvent e) {
    boolean long_click = (System.currentTimeMillis() - down_click_time > 500);
    // obsolete
    boolean right_click = false;

    // find the trace id that was clicked on:
    Point root_point = null;
    String id = null;
    Enumeration en = drawn_y.keys();
    int y = e.getY();
    int x = e.getX();
    while (en.hasMoreElements()) {
      // find which sequence ID was clicked on
      String id2 = (String) en.nextElement();
      int i = ((Integer) drawn_y.get(id2)).intValue();
      if (y > (i - font_ascent) && y < (i + font_descent)) {
	//	  System.out.println("csv:" + contig_start_view);  // debug
	id = id2;
	Point p = Funk.Gr.get_frame_border(this, "s");
	root_point = Funk.Gr.get_root_xy(this, x, i + font_descent);
	root_point.y = p.y;
	break;
      }
    }

    if (id != null) {
      if (id.equals(config.CONSENSUS_TAG)) {
	//
	// clicked on consensus
	//
	String seq = "";
	SNPList snps = config.assembly.get_snps();
	String info = "";
	if (x > leftbuffer && snps != null) {
	  // clicked in the sequence itself; show any SNPs
	  info = "SNPs marked in brackets (click on \"Consensus\" label for raw sequence)";
	  char[] cons = config.assembly.get_consensus_sequence();
	  Hashtable positions = snps.position_hash();
	  StringBuffer buf = new StringBuffer();
	  for (int i = 0; i < cons.length; i++) {
	    char c = cons[i];
	    if (c != ALIGNMENT_GAP_CHAR && c != ALIGNMENT_DELETION_CHAR) {
	      int cpos = i + 1;
	      if (positions.containsKey(Integer.toString(cpos))) {
		buf.append("[FIX/ME]");
// 		BaseCounter bc = config.assembly.get_bases_at(cpos);
// 		buf.append("[" + 
// 			   Trace.TraceFile.index_to_base((short) bc.get_majority()) +
// 			   "/" +
// 			   Trace.TraceFile.index_to_base((short) bc.get_minority()) +
// 			   "]");
	      } else {
		buf.append(c);
	      }
	    }
	  }
	  seq = buf.toString();
	} else {
	  // clicked on the "consensus" tag; show raw consensus sequence
	  info = "raw sequence (click in consensus sequence to show SNPs)";
	  seq = get_consensus_sequence_unpadded().toUpperCase();
	}

	Frame f = new Funk.CloseFrame();
	f.setLayout(new BorderLayout());
	TextArea ta = new TextArea(30,70);
	ta.append(">Consensus; ");
	ta.append(info);
	for (int i = 0; i < seq.length(); i++) {
	  if (i % 60 == 0) ta.append("\n");
	  ta.append("" + seq.charAt(i));
	  // blech
	}
	ta.setEditable(false);
	f.add("Center", ta);
	f.pack();
	f.setVisible(true);
      } else if (x > leftbuffer) {
	// clicked in the sequence area; view trace at this point in sequence
	int consensus_pos = get_consensus_pos(e);
	//	System.out.println("consensus pos:" + consensus_pos);  // debug

	if (ENABLE_TRACE_VIEWER) {
	  if (e.getModifiers() == InputEvent.BUTTON2_MASK) {
	    // button 2: slice detail
// 	    if (sdf == null || sdf.isValid() == false) {
// 	      //	      sdf = new SliceDetailFrame(alignment, consensus_pos);
// 	      System.err.println("FIX ME: SliceDetailFrame Alignment ref");  // debug
// 	      sdf = new SliceDetailFrame(null, consensus_pos);
// 	    } else {
// 	      sdf.refresh(consensus_pos);
// 	      sdf.toFront();
// 	    }
	  } else {
	    // view trace
	    //	    trace_spawner.show(id, consensus_pos, root_point, false);
	  }
	}
      } else {
	System.out.println("clicked on id " + id);  // debug
	//	new ESTDescriber(id);
	get_entrez_report(id);
      }
    }
  }

  protected void finalize () {
    // close any child trace viewers
    //    trace_spawner.finalize();
  }

  private void get_entrez_report (String id) {
    //    java.applet.AppletContext ac = AceSelect.Launcher.getStaticAppletContext();
    java.applet.AppletContext ac = null;
    if (ac == null) {
      System.out.println("eek, no applet context.");  // debug
    } else {
      try {
	String url = "http://www.ncbi.nlm.nih.gov/htbin-post/Entrez/query?db=n&form=6&dopt=g&" + "uid=" + id;
	ac.showDocument(new java.net.URL(url), "Entrez report");
      } catch (Exception e) {}
    }
  }

  public void set_highlights (Hashtable highlights) {
    this.highlights = highlights;
  }

  public void set_heterozygotes (Hashtable heterozygotes) {
    this.heterozygotes = heterozygotes;
  }

  public void set_consensus_highlight (int start, int end) {
    cons_highlight_start = start;
    cons_highlight_end = end;
  }

  // begin MouseMotionListener stubs
  public void mouseDragged(MouseEvent e) {}
  public void mouseMoved(MouseEvent e) {
    update_tooltip(e);
    wheel_motion_start_center=0;
  };
  // begin MouseMotionListener stubs

  private boolean in_consensus_y (MouseEvent e) {
    String id = get_mouse_sequence_id(e);
    return id != null && (id.equals(config.CONSENSUS_TAG) || id.equals(KEY_RULER));
  }

  private boolean in_overview (MouseEvent e) {
    String id = get_mouse_sequence_id(e);
    return id != null && id.indexOf(KEY_OVERVIEW) == 0;
  }

  // begin MouseListener stubs
  public void mousePressed(MouseEvent e) {
    down_click_time = System.currentTimeMillis();
  }

  private String[] decode_gene_and_refseq (String label) {
    // (gag, choke)
    String chunk = label.substring(label.indexOf(",") + 1);
    //    System.err.println("id="+ label+ " chunk="+chunk);  // debug
    String[] fields = chunk.split(" / ");
    String[] results = new String[2];
    if (config.REFSEQ_LABEL_GENE_SYMBOL_FIRST) {
      results[0] = fields[0];
      results[1] = fields[1];
    } else {
      results[0] = fields[1];
      results[1] = fields[0];
    }
    return results;
  }

  public void mouseClicked(MouseEvent e) {
    if (e.getClickCount() == 1) {
      //
      // single-click
      //
      String id = get_mouse_sequence_id(e);
      if (id != null) {
	int mx = e.getX();
	if (id.indexOf(KEY_REFSEQ) == 0 && mx < leftbuffer) {
	  //
	  // clicked on a refseq ID
	  //
	  String gene = null;
	  String refseq = null;
	  String chunk = id.substring(id.indexOf(",") + 1);
	  System.err.println("id="+id + " chunk="+chunk);  // debug
	  String[] fields = chunk.split(" / ");
	  if (config.REFSEQ_LABEL_GENE_SYMBOL_FIRST) {
	    gene = fields[0];
	    refseq = fields[1];
	  } else {
	    gene = fields[1];
	    refseq = fields[0];
	  }
	  
	  String url = null;
	  String target = null;
	  if (mx < (font_width * (fields[0].length() + 1))) {
	    // clicked in first delimited field
	    if (config.REFSEQ_LABEL_GENE_SYMBOL_FIRST) {
	      url = WebTools.entrez_gene_link(fields[0]);
	      target = "eg";
	    } else {
	      url = WebTools.entrez_genbank_link(fields[0]);
	      target = "gb";
	    }
	  } else {
	    // clicked in second delimited field
	    if (config.REFSEQ_LABEL_GENE_SYMBOL_FIRST) {
	      url = WebTools.entrez_genbank_link(fields[1]);
	      target = "gb";
	    } else {
	      url = WebTools.entrez_gene_link(fields[1]);
	      target = "eg";
	    }
	  }
	  
	  if (false) {
	    System.err.println("DEBUG: not linking to " + url);
	  } else {
	    new URLLauncher(url, target);
	  }
	} else if (id.indexOf(KEY_OVERVIEW) == 0) {
	  //
	  // clicked in overview/coverage display: move to that position in assembly
	  //
	  int avail_x = font_width * (contig_end_view - contig_start_view);
	  int mouse_x = e.getX() - leftbuffer;
	  char[] cons = config.assembly.get_consensus_sequence();
	  int new_cpos = (int) (cons.length * ((float) mouse_x / avail_x));
	  //	  move_to(new_cpos);
	  center_on(new_cpos);
	} else if (id.equals(LABEL_DBSNP)) {
	  dbSNP snp = get_dbsnp_snp(e);
	  if (snp != null) {
	    // clicked on a SNP
	    String url = "http://www.ncbi.nlm.nih.gov/projects/SNP/snp_ref.cgi?rs=" +
	      snp.name.substring(2);
	    new URLLauncher(url, "dbsnp");
	  }
	} else {
	  svl.toggle_id(id);
	  repaint();
	}
      }
    } else {
      //
      // double-click
      //
      if (in_consensus_y(e)) {
	String pos = JOptionPane.showInputDialog("Jump to " + config.CONSENSUS_TAG + " nucleotide position:"); 
	try {
	  int p = Integer.parseInt(Funk.Str.trim_whitespace(pos));
	  if (config.intron_compressor != null) {
	    int offset = config.intron_compressor.get_start_shift(p - config.ruler_start, false);
	    p -= offset;
	  }

	  if (config.ruler_start != 0) p -= config.ruler_start;
	  if (config.COUNTER_UNPADDED) p = config.assembly.get_padmap().get_unpadded_to_padded(p);
	  center_on(p);
	} catch (Exception ex) {};
      } else if (!in_overview(e)) {
	//
	// double-click anywhere but in overview/coverage to center on that position
	//
	center_on(get_consensus_pos(e));
      }
    }
  };
  public void mouseReleased(MouseEvent e) {
    if (ENABLE_TRACE_VIEWER) mouse_up(e);
    // 6/2009: hack/debug -- something in here causes a hang when
    // double-clicking in consensus??
  };
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};
  // end MouseListener stubs

  private void flush_it (Graphics g, char [] newbuf, int last_index,
			 int last_type, int i, int y, boolean paint_quality, int q) {
    // my kingdom for a closure
    if (last_type == TYPE_BLANK) {
      // blank, nothing to paint
    } else {
      if (paint_quality && q >= 0) {
	if (last_type == TYPE_TRIM) {
	  g.setColor(quality_colors[0]);
	} else if (q >= 0) {
	  if (q >= MAX_INTENSITY_QUALITY) {
	    g.setColor(Color.white);
	  } else {
	    g.setColor(quality_colors[q]);
	  }
	}
	g.fillRect((leftbuffer + (last_index * font_width)), y - font_ascent,
		   font_width, line_height);
      }

      switch (last_type) {
      case TYPE_TRIM:
	g.setColor(TRIMMED_COLOR); break;
      case TYPE_SEQ_AGREE:
	g.setColor(paint_quality && q >= 0 ? SEQUENCE_COLOR_QUAL : SEQUENCE_COLOR); break;
      case TYPE_SEQ_DISAGREE:
	g.setColor(SNP_COLOR); break;
      }

      String text = new String(newbuf, last_index, i - last_index);
      if (paint_quality && q >= 0) {
	if (q < QUALITY_THRESHOLD_LOWERCASE) {
	  text = text.toLowerCase();
	} else {
	  text = text.toUpperCase();
	}
      }

      g.drawString(text,
		   (leftbuffer + (last_index * font_width)),
		   y);
    }
  }


  private void init_quality_colors() {
    if (quality_colors == null) {
      // singleton
      quality_colors = new Color[MAX_INTENSITY_QUALITY];
      int MAX_VALUE = 255;
      int MIN_VALUE = 50;
      int range = MAX_VALUE - MIN_VALUE;
      for (int q=0; q < MAX_INTENSITY_QUALITY; q++) {
	float fraction = (float) q / MAX_INTENSITY_QUALITY;
	int level = MIN_VALUE + (int) (range * fraction);
	quality_colors[q] = new Color(level, level, level);
	//	System.err.println("qual " + q + " level=" + level);  // debug
      }
      
    }
  }

  private String get_mouse_sequence_id (MouseEvent me) {
    String id = null;
    Enumeration en = drawn_y.keys();
    int y = me.getY();
    while (en.hasMoreElements()) {
      // find which sequence ID was clicked on
      String id2 = (String) en.nextElement();
      int i = ((Integer) drawn_y.get(id2)).intValue();
      //      if (y > (i - font_ascent) && y < (i + font_descent)) {
      if (y >= (i - font_ascent) && y <= (i + font_descent)) {
	//	  System.out.println("csv:" + contig_start_view);  // debug
	id = id2;
	break;
      }
    }
    return id;
  }

  private void update_tooltip (MouseEvent me) {
    int x = me.getX();
    String id = get_mouse_sequence_id(me);
    String text;
    if (id == null || 
	id.indexOf(KEY_NONREF_FREQUENCY) == 0 ||
	id.indexOf(KEY_OVERVIEW) == 0 ||
	id.indexOf(KEY_REFSEQ) == 0 ||
	id.indexOf(KEY_RULER) == 0
	) {
      text = "";
    } else {
      text = id;
    }

    if (!config.assembly.alignment_is_built()) return;
    // not ready yet

    PadMap pm = config.assembly.get_padmap();

    if (x > leftbuffer) {
      //
      // mouse in the sequence area
      //

      //
      // get consensus position:
      //
      int consensus_pos = get_consensus_pos(me);

      if (id != null && id.equals(LABEL_DBSNP) && config.dbsnp != null) {
	int unpadded = pm.get_padded_to_unpadded(consensus_pos);
	// raw unpadded consensus base (used in transformed dbSNP coordinates)

	for (dbSNP snp : config.dbsnp) {
	  if (snp.start == unpadded) {
	    text = ("dbSNP:" + snp.name + " bases:" + snp.get_normalized_observed_bases());
	    // reset text w/dbSNP info
	  }
	}
      }

      //      System.err.println("id="+id);  // debug
      if (id != null && hs != null) {
	if (id.indexOf(KEY_NONREF_FREQUENCY) == 0) {
	  //	System.err.println("cpos="+consensus_pos+ " unpadded="+unpadded);  // debug
	  int unpadded = pm.get_padded_to_unpadded(consensus_pos);
	  text = hs.get_tooltip_text(unpadded);
	} else if (id.indexOf(KEY_OVERVIEW) == 0) {
	  text = "global assembly coverage";

	  if (hs != null) {
	    short[] coverage = hs.get_coverage();
	    if (coverage != null) {
	      int avail_x = font_width * (contig_end_view - contig_start_view);
	      int mouse_x = x - leftbuffer;
	      char[] cons = config.assembly.get_consensus_sequence();
	      int cpos = (int) (cons.length * ((float) mouse_x / avail_x));
	      // FIX ME: centralize
	      if (cpos >= 0 && cpos < cons.length) {
		int unpadded = pm.get_padded_to_unpadded(cpos);

		//		text = text + ": " + coverage[cpos] + " read" + 
		//		  (coverage[cpos] == 1 ? "" : "s") + " at " +
		//  (cpos + config.ruler_start);
		text = text + ": " + coverage[unpadded] + " read" + 
		  (coverage[unpadded] == 1 ? "" : "s") + " at " +
		  //		  (unpadded + config.ruler_start);
		// coverage uses UNPADDED space
		  (get_reference_base_number(cpos));
	      }
	    }
	  }
	  
	  text = text + " (click to move)";

	  setToolTipText(text);
	  return;
	}
      }

      // int mapped_pos;
      // if (config.ruler_start > 0) {
      // 	mapped_pos = config.ruler_start + pm.get_padded_to_unpadded(consensus_pos);
      // } else {
      // 	mapped_pos = pm.get_padded_to_unpadded(consensus_pos);
      // }
      // text = text.concat(" base#:" + mapped_pos);
      text = text.concat(" base#:" + get_reference_base_number(consensus_pos));
    }

    if (in_consensus_y(me)) {
      text = text.concat(" (double-click to jump to a position)");
    } else {
      Sample s = config.assembly.get_sample_for(id);
      if (s != null && s.get_sample_name() != null) {
	text = text.concat(" sample:" + s.get_sample_name());
	String desc = s.get_description();
	if (desc != null) text = text.concat(" tissue:" + desc);
      }
      if (x > leftbuffer) {
	// mouse in the sequence area; get data for this point in sequence
	int consensus_pos = get_consensus_pos(me);

	AssemblySequence as = config.assembly.get_sequence(id);
	if (as != null) {
	  int qual = as.get_quality(consensus_pos);
	  if (qual != -1) {
	    // quality data available
	    //	text = text.concat(", consensus " + consensus_pos + ": quality " + qual);
	    text = text.concat(" quality:" + qual);
	  }

	  if (as instanceof SAMConsensusMapping) {
	    // SAM alignment, show additional info
	    SAMConsensusMapping scm = (SAMConsensusMapping) as;
	    SAMRecord sr = scm.get_samrecord();
	    text = text.concat(" MapQ:" + sr.getMappingQuality());

	    HashMap<String,String> tags = new HashMap<String,String>();
	    for (SAMTagAndValue tav : sr.getAttributes()) {
	      String value = tav.value.toString();
	      if (value.length() > 10) {
		value = value.substring(0, 10) + "...";
	      }
	      tags.put(tav.tag, value);
	      // FIX ME: trim values here if very long...
	    }
	    ArrayList<String> keys = new ArrayList<String>(tags.keySet());
	    Collections.sort(keys);
	    for (String key : keys) {
	      // display sorted list of SAM tags
	      text = text.concat(" " + key + ":" + tags.get(key));
	    }
	  }

	  //	} else if (refseq_ids.contains(id) && config.refgenes != null) {
	  //	} else if (stripped_id != null && refseq_ids.contains(stripped_id) && config.refgenes != null) {
	} else if (id != null && id.indexOf(KEY_REFSEQ) == 0) {
	  String[] gar = decode_gene_and_refseq(id);
	  String refseq = gar[1];

	  if (refseq_ids.contains(refseq) && config.refgenes != null) {
	    // refseq
	    for (RefGene rg : config.refgenes) {
	      if (rg.get_accession().equals(refseq)) {
		// this RefGene
		//		ArrayList<Exon> exons = rg.get_visible_exons(consensus_pos, consensus_pos);
		// won't always work, see other commented-out example

		ArrayList<Exon> exons = rg.get_exons();
		boolean done = false;
		for (Exon exon : exons) {
		  for (Codon codon : exon.get_codons()) {
		    if (codon.intersects_padded_consensus(consensus_pos)) {
		      // Codon refs use STRING offsets (ARGH)
		      text = text.concat(" (" + rg.get_symbol() + " exon " + exon.get_id() + ")");
		      text = text.concat(", codon " + Integer.toString(codon.get_id()));
		      if (codon.is_unmappable()) {
			text = "<html>" + text.concat ("<br><b>NOTE</b>: codon can't be completely mapped; all required exonic regions may not be loaded in viewer</html>");
		      }
		      done = true;
		      break;
		    }
		  }
		  if (done) break;
		}


	      }
	    }
	  }
	}

      }

    }

    setToolTipText(text);
  }

  private void set_leftbuffer() {
    if (config.ENABLE_VARIABLE_WIDTH_FONTS) {
      if (fwt == null || VAR_FONT == null) {
	leftbuffer = 0;
      } else {
	int max_len = fwt.get_max_width();
	//	FontMetrics fm = getFontMetrics(VAR_FONT);

	leftbuffer = max_len;
      }
    } else {
      int max_len = config.assembly.get_max_id_length();
      if (LABEL_NONREF_FREQUENCY.length() > max_len) {
	max_len = LABEL_NONREF_FREQUENCY.length();
      }
      if (config.CONSENSUS_TAG.length() > max_len) {
	max_len = config.CONSENSUS_TAG.length();
      }
      leftbuffer = (max_len * font_width);
    }
    arrow_painter.left_margin = leftbuffer;

    leftbuffer += arrow_painter.total_width;
    if (false) {
      System.err.println("LB fudge");  // debug
      leftbuffer += 100;
    }
  }

  public void zoom (boolean zoom_in) {
    int paintable_nt = contig_end_view - contig_start_view;
    // FUDGY: this is the number of CURRENTLY paintable bases, 
    // will immediately be more/fewer if we're zooming out/in
    int center_x = contig_start_view + (paintable_nt / 2);
    font_size += (zoom_in ? 1 : -1);
    set_font_size(font_size);
    set_leftbuffer();
    // temporary, must be called after set_font_size()
    center_on(center_x);
  }

  // begin MouseWheelListener stub
  public void mouseWheelMoved(MouseWheelEvent e) {
    if ((e.getModifiers() & MouseEvent.MOUSE_DRAGGED) == 0) {
      // ignore mouse wheel events if a drag is in effect, because
      // button 2 (wheel) is also used for drag-scrolling.
      // if we zoom during a drag scroll the view will change/jump unexpectedly.
      //      System.err.println("e="+e);  // debug
      int paintable_nt = contig_end_view - contig_start_view;
      // FUDGY: this is the number of CURRENTLY paintable bases, 
      // will immediately be more/fewer if we're zooming out/in

      int half_paintable = paintable_nt / 2;
      if (wheel_motion_start_center == 0) {
	wheel_motion_start_center = contig_start_view + (half_paintable);
      } else {
	//	System.err.println("cached center");  // debug
      }
      //      System.err.println("center="+wheel_motion_start_center);  // debug
      font_size += (e.getWheelRotation() < 0 ? 1 : -1);
      set_font_size(font_size);

      set_leftbuffer();
      // temporary, must be called after set_font_size()

      int center_x = wheel_motion_start_center;

      if (e.getWheelRotation() < 0) {
	// when zooming in, view center position influenced by mouse position within
	int total = getSize().width - leftbuffer;
	int mp_x = e.getPoint().x - leftbuffer;
	//	System.err.println("mp_x:"+mp_x);  // debug

	float frac = ((float) mp_x / total);
	if (frac < 0) frac = 0;
	// mouse position in nucleotides: 0.5 is halfway (center)
	frac -= 0.50f;
	// -0.5 -> +.5

	float dilution_factor = .4f;

	int offset = (int) (paintable_nt * dilution_factor * frac);

	center_x += offset;

      }
      
      center_on(center_x);
    }
  }
  // end MouseWheelListener stub

  // begin KeyListener stubs 
  public void keyPressed(KeyEvent ke) {
    // KeyCode VK_PLUS doesn't seem to work on my laptop for + (a.k.a. shift-equals)
    // (maybe assumes extended keyboard + key?)
    //    int code = ke.getKeyCode();
    char c = ke.getKeyChar();
    if (c == '+') {
      zoom(true);
    } else if (c == '-') {
      zoom(false);
    }
  }
  public void keyReleased(KeyEvent ke) {}
  public void keyTyped(KeyEvent ke) {}
  // end KeyListener stubs 

  // begin ActionListener stub
  public void actionPerformed(ActionEvent e) {
    // bleh: replace with anonymous handlers?
    JMenuItem src = (JMenuItem) e.getSource();
    String label = src.getText();
    if (label.equals(LABEL_TRACE_VIEWER)) {
      MouseEvent me = pul.getMouseEvent();
      String id = get_mouse_sequence_id(me);
      int consensus_pos = get_consensus_pos(me);
      if (id != null) {
	AssemblySequence as = config.assembly.get_sequence(id);
	if (as instanceof SAMConsensusMapping) {
	  SAMConsensusMapping scm = (SAMConsensusMapping) as;
	  SAMRecord sr = scm.get_samrecord();
	  SAMTracePeakPositions stpp = new SAMTracePeakPositions(sr);
	  int peak_pos = stpp.get_peak_position_for_consensus(scm, consensus_pos);
	  Trace.StreamDelegator.set_local(true);
	  if (config.CHROMAT_DIR == null) {
	    JOptionPane.showMessageDialog(this,
					  "Error: no trace directory specified (use -trace-dir on command line)",
					  "Error",
					  JOptionPane.ERROR_MESSAGE);
	    return;
	  }
	  String trace_name = config.CHROMAT_DIR + File.separator + sr.getReadName();
	  //	  System.err.println("trace="+trace_name);  // debug

	  // don't use assembly name, contains generated suffix
	  //	  System.err.println("ai="+trace_name.indexOf("_alternate") + " len=" + trace_name.length());
	  if (trace_name.indexOf("_alternate") == trace_name.length() - 10) {
	    trace_name = trace_name.substring(0, trace_name.length() - 10);
	    //	    System.err.println("trim=>"+trace_name);  // debug
	  }

	  boolean rc = sr.getReadNegativeStrandFlag();
	  TraceViewer tv = tvc.get_traceviewer(trace_name, rc);
	  TraceFile tf = tv.get_trace();
	  if (rc) {
	    while (true) {
	      //	      System.err.println("samples="+tf.num_samples);  // debug
	      if (tf.loaded()) {
		peak_pos = tf.num_samples - peak_pos;
		System.err.println("flipped pos="+peak_pos);  // debug
		break;
	      } else {
		//		System.err.println("waiting for trace...");  // debug
		try {
		  Thread.sleep(100);
		} catch (InterruptedException ex) {}
	      }
	    }
	  }
	  tv.center_on(peak_pos);
	  tv.setVisible(true);
	}
      }
    } else if (label.equals(LABEL_DEBUG_PEAK_AMP_RATIO)) {
      MouseEvent me = pul.getMouseEvent();
      String id = get_mouse_sequence_id(me);
      int consensus_pos = get_consensus_pos(me);
      if (id != null) {
	AssemblySequence as = config.assembly.get_sequence(id);
	if (as instanceof SAMConsensusMapping) {
	  SAMConsensusMapping scm = (SAMConsensusMapping) as;
	  SAMRecord sr = scm.get_samrecord();

	  SAMAlternatePeakAmplitudeRatios apar = new SAMAlternatePeakAmplitudeRatios(sr);
	  int ratio = apar.get_alternate_peak_amplitude_ratio_for_consensus(scm, consensus_pos);
	  String msg = Integer.toString(ratio) + "%";

	  JOptionPane.showMessageDialog(this,
					msg,
					"Alternate basecall amplitude ratio",
					JOptionPane.INFORMATION_MESSAGE);

	}
      }

    } else if (label.equals(LABEL_PROTEIN_CHANGE)) {
      String msg;
      if (config.refgenes == null || config.refgenes.size() == 0) {
	msg = "No reference gene coding sequences have been loaded.";
      } else {
	int cpos = get_consensus_pos(pul.getMouseEvent());
	CDSChangePredictor cdp = new CDSChangePredictor(config);
	char variant_nt = cdp.get_variant_nt(cpos, this);

	if (variant_nt == 0) {
	  msg = null;
	} else {
	  if (cdp.predict_cds_changes(cpos, variant_nt)) {
	    msg = config.CONSENSUS_TAG + " allele:" + cdp.get_consensus_nt() + " variant:" + variant_nt + "\n" + cdp.toString();
	  } else {
	    msg = "This SNP does not fall within any loaded coding sequences.";
	  }
	}
      }

      if (msg != null) {
	JOptionPane.showMessageDialog(this,
				      msg,
				      "Protein change",
				      JOptionPane.INFORMATION_MESSAGE);
      }
    } else if (label.equals(LABEL_SHOW_ONLY_ALIGNED)) {
      config.manual_clamp_cons_pos = get_consensus_pos(pul.getMouseEvent());
      config.manual_clamp_cons_start = contig_start_view;
      repaint();
    } else if (label.equals(LABEL_SHOW_ONLY_ALIGNED_NO_SKIPS)) {
      config.manual_clamp_cons_pos = get_consensus_pos(pul.getMouseEvent());
      config.manual_clamp_cons_start = contig_start_view;
      repaint();
    } else if (label.equals(LABEL_SHOW_ONLY_ALIGNED_NO_SKIPS_BUT_CONTAINING_SKIP)) {
      config.manual_clamp_cons_pos = get_consensus_pos(pul.getMouseEvent());
      config.manual_clamp_cons_start = contig_start_view;
      repaint();
    } else if (label.equals(LABEL_SHOW_ONLY_ALIGNED_NO_SKIPS_BUT_CONTAINING_SKIP2)) {
      String pos = JOptionPane.showInputDialog("Skip ending at what base number?: "); 
      try {
	config.SKIP_END_BASENUM = Integer.parseInt(pos);
	config.manual_clamp_cons_pos = get_consensus_pos(pul.getMouseEvent());
	config.manual_clamp_cons_start = contig_start_view;
      } catch (Exception x) {}
      repaint();
    } else if (label.equals(LABEL_SHOW_ONLY_NONREFERENCE)) {
      config.clamp_nonref_cons_pos = get_consensus_pos(pul.getMouseEvent());
      config.clamp_nonref_cons_start = contig_start_view;
      repaint();
    } else if (src.equals(jmi_blat)) {
      run_blat(false);
    } else if (src.equals(jmi_blat_mate)) {
      run_blat(true);
    } else if (label.equals(LABEL_SHOW_ONLY_INDEL)) {
      repaint();
    } else {
      System.err.println("unhandled event");  // debug
    }
  }
  // end ActionListener stub

  public int get_consensus_pos (MouseEvent e) {
    return contig_start_view + ((e.getX() - leftbuffer) / font_width);
  }

  public dbSNP get_dbsnp_snp (MouseEvent me) {
    int consensus_pos = get_consensus_pos(me);
    int unpadded = config.assembly.get_padmap().get_padded_to_unpadded(consensus_pos);
    // raw unpadded consensus base (used in transformed dbSNP coordinates)
    
    dbSNP result = null;
    for (dbSNP snp : config.dbsnp) {
      if (snp.start == unpadded) {
	result = snp;
	break;
      }
    }
    return result;
  }

  private String get_consensus_for (int csv, int size) {
    //
    // visible aligned consensus at given consensus start position
    //
    int si = csv - 1;
    StringBuffer result = new StringBuffer();
    int copy = size;
    while (si < 0) {
      si++;
      result.append(" ");
      copy--;
    }

    char[] cons = config.assembly.get_consensus_sequence();
    while (copy > 0 && si < cons.length) {
      result.append(cons[si]);
      copy--;
      si++;
    }
    String res = result.toString();
    if (config.UPPERCASE_REFERENCE_SEQUENCE) res = res.toUpperCase();
    return res;
  }

  private String get_consensus_sequence_unpadded () {
    // return consensus sequence for current contig, without pads
    StringBuffer unpadded = new StringBuffer();
    char[] cons = config.assembly.get_consensus_sequence();
    char c;
    int i;
    for (i=0; i < cons.length; i++) {
      c = cons[i];
      if (c != ALIGNMENT_GAP_CHAR && c != ALIGNMENT_DELETION_CHAR) unpadded.append(c);
    }
    return unpadded.toString();
  }

  public ArrayList<AssemblySequence> get_aligned_sequences() {
    // sequences aligned to visible horizontal region (minus any filtering/clamping)
    return aligned_seqs;
  }

  public ArrayList<AssemblySequence> get_visible_sequences() {
    // sequences actually drawn in display
    return visible_seqs;
  }

  public void set_config (AceViewerConfig config) {
    this.config = config;
    fwt = null;
    contig_start_view = 1;
    config.assembly.addObserver(this);
    first_paint = true;
    repaint();
  }

  public void set_hetero_summary (HeteroSummary hs) {
    this.hs = hs;
    hs.addObserver(this);
    repaint();
  }

  private void draw_hetero_summary (Graphics2D g) {
    draw_multiline_indicator(g, LABEL_NONREF_FREQUENCY, draw_line, config.HETERO_SUMMARY_LINES);

    g.setColor(NONREF_FREQUENCY_COLOR);

    //    System.err.println("csv="+contig_start_view);  // debug
    int csv = contig_start_view;
    byte[] global_nonref_freq = hs.get_global_nonreference_frequency();
    byte[] normal_nonref_freq = hs.get_normal_nonreference_frequency();
    byte[] tumor_nonref_freq = hs.get_tumor_nonreference_frequency();
    
    //    System.err.println("FIX ME: only draw if some data");  // debug
    //    System.err.println("lh="+line_height);  // debug

    int max_line_thickness = 1;
    int buffer = max_line_thickness + 1;

    int top_y = ((draw_line * line_height) - font_ascent) + buffer;

    int bottom_y = (((draw_line + (config.HETERO_SUMMARY_LINES - 1)) * line_height) + font_descent) - buffer;
    int headroom = bottom_y - top_y;
    
    ArrayList<Integer> x_points = new ArrayList<Integer>();
    ArrayList<Integer> y_points = new ArrayList<Integer>();

    //    System.err.println("top="+top_y + " bottom="+bottom_y + " headroom="+headroom);  // debug
    //    System.err.println("font_width="+font_width);  // debug

    int x = leftbuffer;
    int y = (draw_line + (config.HETERO_SUMMARY_LINES - 1)) * line_height;

    g.setColor(SEQID_COLOR);
    draw_fonted_string(g, LABEL_NONREF_FREQUENCY, 0, y);

    PadMap pm = config.assembly.get_padmap();

    int ui;
    // unpadded

    int half_font_width = font_width / 2;

    g.setColor(SUMMARY_PANEL_BACKGROUND_COLOR);
    g.fillRect(x, top_y,
	       (x + (font_width * (contig_end_view - contig_start_view))),
	       (headroom + 1));
    // clear background

    int freq;

    Color tumor_color = new Color(225,0,0);
    // reduced intensity to better blend

    for (int i = contig_start_view - 1; i < contig_end_view; i++, x += font_width) {
      if (i >= 0) {
	if (config.REFERENCE_SEQUENCE_PREPADDED) {
	  ui = i;
	} else {
	  ui = pm.get_padded_to_unpadded(i);
	}
	if (ui < 0) continue;

	if (hs.is_tumor_normal_trackable()) {
	  //
	  // non-reference frequencies for both normal and tumor reads are available
	  //
	  if (normal_nonref_freq == null || ui >= normal_nonref_freq.length) continue;
	  freq = normal_nonref_freq[ui];
	  if (freq > 0) {
	    int height = (int) (headroom * ((float) freq / 100));
	    g.setColor(NORMAL_COLOR);
	    g.fillRect(x, bottom_y - height,
		       font_width, height);
	  }

	  freq = tumor_nonref_freq[ui];
	  if (freq > 0) {
	    int height = (int) (headroom * ((float) freq / 100));
	    g.setColor(tumor_color);
	    g.fillRect(x + 2, bottom_y - height,
		       font_width - 4, height);
	    // FIX ME: use g2.setStroke() of width > 1!
	  }
	} else {
	  //
	  //  we're tracking only a single frequency -- might be tumor, normal, or unknown
	  //
	  if (global_nonref_freq == null || ui >= global_nonref_freq.length || ui < 0) continue;
	  freq = global_nonref_freq[ui];
	  if (freq > 0) {
	    int height = (int) (headroom * ((float) freq / 100));
	    if (hs.is_all_normal()) {
	      g.setColor(NORMAL_COLOR);
	    } else if (hs.is_all_tumor()) {
	      g.setColor(tumor_color);
	    } else {
	      g.setColor(SEQUENCE_COLOR);
	    }
	    g.fillRect(x, bottom_y - height,
		       font_width, height);
	  }

	}
      }
    }
  }

  private void draw_multiline_indicator (Graphics g, String label, int top_line, int total_lines) {
    //    int top_y = ((top_line - 1) * line_height) + (int) (line_height * 0.3f);
    int buffer = 2;
    int top_y = get_top_y_for_line(top_line) + buffer;
    int bottom_y = get_bottom_y_for_line(top_line + total_lines) - buffer;
    FontMetrics fm = config.ENABLE_VARIABLE_WIDTH_FONTS ? getFontMetrics(VAR_FONT) : getFontMetrics(OUR_FONT);

    //    g.setColor(Color.gray);
    int intensity = 70;
    g.setColor(new Color(intensity, intensity, intensity));

    int buf = (int) (font_width * 0.75f);
    int x = fm.stringWidth(label) + font_width;
    int rx1 = leftbuffer - (font_width * 2);
    // FIX ME: use ArrowPainter boundary
    int rx2 = leftbuffer - buf;
    int center_y = bottom_y - (fm.getDescent() + (fm.getAscent() / 3));

    g.drawLine(x, center_y, rx1, center_y);
    g.drawLine(rx1, bottom_y, rx2, bottom_y);
    g.drawLine(rx1, top_y, rx2, top_y);
    g.drawLine(rx1, bottom_y, rx1, top_y);

    if (false) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
      QuadCurve2D q = new QuadCurve2D.Float();
      // draw QuadCurve2D.Float with set coordinates
      q.setCurve(rx2, top_y, rx1, center_y, rx2, bottom_y);
      g2.draw(q);
      g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_OFF);
    }

  }

  private int get_top_y_for_line (int line) {
    int buffer = 0;
    return ((line * line_height) - font_ascent) + buffer;
  }

  private int get_bottom_y_for_line (int line) {
    int buffer = 0;
    return (((line - 1) * line_height) + font_descent) - buffer;
  }

  private void draw_overview (Graphics g) {
    //
    //  draw overview coverage graphic
    //
    int x = leftbuffer;
    int y = (draw_line + (config.OVERVIEW_LINES - 1)) * line_height;
    g.setColor(SEQID_COLOR);
    draw_fonted_string((Graphics2D) g, LABEL_ASSEMBLY_COVERAGE, 0, y);

    draw_multiline_indicator(g, LABEL_ASSEMBLY_COVERAGE, draw_line, config.OVERVIEW_LINES);

    int buffer = 0;

    int top_y = get_top_y_for_line(draw_line);

    //    int bottom_y = (((draw_line + (config.OVERVIEW_LINES - 1)) * line_height) + font_descent) - buffer;
    int bottom_y = get_bottom_y_for_line(draw_line + config.OVERVIEW_LINES);
    
    int headroom = (bottom_y - top_y) + 1;
    int avail_x = font_width * (contig_end_view - contig_start_view);

    char[] cons = config.assembly.get_consensus_sequence();
    int clen = cons.length;

    //
    //  clear background:
    //
    g.setColor(SUMMARY_PANEL_BACKGROUND_COLOR);
    g.fillRect(x, top_y, avail_x, (headroom + 1));

    float x_scaler = (float) avail_x / clen;
    // scale factor to fit assembly data into available horizontal space

    int max_coverage = hs.get_max_coverage();

    float y_scaler = (float) headroom / max_coverage;

    //    System.err.println("scalers=" + x_scaler + " " + y_scaler);  // debug
    // double log2 = Math.log(raw) / Math.log(2);

    short[] coverage = hs.get_coverage();
    g.setColor(COVERAGE_COLOR);

    int i;
    //    System.err.println("clen="+clen + " covlen=" + coverage.length);  // debug

    int[] x_points = new int[coverage.length + 1];
    int[] y_points = new int[coverage.length + 1];

    x_points[coverage.length] = leftbuffer + avail_x;
    if (config.OVERVIEW_INVERTED) {
      Arrays.fill(y_points, bottom_y);
      y_points[coverage.length] = top_y;
    } else {
      //      Arrays.fill(y_points, top_y);
      Arrays.fill(y_points, top_y);
      y_points[coverage.length] = bottom_y;
    }

    float cov_scaled;
    PadMap pm = config.assembly.get_padmap();
    
    int basenum;

    for (i=0; i < coverage.length; i++) {
      //      x_points[i] = leftbuffer + (int) (i * x_scaler);
      basenum = pm.get_unpadded_to_padded(i + 1);
      // coverage overview display is in PADDED reference space,
      // but coverage array is in UNPADDED reference space.
      x_points[i] = leftbuffer + (int) (basenum * x_scaler);
      cov_scaled = (coverage[i] * y_scaler);
      if (cov_scaled > 0 && cov_scaled < 1) cov_scaled = 1f;
      // if any reads present at all, always show at least 1 pixel.
      if (config.OVERVIEW_INVERTED) {
	y = top_y + (int) cov_scaled;
	if (y < y_points[i]) y_points[i] = y;
      } else {
	y = bottom_y - (int) cov_scaled;
	if (y > y_points[i]) y_points[i] = y;
      }
    }

    Graphics2D g2 = (Graphics2D) g;
    int y_origin = config.OVERVIEW_INVERTED ? top_y : bottom_y;
    if (true) {
      //
      // version 3: draw individual polygons for regions with coverage
      // - fixes gap problems caused by rounding of individual line coordinates
      //
      ArrayList<Range> ranges = new ArrayList<Range>();
      Range r = null;
      for (i=0; i < coverage.length; i++) {
	if (y_points[i] == y_origin) {
	  // invalid point
	  r = null;
	} else {
	  // valid datapoint
	  if (r == null) {
	    ranges.add(r = new Range());
	    r.start = i;
	  }
	  r.end = i;
	}
      }
      
      for (Range r2 : ranges) {
	// draw polygon for each detected
	int len = r2.size();
	int[] xs = new int[len + 2];
	int[] ys = new int[len + 2];
	System.arraycopy(x_points, r2.start, xs, 1, len);
	System.arraycopy(y_points, r2.start, ys, 1, len);
	// set start and end points at origin to ensure polygon will be closed:
	xs[0] = xs[1];
	ys[0] = y_origin;
	// start point: 1st X, y origin
	xs[len + 1] = xs[len];
	ys[len + 1] = y_origin;
	// end point: last X, y origin
	g2.fillPolygon(xs, ys, len + 2);
      }
      
    } else if (false) {
      // version 2: 
      // manually draw a line for each coverage point.
      for (i=0; i < coverage.length; i++) {
	if (y_points[i] != y_origin) g2.drawLine(x_points[i], y_points[i], x_points[i], y_origin);
	//	System.err.println("draw " + x_points[i]);  // debug
      }
    } else if (false) {
      // version 1: 
      // polygon fill: doesn't seem to handle low regions well.
      // advantage: no gaps / line width problems.
      Object last_hint = g2.getRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING);
      g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
      g2.fillPolygon(x_points, y_points, x_points.length);
      g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, last_hint);
    }

    if (true) {
      //
      //  highlight region being displayed in main window
      //
      Composite c_orig = g2.getComposite();
      AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.40f);
      g2.setComposite(ac);

      int intensity = 255;
      g.setColor(new Color(intensity, intensity, intensity));
      int sx = leftbuffer + (int) (avail_x * ((float) contig_start_view / clen));
      //    int ex = leftbuffer + (int) (avail_x * ((float) contig_end_view / clen));
      // in this version the "window size" is subject to small width fluctuations
      // due to rounding
      int width = (int) ((contig_end_view - contig_start_view) * ((float) avail_x / clen));
      int ex = sx + width;
      // better

      //    System.err.println("clen:" + clen + " si:"+contig_start_view + " ei:"+contig_end_view+ " sx:"+sx +" ex:"+ex);  // debug
      //    g.fillRect(sx, top_y, ((ex - sx) + 1), headroom);
      g.fillRect(sx, top_y, ((ex - sx) + 1), headroom);

      g2.setComposite(c_orig);
    }

    //
    //  reference sequence mappings
    //
    if (true &&
	config.refgenes != null) {
      g2.setColor(CONSENSUS_COLOR);
      for (RefGene rg : config.refgenes) {
	if (!rg.is_initialized() || rg.is_broken()) continue;
	for (Exon exon : rg.get_exons()) {
	  int exs = exon.get_first_start();
	  int exe = exon.get_last_end();

	  ArrayList<Integer> points = new ArrayList<Integer>();
	  
	  for (Codon codon : exon.get_codons()) {
	    if (codon.is_unmappable()) continue;

	    //	    boolean first = true;
	    //	    int min = 0;
	    //	    int max = 0;
	    if (codon.is_spliced()) {
	      // since codon is partially in a different exon, can't
	      // draw a line from start to end since this would span an intron.
	      for (i=0; i < 3; i++) {
		x = leftbuffer + (int) (codon.consensus_pos[i] * x_scaler);
		g2.drawLine(x, top_y, x, top_y);
		//	      if (first || x < min) min = x;
		//	      if (first || x > max) max = x;
		//	      first = false;
	      }
	    } else {
	      // int start = leftbuffer + (int) (codon.consensus_pos[0] * x_scaler);
	      // int end = leftbuffer + (int) (codon.consensus_pos[2] * x_scaler);
	      // g2.drawLine(start, top_y, end, top_y);

	      points.add(codon.consensus_pos[0]);
	      points.add(codon.consensus_pos[2]);
	    }
	  }

	  if (points.size() > 0) {
	    int min = points.get(0);
	    int max = min;
	    for (Integer p : points) {
	      if (p < min) min = p;
	      if (p > max) max = p;
	    }
	    g2.drawLine(leftbuffer + (int) (min * x_scaler), top_y,
			leftbuffer + (int) (max * x_scaler), top_y);
	  }


	}
	break;
	// only process first usable
      }
    }

    //
    //  non-reference frequency mappings
    //
    if (true &&
	hs.is_tumor_normal_trackable()) {
      byte[] nf = new byte[avail_x];
      byte[] tf = new byte[avail_x];

      scale_array(hs.get_normal_nonreference_frequency(), nf, x_scaler);
      scale_array(hs.get_tumor_nonreference_frequency(), tf, x_scaler);

      byte freq;
      int amp;
      //      System.err.println("hr="+headroom);  // debug

      byte MIN_FREQ = (byte) (config.snp_config.MIN_MINOR_ALLELE_FREQUENCY * 100);

      Composite orig_composite = g2.getComposite();
      Stroke orig_stroke = g2.getStroke();
      g2.setStroke(new BasicStroke(1.5f));
      //      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

      g.setColor(Color.blue);
      for (i=0; i < nf.length; i++) {
	if ((freq = nf[i]) >= MIN_FREQ) {
	  amp = (int) (headroom * ((float) freq / 100));
	  if (amp > 0) {
	    //	    System.err.println(i + ": draw freq " + freq + " amp=" + amp);  // debug
	    g2.drawLine(leftbuffer + i, bottom_y,
		       leftbuffer + i, bottom_y - amp);
	  }
	}
      }

      g2.setStroke(new BasicStroke(0.5f));
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
      g.setColor(Color.red);
      for (i=0; i < tf.length; i++) {
	if ((freq = tf[i]) >= MIN_FREQ) {
	  amp = (int) (headroom * ((float) freq / 100));
	  if (amp > 0) {
	    //	    System.err.println(i + ": draw freq " + freq + " amp=" + amp);  // debug
	    g2.drawLine(leftbuffer + i, bottom_y,
		       leftbuffer + i, bottom_y - amp);
	  }
	}
      }


      g2.setStroke(orig_stroke);
      g2.setComposite(orig_composite);
    }


  }

  private void scale_array (byte[] in, byte[] out, float scale_factor) {
    int i,j;
    int pi;
    byte v;
    PadMap pm = config.assembly.get_padmap();
    for (i=0; i < in.length; i++) {
      pi = pm.get_unpadded_to_padded(i);
      if (pi < 0) pi = 0;
      j = (int) (pi * scale_factor);
      //      if (in[i] > 0.4f) System.err.println("freq at " + i + "=" + j + " => " + in[i]);  // debug
      if (in[i] > out[j]) out[j] = in[i];
    }
    
  }

  private void draw_fonted_string (Graphics2D g, String string, int x, int y) {
    //    Object last_hint = g.getRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING);
    //    g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
    if (config.ENABLE_VARIABLE_WIDTH_FONTS) g.setFont(VAR_FONT);
    g.drawString(string, x, y);
    if (config.ENABLE_VARIABLE_WIDTH_FONTS) g.setFont(OUR_FONT);
    //    g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, last_hint);
  }

  private void draw_variable_ruler (Graphics2D g, int line) {
    int y = line * line_height;
    int flank = 20;
    int csv = contig_start_view - flank;
    int end = contig_end_view + flank;
    int x = leftbuffer - (flank * font_width);
    int offset = 0;

    if (config.intron_trim_sites != null) {
      for (int i : config.intron_trim_sites.keySet()) {
	if (i < csv) offset += config.intron_trim_sites.get(i);
      }
    }
    
    PadMap pm = config.assembly.get_padmap();
    g.setFont(VAR_FONT);
    FontMetrics fm = getFontMetrics(OUR_FONT);

    int half_period = fm.stringWidth(".") / 2;
    int half_width = font_width / 2;

    int mod = 10;
    int dot_spacing = 5;

    //    System.err.println("rstart="+config.ruler_start);  // debug

    int lsize = Integer.valueOf(csv + config.ruler_start).toString().length();
    if (lsize >= 7) {
      mod = 20;
      dot_spacing = 10;
    }

    HashSet<String> drew = new HashSet<String>();
    int descent_y = y + font_descent;

    int bn;
    for (bn = csv; bn < end; bn++, x += font_width) {
      int unpadded = bn < 0 ? 0 : pm.get_padded_to_unpadded(bn);
      int label = config.ruler_start + unpadded + offset;
      String lb = Integer.toString(label);
      if (drew.contains(lb)) {
	//	System.err.println("already handled " + lb);  // debug
	continue;
      }
      drew.add(lb);

      if (label % mod == 0) {
	int w = fm.stringWidth(lb);
	g.setColor(RULER_COLOR);
	g.drawString(lb, (x + half_width) - (w/2), y);
      }

      if (label % 10 == 0) {
	g.setColor(RULER_COLOR);
	g.drawLine(x + half_width, descent_y,
		   x + half_width, descent_y);
      } else if (label % 5 == 0) {
	g.setColor(Color.gray);
	//	g.drawString(".", x + half_period, y);
	g.drawLine(x + half_width, descent_y,
		   x + half_width, descent_y);
	//	g.drawString(".", x, y);
      }

      if (config.intron_trim_sites != null) {
	//
	// intron trimming is active
	//
	if (config.intron_trim_sites.containsKey(bn)) {
	  //
	  // at a trim site: draw skip indicator
	  //
	  g.setColor(BACKGROUND_COLOR);
	  int w = fm.stringWidth(">");
	  g.fillRect(x - w,
		     (y - fm.getAscent())
		     + 2, // feh
		     w * 10, fm.getHeight());
	  g.setColor(INTRON_SPLICE_COLOR);
	  g.drawString(">", x - w, y);

	  //
	  //  move base counter ahead by splice amount
	  //
	  int skip = config.intron_trim_sites.get(bn);
	  offset += skip;
	}
      }
    }

    g.setColor(BACKGROUND_COLOR);
    int ty = get_top_y_for_line(line) + 1;
    int by = get_bottom_y_for_line(line);
    
    //    g.fillRect(0, y - font_ascent, leftbuffer, y + fm.getHeight());
    g.fillRect(0, ty, leftbuffer, by);

    g.setFont(OUR_FONT);
  }

  private void intron_splice_check (Graphics2D g, int start_y) {
    if (config.intron_trim_sites != null) {
      g.setColor(INTRON_SPLICE_COLOR);
      Stroke s_orig = g.getStroke();
      Composite c_orig = g.getComposite();
      //      g.setStroke(new BasicStroke(1.0f));
      //      g.setStroke(new BasicStroke(0.3f));
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

      for (Integer splice_pos : config.intron_trim_sites.keySet()) {
	int sp = splice_pos;
	if (sp >= contig_start_view && sp <= contig_end_view) {
	  int x = leftbuffer + ((sp - contig_start_view) * font_width);
	  g.drawLine(x, start_y, x, 1000);
	}
      }
      g.setStroke(s_orig);
      g.setComposite(c_orig);
    }    
  }

  public int get_reference_base_number (int consensus_pos) {
    // convert a padded reference base number to an unpadded
    // base number in the reference, accounting for splices
    //    int mapped_pos = config.assembly.get_padmap().get_padded_to_unpadded(consensus_pos);
    
    // int mapped_pos = config.assembly.get_padmap().get_padded_to_unpadded(consensus_pos);
    // // unpadded reference base # (string space)
    // //    System.err.println("mapped="+mapped_pos);  // debug

    // if (config.intron_compressor != null) {
    //   //      int offset = config.intron_compressor.get_start_shift(mapped_pos, true);
    //   //      mapped_pos += offset;
    //   mapped_pos = config.intron_compressor.get_trimmed_to_untrimmed(mapped_pos);
    //   //      System.err.println("  needed="+offset + " after="+mapped_pos);  // debug
    // }
    
    // start: padded consensus position

    int offset = 0;

    if (config.intron_compressor != null) {
      offset = config.intron_compressor.get_trimmed_to_untrimmed_shift(consensus_pos);
      // convert coordinates
    }

    int mapped_pos = config.assembly.get_padmap().get_padded_to_unpadded(consensus_pos);
    // convert final padded coordinate to unpadded

    if (config.ruler_start > 0) mapped_pos += config.ruler_start;
    mapped_pos += offset;
    return mapped_pos;
  }

  private void run_blat (boolean blat_mate) {
    MouseEvent me = pul.getMouseEvent();
    String id = get_mouse_sequence_id(me);
    int consensus_pos = get_consensus_pos(me);
    System.err.println("id="+id);  // debug
    AssemblySequence as = config.assembly.get_sequence(id);
    if (id != null) as = config.assembly.get_sequence(id);

    if (id == null || as == null || !(as instanceof SAMConsensusMapping)) {
      JOptionPane.showMessageDialog(this,
				    "Error: mouse must be pointed at an assembled SAM/BAM read",
				    "Error",
				    JOptionPane.ERROR_MESSAGE);
    } else {
      SAMConsensusMapping scm = (SAMConsensusMapping) as;
      SAMRecord sr = scm.get_samrecord();

      String query = new String(sr.getReadBases());

      if (blat_mate) {
	SAMRecord mate = null;
	try {
	  for (SAMResource sres : config.sams) {
	    SAMFileReader sfr = sres.getSAMFileReader();
	    if (sfr != null) {
	      mate = sfr.queryMate(sr);
	      if (mate != null) break;
	    }
	  }
	} catch (IOException ex) {
	  System.err.println("ERROR: " + ex);  // debug
	}

	if (mate == null) {
	  JOptionPane.showMessageDialog(this,
					"Sorry, can't retreive mate read.",
					"Error",
					JOptionPane.ERROR_MESSAGE);
	  return;
	} else {
	  query = new String(mate.getReadBases());
	}
      }

      String url = "http://genome.ucsc.edu/cgi-bin/hgBlat?org=Human&db=" + config.BLAT_GENOME + "&type=DNA&userSeq=" + query; 

      if (false) {
	System.err.println("DEBUG, not launching " + url);  // debug
      } else {
	new URLLauncher(url, "blat");
      }
    }
  }


}

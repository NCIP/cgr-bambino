package Ace2;
// to do:
//  - add default profiles (conservative, forgiving)...
//  - reset to defaults button
//  

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import TCGA.ControlFrame;
import TCGA.ControlFrameListener;
import TCGA.URLLauncher;
import layout.SpringUtilities;

public class SNPControl extends Observable implements ControlFrameListener {
  //  private ControlFrame jf;
  private JDialog jf;
  private SNPConfig config;
  private AceViewerConfig avc;
  private JFrame parent_frame = null;

  SpinnerNumber min_quality, min_minor_allele_frequency, min_alt_allele_count, min_coverage;
  SpinnerNumber min_alt_reads_with_flanking_sequence, min_alt_reads_with_flanking_sequence_window;
  SpinnerNumber min_flanking_quality, min_flanking_quality_window;
  SpinnerNumber mmf_max_lq_mismatch, mmf_max_hq_mismatch, mmf_min_high_quality, mmf_min_low_quality;
  SpinnerNumber mismap_ratio, min_alt_allele_count_for_filter_enable, min_mapq, min_unique_alt_read_names;
  SpinnerNumber min_unique_alt_read_start_pos;
  
  public SNPControl() {
    config = new SNPConfig();
    avc = new AceViewerConfig();
    setup();
  }

  public SNPControl(JFrame parent_frame, Observer o, SNPConfig config, AceViewerConfig avc) {
    addObserver(o);
    // modal
    this.parent_frame = parent_frame;
    this.config = config;
    this.avc = avc;
    setup();
  } 

  public SNPControl(SNPConfig config, AceViewerConfig avc) {
    this.config = config;
    this.avc = avc;
    setup();
  } 

  private void setup() {
    
    if (parent_frame == null) {
      jf = new JDialog();
    } else {
      //    jf = new ControlFrame(this);
      jf = new JDialog(parent_frame, true);
    }

    JPanel jp_main = new JPanel();
    jp_main.setLayout(new BoxLayout(jp_main, BoxLayout.PAGE_AXIS));

    JPanel jp_controls = get_buffer_titled_panel(jp_main, "SNP/indel detection options");

    jp_controls.setLayout(new SpringLayout());

    //
    //  create SpinnerNumber instances, copying config variable values
    //
    min_quality = new SpinnerNumber(config.MIN_QUALITY);
    min_mapq = new SpinnerNumber(config.MIN_MAPPING_QUALITY);
    min_minor_allele_frequency = new SpinnerNumber((double) config.MIN_MINOR_ALLELE_FREQUENCY);
    min_alt_allele_count = new SpinnerNumber(config.MIN_ALT_ALLELE_COUNT);
    min_unique_alt_read_names = new SpinnerNumber(config.MIN_UNIQUE_READ_NAMES_FOR_ALT_ALLELE);
    min_unique_alt_read_start_pos = new SpinnerNumber(config.MIN_UNIQUE_START_POSITIONS_FOR_ALT_ALLELE);

    min_alt_allele_count_for_filter_enable = new SpinnerNumber(config.MIN_ALT_ALLELE_COUNT_FOR_FILTER_ENABLE);

    min_coverage = new SpinnerNumber(config.MIN_COVERAGE);

    min_alt_reads_with_flanking_sequence = new SpinnerNumber(config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE);
    min_alt_reads_with_flanking_sequence_window = new SpinnerNumber(config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE_WINDOW);

    min_flanking_quality = new SpinnerNumber(config.MIN_FLANKING_QUALITY);
    min_flanking_quality_window = new SpinnerNumber(config.MIN_FLANKING_QUALITY_WINDOW);

    mmf_max_lq_mismatch = new SpinnerNumber(config.MISMATCH_FILTER_MAX_LQ_MISMATCH_COUNT);
    mmf_max_hq_mismatch = new SpinnerNumber(config.MISMATCH_FILTER_MAX_HQ_MISMATCH_COUNT);
    mmf_min_high_quality = new SpinnerNumber(config.MISMATCH_FILTER_MIN_HIGH_QUALITY);
    mmf_min_low_quality = new SpinnerNumber(config.MISMATCH_FILTER_MIN_LOW_QUALITY);
    mismap_ratio = new SpinnerNumber(config.MISMAP_BASE_FREQUENCY_THRESHOLD);

    //
    //  build UI:
    //
    int rows = 0;
    int columns = 4;

    generate_spinner(jp_controls,
		     min_quality,
		     0, 60, 1,
		     "Minimum nucleotide quality",
		     true);
    rows++;

    generate_spinner(jp_controls,
		     min_mapq,
		     0, 255, 1,
		     "Minimum mapping quality",
		     true);
    rows++;

    generate_spinner(jp_controls,
		     min_coverage,
		     1, 50, 1,
		     "Minimum coverage",
		     true);
    rows++;

    generate_spinner(jp_controls,
		     min_minor_allele_frequency,
		     0, 1f, .01f,
		     "Minimum frequency of alternative allele",
		     true);
    rows++;

    generate_spinner(jp_controls,
		     min_alt_allele_count,
		     1, 50, 1,
		     "Minimum observations of alternative allele",
		     true);
    rows++;

    generate_spinner(jp_controls,
		     min_unique_alt_read_names,
		     1, 50, 1,
		     "Minimum unique read names supporting alternative allele",
		     true);
    rows++;

    generate_spinner(jp_controls,
		     min_unique_alt_read_start_pos,
		     1, 50, 1,
		     "Minimum unique read mapping start positions supporting alternative allele",
		     true);
    rows++;

    generate_spinner(jp_controls,
		     min_alt_allele_count_for_filter_enable,
		     1, 50, 1,
		     "Minimum observations of alternative allele to enable uniqueness filters",
		     true);
    rows++;


     generate_spinner(jp_controls,
 		     min_alt_reads_with_flanking_sequence,
		      0, 50, 1,
		      "Minimum alternative allele reads with flanking sequence",
 		     false);
     generate_spinner(jp_controls,
 		     min_alt_reads_with_flanking_sequence_window,
 		     1, 25, 1,
 		     "   window size",
 		     false
 		     );
     rows++;

     generate_spinner(jp_controls,
		      min_flanking_quality,
		      0, 50, 1,
		      "Minimum quality of flanking sequence",
		      false);
     generate_spinner(jp_controls,
		      min_flanking_quality_window,
		      1, 20, 1,
		      "   window size",
		      false
		      );
     rows++;

     JCheckBox jcb;
     jp_controls.add(new JLabel("Reference mismatch filter: ", SwingConstants.RIGHT));
     jp_controls.add(jcb = new JCheckBox("enable", config.ENABLE_MISMATCH_FILTER));
     jp_controls.add(new JLabel(""));
     jp_controls.add(new JLabel(""));
     rows++;
     jcb.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  config.ENABLE_MISMATCH_FILTER = ((JCheckBox) e.getSource()).isSelected();
	  //	  System.err.println("HEY NOW: " + config.ENABLE_MISMATCH_FILTER);  // debug
	}
       });

     generate_spinner(jp_controls,
		      mmf_max_hq_mismatch,
		      0, 50, 1,
		      "Maximum allowable mismatches to reference sequence",
		      false);
     generate_spinner(jp_controls,
		      mmf_min_high_quality,
		      0, 50, 1,
		      "  min. quality",
		      false);
     rows++;


     generate_spinner(jp_controls,
		      mmf_max_lq_mismatch,
		      0, 50, 1,
		      //		      "<html>Maximum allowable reference mismatches of <u>any</u> quality:</html>  ",
		      "",
		      false);
     generate_spinner(jp_controls,
		      mmf_min_low_quality,
		      0, 50, 1,
		      "  min. quality",
		      false);
     rows++;

     generate_spinner(jp_controls,
		      mismap_ratio,
		      0, 10f, 0.01f,
		      "Mismap filter: max ratio of suspicious mismatches to usable ones",
		      false);
     jp_controls.add(jcb = new JCheckBox("enable", config.ENABLE_MISMAP_FILTER));
     jcb.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  config.ENABLE_MISMAP_FILTER = ((JCheckBox) e.getSource()).isSelected();
	}
       });
     jp_controls.add(new JLabel(""));
     rows++;

     jp_controls.add(new JLabel("Ignore reads with non-primary alignments: ", SwingConstants.RIGHT));
     jp_controls.add(jcb = new JCheckBox("", config.SKIP_NONPRIMARY_ALIGNMENTS));
     jp_controls.add(new JLabel(""));
     jp_controls.add(new JLabel(""));
     rows++;
     jcb.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  config.SKIP_NONPRIMARY_ALIGNMENTS = ((JCheckBox) e.getSource()).isSelected();
	}
       });

     jp_controls.add(new JLabel("Read end mismatch filter: ", SwingConstants.RIGHT));
     jp_controls.add(jcb = new JCheckBox("", config.ENABLE_END_MISMATCH_FILTER));
     jp_controls.add(new JLabel(""));
     jp_controls.add(new JLabel(""));
     rows++;
     jcb.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  config.ENABLE_END_MISMATCH_FILTER = ((JCheckBox) e.getSource()).isSelected();
	}
       });

     jp_controls.add(new JLabel("Mismapped deletion filter: ", SwingConstants.RIGHT));
     jp_controls.add(jcb = new JCheckBox("", config.ENABLE_MISMAPPED_DELETION_FILTER));
     jp_controls.add(new JLabel(""));
     jp_controls.add(new JLabel(""));
     rows++;
     jcb.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  config.ENABLE_MISMAPPED_DELETION_FILTER = ((JCheckBox) e.getSource()).isSelected();
	}
       });

     jp_controls.add(new JLabel("Mate pair disagreement filter: ", SwingConstants.RIGHT));
     jp_controls.add(jcb = new JCheckBox("", config.ENABLE_MATE_PAIR_DISAGREEMENT_FILTER));
     jp_controls.add(new JLabel(""));
     jp_controls.add(new JLabel(""));
     rows++;
     jcb.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  config.ENABLE_MATE_PAIR_DISAGREEMENT_FILTER = ((JCheckBox) e.getSource()).isSelected();
	}
       });
     
     jp_controls.add(new JLabel("When browsing results using SNP/indel list, only display reads: ", SwingConstants.RIGHT));
     //     ButtonGroup bg = new ButtonGroup();
     // problem with this is we can't turn all buttons off!

     jcb = new JCheckBox("at site", avc.CLAMP_SNP_VIEW);
     jcb.setToolTipText("restrict display to only show reads which overlap the site.");
     //     bg.add(jcb);
     jp_controls.add(jcb);
     jcb.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  avc.CLAMP_SNP_VIEW = ((JCheckBox) e.getSource()).isSelected();
	}
       });


     jcb = new JCheckBox("alternative", avc.CLAMP_SNP_VIEW_NONREFERENCE);
     jcb.setToolTipText("restrict display to only show reads which overlap the site and contain the non-reference allele.");
     //     bg.add(jcb);
     jp_controls.add(jcb);
     jcb.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  avc.CLAMP_SNP_VIEW_NONREFERENCE = ((JCheckBox) e.getSource()).isSelected();
	}
       });
     jp_controls.add(new JLabel(""));

     //     jp_controls.add(new JLabel("alternative"));
     rows++;

     if (false) {
       jp_controls.add(new JLabel("Debugging output:", SwingConstants.RIGHT));
       jp_controls.add(new JLabel(""));
       jp_controls.add(jcb = new JCheckBox("enable", SAMStreamingSNPFinder.VERBOSE));
       jp_controls.add(new JLabel(""));

       jcb.addActionListener(new ActionListener() {
	   public void actionPerformed(ActionEvent e) {
	     SAMStreamingSNPFinder.VERBOSE = ((JCheckBox) e.getSource()).isSelected();
	     System.err.println("verbose="+SAMStreamingSNPFinder.VERBOSE);  // debug
	   }
	 });

       rows++;
     }


     int border_pad = 2;
     SpringUtilities.makeCompactGrid(jp_controls,
 				    rows, columns,
 				    // rows, columns
				     border_pad, border_pad, border_pad, border_pad
				     );

    JPanel jp_buttons = new JPanel();
    //    JButton jb = jf.generate_ok_button();
    JButton jb = new JButton();
    jb.setText("Find SNPs / indels");
    //    System.err.println("FIX ME: OK button");  // debug
    jb.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  apply_changes();
	  jf.setVisible(false);
	}
      });

    jp_buttons.add(jb);

    jp_buttons.add(new JLabel("  "));

    jb = new JButton("Help");
    jp_buttons.add(jb);

    jb.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  URLLauncher.launch_modified_url("/goldenPath/bamview/documentation/index.html#snps", "bamview_docs");
	}
      });

    jp_buttons.add(new JLabel("  "));

    jb = new JButton("Cancel");
    jb.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  jf.setVisible(false);
	}
      });
    
    jp_buttons.add(jb);

    jp_main.add(jp_controls);
    jp_main.add(jp_buttons);

    jf.getContentPane().add(new JScrollPane(jp_main));
    jf.pack();
    jf.setTitle("SNP/indel detector");

    jf.setVisible(true);
  }

  public void apply_changes() {

    config.MIN_QUALITY = min_quality.intValue();
    config.MIN_MAPPING_QUALITY = min_mapq.intValue();
    config.MIN_MINOR_ALLELE_FREQUENCY = min_minor_allele_frequency.floatValue();
    config.MIN_ALT_ALLELE_COUNT = min_alt_allele_count.intValue();
    config.MIN_UNIQUE_READ_NAMES_FOR_ALT_ALLELE = min_unique_alt_read_names.intValue();
    config.MIN_UNIQUE_START_POSITIONS_FOR_ALT_ALLELE = min_unique_alt_read_start_pos.intValue();
    System.err.println("value="+config.MIN_UNIQUE_START_POSITIONS_FOR_ALT_ALLELE);  // debug

    config.MIN_ALT_ALLELE_COUNT_FOR_FILTER_ENABLE = min_alt_allele_count_for_filter_enable.intValue();

    config.MIN_COVERAGE = min_coverage.intValue();
    config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE = min_alt_reads_with_flanking_sequence.intValue();
    System.err.println("min alt reads w/flank " + config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE);  // debug

    config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE_WINDOW = min_alt_reads_with_flanking_sequence_window.intValue();
    config.MIN_FLANKING_QUALITY = min_flanking_quality.intValue();
    config.MIN_FLANKING_QUALITY_WINDOW = min_flanking_quality_window.intValue();

    config.MISMATCH_FILTER_MAX_HQ_MISMATCH_COUNT = mmf_max_hq_mismatch.intValue();
    config.MISMATCH_FILTER_MIN_HIGH_QUALITY = mmf_min_high_quality.intValue();

    config.MISMATCH_FILTER_MAX_LQ_MISMATCH_COUNT = mmf_max_lq_mismatch.intValue();
    config.MISMATCH_FILTER_MIN_LOW_QUALITY = mmf_min_low_quality.intValue();

    //     System.err.println("min_quality " + config.MIN_QUALITY);
//     System.err.println("min_minor_freq " + config.MIN_MINOR_ALLELE_FREQUENCY);
//     System.err.println("min_alt_count " + config.MIN_ALT_ALLELE_COUNT);
//     System.err.println("min_cov " + config.MIN_COVERAGE);
//     System.err.println("min_alt_flank " + config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE);
//     System.err.println(" win " + config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE_WINDOW);
//     System.err.println("min_flank_q " + config.MIN_FLANKING_QUALITY);
//     System.err.println(" win " + config.MIN_FLANKING_QUALITY_WINDOW);

//    System.err.println("clamp: " + avc.CLAMP_SNP_VIEW);  // debug
//    System.err.println("clamp nonref: " + avc.CLAMP_SNP_VIEW_NONREFERENCE);  // debug


    System.err.println("setting changed, obs count="+countObservers());  // debug

    setChanged();
    notifyObservers();
  }


  public static void main (String[] argv) {
    Funk.LookAndFeeler.set_native_lookandfeel();
    SNPControl sc = new SNPControl();
  }

  private JPanel get_buffer_titled_panel (JPanel panel, String title) {
    JPanel jp_buffer = new JPanel();
    jp_buffer.setLayout(new BorderLayout());
    //    jp_buffer.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    // why doesn't this work???  appears to add padding just at top, see example below
    //
    //    jp_buffer.setBorder(BorderFactory.createEmptyBorder(100,100,100,100));

    JPanel jp_titled = new JPanel();
    jp_titled.setLayout(new BoxLayout(jp_titled, BoxLayout.PAGE_AXIS));
    jp_titled.setBorder(BorderFactory.createTitledBorder(title));

    jp_buffer.add("Center", jp_titled);
    panel.add(jp_buffer);

    return jp_titled;
  }

  private void generate_spinner (JPanel jp, SpinnerNumber sn,
				 double min, double max, double step,
				 String label, boolean pad) {
    // new SpinnerNumberModel(min_quality.intValue(), 0, 60, 1),
    Number num = sn.get_number();
    SpinnerNumberModel snm = null;
    if (num instanceof Integer) {
      snm = new SpinnerNumberModel(sn.intValue(), (int) min, (int) max, (int) step);
    } else if (num instanceof Float) {
      snm = new SpinnerNumberModel(sn.floatValue(), min, max, step);
    } else if (num instanceof Double) {
      snm = new SpinnerNumberModel(sn.doubleValue(), min, max, step);
    } 

    JSpinner js = new JSpinner(snm);
    jp.add(new JLabel(label + (label.length() > 0 ? ": " : ""), SwingConstants.RIGHT));
    jp.add(js);
    js.addChangeListener(sn);

    Dimension pref = js.getPreferredSize();
    //    pref.width *= 1.75;
    pref.width *= 1.25;
    // spinner's preferred size is too small, particularly for float values (why???)
    js.setPreferredSize(pref);

    if (pad) {
      jp.add(new JLabel(""));
      jp.add(new JLabel(""));
    }
  }

  
}

package Ace2;
// TO DO:
// - add option to flush database cache
// - auto-detect which annotation database to use from source files
//

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.filechooser.*;
import layout.SpringUtilities;
import java.io.*;
import java.util.prefs.*;

import TCGA.URLLauncher;

public class Launcher extends JFrame implements ActionListener {
  private AceViewerConfig config;
  private JCheckBox jcb_multi, jcb_use_ref, jcb_load_dups, jcb_enable_jdbc, jcb_db_flush;
  private JTextArea jta;
  SuffixFileFilter ff_bam, ff_reference;

  private static final int TYPE_NIB = 1;
  private static final int TYPE_2BIT = 2;
  private static final int TYPE_FASTA_DIR = 3;
  private static final int TYPE_FASTA_FAI = 4;

  private int reference_type = 0;

  private Component deep_sigh;

  JFileChooser fc;
  JTextField nib_dir, fn_annotations;
  JTextField jtf_db_database, jtf_db_server, jtf_db_user, jtf_db_password;
  private Preferences prefs;

  private static final String PREF_NIB_PATH = "nib_path";
  private static final String PREF_BAM_FILES = "bam_files";
  private static final String PREF_REFSEQ_TYPE = "refseq_file_type";

  public Launcher (AceViewerConfig config) {
    deep_sigh = this;
    this.config = config;
    prefs = Preferences.userNodeForPackage(this.getClass());

    if (false) {
      System.err.println("deleting preferences!");  // debug
      try {
	prefs.removeNode();
      } catch (Exception e) {
	System.err.println("error");  // debug
      }
      System.exit(1);
    }

    setup();
  }

  private void setup() {
    title_setup();
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    fc = new JFileChooser();
    jcb_multi = new JCheckBox("allow multiple files", true);
    jcb_multi.setToolTipText("if checked, selecting a file will add it to the list; if unchecked, will reset list");

    ff_bam = new SuffixFileFilter("bam");
    ff_reference = new SuffixFileFilter();
    ff_reference.add_suffix("2bit");
    ff_reference.add_suffix("nib");
    ff_reference.add_suffix("fasta");
    ff_reference.add_suffix("fa");

    JPanel jp_main = new JPanel();
    jp_main.setLayout(new BoxLayout(jp_main, BoxLayout.PAGE_AXIS));

    JPanel p = new JPanel(new SpringLayout());

    //    p.add(new JLabel(".bam/.sam file(s):", JLabel.TRAILING));
    p.add(new JLabel(".bam file(s):", JLabel.TRAILING));
    jta = new JTextArea(4, 80);
    //    new Filfre(jta);

    jta.setText(prefs.get(PREF_BAM_FILES, ""));
    p.add(new JScrollPane(jta));

    JButton jb;

    add_paneled_button(p, jb = new JButton("Browse..."));
    //    p.add(jb = new JButton("Browse..."));
    jb.setToolTipText("select a .bam file to view");

    jb.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  File fn = get_local_file(ff_bam);

	  if (jcb_multi.isSelected()) {
	    // multiple .bam files allowed
	    if (fn != null) {
	      String existing = jta.getText();
	      if (existing != null && existing.length() > 0) jta.append("\n");
	      jta.append(fn.toString());
	    }
	  } else {
	    if (fn == null) {
	      jta.setText("");
	    } else {
	      jta.setText(fn.toString());
	    }
	  }
	}
      });

    p.add(new JLabel(""));
    p.add(jcb_multi);
    p.add(new JLabel(""));
    
    p.add(new JLabel("reference sequence:", JLabel.TRAILING));
    p.add(nib_dir = new JTextField(80));
    nib_dir.addFocusListener(new FocusListener() {
	// begin FocusListener stubs
	public void focusGained(FocusEvent e) {}
	public void focusLost(FocusEvent e) {
	  String s = nib_dir.getText();
	  if (s.length() > 0) configure_reference_sequence(new File(s));
	}
	// end FocusListener stubs
      });

    String nib_path = prefs.get(PREF_NIB_PATH, "");
    nib_dir.setText(nib_path);
    if (nib_path.length() > 0) {
      reference_type = Integer.parseInt(prefs.get(PREF_REFSEQ_TYPE, "0"));
      System.err.println("loaded reference type " + reference_type);  // debug
      configure_reference_sequence(new File(nib_path));
    }

    add_paneled_button(p, jb = new JButton("Browse..."));
    jb.setToolTipText("browse for a reference sequence sequence file in UCSC .2bit or .nib format");

//     JPanel jp = new JPanel();
//     jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));
//     jp.add(jb);
//     p.add(jp);

    jb.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  File fn = get_local_file(ff_reference);
	  //	  File fn = get_local_directory();
	  configure_reference_sequence(fn);
// 	  if (fn == null) {
// 	    nib_dir.setText("");
// 	  } else {
// 	    String s = fn.toString().toLowerCase();
// 	    if (s.indexOf(".nib") == (s.length() - 4)) {
// 	      // .nib file returned: use directory name
// 	      nib_dir.setText(fn.getParent());
// 	      reference_type = TYPE_NIB;
// 	    } else if (s.indexOf(".2bit") == (s.length() - 4)) {
// 	      reference_type = TYPE_2BIT;
// 	      nib_dir.setText(fn.toString());
// 	    } else if ((s.indexOf(".fasta") == (s.length() - 6)) ||
// 		       (s.indexOf(".fa") == (s.length() - 3))) {
// 	      File fai = new File(s + ".fai");
// 	      if (fai.exists()) {
// 		// single FASTA file, indexed with "samtools faidx"
// 		reference_type = TYPE_FASTA_FAI;
// 		nib_dir.setText(fn.toString());
// 	      } else {
// 		// directory of FASTA files, one file per reference sequence
// 		nib_dir.setText(fn.getParent());
// 		reference_type = TYPE_FASTA_DIR;
// 	      }
// 	    } else {
// 	      System.err.println("ERROR: unknown file type");  // debug
// 	      nib_dir.setText("error, unknown file");
// 	    }
// 	  }

	}
      });

    p.add(new JLabel(""));
    p.add(jcb_use_ref = new JCheckBox("use reference sequence (recommended)", true));
    p.add(new JLabel(""));

    p.add(new JLabel(""));
    p.add(new JLabel("Reference sequence in FASTA (directory or samtools .fai indexed), or UCSC .2bit / .nib formats"));
    p.add(new JLabel(""));

    p.add(new JLabel(""));
    JLabel jl;
    p.add(jl = new JLabel("<html><a href=\"bogus\">click to download hg18.2bit</a>"));
    jl.setToolTipText("download and save a binary version of the human reference genome sequence build hg18 to your computer");

    jl.addMouseListener(new MouseListener() {
	// begin MouseListener stubs
	public void mousePressed(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {
	  //	  new URLLauncher("ftp://hgdownload.cse.ucsc.edu/gbdb/hg18/nib/", "hg18");
	  new URLLauncher("ftp://hgdownload.cse.ucsc.edu/gbdb/hg18/nib/hg18.2bit", "hg18");
	};
	public void mouseReleased(MouseEvent e) {};
	public void mouseEntered(MouseEvent e) {};
	public void mouseExited(MouseEvent e) {};
	// end MouseListener stubs
      });
    p.add(new JLabel(""));

    jcb_use_ref.addActionListener(new ActionListener() {
	// begin ActionListener stub
	public void actionPerformed(ActionEvent e) {
	  JCheckBox jcb = (JCheckBox) e.getSource();
	  nib_dir.setEnabled(jcb.isSelected());
	}
	// end ActionListener stub
      });

    SpringUtilities.makeCompactGrid(p,
				    6, 3,
				    // rows, columns
				    6,6,
				    6,6);

    

    jp_main.add(p);

    //
    //  options pane layout
    //
    JPanel jp_options = new JPanel(new SpringLayout());
    jp_options.add(jcb_load_dups = new JCheckBox("load optical/PCR duplicates", false));

    SpringUtilities.makeCompactGrid(jp_options,
 				    1, 1,
 				    // rows, columns
 				    6,6,
 				    6,6);


    //
    //  annotations database pane layout
    //
    JPanel jp_annot = new JPanel(new SpringLayout());


    jtf_db_database = new JTextField(80);
    jtf_db_server = new JTextField(80);
    jtf_db_user = new JTextField(80);
    jtf_db_password = new JTextField(80);

    jp_annot.add(new JLabel(""));
    jp_annot.add(jcb_enable_jdbc = new JCheckBox("Enable database queries", true));
    jp_annot.add(new JLabel(""));
    jp_annot.add(jcb_db_flush = new JCheckBox("Clear database cache", false));
    jcb_enable_jdbc.addActionListener(new ActionListener() {
	// begin ActionListener stub
	public void actionPerformed(ActionEvent e) {
	  JCheckBox jcb = (JCheckBox) e.getSource();
	  boolean enabled = jcb.isSelected();
	  config.ENABLE_JDBC = enabled;
	  jtf_db_database.setEnabled(enabled);
	  jtf_db_server.setEnabled(enabled);
	  jtf_db_user.setEnabled(enabled);
	  jtf_db_password.setEnabled(enabled);
	}
	// end ActionListener stub
      });

    jp_annot.add(new JLabel("Database name:", JLabel.TRAILING));
    jp_annot.add(jtf_db_database);
    jtf_db_database.setText(JDBCCache.UCSC_DB_DATABASE);

    jp_annot.add(new JLabel("server:", JLabel.TRAILING));
    jp_annot.add(jtf_db_server);
    jtf_db_server.setText(JDBCCache.UCSC_DB_SERVER);

    jp_annot.add(new JLabel("username:", JLabel.TRAILING));
    jp_annot.add(jtf_db_user);
    jtf_db_user.setText(JDBCCache.UCSC_DB_USERNAME);

    jp_annot.add(new JLabel("password:", JLabel.TRAILING));
    jp_annot.add(jtf_db_password);
    jtf_db_password.setText(JDBCCache.UCSC_DB_PASSWORD);
    
    SpringUtilities.makeCompactGrid(jp_annot,
 				    6, 2,
 				    // rows, columns
 				    6,6,
 				    6,6);

    //
    //  tabbed pane layout
    //
    Container cp = getContentPane();
    JTabbedPane jtp = new JTabbedPane();

    jtp.add("Data files", jp_main);

    //    jtp.add("Annotation database", new JScrollPane(jp_annot));
    JPanel jp_bogus = new JPanel();
    jp_bogus.add(jp_annot);
    jtp.add("Annotation database", jp_bogus);
    // prevent layout from resizing components

    jtp.add("Options", jp_options);

    JPanel jp = new JPanel();
    //    jp.setLayout(new BorderLayout());
    //    jp.add("Center", new JButton("xxx"));
    jp.add(jb = new JButton("Start viewer"));
    jb.addActionListener(this);

    //    cp.setLayout(new BorderLayout());
    cp.add("Center", jtp);
    cp.add("South", jp);

      //    cp.add("Center", jp_main);
    pack();
    setVisible(true);
  }

  public File get_local_directory () {
    fc.resetChoosableFileFilters();
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    return fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
  }

  public File get_local_file (javax.swing.filechooser.FileFilter ff) {
    fc.addChoosableFileFilter(ff);
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    return fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
  }

    
  public void actionPerformed(ActionEvent e) {
    //
    //  launch
    //
    String error_msg = null;

    JDBCCache.UCSC_DB_DATABASE = jtf_db_database.getText();
    JDBCCache.UCSC_DB_SERVER = jtf_db_server.getText();
    JDBCCache.UCSC_DB_USERNAME = jtf_db_user.getText();
    JDBCCache.UCSC_DB_PASSWORD = jtf_db_password.getText();

    if (jcb_db_flush.isSelected()) {
      JDBCCache cache = new JDBCCache();
      cache.flush_cache();
    }

    if (jcb_use_ref.isSelected()) {
      // use reference sequence
      String nib_path = nib_dir.getText();
      //      System.err.println("path="+nib_path);  // debug
      //      configure_reference_sequence(new File(nib_path));
      // hack: do this even if we've done it before, in case user has 
      // manually entered a new path without using Browse button.
      // manually-entered file may also be of a different type, etc.

      System.err.println("init reference sequence: type=" + reference_type + " path=" + nib_path);  // debug
      if (nib_path != null && nib_path.length() > 0) {
	File path = new File(nib_path);
	if (reference_type == TYPE_NIB) {
	  // directory of .nib files
	  NIB.DEFAULT_NIB_DIR = nib_path;
	  config.reference_sequence = new NIB();
	} else if (reference_type == TYPE_2BIT) {
	  // .2bit file
	  try {
	    config.reference_sequence = new TwoBitFile(nib_path);
	  } catch (Exception exc) {
	    new ErrorReporter(exc);
	  }
	} else if (reference_type == TYPE_FASTA_DIR) {
	  config.reference_sequence = new FASTADirectory(nib_path);
	} else if (reference_type == TYPE_FASTA_FAI) {
	  try {
	    config.reference_sequence = new FASTAIndexedFAI(nib_path);
	  } catch (Exception exc) {
	    new ErrorReporter(exc);
	  }
	} else {
	  System.err.println("ERROR: don't know how to init reference sequence, type= " + reference_type + " path=" + nib_path);  // debug
	}

	if (config.reference_sequence != null) {
	  prefs.put(PREF_NIB_PATH, nib_path);
	  prefs.put(PREF_REFSEQ_TYPE, Integer.toString(reference_type));
	}

      } else {
	error_msg = "no reference sequence specified.  Uncheck \"use reference sequence\" or download a local copy of reference genome data files.";
      }
    } else {
      config.GENERATE_CONSENSUS = true;
    }

    String files = jta.getText();
    if (files != null) {
      files = Funk.Str.trim_whitespace(files);
      if (files.length() > 0) {
	//	String[] names = files.split("\\s+");
	// DUH: breaks if path has spaces in it
	String[] names = files.split("\n");
	// FIX ME: java localized system constant??

	for (int i = 0; i < names.length; i++) {
	  String fname = names[i];

	  if (fname == null || fname.length() == 0) continue;
	  // blank lines

	  if (fname.toLowerCase().indexOf(".bam") != fname.length() - 4) {
	    error_msg = "Specify only .bam files: file \"" + fname + "\"" + " does not end in .bam";
	    break;
	  }

	  fname = Funk.Str.trim_whitespace(fname);
	  File f = new File(fname);
	  if (f.exists()) {
	    // file exists
	    if (SAMUtils.is_bam_indexed(f)) {
	      SAMResource sr = new SAMResource();
	      if (!jcb_load_dups.isSelected()) sr.set_load_duplicates(false);
	      sr.set_file(fname);
	      config.LOCAL_FILE_MODE = true;
	      sr.detect_sample_id();
	      config.sams.add(sr);
	    } else {
	      error_msg = "BAM file " + f + " is not indexed; use \"samtools index file.bam\" to generate.";
	      break;
	    }
	  } else {
	    error_msg = "file \"" + fname + "\" does not exist.";
	    break;
	  }
	}
	      
	prefs.put(PREF_BAM_FILES, files);
      }
    }

    if (error_msg != null) {
      // problem already 
    } else if (config.sams.size() > 0) {
      try {
	SAMRegion region = new SAMRegion();
	config.region = region;
	if (true) {
	  config.region_default_setup();
	} else if (false) {
	  region.tname = "chr17";
	  region.range.start = 7512444;
	  region.range.end = 7531588;
	} else {
	  // FIX ME: null result = keeps doing db query!
	  region.tname = "chr1";
	  region.range.start = 1;
	  region.range.end = 1000;
	}
	// hacktacular
	Exception ex = SAMUtils.sam_config_setup(config, region);
	
	if (ex != null) {
	  System.err.println("ERROR: " + e);  // debug
	  ex.printStackTrace();

	  if (ex instanceof java.sql.SQLException || ex.toString().toLowerCase().indexOf("jdbc") > -1) {
	    // hacky, but may be many class names
	    JOptionPane.showMessageDialog(this,
					  "Problem communicating with database; see Java error log for more details.",
					  "Warning",
					  JOptionPane.WARNING_MESSAGE);

	    
	  }

	}

	      
	//	config.assembly = new SAMAssembly(config.sams, config.target_sequence, config.ruler_start, true);
	config.assembly = new SAMAssembly(config, true);
      } catch (Throwable t) {
	new Funk.ErrorReporter(t);
      }
    } else {
      error_msg = "no files specified.";
    }

    if (error_msg != null) {
      JOptionPane.showMessageDialog(deep_sigh,
				    "Error: " + error_msg,
				    "Error",
				    JOptionPane.ERROR_MESSAGE);
    } else {
      AceViewer av = new AceViewer(config);
      deep_sigh.setVisible(false);
    }
  }

  private void title_setup () {
    String title;
    double rand = Math.random();
    if (rand < 0.65) {
      title = "bambino";
    } else if (rand < 0.94) {
      title = "perbambulator";
    } else if (rand < 0.97) {
      title = "bamboozle";
    } else {
      title = "bamnation";
    }

    setTitle(title + ": launcher");
  }

  private void add_paneled_button (JPanel p, JButton jb) {
    JPanel jp = new JPanel();
    if (true) {
      jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));
      jp.add(jb);
    } else {
      jp.setLayout(new FlowLayout());
      //      jp.add("Center", jb);
      jp.add(jb);
    }
    p.add(jp);
  }

  private void configure_reference_sequence (File fn) {
    //	  File fn = get_local_directory();
    if (fn == null) {
      nib_dir.setText("");
    } else if (fn.isDirectory()) {
      // file is already a directory (loaded from Preferences)
      // question 
      if (reference_type == TYPE_FASTA_DIR || reference_type == TYPE_NIB) {
	nib_dir.setText(fn.toString());
      }
    } else {
      String s = fn.toString().toLowerCase();
      //      System.err.println("returned: " + s);  // debug
      //      System.err.println("index="+s.indexOf(".2bit") + " len="+s.length());
      if (s.indexOf(".nib") == (s.length() - 4)) {
	// .nib file returned: use directory name
	nib_dir.setText(fn.getParent());
	reference_type = TYPE_NIB;
      } else if (s.indexOf(".2bit") == (s.length() - 5)) {
	reference_type = TYPE_2BIT;
	nib_dir.setText(fn.toString());
      } else if ((s.indexOf(".fasta") == (s.length() - 6)) ||
		 (s.indexOf(".fa") == (s.length() - 3))) {
	File fai = new File(s + ".fai");
	if (fai.exists()) {
	  // single FASTA file, indexed with "samtools faidx"
	  reference_type = TYPE_FASTA_FAI;
	  nib_dir.setText(fn.toString());
	} else {
	  // directory of FASTA files, one file per reference sequence
	  nib_dir.setText(fn.getParent());
	  reference_type = TYPE_FASTA_DIR;
	}
      } else {
	System.err.println("ERROR: unknown file type");  // debug
	nib_dir.setText("error, unknown file");
      }
    }
  }


}

package Ace2;

import net.sf.samtools.*;
import net.sf.samtools.SAMRecord.*;
import net.sf.samtools.util.CloseableIterator;
import java.io.*;
import java.util.*;

public class PolyReport {
  
  private static final String ALTERNATE_IGNORE_SUFFIX = "_alternate";
  private static int IGNORE_SUFFIX_LENGTH = ALTERNATE_IGNORE_SUFFIX.length();
  private int target_basenum;
  private static final char NULL_CHAR = '0';

  private String poly_dir = null;
  //"c:\\generatable\\max_poly";
  private String out_file_name = "poly_report.tab";

  private HashMap<String,String> extra_info = null;

  public PolyReport (String poly_dir) {
    this.poly_dir = poly_dir;
  }

  public void report (File bam_file, String ref_name, int target_basenum) {
    this.target_basenum = target_basenum;
    SAMFileReader sfr = new SAMFileReader(bam_file);
    CloseableIterator<SAMRecord> iterator = sfr.queryOverlapping(ref_name,
								 target_basenum,
								 target_basenum
								 );
    get_some(iterator, ref_name + "." + target_basenum, NULL_CHAR, NULL_CHAR);
  }

  public void report (File bam_file, String ref_name, int target_basenum, char ref, char var) {
    this.target_basenum = target_basenum;
    SAMFileReader sfr = new SAMFileReader(bam_file);
    CloseableIterator<SAMRecord> iterator = sfr.queryOverlapping(ref_name,
								 target_basenum,
								 target_basenum
								 );
    get_some(iterator, ref_name + "." + target_basenum, ref, var);
  }

  public void set_poly_dir (String poly_dir) {
    poly_dir = poly_dir;
  }

  public void set_outfile (String out_file) {
    out_file_name = out_file;
  }

  private void get_some (CloseableIterator<SAMRecord> iterator, String site_name, char reference_base, char variant_base) {
    int len, read_i, reference_basenum, end;

    boolean target_mode = reference_base != NULL_CHAR;
    // whether interrogating known reference and variant bases
    // vs. reporting phred's called and alternate base info

    Reporter rpt = new Reporter();

    if (extra_info != null && extra_info.size() > 0) {
      ArrayList<String> labels = new ArrayList<String>(extra_info.keySet());
      Collections.sort(labels);
      for (String label : labels) {
	rpt.add_header(label);
      }
    }

    rpt.add_header("site");
    rpt.add_header("read_name");
    rpt.add_header("strand");
    rpt.add_header("base_quality");
    
    if (target_mode) {
      rpt.add_header("reference_base");
      rpt.add_header("reference_base_amplitude");
      rpt.add_header("reference_base_phred_type");
      rpt.add_header("variant_base");
      rpt.add_header("variant_base_amplitude");
      rpt.add_header("variant_base_phred_type");
    } else {
      rpt.add_header("called_base");
      rpt.add_header("called_base_amplitude");
      rpt.add_header("alternate_base");
      rpt.add_header("alternate_base_amplitude");
    }

    try {
      rpt.set_output_filename(out_file_name);

      while (iterator.hasNext()) {
	SAMRecord sr = iterator.next();
	String trace_name = sr.getReadName();
	//      System.err.println("trace:"+trace_name + " strand:"+ (sr.getReadNegativeStrandFlag() ? "-" : "+"));  // debug

	if (trace_name.indexOf(ALTERNATE_IGNORE_SUFFIX) == 
	    trace_name.length() - IGNORE_SUFFIX_LENGTH) {
	  continue;
	}

	if (extra_info != null && extra_info.size() > 0) {
	  for (String label : extra_info.keySet()) {
	    rpt.set_value(label, extra_info.get(label));
	  }
	}


	rpt.set_value("site", site_name);
	rpt.set_value("read_name", trace_name);
	rpt.set_value("strand", sr.getReadNegativeStrandFlag() ? "-" : "+");

	byte[] bases = sr.getReadBases();
	byte[] quals = sr.getBaseQualities();

	//      System.err.println("target="+target_basenum);  // debug

	boolean found = false;

	for (AlignmentBlock ab : sr.getAlignmentBlocks()) {
	  len = ab.getLength();
	  read_i = ab.getReadStart() - 1;
	  reference_basenum = ab.getReferenceStart();

	  if (read_i < 0 || reference_basenum < 1) continue;

	  //	System.err.println("block start="+reference_basenum);  // debug

	  for (end = read_i + len; read_i < end; read_i++, reference_basenum++) {

	    //	  System.err.println("  ref " + reference_basenum);  // debug

	    if (reference_basenum == target_basenum) {
	      //	    System.err.println("hit!");  // debug

	      File poly_file = new File(poly_dir + File.separator + trace_name + ".poly");
	      //	    System.err.println("polyf="+poly_file);  // debug

	      PhredPolyFile pf = new PhredPolyFile();
	      pf.parse(poly_file);

	      if (sr.getReadNegativeStrandFlag()) {
		pf.reverse_complement();
	      }

	      PhredPolyFileEntry pfe = pf.get_entry_for_index(read_i);
	      char sam_base = (char) bases[read_i];
	      
	      if (sam_base != pfe.called_base) {
		System.err.println("BAM/.poly base mismatch!!");  // debug
		System.exit(1);
	      }

	      rpt.set_value("base_quality", Integer.toString(quals[read_i]));

	      if (target_mode) {
		populate_info(pfe, rpt, reference_base, "reference");
		populate_info(pfe, rpt, variant_base, "variant");
	      } else {
		rpt.set_value("called_base", Character.toString(sam_base));
		rpt.set_value("alternate_base", Character.toString(pfe.uncalled_base));

		rpt.set_value("called_base_amplitude", Float.toString(pfe.get_called_base_amplitude()));
		rpt.set_value("alternate_base_amplitude", Float.toString(pfe.get_uncalled_base_amplitude()));
	      }

	      rpt.end_row();

	      found = true;
	      break;
	    }
	  }

	  if (found) break;
	}

	if (!found) {
	  System.err.println("note: no hit for " + trace_name + "; deletion?");
	}
      }

      rpt.close();
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      System.exit(1);
    }

  }

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);

    String tname = null;
    int basenum = -1;
    File bam = null;
    String poly_dir = null;
    String outfile = "poly_report.tab";
    char reference_base = 0;
    char variant_base = 0;

    HashMap<String,String> report = new HashMap<String,String>();

    for (int i=0; i < argv.length; i++) {
      if (argv[i].equals("-ref")) {
	tname = new String(argv[++i]);
      } else if (argv[i].equals("-base")) {
	basenum = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-outfile")) {
	outfile = new String(argv[++i]);
      } else if (argv[i].equals("-bam")) {
	bam = new File(argv[++i]);
      } else if (argv[i].equals("-poly")) {
	poly_dir = new String(argv[++i]);
      } else if (argv[i].equals("-ref-base")) {
	// target reference base
	String base = argv[++i];
	if (base.length() == 1) {
	  reference_base = base.charAt(0);
	  System.err.println("rb="+reference_base);  // debug
	} else {
	  System.err.println("-ref-base base is a char");  // debug
	  System.exit(1);
	}
      } else if (argv[i].equals("-var-base")) {
	// target variant base
	String base = argv[++i];
	if (base.length() == 1) {
	  variant_base = base.charAt(0);
	  System.err.println("vb="+variant_base);  // debug
	} else {
	  System.err.println("-var-base base is a char");  // debug
	  System.exit(1);
	}
      } else if (argv[i].equals("-report")) {
	String[] stuff = argv[++i].split(",");
	if (stuff.length == 2) {
	  report.put(stuff[0], stuff[1]);
	} else {
	  System.err.println("ERROR: -report uses format column,label");  // debug
	  System.exit(1);
	}
      } else if (argv[i].equals("-demo")) {
	bam = new File("all.primary_phred_15_area_ratio_0.33_untrimmed.sam_diploid.sorted.bam");
	tname = "chr10";
	basenum = 13726648;
	poly_dir = "c:\\generatable\\max_poly\\";
      } else {
	System.err.println("ERROR: unknown argument " + argv[i]);  // debug
	usage();
      }
    }

    if (tname != null &&
	basenum != -1 &&
	poly_dir != null &&
	bam != null) {
      PolyReport pr = new PolyReport(poly_dir);
      if (report.size() > 0) pr.set_report_info(report);
      pr.set_outfile(outfile);
      if (reference_base != 0 || variant_base != 0) {
	if (reference_base != 0 && variant_base != 0) {
	  pr.report(bam, tname, basenum, reference_base, variant_base);
	} else {
	  System.err.println("must specify both -ref-base and -var-base");  // debug
	  System.exit(1);
	}
      } else {
	pr.report(bam, tname, basenum);
      }
    } else {
      usage();
    }
  }

  private static void usage() {
    System.err.println("usage:");  // debug

    System.err.println("  input data:");  // debug
    System.err.println("    -poly [directory containing phred .poly files / poly_dir]");  // debug
    System.err.println("    -bam [bamfile]");  // debug
    System.err.println("  target site:");  // debug
    System.err.println("    -ref [reference sequence name (e.g. chr1)]");
    System.err.println("    -base [base number]");
    System.err.println("  target alleles mode (optional):");  // debug
    System.err.println("    -ref-base [reference allele to query]");
    System.err.println("    -var-base [variant allele to query]");
    System.err.println("  output:");  // debug
    System.err.println("    -outfile [output .tab report (default: poly_report.tab)]");
    System.exit(1);
  }

  private void populate_info (PhredPolyFileEntry pfe, Reporter rpt, char target_base, String label_root) {
    target_base = Character.toUpperCase(target_base);
    float amplitude = 0;
    String phred_call_type = "?";
    if (pfe.called_base == target_base) {
      // desired base was phred's primary call
      phred_call_type = "called_base";
      amplitude = pfe.get_called_base_amplitude();
    } else if (pfe.uncalled_base == target_base) {
      // desired base was phred's secondary call
      phred_call_type = "alternate_base";
      amplitude = pfe.get_uncalled_base_amplitude();
    } else {
      // phred did not call the target base, as either primary or secondary
      phred_call_type = "not_called";
      if (target_base == 'A') {
	amplitude = pfe.called_base_amp_a;
      } else if (target_base == 'C') {
	amplitude = pfe.called_base_amp_c;
      } else if (target_base == 'G') {
	amplitude = pfe.called_base_amp_g;
      } else if (target_base == 'T') {
	amplitude = pfe.called_base_amp_t;
      } else {
	System.err.println("WTF?");  // debug
	System.exit(1);
      }
    }

    String label_base = label_root + "_base";
    String label_amp = label_root + "_base_amplitude";
    String label_phred_type = label_root + "_base_phred_type";

    rpt.set_value(label_base, Character.toString(target_base));
    rpt.set_value(label_amp, Float.toString(amplitude));
    rpt.set_value(label_phred_type, phred_call_type);
  }

  public void set_report_info (HashMap<String,String> extra_info) {
    this.extra_info = extra_info;
  }
  
}

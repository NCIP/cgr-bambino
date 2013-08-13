package Ace2;
// query base

import net.sf.samtools.*;
import net.sf.samtools.SAMRecord.*;
import net.sf.samtools.util.CloseableIterator;
import java.io.*;
import java.util.*;
import Funk.DelimitedFile;

public class ReadReport {
  
  private int target_basenum;
  private int min_base_quality = 15;
  private int min_mapping_quality = 1;
  private String out_file_name = "read_report.tab";
  private boolean summary_mode = false;
  private boolean count_optical_duplicates = false;
  private Reporter passthrough = null;
  private String colname_ref = "reference";
  private String colname_base = "base_num";

  public void report (File bam_file, String ref_name, int target_basenum) {
    this.target_basenum = target_basenum;
    SAMFileReader sfr = new SAMFileReader(bam_file);
    ChromosomeDisambiguator cd = new ChromosomeDisambiguator(sfr);
    CloseableIterator<SAMRecord> iterator = sfr.queryOverlapping(cd.find(ref_name),
								 target_basenum,
								 target_basenum
								 );
    get_some(iterator, ref_name + "." + target_basenum);
  }

  public void report (File bam_file, String ref_name, File passthrough_report) {
    try {
      SAMFileReader sfr = new SAMFileReader(bam_file);
      ChromosomeDisambiguator cd = new ChromosomeDisambiguator(sfr);

      DelimitedFile df = new DelimitedFile();
      // needs updating
      df.set_delimiter_mandatory(false);
      df.parse(passthrough_report);

      ArrayList<String> headers = new ArrayList<String>();
      Vector v = df.get_labels();
      for (Object o : v) {
	headers.add((String) o);
      }
      headers.add("A");
      headers.add("C");
      headers.add("G");
      headers.add("T");
      //      System.err.println("head="+headers);  // debug

      passthrough = new Reporter();
      passthrough.set_output_filename(passthrough_report + ".read_report.tab");
      passthrough.set_headers(headers);

      boolean read_ref_name = ref_name == null;
      // no reference name specified on command line:
      // assume specified in spreadsheet

      for (Object o : df.get_rows()) {
	Hashtable row = (Hashtable) o;
	for (Object ko : row.keySet()) {
	  String key = (String) ko;
	  passthrough.set_value(key, (String) row.get(key));
	  // pass through raw report values
	}

	String s_base = (String) row.get(colname_base);
	if (s_base == null) 
	  throw new IOException("FATAL ERROR: column " + colname_base + " doesn't exist");  // debug

	target_basenum = Integer.parseInt(s_base);

	if (read_ref_name) {
	  ref_name = (String) row.get(colname_ref);
	  if (ref_name == null) 
	    throw new IOException("FATAL: no reference name (command line or input)");
	}

	System.err.println("target: " + ref_name + "." + target_basenum);

	CloseableIterator<SAMRecord> iterator = sfr.queryOverlapping(cd.find(ref_name),
								     target_basenum,
								     target_basenum
								     );
	get_some(iterator, ref_name + "." + target_basenum);
	iterator.close();
	passthrough.end_row();
      }
      passthrough.close();
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }

  public void set_column_reference (String s) {
    colname_ref = s;
  }

  public void set_column_base_num (String s) {
    colname_base = s;
  }

  public void set_summary_mode (boolean v) {
    summary_mode = v;
  }

  public void set_outfile (String out_file) {
    out_file_name = out_file;
  }

  public void set_min_mapping_quality(int q) {
    min_mapping_quality = q;
  }

  public void set_min_base_quality (int q) {
    min_base_quality = q;
  }

  public void count_optical_duplicates (boolean v) {
    count_optical_duplicates = v;
  }

  private void get_some (CloseableIterator<SAMRecord> iterator, String site_name) {
    int len, read_i, reference_basenum, end;

    Reporter rpt = null;

    //    System.err.println("count dups?: " + count_optical_duplicates);  // debug
    if (summary_mode == false && passthrough == null) {
      rpt = new Reporter();
      rpt.add_header("site");
      rpt.add_header("read_name");
      rpt.add_header("read_mapq");
      rpt.add_header("strand");
      rpt.add_header("base");
      rpt.add_header("base_quality");
    }

    try {
      if (rpt != null) rpt.set_output_filename(out_file_name);

      HashCounter counts = new HashCounter();
      int raw_overlapping_reads = 0;
      int cooked_overlapping_reads = 0;

      while (iterator.hasNext()) {
	SAMRecord sr = iterator.next();
	String trace_name = sr.getReadName();
	//      System.err.println("trace:"+trace_name + " strand:"+ (sr.getReadNegativeStrandFlag() ? "-" : "+"));  // debug

	int mapq = sr.getMappingQuality();
	if (mapq < min_mapping_quality) continue;

	if (count_optical_duplicates == false
	    && sr.getDuplicateReadFlag()) continue;
	// exclude optical/PCR duplicates from count
	
	if (rpt != null) {
	  rpt.set_value("site", site_name);
	  rpt.set_value("read_name", trace_name);
	  rpt.set_value("read_mapq", Integer.toString(mapq));
	  rpt.set_value("strand", sr.getReadNegativeStrandFlag() ? "-" : "+");
	}

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
	      //
	      //  read aligns with target site
	      //
	      raw_overlapping_reads++;

	      char sam_base = (char) bases[read_i];
	      
	      int qual = quals[read_i];

	      if (qual >= min_base_quality) {
		//
		// usable observation
		//
		cooked_overlapping_reads++;
		counts.add_ignore_case(sam_base);
		if (rpt != null) {
		  rpt.set_value("base", Character.toString(sam_base));
		  rpt.set_value("base_quality", Integer.toString(qual));
		  rpt.end_row();
		}
	      }

	      found = true;
	      break;
	    }
	  }
	  if (found) break;
	}
      }

      //      System.err.println("overlapping reads: raw=" + raw_overlapping_reads + " cooked=" + cooked_overlapping_reads);  // debug

      if (rpt != null) rpt.close();
      if (summary_mode) {
	HashMap<String,Integer> base2count = counts.get_counts();
	ArrayList<String> bases = new ArrayList<String>(base2count.keySet());
	Collections.sort(bases);
	PrintStream ps;
	WorkingFile wf = null;
	if (out_file_name.equals("-")) {
	  ps = new PrintStream(System.out);
	} else {
	  wf = new WorkingFile(out_file_name);
	  ps = wf.getPrintStream();
	}
	for (String base : bases) {
	  ps.println(base + "\t" + base2count.get(base));  // debug
	}
	if (wf != null) wf.finish();
      } else if (passthrough != null) {
	passthrough.set_value("A", "0");
	passthrough.set_value("C", "0");
	passthrough.set_value("G", "0");
	passthrough.set_value("T", "0");

	HashMap<String,Integer> base2count = counts.get_counts();
	// only populated for observed bases
	for (String base : base2count.keySet()) {
	  passthrough.set_value(base, Integer.toString(base2count.get(base)));
	}
      }
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      System.exit(1);
    }

  }

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);

    int NULL = -1;

    String tname = null;
    int basenum = NULL;
    File bam = null;
    String outfile = "read_report.tab";
    File passthrough_report = null;

    HashMap<String,String> report = new HashMap<String,String>();

    ReadReport pr = new ReadReport();

    for (int i=0; i < argv.length; i++) {
      if (argv[i].equals("-ref")) {
	// reference name
	tname = new String(argv[++i]);
      } else if (argv[i].equals("-base")) {
	// base number
	basenum = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-passthrough")) {
	passthrough_report = new File(argv[++i]);
	//	System.err.println("pr="+passthrough_report);  // debug
      } else if (argv[i].equals("-outfile")) {
	// output
	pr.set_outfile(argv[++i]);
      } else if (argv[i].equals("-bam")) {
	// BAM to scan
	bam = new File(argv[++i]);
      } else if (argv[i].equals("-summary")) {
	pr.set_summary_mode(true);
      } else if (argv[i].equals("-count-optical-duplicates")) {
	pr.count_optical_duplicates(true);
      } else if (argv[i].equals("-min-quality")) {
	// minimum base quality
	pr.set_min_base_quality(Integer.parseInt(argv[++i]));
      } else if (argv[i].equals("-min-mapq")) {
	// minimum mapping quality
	pr.set_min_mapping_quality(Integer.parseInt(argv[++i]));
      } else if (argv[i].equals("-f-reference")) {
	// specify field name for reference name (e.g. chr1)
	pr.set_column_reference(argv[++i]);
      } else if (argv[i].equals("-f-base-num")) {
	// specify field name for base number to check
	pr.set_column_base_num(argv[++i]);
      } else if (argv[i].equals("-demo")) {
	// bam = new File("TCGA-10-0926-01A-01W_mini_tp53.bam");
	bam = new File("tcga-10-0926-11a-01w_mini_tp53.bam");
	tname = "chr17";
	basenum = 7518132;
      } else {
	System.err.println("ERROR: unknown argument " + argv[i]);  // debug
	usage();
      }
    }

    if (bam != null) {
      if (passthrough_report != null) {
	pr.report(bam, tname, passthrough_report);
      } else if (tname != null && basenum != NULL) {
	pr.report(bam, tname, basenum);
      } else {
	usage();
      }
    } else {
      usage();
    }
  }

  private static void usage() {
    System.err.println("usage:");  // debug

    System.err.println("  input data:");  // debug
    System.err.println("    -bam [bamfile]");  // debug
    System.err.println("    -min-quality [value] (default: 15)");  // debug
    System.err.println("    -min-mapq [value] (default: 1)");  // debug
    System.err.println("  target site:");  // debug
    System.err.println("    -ref [reference sequence name (e.g. chr1)]");
    System.err.println("    -base [base number]");
    System.err.println("  output:");  // debug
    System.err.println("    -outfile [output .tab report (default: read_report.tab)]");
    System.err.println("             (specify \"-\" to write to STDOUT)");  // debug

    System.err.println("  misc:");  // debug
    System.err.println("    -summary [only report counts]");
    System.err.println("    -count-optical-duplicates [default: no]");
    System.exit(1);
  }

  // private void populate_info (PhredPolyFileEntry pfe, Reporter rpt, char target_base, String label_root) {
  //   target_base = Character.toUpperCase(target_base);
  //   float amplitude = 0;
  //   String phred_call_type = "?";
  //   if (pfe.called_base == target_base) {
  //     // desired base was phred's primary call
  //     phred_call_type = "called_base";
  //     amplitude = pfe.get_called_base_amplitude();
  //   } else if (pfe.uncalled_base == target_base) {
  //     // desired base was phred's secondary call
  //     phred_call_type = "alternate_base";
  //     amplitude = pfe.get_uncalled_base_amplitude();
  //   } else {
  //     // phred did not call the target base, as either primary or secondary
  //     phred_call_type = "not_called";
  //     if (target_base == 'A') {
  // 	amplitude = pfe.called_base_amp_a;
  //     } else if (target_base == 'C') {
  // 	amplitude = pfe.called_base_amp_c;
  //     } else if (target_base == 'G') {
  // 	amplitude = pfe.called_base_amp_g;
  //     } else if (target_base == 'T') {
  // 	amplitude = pfe.called_base_amp_t;
  //     } else {
  // 	System.err.println("WTF?");  // debug
  // 	System.exit(1);
  //     }
  //   }

  //   String label_base = label_root + "_base";
  //   String label_amp = label_root + "_base_amplitude";
  //   String label_phred_type = label_root + "_base_phred_type";

  //   rpt.set_value(label_base, Character.toString(target_base));
  //   rpt.set_value(label_amp, Float.toString(amplitude));
  //   rpt.set_value(label_phred_type, phred_call_type);
  // }

}

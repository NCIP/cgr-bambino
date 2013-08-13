package Ace2;
// pure BAM header coverage with optional read counting
// MNE 9/2012
//
// to do:
//  - min quality
//  - min mapping quality

import java.util.*;
import java.io.*;
import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;
import java.util.zip.*;

public class SAMCoverage2 {
  public int MIN_QUALITY = 15;
  PrintStream ps = System.out;
  private String name = null;
  private boolean verbose_mode = false;

  boolean WIG_MODE = true;
  boolean WIG_BASES_MODE = false;
  boolean VERBOSE_MODE = false;
  boolean STDOUT_MODE = false;
  boolean SKIP_DUPLICATE_READS = true;
  private File bam_file = null;
  private String outfile = null;

  public void set_outfile (String outfile) {
    this.outfile = outfile;
  }

  public void set_stdout_mode (boolean v) {
    STDOUT_MODE = v;
  }

  public void set_bam_file (File f) {
    bam_file = f;
  }

  public void set_verbose (boolean v) {
    VERBOSE_MODE = v;
  }

  public void set_skip_duplicates (boolean v) {
    SKIP_DUPLICATE_READS = v;
  }

  public void set_wig (boolean v) {
    WIG_MODE = v;
  }

  public void set_wig_bases (boolean v) {
    WIG_BASES_MODE = v;
  }

  public void find_coverage() throws IOException {
    SAMFileReader sfr = new SAMFileReader(bam_file);

    WorkingFile wf = null;
    if (!STDOUT_MODE) {
      if (outfile == null) outfile = bam_file.getName() + (WIG_MODE ? ".wig" : ".coverage");
      System.err.println("outfile="+outfile);  // debug
      wf = new WorkingFile(outfile);
    }


    if (STDOUT_MODE) {
      //	ps = System.out;
      // System.out is line-buffered (SLOW)

      FileOutputStream fdout = new FileOutputStream(FileDescriptor.out);
      BufferedOutputStream bos = new BufferedOutputStream(fdout, 4096);
      ps = new PrintStream(bos, false);
    } else if (WIG_MODE) {
      //      ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(wf)));
      OutputStream os = new BufferedOutputStream(new FileOutputStream(wf));
      if (outfile.indexOf(".gz") == outfile.length() - 3) {
	System.err.println("generating gzipped wig file");  // debug
	os = new GZIPOutputStream(os);
	// note: this is typically quite a bit slower than piping to STDOUT 
	// and using command-line gzip
      }
      ps = new PrintStream(os);
    } else {
      throw new IOException("unknown output format");
    }

    //
    //  find .bam names for each chr and verify sizes (genome version):
    //
    SAMFileHeader h = sfr.getFileHeader();
    SAMSequenceDictionary dict = h.getSequenceDictionary();
    boolean ref_mismatch = false;

    //
    // identify set of reference sequences to process (default=all):
    //
    ArrayList<SAMSequenceRecord> references = new ArrayList<SAMSequenceRecord>();
    // preserve header order
    for (SAMSequenceRecord ssr : dict.getSequences()) {
      references.add(ssr);
      // to do: customize list (e.g. filter out non-chromosome, etc.)
    }

    int null_qual = 0;
    int qual_length_problem = 0;

    for (SAMSequenceRecord ssr : references) {
      //
      //  generate coverage:
      //
      byte[] read,quals;
      int read_i,ref_i,i,end;

      int coverage_len = ssr.getSequenceLength();
      //      System.err.println("allocating " + coverage_len);  // debug

      int[] coverage = new int[coverage_len];

      int[] count_a, count_c, count_g, count_t;
      if (WIG_BASES_MODE) {
	count_a = new int[coverage_len];
	count_c = new int[coverage_len];
	count_g = new int[coverage_len];
	count_t = new int[coverage_len];
      } else {
	count_a = count_c = count_g = count_t = null;
      }

      String ref_name = ssr.getSequenceName();
      boolean has_coverage = false;
      long record_count = 0;

      System.err.println("processing " + ref_name);  // debug
      if (VERBOSE_MODE) System.err.println("query=" + ref_name + " size=" + coverage_len);
      CloseableIterator<SAMRecord> iterator = sfr.queryOverlapping(ref_name, 1, coverage_len);
      SAMRecord sr;
      while (iterator.hasNext()) {
	sr = iterator.next();
	record_count++;
	if (VERBOSE_MODE) {
	  System.err.print(
			   sr.getReadName() + 
			   " unmapped=" + sr.getReadUnmappedFlag() +
			   " dup=" + sr.getDuplicateReadFlag()
			   );
	  if (!sr.getReadUnmappedFlag()) {
	    System.err.print(" pos=" + sr.getAlignmentStart() +
			     "-" + sr.getAlignmentEnd());  // debug
	  }

	  System.err.println("");  // debug
	}

	if (sr.getReadUnmappedFlag()) continue;
	if (SKIP_DUPLICATE_READS && sr.getDuplicateReadFlag()) continue;

	has_coverage = true;

	read = sr.getReadBases();
	quals = sr.getBaseQualities();
	byte base;
	if (quals.length == 0) {
	  if (null_qual++ == 0) System.err.println("ERROR: 0-length qual array for " + sr.getReadName() + " (only warning, counts at end of run)");  // debug
	  // complain only once
	  continue;
	} else if (read.length != quals.length) {
	  if (qual_length_problem++ == 0) System.err.println("ERROR: base/qual lenght mismatch for " + sr.getReadName() + "(" + read.length + " vs " + quals.length + "; only warning, counts at end of run)");  // debug
	  // complain only once
	  continue;
	}

	for (AlignmentBlock ab : sr.getAlignmentBlocks()) {
	  read_i = ab.getReadStart() - 1;
	  ref_i = ab.getReferenceStart() - 1;

	  //	    System.err.println("ref_i="+ref_i + " cov_len=" + coverage.length + " readlen=" + read.length + " qlen=" + quals.length + " read_i=" + read_i + " blen=" + ab.getLength() + " read=" + sr.getReadName() + " alignStart=" + sr.getAlignmentStart() + " cigar=" + sr.getCigar());  // debug

	  for (i = read_i, end = read_i + ab.getLength(); i < end; i++, ref_i++) {
	    if (quals[i] >= MIN_QUALITY && ref_i >= 0 && ref_i < coverage_len) {
	      coverage[ref_i]++;

	      if (WIG_BASES_MODE) {
		if (read[i] == 'a' || read[i] == 'A') {
		  count_a[ref_i]++;
		} else if (read[i] == 'c' || read[i] == 'C') {
		  count_c[ref_i]++;
		} else if (read[i] == 'g' || read[i] == 'G') {
		  count_g[ref_i]++;
		} else if (read[i] == 't' || read[i] == 'T') {
		  count_t[ref_i]++;
		} else {
		  System.err.println("not counting base " + (char) read[i]);
		  // fix me: summary only
		}
	      }

	    }
	  }
	}
      }
      iterator.close();
      if (VERBOSE_MODE) System.err.println("records returned: " + record_count);

      //
      //  write results:
      //
      if (WIG_MODE) {
	// .wig format:
	// http://genome.ucsc.edu/goldenPath/help/wiggle.html
	if (has_coverage) {
	  //	  ps.println("fixedStep chrom=" + ref_name + " start=1 step=1");
	  ps.println("fixedStep chrom=" + Chromosome.standardize_name(ref_name) + " start=1 step=1");
	  // e.g. for chr1 if BAM header uses "1" report as "chr1"
	  if (WIG_BASES_MODE) {
	    // enhanced: also include base counts for A,C,G,T
	    for (i=0; i < coverage_len; i++) {
	      ps.println(Integer.toString(coverage[i]) + "," +
			 Integer.toString(count_a[i]) + "," +
			 Integer.toString(count_c[i]) + "," +
			 Integer.toString(count_g[i]) + "," +
			 Integer.toString(count_t[i])
			 );
	    }	

	  } else {
	    // standard
	    for (i=0; i < coverage_len; i++) {
	      ps.println(Integer.toString(coverage[i]));
	    }	
	  }
	}
      } else {
	throw new IOException("ERROR: unknown output format");  // debug
      }
    }

    if (null_qual > 0) {
      System.err.println("ERROR: " + null_qual + " reads w/0-length base qualities");  // debug
    }

    if (qual_length_problem > 0) {
      System.err.println("ERROR: " + qual_length_problem + " reads w/base/quality array length mismatches");  // debug
    }

    if (ps != null) ps.close();
    if (wf != null) wf.finish();


  }

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    File bam_file = null;
    SAMCoverage2 sc = new SAMCoverage2();

    for (int i=0; i < argv.length; i++) {
      if (argv[i].equals("-bam")) {
	bam_file = new File(argv[++i]);
      } else if (argv[i].equals("-verbose")) {
	sc.set_verbose(true);
      } else if (argv[i].equals("-wig-bases")) {
	sc.set_wig(true);
	sc.set_wig_bases(true);
      } else if (argv[i].equals("-wig")) {
	sc.set_wig(true);
      } else if (argv[i].equals("-of")) {
	sc.set_outfile(new String(argv[++i]));
      } else if (argv[i].equals("-count-duplicates")) {
	sc.set_skip_duplicates(false);
      } else if (argv[i].equals("-stdout")) {
	sc.set_stdout_mode(true);
      } else {
	System.err.println("error: unknown switch " + argv[i]);  // debug
	System.exit(1);
      }
    }

    String error = null;
    if (bam_file == null) {
      error = "specify -bam [file]";
    }

    if (error != null) {
      System.err.println("ERROR: " + error);  // debug
    } else if (bam_file != null) {
      try {
	sc.set_bam_file(bam_file);
	sc.find_coverage();
      } catch (Exception e) {
	System.err.println("ERROR: " + e);  // debug
	e.printStackTrace();
	System.exit(1);
      }
    }
  }



}
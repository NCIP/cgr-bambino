package Ace2;

import java.util.*;
import java.io.*;
import net.sf.samtools.*;

public class SAMHQMismatchReport {
  private File sam_file;
  private SAMFileReader sfr;
  private boolean write_fastq;

  private static int HQ_MISMATCH_EXTRACT_THRESHOLD = 4;

  public SAMHQMismatchReport (File f) {
    sam_file = f;
    sfr = new SAMFileReader(sam_file);
    write_fastq = false;
  }
  
  public void set_write_fastq (boolean fq) {
    write_fastq = fq;
  }

  public void report () {
    SAMMismatchFilter mmf = null;
    String current_reference_name = null;
    String rn;
    byte[] reference_sequence = null;

    int ge_3 = 0;
    int ge_4 = 0;
    int ge_5 = 0;
    int hq;
    int read_count = 0;

    MultiplexedWriter mw_mm = null;
    if (write_fastq) {
      // FIX ME: multiplexed writer here
      // see SAMExtractUnmapped
      mw_mm = new MultiplexedWriter(Funk.Str.basename(sam_file.getPath()) + ".hqmm.", true);
    }

    UniqueReadName urn = new UniqueReadName();
    urn.set_inthash_mode(true, 19);

    try {
      for (SAMRecord sr : sfr) {
	if (++read_count % 250000 == 0) {
	  System.err.println(read_count + "...( >=4: " + ge_4 + ")");  // debug
	}

	if (sr.getReadUnmappedFlag()) continue;
	// skip unmapped

	rn = sr.getReferenceName();

	if (current_reference_name == null || !current_reference_name.equals(rn)) {
	  System.err.println("reference sequence: " + rn);  // debug
	  if (rn.indexOf("_") != -1) {
	    System.err.println("reached nonstandard ref sequence, stopping");  // debug
	    break;
	  } 
	  NIB nib = new NIB(rn);
	  reference_sequence = nib.read_all();
	  mmf = new SAMMismatchFilter(reference_sequence);
	  current_reference_name = rn;
	}

	mmf.filter(sr);
	//	System.err.println(sr.getReadName() + ": " + mmf.get_hq_mismatches());  // debug
	hq = mmf.get_hq_mismatches();
	if (hq >= 3) {
	  ge_3++;
	  if (hq >= 4) ge_4++;
	  if (hq >= 5) ge_5++;

	  if (write_fastq && hq >= HQ_MISMATCH_EXTRACT_THRESHOLD) {
	    String bucket = SAMUtils.bucket_reference_name(rn, true);
	    String info = sr.getReadName() + "." + urn.get_suffix(sr) + 
	      " mapped," + 
	      SAMUtils.bucket_reference_name(sr.getReferenceName(), false) + "," + 
	      sr.getAlignmentStart() + "," + 
	      (sr.getReadNegativeStrandFlag() ? "-" : "+");

	    //	    System.err.println("info="+info);  // debug

	    SAMUtils.write_fastq(mw_mm.getPrintStream(bucket), sr, info);
	    //SAMUtils.write_fastq(System.err, sr, info);
	  }
	}
      }

      mw_mm.finish();

      System.err.println("DONE:");  // debug
      System.err.println("reads: " + read_count);  // debug
      System.err.println("HQ >= 3: " + ge_3);  // debug
      System.err.println("HQ >= 4: " + ge_4);  // debug
      System.err.println("HQ >= 5: " + ge_5);  // debug
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    File f = null;
    boolean write_fastq = false;
    for (int i=0; i < argv.length; i++) {
      if (argv[i].equals("-bam")) {
	f = new File(argv[++i]);
      } else if (argv[i].equals("-fastq")) {
	write_fastq=true;
      } else {
	System.err.println("error: unknown switch " + argv[i]);  // debug
	System.exit(1);
      }
    }

    if (f == null) {
      System.err.println("error: specify -bam [file]");  // debug
    } else {
      SAMHQMismatchReport mmr = new SAMHQMismatchReport(f);
      System.err.println("extracting FASTQ?: " + write_fastq);  // debug
      mmr.set_write_fastq(write_fastq);
      mmr.report();
    }

  }
}

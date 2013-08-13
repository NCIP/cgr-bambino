package Ace2;
// MNE 8/2012

import net.sf.samtools.*;
// import net.sf.samtools.SAMRecord.*;
import java.io.*;
import java.util.*;

public class SAMAdapterTrimmer {

  SAMFileReader sfr = null;
  ReferenceSequence reference = null;
  String adapter = null;
  String adapter_rc = null;
  File output_file = null;
  int minimum_match_length = 1;
  boolean debug_mode = false;
  
  private boolean VERBOSE = false;
  private boolean TRIM_FWD = true;
  private boolean TRIM_REV = true;
  

  public SAMAdapterTrimmer () {
  }

  public void set_bam (File f) {
    sfr = new SAMFileReader(f);
    output_file = new File(f.getAbsolutePath() + ".adapter_trimmed.fastq");
    System.err.println("output="+output_file);  // debug
  }

  public void set_debug_mode (boolean v) {
    debug_mode = v;
  }

  public void set_minimum_match_length (int len) {
    minimum_match_length = len;
  }

  public void set_refseq (ReferenceSequence reference) {
    this.reference = reference;
  }

  public void set_adapter (String adapter) {
    this.adapter = adapter;
    adapter_rc = Funk.Str.reverse_complement(adapter);
  }

  public void get_some() throws FileNotFoundException {
    PrintStream ps = debug_mode ? null : new PrintStream(new BufferedOutputStream(new FileOutputStream(output_file)));

    byte[] ref = null;
    int last_ref_index = -1;

    HashCounter2 trim_counts = new HashCounter2<Integer>();
    int mapped_count = 0;

    try {
      for (SAMRecord sr : sfr) {

	if (sr.getReadUnmappedFlag()) continue;

	mapped_count++;

	//
	//  ensure we have current copy of reference sequence:
	//
	if (sr.getReferenceIndex() != last_ref_index) {
	  String rn = sr.getReferenceName();
	  System.err.println("load new reference: " + rn);  // debug
	  ref = reference.get_all(rn);
	  last_ref_index = sr.getReferenceIndex();
	}

	byte[] bases = sr.getReadBases();
	byte[] mismatch = new byte[bases.length];
	byte[] aligned = new byte[bases.length];
	Arrays.fill(aligned, (byte) 0);
	Arrays.fill(mismatch, (byte) 0);

	// alignment blocks parsing will not cover:
	//   - insertions
	//   - soft clips
	//   - maybe something else I'm forgetting
	//
	// We could manually parse the CIGAR alignment to get these positions.
	// However it might be Good Enough to just parse the alignment blocks
	// and flag all bases NOT covered as mismatches.

	int len, read_i, ref_i, end;
	Base base_read, base_ref;
	System.err.println(sr.getReadName() + " at " + sr.getAlignmentStart());  // debug

	int mismatches = 0;

	for (AlignmentBlock ab : sr.getAlignmentBlocks()) {
	  len = ab.getLength();
	  read_i = ab.getReadStart() - 1;
	  ref_i = ab.getReferenceStart() - 1;
	  
	  for (end = read_i + len; read_i < end && ref_i < ref.length; read_i++, ref_i++) {
	    aligned[read_i] = 1;
	    base_read = Base.valueOf((char) bases[read_i]);
	    base_ref = Base.valueOf((char) ref[ref_i]);
	    if (!base_read.equals(base_ref)) {
	      mismatches++;
	      mismatch[read_i] = 1;
	      //	      System.err.println(base_read + " " + base_ref);  // debug
	    }
	  }
	}

	int mismatches_introduced = 0;
	for (read_i = 0; read_i < bases.length; read_i++) {
	  if (aligned[read_i] == 0) {
	    // indel, soft clip, etc.?
	    mismatch[read_i] = 1;
	    mismatches_introduced++;
	    mismatches++;
	  }
	}
	if (mismatches_introduced > 0) {
	  System.err.println("introduced MMs: " + mismatches_introduced + " CIGAR=" + sr.getCigar());
	}
	
	String info = sr.getReadName();

	int ri;
	if (mismatches > 0) {
	  String rb = new String(bases).toUpperCase();

	  System.err.println(" read="+rb + " strand:" + (sr.getReadNegativeStrandFlag() ? "-" : "+"));  // debug
	  if (sr.getReadNegativeStrandFlag()) {
	    //
	    //  read mapped to - strand
	    // 
	    for (ri = (adapter.length() - 1); ri >= 0; ri--) {
	      if (mismatch[ri] > 0) {
		String chunk = Funk.Str.reverse_complement(rb.substring(0, ri + 1));
		String ac = adapter.substring(0, chunk.length());
		System.err.println("test mm at " + ri + " chunk_rc=" + chunk + " ac="+ac);  // debug
		if (chunk.length() != ac.length()) System.err.println("EPIC FAIL: chunk mismatch");  // debug
		if (chunk.equals(ac) && chunk.length() >= minimum_match_length) {
		  trim_counts.add(chunk.length());
		  System.err.println("trimming - " + ri + " " + chunk + " " + ac + " total_MM:" + mismatches + " len=" + chunk.length() + " CIGAR=" + sr.getCigar());  // debug
		  if (TRIM_REV) {
		    info = info.concat(" adapter_trim " + chunk.length() + " -");
		    trim_adapter_sequence(sr, ri);
		  } else {
		    System.err.println("DEBUG: - trim DISABLED");  // debug
		  }
		  break;
		}
	      }
	    }
	  } else {
	    //
	    //  read mapped to + strand
	    //
	    for (ri = bases.length - adapter.length(); ri < bases.length; ri++) {
	      if (mismatch[ri] > 0) {
		String chunk = rb.substring(ri);
		String ac = adapter.substring(0, chunk.length());
		if (chunk.length() != ac.length()) System.err.println("EPIC FAIL: chunk mismatch");  // debug
		if (chunk.equals(ac) && chunk.length() >= minimum_match_length) {
		  trim_counts.add(chunk.length());
		  System.err.println("trimming + " + ri + " " + chunk + " " + ac + " total_MM:" + mismatches + " len=" + chunk.length() + " CIGAR=" + sr.getCigar());  // debug
		  if (TRIM_FWD) {
		    info = info.concat(" adapter_trim " + chunk.length() + " +");
		    trim_adapter_sequence(sr, ri);
		  } else {
		    System.err.println("DEBUG: + trim DISABLED");  // debug
		  }
		  break;
		}
	      }
	    }
	  }
	}

	if (ps != null) SAMUtils.write_fastq(ps, sr, info);
      }

      //
      //  done
      //
      System.err.println("mapped count: " + mapped_count);  // debug
      System.err.println("adapter-trimmed count: " + trim_counts.get_total());  // debug
      HashMap<Integer,Integer> counts = trim_counts.get_counts();
      for (Integer length : counts.keySet()) {
	System.err.println("  " + length + ": " + counts.get(length));  // debug
      }

      if (ps != null) ps.close();

    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    SAMAdapterTrimmer sat = new SAMAdapterTrimmer();

    for (int i=0; i < argv.length; i++) {
      if (argv[i].equals("-bam")) {
	sat.set_bam(new File(argv[++i]));
      } else if (argv[i].equals("-adapter")) {
	sat.set_adapter((new String(argv[++i])).toUpperCase());
      } else if (argv[i].equals("-min-match-length")) {
	sat.set_minimum_match_length(Integer.parseInt(argv[++i]));
      } else if (argv[i].equals("-debug")) {
	sat.set_debug_mode(true);
      } else if (argv[i].equals("-fasta")) {
	try {
	  String thing = argv[++i];
	  File f = new File(thing);
	  if (f.isFile()) {
	    // .fai-indexed FASTA file
	    sat.set_refseq(new FASTAIndexedFAI(thing));
	  } else {
	    System.err.println("ERROR: not a file: " + thing);  // debug
	  }
	} catch (Exception ex) {
	  System.err.println("WTF");  // debug
	}
      } else {
	System.err.println("ERROR: unknown parameter " + argv[i]);  // debug
	System.exit(1);
      }
    }

    try {
      sat.get_some();
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
    }
  }

  private void trim_adapter_sequence (SAMRecord sr, int ri) {
    byte[] rb_new, qual_new;
    if (sr.getReadNegativeStrandFlag()) {
      //
      // - strand
      // ri is index of last mismatch in trimmed sequence
      //
      rb_new = sub_copy_rev(sr.getReadBases(), ri);
      qual_new = sub_copy_rev(sr.getBaseQualities(), ri);
      if (VERBOSE) {
	System.err.println("before:" + new String(sr.getReadBases()));  // debug
	System.err.println(" after:" + new String(rb_new));  // debug
	System.err.println("before:" + new String(sr.getBaseQualities()));  // debug
	System.err.println(" after:" + new String(qual_new));  // debug
      }
    } else {
      //
      // + strand
      // ri is index of 1st base to trim
      //
      rb_new = sub_copy_fwd(sr.getReadBases(), ri);
      qual_new = sub_copy_fwd(sr.getBaseQualities(), ri);
      if (VERBOSE) {
	System.err.println("before:" + new String(sr.getReadBases()));  // debug
	System.err.println(" after:" + new String(rb_new));  // debug
	System.err.println("before:" + new String(sr.getBaseQualities()));  // debug
	System.err.println(" after:" + new String(qual_new));  // debug
      }
    }
    sr.setReadBases(rb_new);
    sr.setBaseQualities(qual_new);
  }

  private byte[] sub_copy_fwd (byte[] orig, int ri) {
    byte[] n = new byte[ri];
    System.arraycopy(orig, 0, n, 0, ri);
    return n;
  }

  private byte[] sub_copy_rev (byte[] orig, int ri) {
    int start = ri + 1;
    int block_len = orig.length - start;
    byte[] n = new byte[block_len];
    if (n.length != block_len) System.err.println("WTF");
    System.arraycopy(orig, start, n, 0, block_len);
    return n;
  }

}

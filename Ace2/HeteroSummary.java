package Ace2;

import java.util.*;
import net.sf.samtools.*;

public class HeteroSummary extends Observable implements Runnable {
  AceViewerConfig config;
  private boolean is_tn_trackable = false;
  private byte[] global_nonreference_frequency, normal_nonreference_frequency, tumor_nonreference_frequency;
  private short[] coverage;
  private int max_coverage;
  private boolean is_loaded = false;
  private boolean is_all_tumor = false;
  private boolean is_all_normal = false;

  private boolean COVERAGE_LOG2 = false;
  private boolean COVERAGE_LOG1_5 = false;
  private boolean COVERAGE_LOG1_25 = false;

  public HeteroSummary (AceViewerConfig config, boolean async) {
    this.config = config;
    is_loaded = false;
    if (async) {
      new Thread(this).start();
    } else {
      run();
    }
  }

  public void run() {
    char[] reference_seq = new String(config.target_sequence).toLowerCase().toCharArray();
    int start_offset = config.ruler_start;

    is_tn_trackable = config.sams.size() > 1;

    is_all_tumor = is_all_normal = true;
    for (SAMResource sr : config.sams) {
      SAMConsensusMapping[] maps = sr.get_sequences();
      Sample sample = sr.get_sample();
      if (sample.is_tumor()) {
	is_all_normal = false;
      } else if (sample.is_normal()) {
	is_all_tumor = false;
      } else {
	is_tn_trackable = is_all_tumor = is_all_normal = false;
      }
      //      System.err.println("tn=" + sample.is_tumor());  // debug
      //      System.err.println("count="+maps.length);  // debug
    }

    //    System.err.println("rlen="+reference_seq.length);  // debug

    int[] global_reference_count = new int[reference_seq.length];
    int[] global_nonreference_count = new int[reference_seq.length];

    int[] normal_reference_count = new int[reference_seq.length];
    int[] normal_nonreference_count = new int[reference_seq.length];

    int[] tumor_reference_count = new int[reference_seq.length];
    int[] tumor_nonreference_count = new int[reference_seq.length];
    
    Arrays.fill(global_reference_count, 0);
    Arrays.fill(global_nonreference_count, 0);
    Arrays.fill(normal_reference_count, 0);
    Arrays.fill(normal_nonreference_count, 0);
    Arrays.fill(tumor_reference_count, 0);
    Arrays.fill(tumor_nonreference_count, 0);

    global_nonreference_frequency = new byte[reference_seq.length];
    normal_nonreference_frequency = new byte[reference_seq.length];
    tumor_nonreference_frequency = new byte[reference_seq.length];
    coverage = new short[reference_seq.length];

    int i,end;
    SAMRecord sr;
    byte[] read,quals;
    char base, ref_base;

    boolean VERBOSE = false;

    for (SAMResource sres : config.sams) {
      SAMConsensusMapping[] maps = sres.get_sequences();
      Sample sample = sres.get_sample();

      int read_i, ref_i;
      int[] counter_ref, counter_nonref;

      if (is_tn_trackable) {
	if (sample.is_tumor()) {
	  counter_ref = tumor_reference_count;
	  counter_nonref = tumor_nonreference_count;
	} else {
	  counter_ref = normal_reference_count;
	  counter_nonref = normal_nonreference_count;
	}
      } else {
	counter_ref = counter_nonref = null;
      }

      for (i=0; i < maps.length; i++) {
	sr = maps[i].sr;
	if (sr.getDuplicateReadFlag()) continue;
	read = sr.getReadBases();
	quals = sr.getBaseQualities();

	if (VERBOSE) System.err.println("read " + sr.getReadName());  // debug

	for (AlignmentBlock ab : sr.getAlignmentBlocks()) {
	  read_i = ab.getReadStart() - 1;
	  ref_i = ab.getReferenceStart() - start_offset - 1;

	  if (false) {
	    // debug
	    VERBOSE = ab.getReferenceStart() >= 30946406 &&
	      ab.getReferenceStart() <= 30946430;
	    if (VERBOSE) {
	      System.err.println("read " + sr.getReadName());  // debug
	    }
	  }

	  if (VERBOSE) System.err.println("  start_ref " + (ref_i + start_offset + 1));  // debug

	  if (config.intron_compressor != null) {
	    //	    ref_i -= config.intron_compressor.get_start_shift(ref_i + 1, true);
	    ref_i -= config.intron_compressor.get_start_shift(ref_i + 1, true);
	    // +1 ???
	    if (VERBOSE) System.err.println("  adjusted="+(ref_i+start_offset+1));  // debug

	  }

	  for (end = read_i + ab.getLength(); read_i < end; read_i++, ref_i++) {
	    if (ref_i >= 0 && ref_i < reference_seq.length) {
	      if (quals[read_i] >= config.snp_config.MIN_QUALITY) {
		// 
		//  base is of sufficient quality
		//
		coverage[ref_i]++;
		base = Character.toLowerCase((char) read[read_i]);
		//		System.err.println("  pos=" + (ref_i + start_offset + 1) +  " base=" +base + " ref=" + reference_seq[ref_i]);  // debug

		if (base == reference_seq[ref_i]) {
		  //
		  // base matches reference sequence
		  //
		  global_reference_count[ref_i]++;
		  if (is_tn_trackable) counter_ref[ref_i]++;
		} else {
		  if (base != 'n') {
		    global_nonreference_count[ref_i]++;
		    if (is_tn_trackable) counter_nonref[ref_i]++;
		  }
		}
		//		coverage[ref_i]++;
	      }
	    }
	  }
	}
      }
    }

    if (COVERAGE_LOG2) {
      System.err.println("generating log2 coverage");  // debug
      for (i=0; i < reference_seq.length; i++) {
	coverage[i] = (short) (Math.log((double) coverage[i]) / Math.log(2d));
      }
    } else if (COVERAGE_LOG1_5) {
      System.err.println("generating log1.5 coverage");  // debug
      for (i=0; i < reference_seq.length; i++) {
	coverage[i] = (short) (Math.log((double) coverage[i]) / Math.log(1.5d));
      }
    } else if (COVERAGE_LOG1_25) {
      System.err.println("generating log1.25 coverage");  // debug
      for (i=0; i < reference_seq.length; i++) {
	coverage[i] = (short) (Math.log((double) coverage[i]) / Math.log(1.25d));
      }
    }

    max_coverage = 0;
    for (i=0; i < reference_seq.length; i++) {
      if (coverage[i] > max_coverage) max_coverage = coverage[i];
    }

    build_frequencies(global_nonreference_frequency, global_reference_count, global_nonreference_count, null, null);
    build_frequencies(normal_nonreference_frequency, normal_reference_count, normal_nonreference_count, tumor_reference_count, tumor_nonreference_count);
    build_frequencies(tumor_nonreference_frequency, tumor_reference_count, tumor_nonreference_count, normal_reference_count, tumor_reference_count);

//     for (i=0; i < reference_seq.length; i++) {
//       int total = global_reference_count[i] + global_nonreference_count[i];
//       if (total > 0) {
// 	global_nonreference_frequency[i] = (byte) (global_nonreference_count[i] * 100 / total);
//       }
//       //      System.err.println("freq=" + global_nonreference_frequency[i]);  // debug
//     }

    is_loaded = true;
    setChanged();
    notifyObservers();
  }

  private void build_frequencies (byte[] freq, int[] ref, int[] non_ref, int[] other_ref, int[] other_non_ref) {
    int len = freq.length;
    int total, coverage;
    int half_required_coverage = config.snp_config.MIN_COVERAGE;
    for (int i=0; i < len; i++) {
      total = ref[i] + non_ref[i];
      if (other_ref == null) {
	// no other information required to calculate full coverage
	coverage = total;
      } else {
	coverage = total + other_ref[i] + other_non_ref[i];
	// to calculate full coverage, need to include counts for other set
      }

      if (total > 0 &&
	  total >= half_required_coverage && 
	  coverage >= config.snp_config.MIN_COVERAGE) {
	freq[i] = (byte) (non_ref[i] * 100 / total);
      }
      //      System.err.println("freq=" + global_nonreference_frequency[i]);  // debug
    }
    
  }

  public byte[] get_global_nonreference_frequency() {
    return global_nonreference_frequency;
  }

  public byte[] get_normal_nonreference_frequency() {
    return normal_nonreference_frequency;
  }

  public byte[] get_tumor_nonreference_frequency() {
    return tumor_nonreference_frequency;
  }

  public short[] get_coverage() {
    return coverage;
  }

  public int get_max_coverage() {
    return max_coverage;
  }

  public String get_tooltip_text(int unpadded) {
    unpadded--;
    // base number -> index
    
    String result = "";
    if (unpadded >= 0) {
      if (is_tn_trackable) {
	if (normal_nonreference_frequency != null) {
	  result = "nonreference allele frequencies: normal=" + normal_nonreference_frequency[unpadded] + "%, tumor=" + tumor_nonreference_frequency[unpadded] + "%";
	}
      } else if (global_nonreference_frequency != null) {
	result = "nonreference allele frequency=" + global_nonreference_frequency[unpadded] + "%";
      }
    }
    return result;
  }

  public boolean is_tumor_normal_trackable () {
    return is_tn_trackable;
  }

  public boolean is_all_normal() {
    return is_all_normal;
  }

  public boolean is_all_tumor() {
    return is_all_tumor;
  }

  public boolean is_loaded() {
    return is_loaded;
  }
  
  public void reset(boolean async) {
    is_loaded = false;
    if (async) {
      new Thread(this).start();
    } else {
      run();
    }
  }

}
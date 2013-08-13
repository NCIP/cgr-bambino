package Ace2;

import java.util.*;

public class IntronCompressor {
  //
  // find intronic regions to trim from an assembly
  //
  AceViewerConfig config;
  Assembly assembly;
  ArrayList<Range> ranges_to_trim;
  // PADDED coordinates
  PadMap pm_raw;

  public static int KEEP_NT = 12;
  static int MIN_SPAN_TO_TRIM = KEEP_NT * 2;

  private static final int V_NULL = 0;
  private static final int V_USABLE = 1;
  private static final int V_UNUSABLE = -1;

  public IntronCompressor (AceViewerConfig config, Assembly assembly) {
    this.config = config;
    this.assembly = assembly;
    setup();
  }

  private void setup() {
    identify_trim_regions();
    trim_assembly_reads();
    trim_reference_sequence();
  }

  private void identify_trim_regions() {
    int left = assembly.get_leftmost();
    int right = assembly.get_rightmost();
    //    System.err.println("range="+left + " " +right);  // debug
    int[] tracker = new int[right + 1];
    Arrays.fill(tracker, V_NULL);

    //
    // find which bases in the assembly consist exclusively of
    // spanning regions (i.e. SAM N tag):
    //
    char c;
    int ci,i;
    for (AssemblySequence as : assembly.get_sequences()) {
      SAMConsensusMapping scm = (SAMConsensusMapping) as;
      int start = scm.get_asm_start();
      int end = scm.get_asm_end();
      byte[] buf = scm.get_sequence_buffer();
      for (ci = start, i = 0;
	   i < buf.length && ci < tracker.length;
	   ci++, i++) {
	if (buf[i] == Constants.ALIGNMENT_SKIPPED_CHAR_F ||
	    buf[i] == Constants.ALIGNMENT_SKIPPED_CHAR_R) {
	  if (tracker[ci] == V_NULL) tracker[ci] = V_USABLE;
	} else {
	  tracker[ci] = V_UNUSABLE;
	}
      }
    }

    //
    // bucket identified bases into regions:
    //
    ArrayList<Range> ranges = new ArrayList<Range>();
    Range current = null;
    for (ci=0; ci <= right; ci++) {
      if (tracker[ci] == V_USABLE) {
	if (current == null) {
	  current = new Range();
	  ranges.add(current);
	  current.start = ci;
	}
	current.end = ci;
      } else {
	current = null;
      }
    }

    //
    // identify regions w/required minimum sizes and adjust
    // to preserve a minimum amount of span:
    //
    ranges_to_trim = new ArrayList<Range>();
    int half_keep = KEEP_NT / 2;
    for (Range r : ranges) {
      //      System.err.println("raw range: " +
      //			 r.start + "-" +  r.end + " " +
      //			 (config.ruler_start + r.start) + "-" +  (config.ruler_start + r.end));
      if (r.size() >= MIN_SPAN_TO_TRIM) {
	Range r2 = new Range();
	r2.start = r.start + half_keep;
	r2.end = r.end - half_keep;
	//	System.err.println("cooked range: " + r2);  // debug
	ranges_to_trim.add(r2);
      }
    }
  }

  public boolean is_completely_trimmed (int start, int end, boolean padded) {
    if (!padded) {
      // query is in unpadded space, convert to padded
      PadMap pm = config.assembly.get_padmap();
      start = pm.in_unpadded_range(start) ? pm.get_unpadded_to_padded(start) : start;
      end = pm.in_unpadded_range(end) ? pm.get_unpadded_to_padded(end) : end;
    }
    boolean result = false;
    for (Range r : ranges_to_trim) {
      if (start >= r.start && end <= r.end) {
	result = true;
	break;
      }
    }
    return result;
  }

  public int get_trimmed_to_untrimmed_shift (int pos) {
    // convert a position in the trimmed sequence to a position
    // in the untrimmed sequence.
    boolean VERBOSE = false;

    int shift = 0;

    if (VERBOSE) System.err.println("start: " + pos);  // debug
    for (Range r : ranges_to_trim) {
      if (VERBOSE) System.err.println("  pos=" + pos + " r="+r);  // debug
      if (r.start > pos) {
	if (VERBOSE) System.err.println("  stop");  // debug
	break;
      } else if (r.end < pos) {
	if (VERBOSE) System.err.println("  add before");  // debug
	shift += r.size();
	pos += r.size();
      } else if (pos >= r.start && pos <= r.end) {
	if (VERBOSE) System.err.println("  add inside");  // debug
	shift += r.size();
	pos += r.size();
      }
    }
    if (VERBOSE) System.err.println("shift="+shift);
    return shift;
  }

  public int get_start_shift(int start, boolean include_interior) {
    //
    //  compensate for trimming that occurred before specified start pos
    //
    int shift_needed = 0;
    for (Range r : ranges_to_trim) {
      if (r.end < start) {
	// trimming occurred before this alignment
	shift_needed += r.size();
      } else if (include_interior && start >= r.start && start <= r.end) {
	shift_needed += r.size();
      }
    }
    return shift_needed;
  }

  private void trim_assembly_reads() {
    //
    //  trim aligned reads in assembly based on identified regions
    //
    for (AssemblySequence as : assembly.get_sequences()) {
      SAMConsensusMapping scm = (SAMConsensusMapping) as;

      //
      //  check for trimming within this alignment:
      //
      RangeMasker rm = new RangeMasker(ranges_to_trim, scm.get_sequence_buffer());
      //      rm.set_offset(- a_start);
      rm.set_offset(- as.get_asm_start());

      //      System.err.println("checking " + scm.get_name() + " start=" + as.get_asm_start() + " end="+as.get_asm_end());  // debug

      //      rm.mask(a_start, as.get_asm_end());
      rm.mask(as.get_asm_start(), as.get_asm_end());
      byte[] trimmed = rm.get_trimmed_sequence();

      if (trimmed != null) {
	//
	//  this sequence had some trimming applied
	//
	//	System.err.println("trimmed " + scm.get_name());  // debug

	scm.set_sequence_buffer(trimmed);
	// apply changes to bases

	scm.set_quality_buffer(rm.trim_byte_array(scm.get_quality_buffer()));
	// apply changes to quality array

	scm.apply_end_shift(- rm.get_masked_base_count());
	// apply changes to alignment and clipping endpoints
	// (align start positions aren't affected)
      } else {
	//	System.err.println("no trim for " + scm.get_name());  // debug
      }

      //
      //  compensate for trimming before this alignment started.
      //  Do this LAST because it modifies alignment coordinates,
      //  which interferes with masking above.
      //
      int shift_needed = get_start_shift(as.get_asm_start(), false);
      if (shift_needed > 0) scm.apply_alignment_shift(- shift_needed);


    }
  }

  public void trim_reference_sequence() {
    //
    //  trim the padded reference sequence
    //
    char[] cons = assembly.get_consensus_sequence();
    char[] masked = cons.clone();
    int offset = -1;
    // 1-based to 0-based (I think)
    //int offset = 0;
    //    System.err.println("offset="+offset);  // debug

    int masked_bases = 0;
    for (Range r : ranges_to_trim) {
      int start = r.start + offset;
      int end = r.end + offset;
      //      System.err.println("mask " + (config.ruler_start + r.start) + 
      //			 "-" + 
      //			 (config.ruler_start + r.end + "  ruler=" + config.ruler_start)
      //			 );  // debug

      for (int i = start; i <= end; i++) {
	//	cons[i] = '!';
	masked[i] = RangeMasker.TRIM_CHAR;
	masked_bases++;
      }
    }
    //    System.err.println("masked: " + new String(masked));

    char[] trimmed = new char[masked.length - masked_bases];
    int ti = 0;
    int trimmed_bases = 0;
    config.intron_trim_sites = new HashMap<Integer,Integer>();
    for (int i = 0; i < masked.length; i++) {
      if (masked[i] == RangeMasker.TRIM_CHAR) {
	trimmed_bases++;
      } else {
	if (trimmed_bases > 0) {
	  config.intron_trim_sites.put(ti + 1, trimmed_bases);
	  // store as 1-based positions (padded)
	  //	  System.err.println("store trim at " + (ti + 1) + " of " + trimmed_bases);  // debug
	}

	trimmed[ti++] = masked[i];
	trimmed_bases = 0;
      }
    }
    
    pm_raw = assembly.get_padmap();
    // preserve original PadMap (which contains a copy of raw reference sequence).
    // This is required for proper RefGene setup (in case our intron
    // compression deletes canonical exon(s), all of which are required to
    // correctly perform protein translation).
    
    SAMAssembly sasm = (SAMAssembly) assembly;
    // avert your eyes
    sasm.set_consensus_sequence(trimmed);

    //
    //  generate unpadded raw sequence:
    //
    StringBuffer sb = new StringBuffer();
    char c;
    int i;
    for (i = 0; i < trimmed.length; i++) {
      c = (char) trimmed[i];
      if (c != Constants.ALIGNMENT_GAP_CHAR) sb.append(c);
    }
    char[] unpadded_c = sb.toString().toCharArray();
    byte[] unpadded_b = new byte[unpadded_c.length];
    for (i = 0; i < unpadded_c.length; i++) {
      unpadded_b[i] = (byte) unpadded_c[i];
    }
    config.target_sequence = unpadded_b;

    //    System.err.println("trimmed="+new String(trimmed));  // debug
    //    System.err.println(" target=" + new String(config.target_sequence));  // debug

    //    System.err.println("FIX ME: PadMap reset??");  // debug
    sasm.reset_padmap();
  }

  public PadMap get_raw_padmap () {
    return pm_raw;
  }

}

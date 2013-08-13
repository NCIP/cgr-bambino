package Ace2;

import java.util.*;
import net.sf.samtools.*;

public class SAMMismatchFilter {
  private int mismatches,hq_mismatches;

  private byte[] reference_sequence;
  private int start_offset;

  public static byte MASK_CHAR = '!';
  private ArrayList<Integer> hq_mismatch_sites;
  private static final boolean VERBOSE = false;

  private HashMap<Integer,EnumMap<Base,Integer>> hq_mismatch_bases;
  // 0-based (string index), NOT base number

  private SNPConfig config = null;

  public SAMMismatchFilter () {
    setup();
  }

  public SAMMismatchFilter (byte[] reference_sequence) {
    this.reference_sequence = reference_sequence;
    start_offset = 0;
    setup();
  }

  public SAMMismatchFilter (SNPConfig config, byte[] reference_sequence, int start_offset) {
    this.config = config;
    this.reference_sequence = reference_sequence;
    this.start_offset = start_offset;
    setup();
  }

  public SNPConfig get_config() {
    return config;
  }
  
  private void setup() {
    if (config == null) config = new SNPConfig();
    if (config.ENABLE_MISMAP_FILTER) {
      hq_mismatch_bases = new HashMap<Integer,EnumMap<Base,Integer>>();
    }
  }

  public EnumMap<Base,Integer> get_hq_mismatches (int ref_base_num) {
    // complete reference base number
    // - 1-based
    // - not altered for start position
    return hq_mismatch_bases.get((ref_base_num - start_offset - 1));
  }

  public void set_reference_sequence (byte[] reference_sequence) {
    this.reference_sequence = reference_sequence;
  }

  public boolean filter (SAMRecord sr) {
    // returns true if read OK, false if it fails any filters

    //    Cigar c = sr.getCigar();

    byte[] read = sr.getReadBases();

    byte[] read_masked = config.ENABLE_POLY_X_RUN_MASK_SNP ? generate_masked_sequence(read, config) : null;

    //    System.err.println(sr.getReadName() + " cigar="+SAMUtils.cigar_to_string(sr.getCigar()) + " seq=" + sr.getReadString() + " masked=" + new String(read_masked));

    byte[] quals = sr.getBaseQualities();
    int len, read_i, ref_i, end;
    mismatches=0;
    hq_mismatches=0;

    hq_mismatch_sites = new ArrayList<Integer>();

    HashMap<Integer,EnumMap<Base,Integer>> hqmm_temp = null;
    if (config.ENABLE_MISMAP_FILTER) {
      hqmm_temp = new HashMap<Integer,EnumMap<Base,Integer>>();
    }

    for (AlignmentBlock ab : sr.getAlignmentBlocks()) {
      len = ab.getLength();
      read_i = ab.getReadStart() - 1;
      ref_i = ab.getReferenceStart() - start_offset - 1;

      if (read_i < 0) {
	System.err.println("ERROR: can't filter " + sr.getReadName() + ", read_i < 0");
      } else if (ref_i < 0) {
	System.err.println("ERROR: can't filter " + sr.getReadName() + ", ref_i < 0");
      } else {
	//      System.err.println(" ref: " + new String(reference_sequence, ref_i, len));  // debug
	//      System.err.println("read: " + new String(read, read_i, len));  // debug

	end = read_i + len;
	if (end >= reference_sequence.length) {
	  System.err.println("trimming end to refseq length");  // debug
	  end = reference_sequence.length;
	}

	if (ref_i >= reference_sequence.length) {
	  System.err.println("WTF? refseq bounds error for " + sr.getReadName() + " " + ref_i + " " + reference_sequence.length);  // debug
	  continue;
	}

	int ref_char;

	for (; read_i < end; read_i++, ref_i++) {
	  if (read_i >= read.length) {
	    System.err.println("WTF, read index out of bounds");  // debug
	    break;
	  }
	  if (ref_i >= reference_sequence.length) {
	    System.err.println("WTF, refseq index out of bounds");  // debug
	    break;
	  }

	  ref_char = Character.toUpperCase(reference_sequence[ref_i]);
	  if (ref_char != read[read_i] && ref_char != 'N') {
	    //
	    // mismatch
	    //
	    //	    System.err.println("mismatch: ref=" + (char) reference_sequence[ref_i] + " read=" + (char) read[read_i]);  // debug


	    if (config.MISMATCH_FILTER_FORGIVE_DBSNP_MATCHES && config.snp_query != null) {
	      //
	      // if this mismatch hits a dbSNP entry using the same base,
	      // don't consider a "strike" against disqualifying the read.
	      //
	      if (config.snp_query.snp_matches(ref_i + 1,
					       Base.valueOf((char) read[read_i]),
					       Base.valueOf((char) reference_sequence[ref_i])
					       )
		  ) {
		//		System.err.println("ref seq mismatch of " + (char) read[read_i] + " hit SNP at " + (ref_i + 1) + " for " + SAMUtils.get_printable_read_name(sr));
		continue;
	      }
	    }

	    if (read_i >= quals.length) {
	      if (VERBOSE) System.err.println("SMF: no qual info available!");  // debug
	      continue;
	    }
	    if (quals[read_i] >= config.MISMATCH_FILTER_MIN_LOW_QUALITY) mismatches++;
	    if (quals[read_i] >= config.MISMATCH_FILTER_MIN_HIGH_QUALITY) {
	      //	      System.err.println(sr.getReadName() + " hq mismatch: " + ((char) read[read_i]) + "/" + quals[read_i]);  // debug

	      if (config.ENABLE_MISMAP_FILTER) {
		EnumMap<Base,Integer> bases = hqmm_temp.get(ref_i);
		if (bases == null) 
		  hqmm_temp.put(ref_i, bases = new EnumMap<Base,Integer>(Base.class));
		Base b = Base.valueOf((char) read[read_i]);
		Integer count = bases.get(b);
		if (count == null) count = Integer.valueOf(0);
		//		System.err.println("saving mismap for " + b + " at ref_i " + ref_i + " full=" + (ref_i + start_offset + 1) + " seq=" + sr.getReadName() + (sr.getReadNegativeStrandFlag() ? ".R" : ".F") + " dup=" + sr.getDuplicateReadFlag());  // debug
		bases.put(b, count + 1);
	      }

	      if (config.ENABLE_POLY_X_RUN_MASK_SNP && read_masked[read_i] == MASK_CHAR) continue;
	      // only consider mismatches high-quality if not in poly-X region

	      hq_mismatches++;
	      hq_mismatch_sites.add(read_i);

	    }

	  }
	}
      }
    }

    //    System.err.println("read " + SAMUtils.get_printable_read_name(sr) + " mm:" + mismatches + " hqmm:" + hq_mismatches);  // debug
    
    boolean passed;
    if (config.ENABLE_MISMATCH_FILTER == false ||
	(mismatches <= config.MISMATCH_FILTER_MAX_LQ_MISMATCH_COUNT &&
	 hq_mismatches <= config.MISMATCH_FILTER_MAX_HQ_MISMATCH_COUNT)) {
      passed = true;
    } else {
      passed = false;
      if (config.ENABLE_MISMAP_FILTER) {
	// sequence didn't pass mismatch filter.
	// copy observed high-quality mismatch observations into tracker.
	//	System.err.println("filter failed for " + sr.getReadName() + (sr.getReadNegativeStrandFlag() ? ".R" : ".F"));
	for (Integer position : hqmm_temp.keySet()) {
	  EnumMap<Base,Integer> bucket = hqmm_temp.get(position);

	  EnumMap<Base,Integer> tracker_bucket = hq_mismatch_bases.get(position);
	  if (tracker_bucket == null) {
	    hq_mismatch_bases.put(position, tracker_bucket = new EnumMap<Base,Integer>(Base.class));
	  }
	  
	  for (Base b : bucket.keySet()) {
	    Integer count = bucket.get(b);
	    //	    System.err.println("  saving mismap at " + (position + start_offset + 1));  // debug
	    Integer tracker_count = tracker_bucket.get(b);
	    if (tracker_count == null) tracker_count = Integer.valueOf(0);
	    tracker_bucket.put(b, tracker_count + count);
	  }
	}

      }
    }
    return passed;
  }

  public int get_hq_mismatches() {
    return hq_mismatches;
  }

  public int get_hq_mismatches_window (int window_size) {
    ArrayList<Integer> window = new ArrayList<Integer>();

    //    System.err.println("starting scan");  // debug
    int best_size = 0;
    int size,span;
    for (Integer v : hq_mismatch_sites) {
      window.add(v);
      while (true) {
	size = window.size();
	span = window.get(size - 1) - window.get(0);
	//	System.err.println("window = " + window + " size=" + size + " span=" + span);
	if (span > window_size) {
	  //	  System.err.println("pop");  // debug
	  window.remove(0);
	  // pop
	} else {
	  // window ok
	  if (size > best_size) {
	    best_size = size;
	    //	    System.err.println("new best size: " + best_size);  // debug
	  }
	  break;
	  // window within tolerances, continue
	}
      }
    }
    //    System.err.println("raw: " + hq_mismatches + " window_best:" + best_size);  // debug

    return best_size;
  }


  private static void mask_sequence (byte[] array, int start_pos, int len) {
    int end = start_pos + len;
    for (int i=start_pos; i < end; i++) {
      array[i] = MASK_CHAR;
    }
  }

  public static byte[] generate_masked_sequence(byte[] read, SNPConfig config) {
    byte[] read_masked = new byte[read.length];
    System.arraycopy(read, 0, read_masked, 0, read.length);
    int run_length = 0;
    // trim poly-X sequences
    int i;
    for (i = 1; i < read.length; i++) {
      if (Character.toLowerCase(read[i]) == Character.toLowerCase(read[i - 1])) {
	run_length++;
      } else {
	if (run_length >= config.POLY_X_MIN_RUN_LENGTH) {
	  //	    System.err.println("before: " + new String(read));  // debug
	  mask_sequence(read_masked, i - run_length - 1, run_length + 1);
	  //	    System.err.println(" after: " + new String(read_masked));  // debug
	}
	run_length = 0;
      }
    }
    
    if (run_length >= config.POLY_X_MIN_RUN_LENGTH) {
      // trim at end boundary
      //	System.err.println("before: " + new String(read));  // debug
      mask_sequence(read_masked, i - run_length - 1, run_length + 1);
      //	System.err.println(" after: " + new String(read_masked));  // debug
    }
    return read_masked;
  }

  public static void main (String[] argv) {
    ArrayList<Integer> al_sites = new ArrayList<Integer>();
    al_sites.add(5);
    al_sites.add(11);
    al_sites.add(25);
    al_sites.add(28);
    al_sites.add(29);
    al_sites.add(31);

    int[] sites = new int[al_sites.size()];
    int i=0;

    int WINDOW_SIZE = 10;

    ArrayList<Integer> window = new ArrayList<Integer>();

    int best_size = 0;
    for (Integer v : al_sites) {
      window.add(v);
      while (true) {
	int size = window.size();
	int span = window.get(size - 1) - window.get(0);
	System.err.println("window = " + window + " size=" + size + " span=" + span);
	if (span > WINDOW_SIZE) {
	  System.err.println("pop");  // debug
	  window.remove(0);
	  // pop
	} else {
	  // window ok
	  if (size > best_size) {
	    best_size = size;
	    System.err.println("new best size: " + best_size);  // debug
	  }
	  break;
	}

      }

    }
  }

  public void clean_hq_mismatch_tracker_through (int ref_base_num) {
    //
    // SNP caller is finished with data up to and including specified base;
    // remove unneeded tracker entries.
    //
    int max_i = ref_base_num - start_offset - 1;

    ArrayList<Integer> flush = new ArrayList<Integer>();

    for (Integer pos : hq_mismatch_bases.keySet()) {
      if (pos < max_i) flush.add(pos);
      // <= rather than < likely ok
    }

    //    System.err.println("cleaner: keys=" + hq_mismatch_bases.keySet().size() + " flushed=" + flush.size());  // debug

    for (Integer pos : flush) {
      hq_mismatch_bases.remove(pos);
    }
  }

  public int get_start_clipped_hq_mismatches (SAMRecord sr) {
    // get count of high-quality matches in the beginning of the read.
    // "beginning" in this context means the start of the raw read,
    // rather than the alignment.
    // 
    // So, for reads aligned to the + strand this returns mismatches
    // in the alignment start, for - strand reads it returns mismatches to the 
    // alignment end.
    // 
    // Since read quality tends to be better at the beginning, high-quality
    // mismatches in the starts of reads might be better indicators of
    // mismapped reads and possible indels.
    // 
    if (sr.getReadNegativeStrandFlag()) {
      return get_alignment_end_clipped_hq_mismatches(sr);
    } else {
      return get_alignment_start_clipped_hq_mismatches(sr);
    }
  }

  public int get_end_clipped_hq_mismatches (SAMRecord sr) {
    // get count of high-quality matches in the end of the read.
    // "end" in this context means the start of the raw read,
    // rather than the alignment.
    // 
    // So, for reads aligned to the + strand this returns mismatches
    // in the alignment end, for - strand reads it returns mismatches to the 
    // alignment start.
    // 
    // Since read quality tends to be better at the beginning, high-quality
    // mismatches in the starts of reads might be better indicators of
    // mismapped reads and possible indels.
    // 
    if (sr.getReadNegativeStrandFlag()) {
      return get_alignment_start_clipped_hq_mismatches(sr);
    } else {
      return get_alignment_end_clipped_hq_mismatches(sr);
    }
  }

  private int get_alignment_end_clipped_hq_mismatches (SAMRecord sr) {
    //
    // hq mismatches in clipped alignment end (regardless of strand)
    // 
    boolean verbose = false;
    if (verbose) System.err.println("get_alignment_end_clipped_hq_mismatches():");  // debug
    int ae = sr.getAlignmentEnd();
    int aeu = sr.getUnclippedEnd();
    CigarOperator co;
    int results = 0;
    if (ae != aeu) {
      // clipping present at end of read
      Cigar c = sr.getCigar();
      boolean ok = false;
      int len=-1;
      for (CigarElement ce : c.getCigarElements()) {
	co = ce.getOperator();
	ok = co.equals(CigarOperator.SOFT_CLIP);
	// last operator
	len = ce.getLength();
      }
      if (ok) {
	if (verbose) {
	  System.err.println("trailing soft clipping for " + SAMUtils.get_printable_read_name(sr));  // debug
	  System.err.println("len="+len);  // debug
	}

	byte[] read = sr.getReadBases();
	byte[] quals = sr.getBaseQualities();

	int read_i = read.length - len;
	int ref_i = ae - start_offset;
	// +1  ???

	if (verbose) System.err.println("full read: " + new String(read));

	for (int i = 0; i < len; i++, read_i++, ref_i++) {
	  if (ref_i < 0) continue;
	  if (ref_i >= reference_sequence.length) break;
	  // read aligned past end of reference sequence, stop
	  if (verbose) System.err.println("ref:" + (ref_i + 1) + "=" + (char) reference_sequence[ref_i] + " read:" + (char) read[read_i] + " qual:" + quals[read_i]);
	  if (Character.toUpperCase((char) reference_sequence[ref_i]) != read[read_i] &&
	      quals[read_i] >= config.MISMATCH_FILTER_MIN_HIGH_QUALITY) {
	    if (verbose) System.err.println("  HQ mismatch");  // debug
	    results++;
	  }
	}

      } else {
	System.err.println("ERROR getting clip mismatches for " + SAMUtils.get_printable_read_name(sr));  // debug
	// hard clipping?
      }
    }
    if (verbose) System.err.println("total="+results);  // debug
    return results;
  }

  private int get_alignment_start_clipped_hq_mismatches (SAMRecord sr) {
    //
    // hq mismatches in clipped alignment start (regardless of strand)
    // 
    boolean verbose = false;
    if (verbose) System.err.println("get_alignment_start_clipped_hq_mismatches():");  // debug
    int as = sr.getAlignmentStart();
    int asu = sr.getUnclippedStart();
    CigarOperator co;
    int results = 0;
    if (as != asu) {
      // clipping present at start of read
      Cigar c = sr.getCigar();
      boolean ok = false;
      int len=-1;
      for (CigarElement ce : c.getCigarElements()) {
	co = ce.getOperator();
	ok = co.equals(CigarOperator.SOFT_CLIP);
	len = ce.getLength();
	break;
      }
      if (ok) {
	if (verbose) {
	  System.err.println("lead soft clipping for " + SAMUtils.get_printable_read_name(sr));  // debug
	  System.err.println("len="+len);  // debug
	}

	int read_i = 0;
	int ref_i = asu - start_offset - 1;
	byte[] read = sr.getReadBases();
	byte[] quals = sr.getBaseQualities();

	for (int i = 0; i < len; i++, read_i++, ref_i++) {
	  if (ref_i < 0) continue;
	  if (ref_i >= reference_sequence.length) break;
	  // read aligned past end of reference sequence, stop
	  if (verbose) System.err.println("ref:" + (ref_i + 1) + "=" + (char) reference_sequence[ref_i] + " read:" + (char) read[read_i] + " qual:" + quals[read_i]);
	  if (Character.toUpperCase((char) reference_sequence[ref_i]) != read[read_i] &&
	      quals[read_i] >= config.MISMATCH_FILTER_MIN_HIGH_QUALITY) {
	    if (verbose) System.err.println("  HQ mismatch");  // debug
	    results++;
	  }
	}


      } else {
	System.err.println("ERROR getting clip mismatches for " + SAMUtils.get_printable_read_name(sr));  // debug
	// hard clipping?
      }
    }
    if (verbose) System.err.println("total="+results);  // debug
    return results;
  }



}
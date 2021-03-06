package Ace2;

import net.sf.samtools.*;
import java.io.*;
import java.util.*;
import java.net.*;

import static Ace2.Constants.ALIGNMENT_GAP_CHAR;

public class SAMConsensusBuilder {
  private int start_offset = 0;
  // offset to apply to all reads (i.e. when generating an assembly from a chromosome subregion)
  private byte[] reference_sequence;
  // unpadded (input to this class)
  private char[] consensus;
  // padded (generated by this class)
  //  private HashMap<Integer,Integer> gaps = new HashMap<Integer,Integer>();
  private HashMap<Integer,Integer> gaps = null;
  // 1-BASED, not 0-BASED

  private ArrayList<SAMRecord[]> all_sams = new ArrayList<SAMRecord[]>();
  private boolean need_digest = true;
  private static final boolean VERBOSE = false;
  private boolean REFERENCE_IS_PADDED = false;

  public SAMConsensusBuilder(byte[] reference_sequence) {
    this.reference_sequence = reference_sequence;
  }

  public SAMConsensusBuilder(byte[] reference_sequence, int start_offset) {
    this.reference_sequence = reference_sequence;
    this.start_offset = start_offset;
  }

  public void set_start_offset (int i) {
    start_offset = i;
  }

  public char[] get_consensus() {
    return consensus;
  }

  public byte[] get_reference_sequence() {
    // UNPADDED reference sequence
    return reference_sequence;
  }

  public void add_samrecords (SAMRecord[] sams) {
    all_sams.add(sams);
    need_digest = true;
  }
  
  public void digest_sams() throws IOException {
    int si=0;
    SAMRecord sr;
    CigarOperator co;

    int ai;
    // alignment index
    int i,len;

    HashSet<Integer> last_gap_positions = null;

    HashMap<Integer,Integer> local_cumulative_gaps;

    while (true) {
      gaps = new HashMap<Integer,Integer>();

      for (SAMRecord[] sams : all_sams) {
	for (si=0; si < sams.length; si++) {
	  sr = sams[si];

	  local_cumulative_gaps = new HashMap<Integer,Integer>();
	  // it's possible to have 2 insertions which resolve to the same site,
	  // separated by a zero-length CIGAR tag.  In this case the tracked 
	  // insertion sizes should be COMBINED.
	  //
	  // example: cigar_p.bat -restrict GS17754-FS3-L03-2:19542884
	  // GS17754-FS3-L03-2:19542884.F1
	  //                sequence: AGGGTCTTGATGCTGTGGTCTTCAGTGGTCTTCA
	  //                   CIGAR: M9I5N0I9M1N4M10
	  // 
	  // CIGAR:
	  //    M9  AGGGTCTTG
	  //    I5  ATGCT      <----- insertion #1
	  //    N0             <----- zero-sized intervening tag
	  //    I9  GTGGTCTTC  <----- insertion #2 (same site!)
	  //    M1  A
	  //    N4
	  //   M10  GTGGTCTTCA

	  //      ai = sr.getAlignmentStart() - start_offset;
	  ai = sr.getUnclippedStart() - start_offset;
	  // position in reference sequence;
	  // compensate for any start position

	  //      System.err.println("start ai="+ai);  // debug

	  Cigar c = sr.getCigar();
	  if (VERBOSE) System.err.println(SAMUtils.get_printable_read_name(sr) + " cigar="+SAMUtils.cigar_to_string(c) + " seq=" + sr.getReadString());

	  //
	  // FIX ME: this would be a good place to filter out reads that 
	  // don't meet a particular quality filter, e.g. too many high-quality
	  // mismatches with consensus.  Prevent poor-quality reads from
	  // creating consensus gaps, etc.
	  // 

	  for (CigarElement ce : c.getCigarElements()) {
	    co = ce.getOperator();
	    len = ce.getLength();
	    if (co.equals(CigarOperator.MATCH_OR_MISMATCH) ||
		co.equals(CigarOperator.SOFT_CLIP) ||
		co.equals(CigarOperator.EQ) ||
		// match to alignment
		co.equals(CigarOperator.X)
		// mismatch to alignment
		) {
	      // ordinary alignment, no padding involved
	      ai += len;
	    } else if (co.equals(CigarOperator.DELETION) || co.equals(CigarOperator.SKIPPED_REGION)) {
	      // deletion from the reference, or skipped region (mrna->genome mapping, etc)
	      // = pads in query sequence, so alignment position should be incremented
	      if (VERBOSE) System.err.println("del=" + len);  // debug
	      if (last_gap_positions != null) {
		for (i=0; i < len; i++) {
		  if (last_gap_positions.contains(ai)) {
		    if (VERBOSE) System.err.println("HEY TO THE NOW NOW");  // debug
		    i--;
		  }
		  if (VERBOSE) System.err.println("  " + (ai + start_offset));  // debug
		  ai++;
		}
	      } else {
		ai += len;
	      }
	    } else if (co.equals(CigarOperator.INSERTION)) {
	      // insertion to the reference
	      //	  System.err.println("HEY NOW: " + co + ce.getLength() + " ai="+ai + " " + sr.getReadName() + " start="+ (sr.getAlignmentStart() - start_offset) + " " + sr.getReadString());  // debug
	      Integer key = Integer.valueOf(ai);
	      Integer gap_size = Integer.valueOf(len);

	      Integer prev_local_size = local_cumulative_gaps.get(key);
	      if (prev_local_size != null) gap_size += prev_local_size;
	      Integer og = gaps.get(key);

	      if (VERBOSE) System.err.println("gap of " + gap_size + " at " + (ai + start_offset));
	      if (og == null || gap_size > og) gaps.put(key, gap_size);
	      // track largest gap at each position
	      
	      local_cumulative_gaps.put(key, gap_size);

	    } else if (co.equals(CigarOperator.HARD_CLIP)) {
	      if (VERBOSE) System.err.println("hard clip of " + len);  // debug
	      // hard-clipped bases are not present in the read array
	      ai += len;
	      // need to update reference position because we started
	      // at unclipped position
	      // NEEDS TESTING
	    } else if (co.equals(CigarOperator.PADDING)) {
	      // "silent" padding for (non-provided) padded reference sequence: ignore
	      //
	      //	      System.err.println("PADDING1");
	      //	      ai += len;
	    } else {
	      throw new IOException("ERROR: unhandled SAM operator " + co + " for " + SAMUtils.get_printable_read_name(sr));
	    }
	  }
	}
      } // for SAMRecords...


      //
      // finished building insertion gap sites for all datasets.
      // Now look for contentions and recurse if necessary.
      //
      boolean indel_contention = false;
      HashSet<Integer> gap_positions = new HashSet<Integer>();
      for (Integer gap_pos : gaps.keySet()) {
	len = gaps.get(gap_pos);
	for (i = gap_pos + (len - 1); i >= gap_pos; i--) {
	  gap_positions.add(i);
	}
      }

      for (SAMRecord[] sams : all_sams) {
	for (si=0; si < sams.length; si++) {
	  sr = sams[si];

	  ai = sr.getUnclippedStart() - start_offset;

	  Cigar c = sr.getCigar();

	  for (CigarElement ce : c.getCigarElements()) {
	    co = ce.getOperator();
	    len = ce.getLength();

	    if (co.equals(CigarOperator.MATCH_OR_MISMATCH) ||
		co.equals(CigarOperator.SOFT_CLIP) ||
		co.equals(CigarOperator.HARD_CLIP) ||
		co.equals(CigarOperator.EQ) ||
		co.equals(CigarOperator.X)
		) {
	      // ordinary alignment, no padding involved
	      ai += len;
	    } else if (co.equals(CigarOperator.INSERTION)) {
	      // not sure about this, surprisingly doesn't happen often
	      ai += len;
	    } else if (co.equals(CigarOperator.DELETION) || co.equals(CigarOperator.SKIPPED_REGION)) {
	      // deletion from the reference
	      // = pads in query query sequence, so alignment position should be incremented
	      for (i=0; i < len; i++, ai++) {
		if (gap_positions.contains(ai)) {
		  if (VERBOSE) System.err.println("CONTENTION at " + (ai + start_offset));  // debug
		  indel_contention = true;
		}
	      }
	    } else if (co.equals(CigarOperator.PADDING)) {
	      // "silent" padding for (non-provided) padded reference sequence: ignore
	      //	      System.err.println("PADDING2");  // debug
	      //	      ai += len;
	    } else {
	      throw new IOException("ERROR: unhandled SAM operator " + co + " for " + SAMUtils.get_printable_read_name(sr));
	    }

	  }
	}
      }

      if (indel_contention) {
	if (VERBOSE) System.err.println("indel contention:");  // debug
	ArrayList<Integer> gp = new ArrayList<Integer>();
	for (Integer pos : gap_positions) {
	  gp.add(pos + start_offset);
	}
	Collections.sort(gp);
	if (VERBOSE) System.err.println("gap_positions: " + gp);  // debug

	if (true) {
	  //	  System.err.println("DEBUG, NEVER recursing");  // debug
	  break;
	  // not sure this addresses the problem, fix to 
	  // SAMConsensusMapFactory may have addressed this, so abandon
	  // (see error2.bat)
	}

	if (last_gap_positions != null &&
	    gap_positions.equals(last_gap_positions)) {
	  System.err.println("same results, done!");  // debug
	  break;
	} else {
	  System.err.println("need to recurse");  // debug
	  last_gap_positions = gap_positions;
	}
      } else {
	// no indel contention, so no need to recurse
	break;
      }

    } // while true
  }

  public void build_consensus() throws IOException {

    //
    //  check to see if reference sequence is already padded.
    //  If so, no postprocessing needed.
    //
    REFERENCE_IS_PADDED = false;
    for (int ri=0; ri < reference_sequence.length; ri++) {
      if ((char) reference_sequence[ri] == ALIGNMENT_GAP_CHAR) {
	REFERENCE_IS_PADDED = true;
	break;
      }
    }

    if (REFERENCE_IS_PADDED) {
      System.err.println("padded reference sequence detected, not postprocessing");  // debug
      consensus = new char[reference_sequence.length];
      for (int ri=0; ri < reference_sequence.length; ri++) {
	consensus[ri] = (char) reference_sequence[ri];
      }
      return;
    }

    //
    //  build padded version of reference sequence:
    //
    if (need_digest) digest_sams();

    StringBuffer sb_cons = new StringBuffer();
    int ri;

    ArrayList<Integer> g2 = new ArrayList<Integer>(gaps.keySet());
    Collections.sort(g2);
    //    System.err.println("gaps:" + g2);  // debug

    Integer gap;
    int gi;

    if (reference_sequence == null) {
      System.err.println("ERROR: null reference sequence!  Check and/or re-download your reference sequence file to make sure it is not corrupt.");  // debug
      throw new IOException("null reference sequence");
    }

    for (ri=1; ri <= reference_sequence.length; ri++) {
      gap = gaps.get(Integer.valueOf(ri));
      if (gap != null) {
	// gap here
	//	System.err.println("hit gap at " + ri + " after=" + reference_sequence.substring(ri, ri + 20));
	for (gi=0; gi < gap; gi++) {
	  sb_cons.append(ALIGNMENT_GAP_CHAR);
	}
      }
      sb_cons.append((char) reference_sequence[ri - 1]);
    }

    consensus = sb_cons.toString().toCharArray();
  }

  public static void main (String[] argv) {
    try {
//       URL url = new URL("file://localhost/c:/me/work/java2/Ace2/egfr/egfr.fasta");
//       BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
//       FASTASequenceReader fa = new FASTASequenceReader(br, false);
//       StringBuffer sb = fa.get_sequences().get("chr7");
//       System.err.println("len="+sb.length());  // debug

//       SAMConsensusBuilder scb = new SAMConsensusBuilder(sb.toString().toCharArray(), 55054118);

//       SAMRecord[] sams = SAMUtils.load_sams(new File("egfr_1.bam"));
//       scb.add_samrecords(sams);

//       sams = SAMUtils.load_sams(new File("egfr_2.bam"));
//       scb.add_samrecords(sams);
//       scb.build_consensus();

//       char[] cons = scb.get_consensus();
//       PadMap pm = new PadMap(cons);

//       //      int pi = 4589 - 1;
//       //      int pi=127482;

//       for (int si=0; si < sams.length; si++) {
// 	SAMRecord sr = sams[si];
//       }

//       int upi=127482;

//       int pi = pm.get_unpadded_to_padded(upi);
//       pi--;
//       // convert to 0-based
//       System.err.println("cons align:" + new String(cons, pi, 100));  // debug
      
//       System.err.println("consensus:");  // debug
//       System.err.println(cons);  // debug

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public boolean is_reference_prepadded() {
    return REFERENCE_IS_PADDED;
  }

}

package Ace2;
// extract data packed in custom SAM tags representing chromatogram peak positions
// (for use w/invoking trace viewer)

import net.sf.samtools.*;

import static Ace2.Constants.ALIGNMENT_GAP_CHAR;
import static Ace2.Constants.ALIGNMENT_DELETION_CHAR;
import static Ace2.Constants.ALIGNMENT_SKIPPED_CHAR_F;
import static Ace2.Constants.ALIGNMENT_SKIPPED_CHAR_R;
import static Ace2.Constants.ALIGNMENT_HARD_CLIPPED_CHAR;

public class SAMTracePeakPositions {
  static final String TAG_TRACE_PEAK_POSITIONS = "YP";
  static final String TAG_TRACE_PEAK_POSITIONS_NORMALIZATION_FACTOR = "YF";
  // see ~edmonson/bin/sanger2fastq
  private int[] peak_positions;

  public SAMTracePeakPositions (SAMRecord sr) {
    setup(sr);
  }

  private void setup (SAMRecord sr) {
    int normalization_factor = 0;
    Object nf = sr.getAttribute(TAG_TRACE_PEAK_POSITIONS_NORMALIZATION_FACTOR);
    if (nf != null) {
      System.err.println("HEY NOW: normalization factor found...");  // debug
      if (nf instanceof Integer) {
	normalization_factor = (Integer) nf;
	System.err.println("using normalization factor of " + nf);  // debug
      } else {
	System.err.println("ERROR parsing normalization factor, obj="+nf);  // debug
      }
    }

    Object p = sr.getAttribute(TAG_TRACE_PEAK_POSITIONS);
    if (p == null) {
      System.err.println("ERROR: no trace peak positions tag " + TAG_TRACE_PEAK_POSITIONS);  // debug
    } else {
      byte[] ba = ((String) p).getBytes();
      int len = ba.length;
      
      peak_positions = new int[len];
      int i;
      int last_value = 0;

      for (i = 0; i < len; i++) {
	last_value = peak_positions[i] = last_value + (ba[i] - 33 - normalization_factor);
	//	System.err.println("raw unpacked at " + i + " = " + peak_positions[i]);  // debug
      }

      if (sr.getReadNegativeStrandFlag()) {
	//	System.err.println("reversing!");  // debug
	int[] rev = new int[len];
	int j = len - 1;
	for (i = 0; i < len; i++, j--) {
	  rev[j] = peak_positions[i];
	}
	peak_positions = rev;

// 	for(i=0; i < len; i++) {
// 	  System.err.println("final unpacked at " + i + " = " + peak_positions[i]);  // debug
// 	}


      }

    }

    // FIX ME: deal with - strand
  }

  public int get_peak_position_for_consensus (SAMConsensusMapping scm, int cpos) {
    int result = 0;

    int pi = scm.get_padded_index(cpos);
    byte[] padded = scm.get_sequence_buffer();
    int i;
    int unpadded_i = 0;
    for (i=0; i < pi; i++) {
      //      System.err.println("pi="+i + " base="+(char) padded[i] + " upi="+unpadded_i);  // debug

      if (padded[i] != ALIGNMENT_GAP_CHAR &&
	  padded[i] != ALIGNMENT_DELETION_CHAR &&
	  padded[i] != ALIGNMENT_SKIPPED_CHAR_F &&
	  padded[i] != ALIGNMENT_SKIPPED_CHAR_R) {
	unpadded_i++;
      }
    }
    //    System.err.println("HEY NOW: cpos="+ cpos + " pi:" + pi + " upi:"+unpadded_i+ " pos:"+peak_positions[unpadded_i]);  // debug

    return peak_positions[unpadded_i];
  }

}
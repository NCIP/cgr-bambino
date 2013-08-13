package Ace2;

import net.sf.samtools.*;

import static Ace2.Constants.ALIGNMENT_GAP_CHAR;
import static Ace2.Constants.ALIGNMENT_DELETION_CHAR;
import static Ace2.Constants.ALIGNMENT_SKIPPED_CHAR_F;
import static Ace2.Constants.ALIGNMENT_SKIPPED_CHAR_R;
import static Ace2.Constants.ALIGNMENT_HARD_CLIPPED_CHAR;

public class SAMAlternatePeakAmplitudeRatios {
  static final String TAG_ALTERNATE_PEAK_AMPLITUDE_RATIOS = "YR";
  static final int SAM_CHARACTER_DYNAMIC_RANGE = 92;
  // possible range of values packable into a SAM character

  private byte[] ratios;

  public SAMAlternatePeakAmplitudeRatios (SAMRecord sr) {
    setup(sr);
  }

  private void setup (SAMRecord sr) {
    Object p = sr.getAttribute(TAG_ALTERNATE_PEAK_AMPLITUDE_RATIOS);
    if (p == null) {
      System.err.println("ERROR: no trace alternate peak amplitude ratios tag " + TAG_ALTERNATE_PEAK_AMPLITUDE_RATIOS);  // debug
    } else {
      ratios = ((String) p).getBytes();
      int len = ratios.length;

      float cooked;
      for (int i = 0; i < len; i++) {
	cooked = Math.round((((float) (ratios[i] - 33)) / SAM_CHARACTER_DYNAMIC_RANGE) * 100);
	//	cooked = (byte) Math.round(((float) ratios[i] - 33) / SAM_CHARACTER_DYNAMIC_RANGE);
	//	System.err.println("raw=" + ratios[i] + " r2=" + (ratios[i] - 33) + " cooked="+f + " round=" + Math.round(f));  // debug
	//	System.err.println("raw=" + ratios[i] + " r2=" + (ratios[i] - 33) + " cooked="+cooked);
	ratios[i] = (byte) cooked;
      }
    }    
  }

  public byte get_alternate_peak_amplitude_ratio_for_consensus (SAMConsensusMapping scm, int cpos) {
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

    return ratios[unpadded_i];
  }


}
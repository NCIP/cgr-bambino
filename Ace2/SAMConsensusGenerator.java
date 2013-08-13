package Ace2;

import java.util.*;
import net.sf.samtools.*;

public class SAMConsensusGenerator {
  private AceViewerConfig config;

  public SAMConsensusGenerator (AceViewerConfig config) {
    this.config = config;
  }

  public byte[] generate_consensus () {
    SAMRecord sr;
    int ref_i, read_i;

    SAMRegion region = config.sams.get(0).get_region();

    int start_base = config.ruler_start;

    int region_len = (region.range.end - region.range.start) + 1;
    int[] count_a = new int[region_len];
    int[] count_c = new int[region_len];
    int[] count_g = new int[region_len];
    int[] count_t = new int[region_len];
    Arrays.fill(count_a, 0);
    Arrays.fill(count_c, 0);
    Arrays.fill(count_g, 0);
    Arrays.fill(count_t, 0);
    
    Base b;
    int i, end;

    byte[] result = new byte[region_len];
    Arrays.fill(result, (byte) 'N');

    //
    // scan .bam files and count nucleotides at each position
    //
    for (SAMResource resource : config.sams) {
      SAMRecord[] sams = resource.get_sams();
      if (sams == null) {
	System.err.println("ERROR: no SAM records retrieved!");  // debug
	continue;
      }

      System.err.println("SAM count:" + sams.length);  // debug

      int[] bucket;
      
      for (int si=0; si < sams.length; si++) {
	sr = sams[si];
	byte[] bases = sr.getReadBases();
	for (AlignmentBlock ab : sr.getAlignmentBlocks()) {
	  read_i = ab.getReadStart() - 1;
	  ref_i = ab.getReferenceStart() - start_base - 1;

	  for (i = read_i, end = read_i + ab.getLength(); i < end; i++, ref_i++) {
	    if (ref_i >= region_len) {
	      break;
	    } else if (ref_i >= 0) {
	      b = Base.valueOf((char) bases[i]);
	      bucket = null;
	      if (b.equals(Base.BASE_A)) {
		count_a[ref_i]++;
	      } else if (b.equals(Base.BASE_C)) {
		count_c[ref_i]++;
	      } else if (b.equals(Base.BASE_G)) {
		count_g[ref_i]++;
	      } else if (b.equals(Base.BASE_T)) {
		count_t[ref_i]++;
	      }
	    }
	  }
	}
      }
    }

    for (i=0; i < region_len; i++) {
      byte best_nt = 'N';
      int best_count = 0;
      int count;

      count = count_a[i];
      if (count > best_count) { best_count = count; best_nt = 'a'; }

      count = count_c[i];
      if (count > best_count) { best_count = count; best_nt = 'c'; }

      count = count_g[i];
      if (count > best_count) { best_count = count; best_nt = 'g'; }

      count = count_t[i];
      if (count > best_count) { best_count = count; best_nt = 't'; }
      
      result[i] = best_nt;
      //     System.err.println("cons base="+best_nt);  // debug

    }

    return result;
  }

}
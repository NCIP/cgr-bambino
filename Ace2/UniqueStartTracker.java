package Ace2;

import java.util.*;
import net.sf.samtools.*;

public class UniqueStartTracker {
  HashSet<Integer> starts, starts_f, starts_r;
  SNPConfig config;

  public UniqueStartTracker (SNPConfig config, ArrayList list) {
    this.config = config;

    starts = new HashSet<Integer>();
    starts_f = new HashSet<Integer>();
    starts_r = new HashSet<Integer>();

    int start;
    SAMRecord sr = null;
    for (Object o : list) {
      if (o instanceof SNPTrackInfo) {
	sr = ((SNPTrackInfo) o).sr;
	//	int start = sti.sr.getAlignmentStart();
      } else if (o instanceof IndelInfo) {
	sr = ((IndelInfo) o).sr;
      } else {
	System.err.println("ERROR: unhandled object " + o);  // debug
	System.exit(1);
      }

      start = sr.getUnclippedStart();
      // 3/2010: example file which implements hard clipping to
      // make all reads appear to start at the same base
      // position.  Not sure if this was done for targeted
      // resequencing/capture or what.  In any case this
      // trimming triggers the filter and so rejects otherwise
      // valid-looking SNPs.  So, always use unclipped start position
      // for purposes of this tracking.
      starts.add(start);

      if (sr.getReadNegativeStrandFlag()) {
	starts_r.add(start);
      } else {
	starts_f.add(start);
      }

    }
  }

  public int get_unique_read_start_positions() {
    return starts.size();
  }

  public int get_unique_read_start_positions_fwd() {
    return starts_f.size();
  }

  public int get_unique_read_start_positions_rev() {
    return starts_r.size();
  }

  public boolean are_counts_ok() {
    boolean ok = true;

    if (starts.size() < config.MIN_UNIQUE_START_POSITIONS_FOR_ALT_ALLELE) ok = false;
    // TO DO: optional additional separate counts for + and - strand

    return ok;
  }

}

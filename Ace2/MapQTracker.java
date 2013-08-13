package Ace2;

import java.util.*;
import net.sf.samtools.*;

public class MapQTracker {
  private SNPConfig config;

  private int average_mapping_quality;

  public MapQTracker (ArrayList list) {
    long total = 0;
    int count = 0;
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
      total += sr.getMappingQuality();
      count++;
    }
    average_mapping_quality = (int) (total / count);
  }
  
  public int get_average_mapping_quality() {
    return average_mapping_quality;
  }
  
}


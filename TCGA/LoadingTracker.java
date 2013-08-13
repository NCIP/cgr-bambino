package TCGA;

import java.util.*;

public class LoadingTracker {

  HashMap<GenomicMeasurement,Integer> col_count;

  public LoadingTracker () {
    col_count = new HashMap<GenomicMeasurement,Integer>();
  }
  
  public void track (GenomicMeasurement gm) {
    if (!col_count.containsKey(gm)) {
      col_count.put(gm, gm.get_headers().length);
      // record header count only once, at first opportunity; 
      // may be reduced if prefiltering
    }
  }

  public int get_file_count() {
    return col_count.size();
  }

  public int get_cell_count() {
    int cell_count = 0;
    for (GenomicMeasurement gm : col_count.keySet()) {
      int cells = gm.get_lines_read() * col_count.get(gm);
      //      System.err.println("count " + gm + " " + cells);  // debug
      cell_count += cells;
    }
    return cell_count;
  }
  
}

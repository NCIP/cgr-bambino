package TCGA;

import java.awt.Rectangle;
import java.util.ArrayList;

public class GenomicBin {
  String bin_name;
  int start,end;
  float center;

  public GenomicBin (String bin_name, int start, int end) {
    this.bin_name = bin_name;
    set_start_end(start, end);
  }

  public void set_start_end (int start, int end) {
    this.start = start;
    this.end = end;
    int len = (end - start) + 1;
    center = start + (((float) len) / 2);
  }

  public Rectangle get_rectangle() {
    return new Rectangle(
			 (start - 1),
			 0,
			 (end - start) + 1,
			 1
			 );
  }

  public GenomicBin map (ArrayList<Integer> bins, int new_start) {
    // return a new GenomicBin, collapsed to contain only bins specified in given list
    //    System.err.println("map(): " + bins);  // debug
    int si = start - 1;
    int ei = end - 1;
    int hits = 0;
    for (Integer i : bins) {
      if (i >= si && i <= ei) {
	hits++;
      }
    }
    GenomicBin result = null;
    if (hits > 0) {
      result = new GenomicBin(bin_name, new_start, (new_start + hits) - 1);
      //      System.err.println("new bin: " + bin_name + " " + result.start + " " + result.end);  // debug
    }
    return result;
  }

  public void change_start (int new_start) {
    set_start_end(new_start, new_start + (end - start));
  }
    

}

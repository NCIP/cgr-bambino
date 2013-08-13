package TCGA;
//
//  map a GenomicLocation to a GenomicBin bin space
//

import java.util.*;

public class GenomicBinRange {
  private GenomicBin bin;
  private GenomicLocation gl;
  private GenomeScaler gs;

  HashSet<Integer> bins_hit_set;
  ArrayList<Integer> bins_hit_list;

  private int first_bin, last_bin;

  public GenomicBinRange (GenomicBin bin, GenomicLocation gl, GenomeScaler gs) {
    this.bin = bin;
    this.gl = gl;
    this.gs = gs;
    setup();
  }

  private void setup() {
    int chr_len = gs.get_chromosome_length_bp(gl.chromosome);
    int bin_count = (bin.end - bin.start) + 1;
    int nt_per_bin = chr_len / bin_count;
    first_bin = bin.start + (gl.start / nt_per_bin);
    last_bin = bin.start + (gl.end / nt_per_bin) + 1;
    
    bins_hit_set = new HashSet<Integer>();
    bins_hit_list = new ArrayList<Integer>();

    for (int i = first_bin; i <= last_bin; i++) {
      bins_hit_set.add(i);
      bins_hit_list.add(i);
    }

    //    System.err.println("len="+chr_len + " bin.start=" + bin.start + " bin.end="+bin.end);  // debug
    //    System.err.println("count:"+bin_count + " per_bin:"+nt_per_bin + " first="+first_bin+ " last="+last_bin);  // debug
  }

  public boolean contains (int bin_num) {
    return bins_hit_set.contains(bin_num);
  }

  public int get_distance(int bin_num) {
    // how many bins away is given bin from the range?
    int distance = -1;
    if (contains(bin_num)) {
      distance = 0;
    } else if (bin_num < first_bin) {
      distance = first_bin - bin_num;
    } else {
      distance = bin_num - last_bin;
    }
    return distance;
  }

}

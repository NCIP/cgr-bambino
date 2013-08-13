package Ace2;

import java.util.*;

public class GenomeBuildInfo {
  String build_name;
  HashMap<Chromosome,Integer> chr_lengths;

  public GenomeBuildInfo(String build_name) {
    this.build_name = build_name;
    chr_lengths = new HashMap<Chromosome,Integer>();
  }

  public void set (String cname, String len_string) {
    // import cut/paste data from chromInfo table
    Chromosome chrom = Chromosome.valueOfString(cname);
    if (chrom == null) {
      System.err.println("ERROR for " +cname);  // debug
    } else {
      chr_lengths.put(chrom, Integer.parseInt(len_string));
    }
  }

  public String get_name() {
    return build_name;
  }

  public int get_length_for (String s) {
    Chromosome c = Chromosome.valueOfString(s);
    return c == null ? -1 : chr_lengths.get(c);
  }

}

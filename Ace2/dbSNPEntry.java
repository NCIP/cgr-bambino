package Ace2;
// dbSNP blob database record

import java.util.*;

public class dbSNPEntry {
  byte observed;
  int rs_num, chrom_start;

  static final byte MASK_A = 0x01;
  static final byte MASK_C = 0x02;
  static final byte MASK_G = 0x04;
  static final byte MASK_T = 0x08;

  public dbSNPEntry (int rs_num, int chrom_start, byte observed) {
    this.rs_num = rs_num;
    this.chrom_start = chrom_start;
    this.observed = observed;
  }

  public boolean matches (int position, Base b1, Base b2) {
//     if (position == chrom_start) {
//       System.err.println("hit for " + position);  // debug
//       System.err.println("encoded bases: " + observed);  // debug

//       System.err.println("b1: " + b1 + " match:" + matches_base(b1));  // debug
//       System.err.println("b2: " + b2 + " match:" + matches_base(b2));  // debug
//     }

    return position == chrom_start && matches_base(b1) && matches_base(b2);
  }

  private boolean matches_base (Base b) {
    boolean match = false;
    switch (b) {
    case BASE_A: match = (observed & MASK_A) > 0; break;
    case BASE_C: match = (observed & MASK_C) > 0; break;
    case BASE_G: match = (observed & MASK_G) > 0; break;
    case BASE_T: match = (observed & MASK_T) > 0; break;
    }
    return match;
  }

  public String get_name() {
    return "rs" + rs_num;
  }

  public String describe() {
    String id = get_name();
    ArrayList<String> bases = new ArrayList<String>();
    if ((observed & MASK_A) > 0) bases.add("A");
    if ((observed & MASK_C) > 0) bases.add("C");
    if ((observed & MASK_G) > 0) bases.add("G");
    if ((observed & MASK_T) > 0) bases.add("T");

    
    return id + " " + chrom_start + " " + Funk.Str.join(",", bases);
  }

}

package Ace2;

import java.util.*;

public class AlleleSkewInfo {
  ArrayList<Base> bases;
  TumorNormal tumor_normal;

  public AlleleSkewInfo(TumorNormal tumor_normal) {
    bases = new ArrayList<Base>();
    this.tumor_normal = tumor_normal;
  }

  public void add_base (Base b) {
    bases.add(b);
  }

  public ArrayList<Base> get_bases() {
    return bases;
  }

  public String toString () {
    return 
      //      tumor_normal + " " + 
      (bases.size() == 1 ? "homozygous" : "heterozyous") + " " +
      Funk.Str.join("/", bases);
  }

  public boolean equals(Object o) {
    AlleleSkewInfo other = (AlleleSkewInfo) o;
    return bases.equals(other.bases);
  }


}

package Ace2;

import java.util.*;

// a list of SNPs; a subclass of Vector that remembers its index
// and provides methods for moving back and forth and reporting
// SNP data for the current index.

public class SNPList extends Vector {
  static final int BEST = 1;
  static final int NEXT = 2;
  static final int PREV = 3;
  
  private int index = 0;
  private boolean is_temp = false;
  // a placeholder list, i.e. to temporarily represent
  // a navigation site, etc.

  public void set_temporary (boolean temp) {
    is_temp = temp;
  }

  public boolean is_temporary() {
    return is_temp;
  }

  public void move (int direction) {
    if (direction == BEST) {
      // move to best
      index = 0;
    } else if (direction == NEXT) {
      index++;
    } else if (direction == PREV) {
      index--;
    } else {
      System.out.println("snplist: bogus index!");
    }
    bounds_check();
    //    System.out.println("** current loc:" + current_position() + " score:" + current_score());  // debug
  }

  public void set_index(int i) {
    index = i;
    bounds_check();
  }

  public SNP get_snp() {
    return ((SNP) elementAt(index));
  }

  public int position_of (int i) {
    return ((SNP) elementAt(i)).position;
  }

  public double score_of (int i) {
    return ((SNP) elementAt(i)).score;
  }

  public boolean is_current (int i) {
    // is the given index the current one in the list?
    return (i == index);
  }

  public int current_position () {
    return ((SNP) elementAt(index)).position;
  }

  public double current_score () {
    return ((SNP) elementAt(index)).score;
  }

  public int current_location_id () {
    return ((SNP) elementAt(index)).location_id;
  }

  public int index () {
    return index;
  }
  
  private void bounds_check () {
    int max = size() - 1;
    if (index < 0) index = 0;
    if (index > max) index = max;
  }

  public void sort () {
    // sort the list by snp score.
    // FIX ME yikes!
    int max = size() - 1;
    int i;
    SNP s1,s2,temp;
    boolean done = false;
    while (done == false) {
      done = true;
      for (i=0; i < max; i++) {
	s1 = (SNP) elementAt(i);
	s2 = (SNP) elementAt(i + 1);
	if (s1.score < s2.score) {
	  done = false;
	  setElementAt(s2, i);
	  setElementAt(s1, i + 1);
	}
      }
    }
  }

  public Hashtable position_hash () {
    // hashtable seeded with SNP offsets.
    Hashtable offsets = new Hashtable();
    Enumeration e = elements();
    Integer dummy = new Integer(1);
    while (e.hasMoreElements()) {
      SNP s = (SNP) e.nextElement();
      offsets.put(Integer.toString(s.position), dummy);
    }
    return offsets;
  }
}

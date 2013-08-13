package Funk;

import java.util.*;

public class WarningTracker {
  // track warnings and count by instance
  TreeMap<String,Integer> warnings;

  public WarningTracker() {
    warnings = new TreeMap<String,Integer>();
  }

  public void add (String msg) {
    Integer count = warnings.get(msg);
    if (count == null) count = Integer.valueOf(0);
    warnings.put(msg, count + 1);
  }

  public void report() {
    for (String msg : warnings.keySet()) {
      int count = warnings.get(msg);
      System.err.print(msg);
      if (count > 1) System.err.print(" (x" + count + ")");  // debug
      System.err.println("");  // debug

    }
  }
  

  public static void main (String[] argv) {
    WarningTracker wt = new WarningTracker();
    wt.add("warning1");
    wt.add("warning1");
    wt.add("warning1");
    wt.add("warning2");
    wt.report();
  }
}

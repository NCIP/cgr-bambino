package Ace2;

import java.util.*;

public class Counter {
  private HashMap<String,Long> counts;

  public Counter() {
    counts = new HashMap<String,Long>();
  }

  public long increment (String label) {
    Long v = counts.get(label);
    if (v == null) v = Long.valueOf(0);
    v++;
    counts.put(label, v);
    return v;
  }

  public long get_count(String label) {
    return counts.get(label);
  }

  public long get_total () {
    long result = 0;
    for (String label : counts.keySet()) {
      result += counts.get(label);
    }
    return result;
  }

  public static void main (String[] argv) {
    Counter c = new Counter();
    c.increment("duplicate");
    c.increment("else");
    c.increment("duplicate");
    System.err.println("total="+c.get_total());  // debug
    System.err.println("summary="+c.get_summary());  // debug
  }

  public HashMap<String,Long> get_counts() {
    return counts;
  }

  public String get_summary() {
    ArrayList<String> labels = new ArrayList(counts.keySet());
    Collections.sort(labels);
    ArrayList<String> things = new ArrayList<String>();
    for (String l : labels) {
      things.add(l + "=" + counts.get(l));
    }
    return Funk.Str.join(" ", things);
  }

}

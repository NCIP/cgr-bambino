package Ace2;

import java.util.*;

public class LongCounter {
  private int MAX_VALUE = 0;
  private long[] tracker;
  
  public LongCounter (int mv) {
    MAX_VALUE = mv;
    tracker = new long[MAX_VALUE + 1];
    Arrays.fill(tracker, 0);
  }

  public long[] get_array() {
    // faster
    return tracker;
  }

  public void increment (int v) {
    if (v < 0 || v > MAX_VALUE) {
      System.err.println("ERROR: can't track out of bounds!!");  // debug
    } else {
      tracker[v]++;
    }
  }

  private long get_total_observations() {
    int total = 0;
    for (int i = 0; i < tracker.length; i++) {
      total += tracker[i];
    }
    return total;
  }

  private long get_total_values() {
    int total = 0;
    for (int i = 0; i < tracker.length; i++) {
      total += tracker[i] * i;
    }
    return total;
  }

  public double get_mean () {
    return (double) get_total_values() / get_total_observations();
  }

  public int get_median() {
    return get_fraction(0.5f);
  }

  public int get_fraction (double position) {
    double halt_at_count = get_total_observations() * position;
    int result = -1;
    double count = 0;
    //    System.err.println("stop count="+halt_at_count);  // debug
    for (int i=0; i < tracker.length; i++) {
      count += tracker[i];
      if (count >= halt_at_count) {
	result = i;
	break;
      }
    }
    return result;
  }


  public static void main (String[] argv) {
    LongCounter lc = new LongCounter(100);
    lc.increment(1);
    lc.increment(2);
    lc.increment(3);
    lc.increment(8);
    lc.increment(12);
    System.err.println("mean="+ lc.get_mean());  // debug
    System.err.println("median="+ lc.get_median());  // debug

    for (double percentile = 0; percentile <= 1; percentile += 0.05d) {
      System.err.println(percentile + " percentile="+ lc.get_fraction(percentile));
    }

  }

}

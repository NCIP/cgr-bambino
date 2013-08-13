package Funk;

import java.util.*;

public class StreamingDuplicateFilter {
  static int DEFAULT_QUEUE_MAX = 100000;
  static double TRIM_FRACTION = 0.25;
  boolean VERBOSE = false;

  int queue_max;
  ArrayList<String> queue;
  HashSet<String> queue_hash;

  public StreamingDuplicateFilter() {
    set_queue_max(DEFAULT_QUEUE_MAX);
  }

  public StreamingDuplicateFilter(int max) {
    set_queue_max(max);
  }

  public void set_queue_max(int queue_max) {
    this.queue_max = queue_max;
    queue = new ArrayList<String>();
    queue_hash = new HashSet<String>();
  }

  public boolean add (String item) {
    // returns true if item added is a duplicate in current queue
    boolean result = is_duplicate(item);
    queue.add(item);
    queue_hash.add(item);
    trim_check();
    return result;
  }

  public boolean is_duplicate (String item) {
    return queue_hash.contains(item);
  }

  private void trim_check() {
    if (queue.size() > queue_max) {
      if (VERBOSE) {
	System.err.println("trim needed");  // debug
	System.err.println("before:" + queue);  // debug
      }

      int trim_index = (int) (queue.size() * TRIM_FRACTION);
      for (int i = 0; i < trim_index; i++) {
	queue.remove(0);
	// don't understand why removeRange() is protected   :/
      }
      if (VERBOSE) System.err.println("after:" + queue);  // debug

      queue_hash = new HashSet<String>(queue);
      if (VERBOSE) System.err.println("hash="+queue_hash);  // debug

    }
  }

  public static void main (String[] argv) {
    StreamingDuplicateFilter sdf = new StreamingDuplicateFilter();
    sdf.set_queue_max(10);

    String[] values = {
      "a",
      "b",
      "c",
      "d",
      "e",
      "f",
      "a",
      "z"
    };

    for (int i = 0; i < values.length; i++) {
      System.err.println(values[i] + " " + sdf.add(values[i]));
    }
    

  }
  
}

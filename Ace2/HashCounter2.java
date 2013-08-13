package Ace2;

import java.util.*;

public class HashCounter2<K> {
  // generics version

  HashMap<K,Integer> counts;

  public HashCounter2 () {
    counts = new HashMap<K,Integer>();
  }

  public void add (K k) {
    Integer count = counts.get(k);
    if (count == null) count = Integer.valueOf(0);
    counts.put(k, count + 1);
  }

  public int get_total() {
    int total = 0;
    for(Integer count : counts.values()) {
      total += count;
    }
    return total;
  }

  public HashMap<K,Integer> get_counts() {
    return counts;
  }

}
package Ace2;
import java.util.*;

public class StringCounter {
  HashMap<String,Integer> counts = new HashMap<String,Integer>();

  public void increment (String s) {
    Integer count = counts.get(s);
    if (count == null) count = Integer.valueOf(0);
    counts.put(s, count + 1);
  }

  public int get_count_for (String s) {
    Integer count = counts.get(s);
    if (count == null) count = Integer.valueOf(0);
    return count;
  }

  
}
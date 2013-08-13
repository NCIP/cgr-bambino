package Ace2;

import java.util.*;

public class IntBucketIndex {
  private HashMap<Integer,ArrayList<Object>> map = new HashMap<Integer,ArrayList<Object>>();
  private int bucket_size;

  public IntBucketIndex (int bucket_size) {
    this.bucket_size = bucket_size;
  }

  public void add (int index, Object thing) {
    Integer i = Integer.valueOf(index / bucket_size);
    ArrayList<Object> sublist = map.get(i);
    if (sublist == null) map.put(i, sublist = new ArrayList<Object>());
    sublist.add(thing);
  }

  public void add_range (int index1, int index2, Object thing) {
    //
    // index the object for all ranges from index1 to index2
    //
    HashSet<Integer> keys = new HashSet<Integer>();
    for (int i = index1; i <= index2; i++) {
      //      keys.add(Integer.valueOf(i / bucket_size));
      keys.add(i / bucket_size);
      // autobox
    }

    for (Integer key : keys) {
      ArrayList<Object> sublist = map.get(key);
      if (sublist == null) map.put(key, sublist = new ArrayList<Object>());
      Integer i2 = Integer.valueOf(index2 / bucket_size);
      sublist.add(thing);
    }
  }

  public ArrayList<Object> find (int wanted) {
    ArrayList<Object> results = new ArrayList<Object>();
    ArrayList<Object> hits = map.get(Integer.valueOf(wanted / bucket_size));
    if (hits != null) results.addAll(hits);
    return results;
  }

  public static void main (String[] argv) {
    IntBucketIndex idx = new IntBucketIndex(1000);
    idx.add(1, "object at 1");
    idx.add(1, "another object at 1");
    idx.add(1, "yet another object at 1");

    for (int i = 5; i < 10; i++) {
      idx.add(i, "some obj at " + i);
    }

    for (int i = 5000; i < 5020; i++) {
      idx.add(i, "some obj at " + i);
    }

    idx.add(10, "object at 10");
    idx.add(13, "object at 13");

    ArrayList<Object> results = idx.find(5000);

    System.err.println("results:");  // debug
    for (Object o : results) {
      System.err.println((String) o);
    }
  }

}
